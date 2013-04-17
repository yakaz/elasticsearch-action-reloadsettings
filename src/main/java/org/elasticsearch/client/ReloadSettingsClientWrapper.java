package org.elasticsearch.client;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.reloadsettings.ReloadSettingsAction;
import org.elasticsearch.action.reloadsettings.ReloadSettingsClusterAction;
import org.elasticsearch.action.reloadsettings.ReloadSettingsClusterRequest;
import org.elasticsearch.action.reloadsettings.ReloadSettingsClusterRequestBuilder;
import org.elasticsearch.action.reloadsettings.ReloadSettingsClusterResponse;
import org.elasticsearch.action.reloadsettings.ReloadSettingsRequest;
import org.elasticsearch.action.reloadsettings.ReloadSettingsRequestBuilder;
import org.elasticsearch.action.reloadsettings.ReloadSettingsResponse;

public class ReloadSettingsClientWrapper implements ReloadSettingsClient {

    protected final ClusterAdminClient client;

    public ReloadSettingsClientWrapper(ClusterAdminClient client) {
        this.client = client;
    }

    @Override
    public void reloadSettings(ReloadSettingsRequest request, ActionListener<ReloadSettingsResponse> listener) {
        client.execute(ReloadSettingsAction.INSTANCE, request, listener);
    }

    @Override
    public ActionFuture<ReloadSettingsResponse> reloadSettings(ReloadSettingsRequest request) {
        return client.execute(ReloadSettingsAction.INSTANCE, request);
    }

    @Override
    public ReloadSettingsRequestBuilder prepareReloadSettings() {
        return new ReloadSettingsRequestBuilder(client);
    }

    @Override
    public void clusterSettingsTimestamp(ReloadSettingsClusterRequest request, ActionListener<ReloadSettingsClusterResponse> listener) {
        client.execute(ReloadSettingsClusterAction.INSTANCE, request, listener);
    }

    @Override
    public ActionFuture<ReloadSettingsClusterResponse> clusterSettingsTimestamp(ReloadSettingsClusterRequest request) {
        return client.execute(ReloadSettingsClusterAction.INSTANCE, request);
    }

    @Override
    public ReloadSettingsClusterRequestBuilder prepareClusterSettingsTimestamp() {
        return new ReloadSettingsClusterRequestBuilder(client);
    }

}
