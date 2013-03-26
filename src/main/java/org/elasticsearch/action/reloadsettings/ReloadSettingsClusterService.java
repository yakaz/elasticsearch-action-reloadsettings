package org.elasticsearch.action.reloadsettings;

import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.settings.NodeSettingsService;

public class ReloadSettingsClusterService extends AbstractComponent implements NodeSettingsService.Listener {

    private DateTime lastClusterSettingsTimestamp;
    protected final ClusterService clusterService;

    @Inject
    public ReloadSettingsClusterService(Settings settings, ClusterService clusterService, NodeSettingsService nodeSettingsService) {
        super(settings);
        this.clusterService = clusterService;
        nodeSettingsService.addListener(this);
    }

    @Override
    public void onRefreshSettings(Settings settings) {
        lastClusterSettingsTimestamp = DateTime.now();
    }

    public DateTime getLastClusterSettingsTimestamp() {
        return lastClusterSettingsTimestamp;
    }

    public MetaData getLastMetaData() {
        return clusterService.state().metaData();
    }

}
