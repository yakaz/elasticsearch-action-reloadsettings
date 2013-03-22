package org.elasticsearch.action.reloadsettings;

import org.elasticsearch.action.support.nodes.NodesOperationRequest;

public class ReloadSettingsRequest extends NodesOperationRequest<ReloadSettingsRequest> {

    public ReloadSettingsRequest() {
    }

    public ReloadSettingsRequest(String... nodesIds) {
        super(nodesIds);
    }

}
