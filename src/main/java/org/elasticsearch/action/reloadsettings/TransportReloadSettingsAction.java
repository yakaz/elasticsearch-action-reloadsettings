package org.elasticsearch.action.reloadsettings;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.support.nodes.NodeOperationRequest;
import org.elasticsearch.action.support.nodes.TransportNodesOperationAction;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.settings.ClusterDynamicSettings;
import org.elasticsearch.cluster.settings.DynamicSettings;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.node.internal.InternalSettingsPreparer;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class TransportReloadSettingsAction extends TransportNodesOperationAction<ReloadSettingsRequest, ReloadSettingsResponse, TransportReloadSettingsAction.ReloadSettingsRequest, ReloadSettings> {

    public static final String RANDOM_VALUE_AT_STARTUP = "{RANDOM_VALUE_AT_STARTUP}";

    private final DynamicSettings dynamicSettings;
    private final ReloadSettingsClusterService reloadSettingsClusterService;

    @Inject
    public TransportReloadSettingsAction(Settings settings, ClusterName clusterName, ThreadPool threadPool,
                                         ClusterService clusterService, TransportService transportService,
                                         ReloadSettingsClusterService reloadSettingsClusterService,
                                         @ClusterDynamicSettings DynamicSettings dynamicSettings) {
        super(settings, clusterName, threadPool, clusterService, transportService);
        this.reloadSettingsClusterService = reloadSettingsClusterService;
        this.dynamicSettings = dynamicSettings;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.MANAGEMENT;
    }

    @Override
    protected String transportAction() {
        return ReloadSettingsAction.NAME;
    }

    @Override
    protected org.elasticsearch.action.reloadsettings.ReloadSettingsRequest newRequest() {
        return new org.elasticsearch.action.reloadsettings.ReloadSettingsRequest();
    }

    @Override
    protected ReloadSettingsResponse newResponse(org.elasticsearch.action.reloadsettings.ReloadSettingsRequest request, AtomicReferenceArray nodesResponses) {
        final List<ReloadSettings> responses = new ArrayList<ReloadSettings>();
        for (int i = 0; i < nodesResponses.length(); i++) {
            Object resp = nodesResponses.get(i);
            if (resp instanceof ReloadSettings)
                responses.add((ReloadSettings) resp);
        }
        ReloadSettings.Cluster clusterResponse = new ReloadSettings.Cluster();
        clusterResponse.setTimestamp(reloadSettingsClusterService.getLastClusterSettingsTimestamp());
        clusterResponse.setVersion(reloadSettingsClusterService.getLastMetaData().version());
        clusterResponse.setEffectiveSettings(reloadSettingsClusterService.getLastMetaData().settings());
        clusterResponse.setTransientSettings(reloadSettingsClusterService.getLastMetaData().transientSettings());
        clusterResponse.setPersistentSettings(reloadSettingsClusterService.getLastMetaData().persistentSettings());
        return new ReloadSettingsResponse(clusterName, clusterResponse, responses.toArray(new ReloadSettings[responses.size()]), dynamicSettings);
    }

    @Override
    protected ReloadSettingsRequest newNodeRequest() {
        return new ReloadSettingsRequest();
    }

    @Override
    protected ReloadSettingsRequest newNodeRequest(String nodeId, org.elasticsearch.action.reloadsettings.ReloadSettingsRequest request) {
        return new ReloadSettingsRequest(nodeId, request);
    }

    @Override
    protected ReloadSettings newNodeResponse() {
        return new ReloadSettings(clusterService.localNode());
    }

    @Override
    protected ReloadSettings nodeOperation(ReloadSettingsRequest nodeRequest) throws ElasticsearchException {
        org.elasticsearch.action.reloadsettings.ReloadSettingsRequest request = nodeRequest.request;
        ReloadSettings nodeResponse = new ReloadSettings(clusterService.localNode());

        // Reconstruct the initial environment
        // This assumes that the nodes have been built using Bootstrap,
        // with an empty pSettings parameter to InternalSettingsPerparer.prepareSettings().
        // We cannot have the original value of pSettings as InternalSettingsPerparer.prepareSettings()
        // erases the initial environment before returning.
        Settings pSettings = ImmutableSettings.builder()
                .put("name", RANDOM_VALUE_AT_STARTUP) // neutralize randomly chosen name for response consistency
                .build();
        Tuple<Settings, Environment> startupConf = InternalSettingsPreparer.prepareSettings(pSettings, true);

        nodeResponse.setInitialSettings(settings);
        nodeResponse.setFileSettings(startupConf.v1());
        nodeResponse.setEnvironmentSettings(startupConf.v2().settings());
        nodeResponse.setFileTimestamp(ReloadSettingsNodeUtil.getLastFileTimestamp(pSettings));
        return nodeResponse;
    }

    @Override
    protected boolean accumulateExceptions() {
        return false;
    }

    static class ReloadSettingsRequest extends NodeOperationRequest {

        org.elasticsearch.action.reloadsettings.ReloadSettingsRequest request;

        ReloadSettingsRequest() {
        }

        ReloadSettingsRequest(String nodeId, org.elasticsearch.action.reloadsettings.ReloadSettingsRequest request) {
            super(request, nodeId);
            this.request = request;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            request = new org.elasticsearch.action.reloadsettings.ReloadSettingsRequest();
            request.readFrom(in);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            request.writeTo(out);
        }
    }

}
