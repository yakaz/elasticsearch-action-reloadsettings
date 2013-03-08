package org.elasticsearch.action.reloadsettings;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.Client;

public class ReloadSettingsAction extends Action<ReloadSettingsRequest, ReloadSettingsResponse, ReloadSettingsRequestBuilder> {

    public static final ReloadSettingsAction INSTANCE = new ReloadSettingsAction();
    public static final String NAME = "reloadsettings";

    public ReloadSettingsAction() {
        super(NAME);
    }

    @Override
    public ReloadSettingsRequestBuilder newRequestBuilder(Client client) {
        return new ReloadSettingsRequestBuilder(client);
    }

    @Override
    public ReloadSettingsResponse newResponse() {
        return new ReloadSettingsResponse();
    }
}
