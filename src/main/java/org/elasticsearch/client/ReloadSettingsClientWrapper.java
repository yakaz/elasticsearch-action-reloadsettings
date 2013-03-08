package org.elasticsearch.client;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.reloadsettings.ReloadSettingsAction;
import org.elasticsearch.action.reloadsettings.ReloadSettingsRequest;
import org.elasticsearch.action.reloadsettings.ReloadSettingsRequestBuilder;
import org.elasticsearch.action.reloadsettings.ReloadSettingsResponse;

public class ReloadSettingsClientWrapper implements ReloadSettingsClient {

    protected final Client client;

    public ReloadSettingsClientWrapper(Client client) {
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

}
