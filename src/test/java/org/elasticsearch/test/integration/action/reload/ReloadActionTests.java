package org.elasticsearch.test.integration.action.reload;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.reload.ReloadResponse;
import org.elasticsearch.client.ReloadClient;
import org.elasticsearch.client.ReloadClientWrapper;
import org.elasticsearch.test.integration.AbstractNodesTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@Test
public class ReloadActionTests extends AbstractNodesTests {

    public ReloadClient reloadClient(String id) {
        return new ReloadClientWrapper(client(id));
    }

    @BeforeMethod
    public void startCluster() throws Exception {
        logger.info("--> starting 2 nodes");
        startNode("node1");
        startNode("node2");

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
        ReloadResponse response = reloadClient("node1").prepareReload().execute().actionGet();
        assertThat(response, notNullValue());
    }

}
