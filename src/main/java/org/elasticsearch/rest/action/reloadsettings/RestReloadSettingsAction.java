package org.elasticsearch.rest.action.reloadsettings;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.reloadsettings.ReloadSettingsRequest;
import org.elasticsearch.action.reloadsettings.ReloadSettingsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ReloadSettingsClientWrapper;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.XContentRestResponse;
import org.elasticsearch.rest.XContentThrowableRestResponse;
import org.elasticsearch.rest.action.support.RestActions;
import org.elasticsearch.rest.action.support.RestXContentBuilder;

import java.io.IOException;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestStatus.OK;

public class RestReloadSettingsAction extends BaseRestHandler {

    final protected ReloadSettingsClientWrapper reloadSettingsClientWrapper;

    @Inject
    public RestReloadSettingsAction(Settings settings, Client client, RestController controller) {
        super(settings, client);
        reloadSettingsClientWrapper = new ReloadSettingsClientWrapper(client.admin().cluster());
        controller.registerHandler(GET, "/_nodes/settings/reload", this);
        controller.registerHandler(POST, "/_nodes/{nodeId}/settings/reload", this);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel) {
        String[] nodesIds = RestActions.splitNodes(request.param("nodeId"));
        ReloadSettingsRequest reloadSettingsRequest = new ReloadSettingsRequest(nodesIds);

        reloadSettingsClientWrapper.reloadSettings(reloadSettingsRequest, new ActionListener<ReloadSettingsResponse>() {
            @Override
            public void onResponse(ReloadSettingsResponse response) {
                try {
                    XContentBuilder builder = RestXContentBuilder.restContentBuilder(request);

                    builder.startObject();
                    builder.field(Fields.OK, true); // XXX
                    response.toXContent(builder, ToXContent.EMPTY_PARAMS);
                    builder.endObject();

                    channel.sendResponse(new XContentRestResponse(request, OK, builder));
                } catch (IOException e) {
                    onFailure(e);
                }
            }

            @Override
            public void onFailure(Throwable e) {
                try {
                    channel.sendResponse(new XContentThrowableRestResponse(request, e));
                } catch (IOException e1) {
                    logger.error("Failed to send failure response", e1);
                }
            }
        });
    }

    static final class Fields {
        static final XContentBuilderString OK = new XContentBuilderString("ok");
    }

}
