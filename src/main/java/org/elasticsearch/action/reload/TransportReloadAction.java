package org.elasticsearch.action.reload;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.TransportAction;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.BaseTransportRequestHandler;
import org.elasticsearch.transport.TransportChannel;
import org.elasticsearch.transport.TransportService;

public class TransportReloadAction extends TransportAction<ReloadRequest, ReloadResponse> {

    @Inject
    public TransportReloadAction(Settings settings,
                                 ThreadPool threadPool,
                                 TransportService transportService) {
        super(settings, threadPool);
        transportService.registerHandler(ReloadAction.NAME, new TransportHandler());
    }

    @Override
    protected void doExecute(final ReloadRequest request, final ActionListener<ReloadResponse> listener) {
        // TODO
        listener.onResponse(new ReloadResponse());
    }

    private class TransportHandler extends BaseTransportRequestHandler<ReloadRequest> {

        @Override
        public ReloadRequest newInstance() {
            return new ReloadRequest();
        }

        @Override
        public String executor() {
            return ThreadPool.Names.SAME;
        }

        @Override
        public void messageReceived(ReloadRequest request, final TransportChannel channel) throws Exception {
            // no need to have a threaded listener since we just send back a response
            request.listenerThreaded(false);
            doExecute(request, new ActionListener<ReloadResponse>() {

                public void onResponse(ReloadResponse result) {
                    try {
                        channel.sendResponse(result);
                    } catch (Exception e) {
                        onFailure(e);
                    }
                }

                public void onFailure(Throwable e) {
                    try {
                        channel.sendResponse(e);
                    } catch (Exception e1) {
                        logger.warn("Failed to send response for get", e1);
                    }
                }
            });
        }

    }

}
