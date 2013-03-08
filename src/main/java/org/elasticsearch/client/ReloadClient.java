package org.elasticsearch.client;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.reload.ReloadRequest;
import org.elasticsearch.action.reload.ReloadRequestBuilder;
import org.elasticsearch.action.reload.ReloadResponse;

public interface ReloadClient {

    /**
     * Reload the dynamic settings.
     *
     * @param request The reload request.
     * @param listener A listener that notifies the caller when the reload operation has completed
     */
    void reload(ReloadRequest request, ActionListener<ReloadResponse> listener);

    /**
     * Performs the same action as in {@link #reload(org.elasticsearch.action.reload.ReloadRequest,
     * org.elasticsearch.action.ActionListener)}, but works with an {@link ActionFuture} instead of a {@link ActionListener}.
     *
     * @param request The reload request
     * @return The result future
     */
    ActionFuture<ReloadResponse> reload(ReloadRequest request);

    /**
     * Prepares a reload of dynamic settings.
     *
     * @return a builder instance
     */
    ReloadRequestBuilder prepareReload();

}
