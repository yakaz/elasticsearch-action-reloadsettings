package org.elasticsearch.action.reloadsettings;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ReloadSettingsClientWrapper;
import org.elasticsearch.client.internal.InternalGenericClient;

public class ReloadSettingsRequestBuilder extends ActionRequestBuilder<ReloadSettingsRequest, ReloadSettingsResponse, ReloadSettingsRequestBuilder> {

    protected final ReloadSettingsClientWrapper reloadSettingsClientWrapper;

    public ReloadSettingsRequestBuilder(Client client) {
        super((InternalGenericClient)client, new ReloadSettingsRequest());
        reloadSettingsClientWrapper = new ReloadSettingsClientWrapper(client);
    }

    // TODO

    @Override
    protected void doExecute(ActionListener<ReloadSettingsResponse> listener) {
        reloadSettingsClientWrapper.reloadSettings(request, listener);
    }

}
