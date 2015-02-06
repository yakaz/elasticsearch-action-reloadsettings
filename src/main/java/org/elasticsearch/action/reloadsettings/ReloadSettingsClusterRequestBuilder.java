package org.elasticsearch.action.reloadsettings;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.master.MasterNodeOperationRequestBuilder;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.client.ReloadSettingsClientWrapper;

public class ReloadSettingsClusterRequestBuilder extends MasterNodeOperationRequestBuilder<ReloadSettingsClusterRequest, ReloadSettingsClusterResponse, ReloadSettingsClusterRequestBuilder, ClusterAdminClient> {

    protected final ReloadSettingsClientWrapper reloadSettingsClientWrapper;

    public ReloadSettingsClusterRequestBuilder(ClusterAdminClient client) {
        super(client, new ReloadSettingsClusterRequest());
        reloadSettingsClientWrapper = new ReloadSettingsClientWrapper(client);
    }

    public ReloadSettingsClusterRequestBuilder setVersion(long version) {
        request.setVersion(version);
        return this;
    }

    @Override
    protected void doExecute(ActionListener<ReloadSettingsClusterResponse> listener) {
        reloadSettingsClientWrapper.clusterSettingsTimestamp(request, listener);
    }

}
