package org.elasticsearch.test.integration.action.reloadsettings;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.reloadsettings.ReloadSettingsResponse;
import org.elasticsearch.client.ReloadSettingsClient;
import org.elasticsearch.client.ReloadSettingsClientWrapper;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.test.integration.AbstractNodesTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.elasticsearch.client.Requests.clusterUpdateSettingsRequest;
import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@Test
public class ReloadSettingsActionTests extends AbstractNodesTests {

    public ReloadSettingsClient reloadSettingsClient(String id) {
        return new ReloadSettingsClientWrapper(client(id).admin().cluster());
    }

    @BeforeMethod
    public void startCluster() throws Exception {
        logger.info("--> starting 2 nodes");
        startNode("node1", ImmutableSettings.settingsBuilder().put("discovery.zen.minimum_master_nodes", 1));
        startNode("node2", ImmutableSettings.settingsBuilder().put("discovery.zen.minimum_master_nodes", 2));

        logger.info("--> creating an index with no replicas");
        client("node1").admin().indices().prepareCreate("test")
                .setSettings(settingsBuilder().put("index.number_of_replicas", 0))
                .execute().actionGet();

        ClusterHealthResponse clusterHealthResponse = client("node1").admin().cluster().prepareHealth().setWaitForGreenStatus().execute().actionGet();
        assertThat(clusterHealthResponse.isTimedOut(), equalTo(false));

        logger.info("--> index some data");
        client("node1").prepareIndex("test", "type", "0").setSource("field", "value0").execute().actionGet();
        client("node1").admin().indices().prepareRefresh().execute().actionGet();
        client("node2").prepareIndex("test", "type", "1").setSource("field", "value1").execute().actionGet();
        client("node2").admin().indices().prepareRefresh().execute().actionGet();
    }

    @Test
    public void testRestEndpoint() throws Exception {
        client("node1").admin().cluster().updateSettings(clusterUpdateSettingsRequest().persistentSettings("{discovery:{zen:{minimum_master_nodes:2}}}")).actionGet();
        client("node1").admin().cluster().updateSettings(clusterUpdateSettingsRequest().transientSettings("{discovery:{zen:{minimum_master_nodes:1}}}")).actionGet();
        String node1 = formatSettings(getSettings("node1"));
        String node2 = formatSettings(getSettings("node2"));
        logger.debug(node1);
        assertThat(node1, equalTo(node2));
    }

    protected ReloadSettingsResponse getSettings(String node) {
        ReloadSettingsResponse response = reloadSettingsClient(node).prepareReloadSettings().execute().actionGet();
        assertThat(response, notNullValue());
        return response;
    }

    protected String formatSettings(ReloadSettingsResponse response) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.prettyPrint();
            builder.startObject();
            response.toXContent(builder, ToXContent.EMPTY_PARAMS);
            builder.endObject();
            return builder.bytes().toUtf8();
        } catch (IOException e) {
            throw new RuntimeException("Could not build string representation of setting", e);
        }
    }

}
