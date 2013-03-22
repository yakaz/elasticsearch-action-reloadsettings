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
        Map<String, String> settingsConsistency = new HashMap<String, String>();
        Map<String, Settings> effectiveSettingsPerNode = new HashMap<String, Settings>();

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
            builder.startObject(nodeId);
            ReloadSettings reloadSettings = entry.getValue();
            Settings effective = effectiveSettings(reloadSettings.getInitialSettings());
            builder.field("effective", effective.getAsMap());
            effectiveSettingsPerNode.put(nodeId, effective);
            noteInconsistencies(settingsConsistency, effective, first);
            reloadSettings.toXContent(builder, params);
            Settings desired = desiredSettings(reloadSettings.getFileSettings(), reloadSettings.getEnvironmentSettings());
            builder.field("desired", desired.getAsMap());
            builder.startObject("updateable");
            for (String key : settingsDifference(effective, desired, true)) {
                builder.startObject(key);
                builder.field("effective", effective.get(key));
                builder.field("desired", desired.get(key));
                builder.endObject();
            }
            builder.endObject();
            builder.startObject("not_updateable");
            for (String key : settingsDifference(effective, desired, false)) {
                builder.startObject(key);
                builder.field("effective", effective.get(key));
                builder.field("desired", desired.get(key));
                builder.endObject();
            }
            builder.endObject();
            builder.endObject();
            first = false;
        }
        builder.endObject();

        Map<String, String> consistencies = extractConsistencies(settingsConsistency, true);
        builder.field("consistencies", consistencies);

        builder.startObject("inconsistencies");
        Map<String, String> inconsistencies = extractConsistencies(settingsConsistency, false);
        for (String inconsistentKeys : inconsistencies.keySet()) {
            builder.startObject(inconsistentKeys);
            for (Map.Entry<String, Settings> entry : effectiveSettingsPerNode.entrySet()) {
                builder.field(entry.getKey(), entry.getValue().get(inconsistentKeys));
            }
            builder.endObject();
        }
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
        return value != INCONSISTENCY;
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

    public Set<String> settingsDifference(Settings effectiveSettings, Settings desiredSettings, boolean dynamicallyUpdatable) {
        Set<String> rtn = new HashSet<String>();
        for (Map.Entry<String, String> desiredEntry : desiredSettings.getAsMap().entrySet()) {
            String key = desiredEntry.getKey();
            String desired = desiredEntry.getValue();
            Object effective = effectiveSettings.get(key);
            if (desired == null // unspecified values should not be listed at all
                    || TransportReloadSettingsAction.RANDOM_VALUE_AT_STARTUP.equals(desired) // filter special value
                    || desired.equals(effective) // no change
                    || dynamicallyUpdatable != dynamicSettings.hasDynamicSetting(key)) // not the asked dynamic updatability
                continue;
            rtn.add(key);
        }
        return rtn;
    }

}
