package org.elasticsearch.action.reloadsettings;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.nodes.NodesOperationRequestBuilder;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.client.ReloadSettingsClientWrapper;

public class ReloadSettingsRequestBuilder extends NodesOperationRequestBuilder<ReloadSettingsRequest, ReloadSettingsResponse, ReloadSettingsRequestBuilder> {

    protected final ReloadSettingsClientWrapper reloadSettingsClientWrapper;

    public ReloadSettingsRequestBuilder(ClusterAdminClient client) {
        super(client, new ReloadSettingsRequest());
        reloadSettingsClientWrapper = new ReloadSettingsClientWrapper(client);
    }

    @Override
    protected void doExecute(ActionListener<ReloadSettingsResponse> listener) {
        reloadSettingsClientWrapper.reloadSettings(request, listener);
    }

}
