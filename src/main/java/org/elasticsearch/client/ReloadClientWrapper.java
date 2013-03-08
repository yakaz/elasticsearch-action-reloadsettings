package org.elasticsearch.client;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.reload.ReloadAction;
import org.elasticsearch.action.reload.ReloadRequest;
import org.elasticsearch.action.reload.ReloadRequestBuilder;
import org.elasticsearch.action.reload.ReloadResponse;

public class ReloadClientWrapper implements ReloadClient {

    protected final Client client;

    public ReloadClientWrapper(Client client) {
        this.client = client;
    }

    @Override
    public void reload(ReloadRequest request, ActionListener<ReloadResponse> listener) {
        client.execute(ReloadAction.INSTANCE, request, listener);
    }

    @Override
    public ActionFuture<ReloadResponse> reload(ReloadRequest request) {
        return client.execute(ReloadAction.INSTANCE, request);
    }

    @Override
    public ReloadRequestBuilder prepareReload() {
        return new ReloadRequestBuilder(client);
    }

}
