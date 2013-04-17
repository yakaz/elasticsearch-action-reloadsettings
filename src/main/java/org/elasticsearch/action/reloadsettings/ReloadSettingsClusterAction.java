package org.elasticsearch.action.reloadsettings;

import org.elasticsearch.action.admin.cluster.ClusterAction;
import org.elasticsearch.client.ClusterAdminClient;

public class ReloadSettingsClusterAction extends ClusterAction<ReloadSettingsClusterRequest, ReloadSettingsClusterResponse, ReloadSettingsClusterRequestBuilder> {

    public static final ReloadSettingsClusterAction INSTANCE = new ReloadSettingsClusterAction();
    public static final String NAME = "cluster/settings/timestamp";

    public ReloadSettingsClusterAction() {
        super(NAME);
    }

    @Override
    public ReloadSettingsClusterRequestBuilder newRequestBuilder(ClusterAdminClient client) {
        return new ReloadSettingsClusterRequestBuilder(client);
    }

    @Override
    public ReloadSettingsClusterResponse newResponse() {
        return new ReloadSettingsClusterResponse();
    }
}
