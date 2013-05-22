package org.elasticsearch.action.reloadsettings;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.settings.NodeSettingsService;

public class ReloadSettingsClusterService extends AbstractLifecycleComponent<ReloadSettingsClusterService> implements NodeSettingsService.Listener {

    protected final ESLogger logger = Loggers.getLogger(getClass());

    private DateTime lastClusterSettingsTimestamp;

    protected final ClusterService clusterService;
    protected final Injector injector;
    protected TransportReloadSettingsClusterAction reloadSettingsClusterAction;
    protected final NodeSettingsService nodeSettingsService;

    @Inject
    public ReloadSettingsClusterService(Settings settings, ClusterService clusterService,
                                        Injector injector,
                                        NodeSettingsService nodeSettingsService) {
        super(settings);
        this.clusterService = clusterService;
        this.injector = injector;
        this.nodeSettingsService = nodeSettingsService;
    }

    @Override
    protected void doStart() throws ElasticSearchException {
        reloadSettingsClusterAction = injector.getInstance(TransportReloadSettingsClusterAction.class);
        nodeSettingsService.addListener(this);
    }

    @Override
    protected void doClose() throws ElasticSearchException {
    }

    @Override
    protected void doStop() throws ElasticSearchException {
        reloadSettingsClusterAction = null;
        nodeSettingsService.removeListener(this);
    }

    @Override
    public void onRefreshSettings(Settings settings) {
        if (clusterService.state().nodes().localNodeMaster()) {
            lastClusterSettingsTimestamp = DateTime.now();
            logger.info("[{}] master received new metadata cluster settings version {}, assigning timestamp {}", clusterService.localNode().getName(), clusterService.state().metaData().version(), lastClusterSettingsTimestamp.toString(ISODateTimeFormat.dateTime()));
        } else {
            final long version = clusterService.state().metaData().version();
            lastClusterSettingsTimestamp = null;
            ReloadSettingsClusterRequest request = new ReloadSettingsClusterRequest();
            request.setVersion(version);
            logger.info("[{}] request timestamp of metadata cluster settings version {} to master", clusterService.localNode().getName(), version);
            reloadSettingsClusterAction.execute(request, new ActionListener<ReloadSettingsClusterResponse>() {
                @Override
                public void onResponse(ReloadSettingsClusterResponse reloadSettingsClusterResponse) {
                    long currentVersion = clusterService.state().metaData().version();
                    if (currentVersion == version // version is still the same on this node
                            && version == reloadSettingsClusterResponse.getVersion() // version is still the same on the master node
                            ) {
                        lastClusterSettingsTimestamp = reloadSettingsClusterResponse.getTimestamp();
                        logger.info("[{}] got timestamp {} for metadata cluster settings version {}", clusterService.localNode().getName(), lastClusterSettingsTimestamp.toString(ISODateTimeFormat.dateTime()), version);
                    } else {
                        logger.debug("[{}] metadata cluster settings version changed between request and response [{} became {}]", clusterService.localNode().getName(), version, currentVersion);
                    }
                }
                @Override
                public void onFailure(Throwable e) {
                    logger.error("[{}] Error while retrieving metadata cluster settings timestamp for version {} from master", e, clusterService.localNode().getName(), version);
                }
            });
        }
    }

    public DateTime getLastClusterSettingsTimestamp() {
        logger.trace("[{}] giving metadata cluster settings timestamp [{}]", clusterService.localNode().getName(), lastClusterSettingsTimestamp == null ? "null" : lastClusterSettingsTimestamp.toString(ISODateTimeFormat.dateTime()));
        return lastClusterSettingsTimestamp;
    }

    public MetaData getLastMetaData() {
        return clusterService.state().metaData();
    }

}
