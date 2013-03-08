package org.elasticsearch.action.reloadsettings;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.TransportAction;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.BaseTransportRequestHandler;
import org.elasticsearch.transport.TransportChannel;
import org.elasticsearch.transport.TransportService;

public class TransportReloadSettingsAction extends TransportAction<ReloadSettingsRequest, ReloadSettingsResponse> {

    @Inject
    public TransportReloadSettingsAction(Settings settings,
                                         ThreadPool threadPool,
                                         TransportService transportService) {
        super(settings, threadPool);
        transportService.registerHandler(ReloadSettingsAction.NAME, new TransportHandler());
    }

    @Override
    protected void doExecute(final ReloadSettingsRequest request, final ActionListener<ReloadSettingsResponse> listener) {
        // TODO
        listener.onResponse(new ReloadSettingsResponse());
    }

    private class TransportHandler extends BaseTransportRequestHandler<ReloadSettingsRequest> {

        @Override
        public ReloadSettingsRequest newInstance() {
            return new ReloadSettingsRequest();
        }

        @Override
        public String executor() {
            return ThreadPool.Names.SAME;
        }

        @Override
        public void messageReceived(ReloadSettingsRequest request, final TransportChannel channel) throws Exception {
            // no need to have a threaded listener since we just send back a response
            request.listenerThreaded(false);
            doExecute(request, new ActionListener<ReloadSettingsResponse>() {

                public void onResponse(ReloadSettingsResponse result) {
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
