package org.elasticsearch.action.reload;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.Client;

public class ReloadAction extends Action<ReloadRequest, ReloadResponse, ReloadRequestBuilder> {

    public static final ReloadAction INSTANCE = new ReloadAction();
    public static final String NAME = "reload";

    public ReloadAction() {
        super(NAME);
    }

    @Override
    public ReloadRequestBuilder newRequestBuilder(Client client) {
        return new ReloadRequestBuilder(client);
    }

    @Override
    public ReloadResponse newResponse() {
        return new ReloadResponse();
    }
}
