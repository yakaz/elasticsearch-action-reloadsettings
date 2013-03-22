package org.elasticsearch.action.reloadsettings;

import org.elasticsearch.action.support.nodes.NodesOperationResponse;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.settings.DynamicSettings;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ReloadSettingsResponse extends NodesOperationResponse<ReloadSettings> implements ToXContent {

    private static final String INCONSISTENCY = new String();

    private final DynamicSettings dynamicSettings;
    private ReloadSettings.Cluster clusterSettings;

    public ReloadSettingsResponse() {
        dynamicSettings = null;
    }

    public ReloadSettingsResponse(ClusterName clusterName, ReloadSettings.Cluster clusterSettings, ReloadSettings[] nodeSettings, DynamicSettings dynamicSettings) {
        super(clusterName, nodeSettings);
        this.clusterSettings = clusterSettings;
        this.dynamicSettings = dynamicSettings;
    }

    public ReloadSettings.Cluster getClusterSettings() {
        return clusterSettings;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        Map<String, String> effectiveSettingsConsistency = new HashMap<String, String>();
        Map<String, String> initialSettingsConsistency = new HashMap<String, String>();
        Map<String, String> desiredSettingsConsistency = new HashMap<String, String>();
        Map<String, Settings> effectiveSettingsPerNode = new HashMap<String, Settings>();
        Map<String, Settings> initialSettingsPerNode = new HashMap<String, Settings>();
        Map<String, Settings> desiredSettingsPerNode = new HashMap<String, Settings>();

        Map<String, String> mapParams = new HashMap<String, String>();
        params = new MapParams(mapParams);
        mapParams.put("wrap-object", "false");

        builder.startObject("settings");

        builder.startObject("cluster");
        if (clusterSettings != null) {
            mapParams.put("cluster", "true");
            clusterSettings.toXContent(builder, params);
        }
        builder.endObject();

        boolean first = true;
        builder.startObject("nodes");
        mapParams.put("cluster", "false");
        for (Map.Entry<String, ReloadSettings> entry : getNodesMap().entrySet()) {
            String nodeId = entry.getKey();
            ReloadSettings reloadSettings = entry.getValue();

            Settings effective = effectiveSettings(reloadSettings.getInitialSettings());
            Settings desired = desiredSettings(reloadSettings.getFileSettings(), reloadSettings.getEnvironmentSettings());
            effectiveSettingsPerNode.put(nodeId, effective);
            initialSettingsPerNode.put(nodeId, reloadSettings.getInitialSettings());
            desiredSettingsPerNode.put(nodeId, desired);
            noteInconsistencies(effectiveSettingsConsistency, effective, first);
            noteInconsistencies(initialSettingsConsistency, reloadSettings.getInitialSettings(), first);
            noteInconsistencies(desiredSettingsConsistency, desired, first);

            builder.startObject(nodeId);

            builder.field("effective", effective.getAsMap());

            reloadSettings.toXContent(builder, params);

            builder.field("desired", desired.getAsMap());

            builder.startObject("inconsistencies");
            for (String key : settingsDifference(effective, desired)) {
                builder.startObject(key);
                builder.field("effective", effective.get(key));
                builder.field("desired", desired.get(key));
                builder.field("_updatable", dynamicSettings.hasDynamicSetting(key));
                builder.endObject();
            }
            builder.endObject();

            builder.endObject();
            first = false;
        }
        builder.endObject();

        builder.startObject("consistencies");
        builder.field("effective", extractConsistencies(effectiveSettingsConsistency, true));
        builder.field("initial", extractConsistencies(initialSettingsConsistency, true));
        builder.field("desired", extractConsistencies(desiredSettingsConsistency, true));
        builder.endObject();

        builder.startObject("inconsistencies");
        builder.startObject("effective");
        buildInconsistency(builder, effectiveSettingsConsistency, effectiveSettingsPerNode);
        builder.endObject();
        builder.startObject("initial");
        buildInconsistency(builder, initialSettingsConsistency, initialSettingsPerNode);
        builder.endObject();
        builder.startObject("desired");
        buildInconsistency(builder, desiredSettingsConsistency, desiredSettingsPerNode);
        builder.endObject();
        builder.endObject();

        builder.endObject();

        return builder;
    }

    private static void noteInconsistencies(Map<String, String> referent, Settings version, boolean isFirst) {
        for (Map.Entry<String, String> setting : version.getAsMap().entrySet()) {
            String key = setting.getKey();
            String value = setting.getValue();
            if (referent.containsKey(key)) {
                if (value == null && referent.get(key) != null
                        || value != null && !value.equals(referent.get(key))) {
                    referent.put(key, INCONSISTENCY);
                }
            } else if (isFirst) {
                referent.put(key, value);
            } else {
                referent.put(key, INCONSISTENCY);
            }
        }
    }

    private static Map<String, String> extractConsistencies(Map<String, String> referent, boolean returnConsistencies) {
        Map<String, String> rtn = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : referent.entrySet())
            if (isConsistent(entry.getValue()) == returnConsistencies)
                rtn.put(entry.getKey(), entry.getValue());
        return rtn;
    }

    private static boolean isConsistent(String value) {
        // use pointer comparison here
        return value != INCONSISTENCY;
    }

    private XContentBuilder buildInconsistency(XContentBuilder builder, Map<String, String> reference, Map<String, Settings> settingsPerNode) throws IOException {
        Map<String, String> inconsistencies = extractConsistencies(reference, false);
        for (String inconsistentKeys : inconsistencies.keySet()) {
            builder.startObject(inconsistentKeys);
            for (Map.Entry<String, Settings> entry : settingsPerNode.entrySet()) {
                builder.field(entry.getKey(), entry.getValue().get(inconsistentKeys));
            }
            builder.field("_updatable", dynamicSettings.hasDynamicSetting(inconsistentKeys));
            builder.endObject();
        }
        return builder;
    }

    public Settings effectiveSettingsForNode(Settings nodeId) {
        return effectiveSettings(getNodesMap().get(nodeId).getInitialSettings());
    }

    public Settings effectiveSettings(Settings initialSettings) {
        return effectiveSettings(clusterSettings.getEffectiveSettings(), initialSettings);
    }

    public static Settings effectiveSettings(Settings nodeSettings, Settings initialSettings) {
        return ImmutableSettings.builder()
                .put(initialSettings)
                .put(nodeSettings)
                .build();
    }

    public static Settings desiredSettings(Settings fileSettings, Settings environmentSettings) {
        return ImmutableSettings.builder()
                .put(environmentSettings)
                .put(fileSettings)
                .build();
    }

    public Set<String> settingsDifference(Settings effectiveSettings, Settings desiredSettings) {
        Set<String> rtn = new HashSet<String>();
        for (Map.Entry<String, String> desiredEntry : desiredSettings.getAsMap().entrySet()) {
            String key = desiredEntry.getKey();
            String desired = desiredEntry.getValue();
            Object effective = effectiveSettings.get(key);
            if (desired == null // unspecified values should not be listed at all
                    || TransportReloadSettingsAction.RANDOM_VALUE_AT_STARTUP.equals(desired) // filter special value
                    || desired.equals(effective)) // no change
                continue;
            rtn.add(key);
        }
        return rtn;
    }

}
