package org.elasticsearch.action.reloadsettings;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.support.nodes.NodeOperationRequest;
import org.elasticsearch.action.support.nodes.TransportNodesOperationAction;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.node.internal.InternalSettingsPerparer;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class TransportReloadSettingsAction extends TransportNodesOperationAction<ReloadSettingsRequest, ReloadSettingsResponse, TransportReloadSettingsAction.ReloadSettingsRequest, ReloadSettings> {

    @Inject
    public TransportReloadSettingsAction(Settings settings, ClusterName clusterName, ThreadPool threadPool,
                                         ClusterService clusterService, TransportService transportService) {
        super(settings, clusterName, threadPool, clusterService, transportService);
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
            if (resp instanceof ReloadSettings) {
                responses.add((ReloadSettings) resp);
            }
        }
        return new ReloadSettingsResponse(clusterName, responses.toArray(new ReloadSettings[responses.size()]));
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
        return new ReloadSettings();
    }

    @Override
    protected ReloadSettings nodeOperation(ReloadSettingsRequest nodeRequest) throws ElasticSearchException {
        org.elasticsearch.action.reloadsettings.ReloadSettingsRequest request = nodeRequest.request;
        ReloadSettings nodeResponse = new ReloadSettings(clusterService.state().nodes().localNode());
        if (clusterService.state().nodes().localNodeMaster()) {
            nodeResponse.setNodeSettings(clusterService.state().metaData().settings());
            nodeResponse.setTransientSettings(clusterService.state().metaData().transientSettings());
            nodeResponse.setPersistentSettings(clusterService.state().metaData().persistentSettings());
        }
        Settings pSettings = ImmutableSettings.builder().put("name", "{RANDOM}").build(); // neutralize randomly chosen name for response consistency
        Tuple<Settings, Environment> startupConf = InternalSettingsPerparer.prepareSettings(pSettings, true);
        nodeResponse.setFileSettings(startupConf.v1());
        nodeResponse.setEnvironmentSettings(startupConf.v2().settings());
        nodeResponse.setInitialSettings(settings);
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
