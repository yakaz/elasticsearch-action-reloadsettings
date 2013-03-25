package org.elasticsearch.test.integration.action.reloadsettings;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.reloadsettings.ReloadSettingsResponse;
import org.elasticsearch.action.reloadsettings.inconsistencies.NodeInconsistency;
import org.elasticsearch.client.ReloadSettingsClient;
import org.elasticsearch.client.ReloadSettingsClientWrapper;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.integration.AbstractNodesTests;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

@Test
public class ReloadSettingsActionTests extends AbstractNodesTests {

    public ReloadSettingsClient reloadSettingsClient(String id) {
        return new ReloadSettingsClientWrapper(client(id).admin().cluster());
    }

    @Test
    public void testEffectiveDesiredInconsistencies() throws Exception {
        logger.info("--> starting 1 node");
        startNode("node1");

        ClusterHealthResponse clusterHealthResponse = client("node1").admin().cluster().prepareHealth().setWaitForGreenStatus().execute().actionGet();
        assertThat(clusterHealthResponse.isTimedOut(), equalTo(false));

        ReloadSettingsResponse reloadSettings = getSettings("node1");
        Settings effective = reloadSettings.effectiveSettingsForNode(reloadSettings.getNodes()[0].getNode().id());
        assertThat(effective.get("cluster.name"), startsWith("test-cluster-"));
        Settings initial = reloadSettings.getNodes()[0].getInitialSettings();
        assertThat(initial.get("cluster.name"), startsWith("test-cluster-"));
        Settings desired = reloadSettings.getNodes()[0].getDesiredSettings();
        assertThat(desired.get("cluster.name"), equalTo("elasticsearch"));

        NodeInconsistency inconsistency = reloadSettings.getNodes()[0].getInconsistentSettings().get("cluster.name");
        assertThat(inconsistency, notNullValue());
        assertThat(inconsistency.getEffective(), startsWith("test-cluster-"));
        assertThat(inconsistency.getDesired(), equalTo("elasticsearch"));
    }

    @Test
    public void testResponseIsConsistentAcrossCluster() throws Exception {
        logger.info("--> starting 2 nodes");
        startNode("node1", ImmutableSettings.settingsBuilder().put("discovery.zen.minimum_master_nodes", 1));
        startNode("node2", ImmutableSettings.settingsBuilder().put("discovery.zen.minimum_master_nodes", 2));

        ClusterHealthResponse clusterHealthResponse = client("node1").admin().cluster().prepareHealth().setWaitForGreenStatus().execute().actionGet();
        assertThat(clusterHealthResponse.isTimedOut(), equalTo(false));

        ReloadSettingsResponse node1 = getSettings("node1");
        ReloadSettingsResponse node2 = getSettings("node2");
        logger.debug(node1.toString());
        assertThat(node1, equalTo(node2));
    }

    protected ReloadSettingsResponse getSettings(String node) {
        ReloadSettingsResponse response = reloadSettingsClient(node).prepareReloadSettings().execute().actionGet();
        assertThat(response, notNullValue());
        return response;
    }

}
