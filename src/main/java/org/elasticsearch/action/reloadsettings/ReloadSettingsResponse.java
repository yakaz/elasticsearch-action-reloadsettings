package org.elasticsearch.action.reloadsettings;

import org.elasticsearch.action.support.nodes.NodesOperationResponse;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ReloadSettingsResponse extends NodesOperationResponse<ReloadSettings> implements ToXContent {

    public ReloadSettingsResponse() {
    }

    public ReloadSettingsResponse(ClusterName clusterName, ReloadSettings[] nodes) {
        super(clusterName, nodes);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        ReloadSettings clusterSettings = null;
        for (ReloadSettings reloadSettings : getNodes()) {
            if (reloadSettings.getNodeSettings() != null) {
                clusterSettings = reloadSettings;
                break;
            }
        }

        Map<String, String> mapParams = new HashMap<String, String>();
        params = new MapParams(mapParams);

        builder.startObject("settings");

        builder.field("cluster");
        mapParams.put("cluster", "true");
        clusterSettings.toXContent(builder, params);

        builder.startObject("nodes");
        mapParams.put("cluster", "false");
        for (Map.Entry<String, ReloadSettings> entry : getNodesMap().entrySet()) {
            builder.field(entry.getKey());
            entry.getValue().toXContent(builder, params);
        }
        builder.endObject();

        builder.endObject();

        return builder;
    }

}
