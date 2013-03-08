package org.elasticsearch.rest.action.reload;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.reload.ReloadRequest;
import org.elasticsearch.action.reload.ReloadResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ReloadClientWrapper;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.XContentRestResponse;
import org.elasticsearch.rest.XContentThrowableRestResponse;
import org.elasticsearch.rest.action.support.RestXContentBuilder;

import java.io.IOException;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestStatus.OK;

public class RestReloadAction extends BaseRestHandler {

    final protected ReloadClientWrapper reloadClientWrapper;

    @Inject
    public RestReloadAction(Settings settings, Client client, RestController controller) {
        super(settings, client);
        reloadClientWrapper = new ReloadClientWrapper(client);
        controller.registerHandler(GET, "/_cluster/settings/reload", this);
        controller.registerHandler(POST, "/_cluster/settings/reload", this);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel) {
        ReloadRequest reloadRequest = new ReloadRequest();

        // TODO Parsing

        reloadClientWrapper.reload(reloadRequest, new ActionListener<ReloadResponse>() {
            @Override
            public void onResponse(ReloadResponse response) {
                try {
                    XContentBuilder builder = RestXContentBuilder.restContentBuilder(request);

                    // TODO Build answer
                    builder.startObject();
                    builder.field(Fields.OK, true); // XXX
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
