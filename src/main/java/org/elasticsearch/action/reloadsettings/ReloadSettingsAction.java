package org.elasticsearch.action.reloadsettings;

import org.elasticsearch.action.admin.cluster.ClusterAction;
import org.elasticsearch.client.ClusterAdminClient;

public class ReloadSettingsAction extends ClusterAction<ReloadSettingsRequest, ReloadSettingsResponse, ReloadSettingsRequestBuilder> {

    public static final ReloadSettingsAction INSTANCE = new ReloadSettingsAction();
    public static final String NAME = "reloadsettings";

    public ReloadSettingsAction() {
        super(NAME);
    }

    @Override
    public ReloadSettingsRequestBuilder newRequestBuilder(ClusterAdminClient client) {
        return new ReloadSettingsRequestBuilder(client);
    }

    @Override
    public ReloadSettingsResponse newResponse() {
        return new ReloadSettingsResponse();
    }
}
