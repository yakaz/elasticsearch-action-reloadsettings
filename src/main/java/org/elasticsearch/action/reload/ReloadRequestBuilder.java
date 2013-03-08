package org.elasticsearch.action.reload;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ReloadClientWrapper;
import org.elasticsearch.client.internal.InternalGenericClient;

public class ReloadRequestBuilder extends ActionRequestBuilder<ReloadRequest, ReloadResponse, ReloadRequestBuilder> {

    protected final ReloadClientWrapper reloadClientWrapper;

    public ReloadRequestBuilder(Client client) {
        super((InternalGenericClient)client, new ReloadRequest());
        reloadClientWrapper = new ReloadClientWrapper(client);
    }

    // TODO

    @Override
    protected void doExecute(ActionListener<ReloadResponse> listener) {
        // TODO
        reloadClientWrapper.reload(request, listener);
    }

}
