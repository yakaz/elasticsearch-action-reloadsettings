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
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReloadSettingsResponse extends NodesOperationResponse<ReloadSettings> implements ToXContent {

    private final DynamicSettings dynamicSettings;
    private ReloadSettings clusterSettings;

    public ReloadSettingsResponse() {
        dynamicSettings = null;
    }

    public ReloadSettingsResponse(ClusterName clusterName, ReloadSettings[] nodes, DynamicSettings dynamicSettings) {
        super(clusterName, nodes);
        this.dynamicSettings = dynamicSettings;
        for (ReloadSettings reloadSettings : getNodes()) {
            if (reloadSettings.getNodeSettings() != null) {
                clusterSettings = reloadSettings;
                break;
            }
        }
    }

    public ReloadSettings getClusterSettings() {
        return clusterSettings;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        Map<String, String> mapParams = new HashMap<String, String>();
        params = new MapParams(mapParams);

        builder.startObject("settings");

        builder.field("cluster");
        mapParams.put("cluster", "true");
        clusterSettings.toXContent(builder, params);

        builder.startObject("nodes");
        mapParams.put("cluster", "false");
        mapParams.put("wrap-object", "false");
        for (Map.Entry<String, ReloadSettings> entry : getNodesMap().entrySet()) {
            builder.startObject(entry.getKey());
            ReloadSettings reloadSettings = entry.getValue();
            Settings effective = effectiveSettings(reloadSettings.getInitialSettings());
            builder.field("effective", effective.getAsMap());
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
        }
        builder.endObject();

        builder.endObject();

        return builder;
    }

    public Settings effectiveSettingsForNode(Settings nodeId) {
        return effectiveSettings(getNodesMap().get(nodeId).getInitialSettings());
    }

    public Settings effectiveSettings(Settings initialSettings) {
        return effectiveSettings(clusterSettings.getNodeSettings(), initialSettings);
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
