package org.elasticsearch.action.reloadsettings;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.master.TransportMasterNodeOperationAction;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

public class TransportReloadSettingsClusterAction extends TransportMasterNodeOperationAction<ReloadSettingsClusterRequest, ReloadSettingsClusterResponse> {

    private final ReloadSettingsClusterService reloadSettingsClusterService;

    @Inject
    public TransportReloadSettingsClusterAction(Settings settings, TransportService transportService,
                                                ClusterService clusterService, ThreadPool threadPool,
                                                ReloadSettingsClusterService reloadSettingsClusterService) {
        super(settings, transportService, clusterService, threadPool);
        this.reloadSettingsClusterService = reloadSettingsClusterService;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.MANAGEMENT;
    }

    @Override
    protected String transportAction() {
        return ReloadSettingsClusterAction.NAME;
    }

    @Override
    protected ReloadSettingsClusterRequest newRequest() {
        return new ReloadSettingsClusterRequest();
    }

    @Override
    protected ReloadSettingsClusterResponse newResponse() {
        return new ReloadSettingsClusterResponse();
    }

    @Override
    protected void masterOperation(ReloadSettingsClusterRequest request, ClusterState state, ActionListener<ReloadSettingsClusterResponse> responseListener) throws ElasticsearchException {
        ReloadSettingsClusterResponse rtn = new ReloadSettingsClusterResponse();
        rtn.setVersion(reloadSettingsClusterService.getLastMetaData().version());
        rtn.setTimestamp(reloadSettingsClusterService.getLastClusterSettingsTimestamp());
        responseListener.onResponse(rtn);
    }

}
