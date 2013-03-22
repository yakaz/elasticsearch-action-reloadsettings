package org.elasticsearch.action.reloadsettings;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.TransportAction;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.settings.NodeSettingsService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.BaseTransportRequestHandler;
import org.elasticsearch.transport.TransportChannel;
import org.elasticsearch.transport.TransportService;

public class TransportReloadSettingsAction extends TransportAction<ReloadSettingsRequest, ReloadSettingsResponse> {

    private final ClusterService clusterService;

    @Inject
    public TransportReloadSettingsAction(Settings settings,
                                         ThreadPool threadPool,
                                         ClusterService clusterService,
                                         TransportService transportService) {
        super(settings, threadPool);
        this.clusterService = clusterService;
        transportService.registerHandler(ReloadSettingsAction.NAME, new TransportHandler());
    }

    @Override
    protected void doExecute(final ReloadSettingsRequest request, final ActionListener<ReloadSettingsResponse> listener) {
        ReloadSettingsResponse response = new ReloadSettingsResponse();
        response.setNodeSettings(NodeSettingsService.getGlobalSettings());
        if (clusterService.state().nodes().localNodeMaster()) {
            response.setTransientSettings(clusterService.state().metaData().transientSettings());
            response.setPersistentSettings(clusterService.state().metaData().persistentSettings());
        }
        response.setFileSettings(null); // TODO
        response.setInitialSettings(settings);
        listener.onResponse(response);
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
