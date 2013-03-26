package org.elasticsearch.test.integration.action.reloadsettings;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.reloadsettings.ReloadSettingsResponse;
import org.elasticsearch.action.reloadsettings.inconsistencies.ClusterInconsistency;
import org.elasticsearch.action.reloadsettings.inconsistencies.NodeInconsistency;
import org.elasticsearch.client.ReloadSettingsClient;
import org.elasticsearch.client.ReloadSettingsClientWrapper;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.integration.AbstractNodesTests;
import org.testng.annotations.Test;

import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;

import static org.elasticsearch.client.Requests.clusterUpdateSettingsRequest;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
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

        // Note that this is actually an artifact of the fact that we cannot keep the initial environment

        ReloadSettingsResponse reloadSettings = getSettings("node1");
        assertThat(reloadSettings.getNodes().length, equalTo(1));
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
        startNode("node1");
        startNode("node2");

        ClusterHealthResponse clusterHealthResponse = client("node1").admin().cluster().prepareHealth().setWaitForGreenStatus().execute().actionGet();
        assertThat(clusterHealthResponse.isTimedOut(), equalTo(false));

        ReloadSettingsResponse node1 = getSettings("node1");
        ReloadSettingsResponse node2 = getSettings("node2");
        assertThat(node1.getNodes().length, equalTo(2));
        assertThat(node2.getNodes().length, equalTo(2));
        logger.debug(node1.toString());
        assertThat(node1, equalTo(node2));
    }

    @Test
    public void testClusterInconsistencies() throws Exception {
        logger.info("--> starting 2 nodes");
        startNode("node1", ImmutableSettings.settingsBuilder().put("discovery.zen.minimum_master_nodes", 1));
        startNode("node2", ImmutableSettings.settingsBuilder().put("discovery.zen.minimum_master_nodes", 2));

        ClusterHealthResponse clusterHealthResponse = client("node1").admin().cluster().prepareHealth().setWaitForGreenStatus().execute().actionGet();
        assertThat(clusterHealthResponse.isTimedOut(), equalTo(false));

        ReloadSettingsResponse response = getSettings("node1");
        assertThat(response.getNodes().length, equalTo(2));

        ClusterInconsistency inconsistency = response.getInconsistentInitialSettings().get("discovery.zen.minimum_master_nodes");
        assertThat(inconsistency, notNullValue());
        assertThat(new HashSet<String>(inconsistency.values()), equalTo(new HashSet<String>(Arrays.asList("1", "2"))));
        assertThat(inconsistency.isUpdatable(), equalTo(true));

        inconsistency = response.getInconsistentEffectiveSettings().get("discovery.zen.minimum_master_nodes");
        assertThat(inconsistency, notNullValue());
        assertThat(new HashSet<String>(inconsistency.values()), equalTo(new HashSet<String>(Arrays.asList("1", "2"))));
        assertThat(inconsistency.isUpdatable(), equalTo(true));

        // Now set cluster-wide setting to fix the inconsistency (in effective settings only, not in initial settings)
        client("node1").admin().cluster().updateSettings(clusterUpdateSettingsRequest().transientSettings("{discovery:{zen:{minimum_master_nodes:2}}}")).actionGet();

        response = getSettings("node1");
        assertThat(response.getNodes().length, equalTo(2));

        inconsistency = response.getInconsistentInitialSettings().get("discovery.zen.minimum_master_nodes");
        assertThat(inconsistency, notNullValue());
        assertThat(new HashSet<String>(inconsistency.values()), equalTo(new HashSet<String>(Arrays.asList("1", "2"))));
        assertThat(inconsistency.isUpdatable(), equalTo(true));

        inconsistency = response.getInconsistentEffectiveSettings().get("discovery.zen.minimum_master_nodes");
        assertThat(inconsistency, nullValue());
    }

    @Test
    public void testUpdateConfigFile() throws Exception {
        File tmp = File.createTempFile("elasticsearch-test-", "-config.yml");
        PrintStream ps = new PrintStream(tmp);
        ps.println("discovery.zen.minimum_master_nodes: 1");
        ps.close();

        String oldEsConfig = System.getProperty("es.config");
        System.setProperty("es.config", tmp.getAbsolutePath());

        logger.info("--> starting 1 node");
        startNode("node1", ImmutableSettings.settingsBuilder().put("config.ignore_system_properties", false));

        ClusterHealthResponse clusterHealthResponse = client("node1").admin().cluster().prepareHealth().setWaitForGreenStatus().execute().actionGet();
        assertThat(clusterHealthResponse.isTimedOut(), equalTo(false));

        // We should be able to get rid of the following call
        //   System.clearProperty("es.config");
        // But see TransportReloadSettingsAction's call to InternalSettingsPerparer.prepareSettings():
        // the original environment is not preserved.

        ReloadSettingsResponse response = getSettings("node1");
        assertThat(response.getNodes().length, equalTo(1));
        logger.info(response.toString(true));
        assertThat(response.getNodes()[0].getEffectiveSettings().get("discovery.zen.minimum_master_nodes"), equalTo("1"));
        assertThat(response.getNodes()[0].getInconsistentSettings().get("discovery.zen.minimum_master_nodes"), nullValue());

        ps = new PrintStream(tmp);
        ps.println("discovery.zen.minimum_master_nodes: 2");
        ps.close();

        response = getSettings("node1");
        assertThat(response.getNodes().length, equalTo(1));
        logger.info(response.toString(true));
        assertThat(response.getNodes()[0].getEffectiveSettings().get("discovery.zen.minimum_master_nodes"), equalTo("1"));
        NodeInconsistency inconsistency = response.getNodes()[0].getInconsistentSettings().get("discovery.zen.minimum_master_nodes");
        assertThat(inconsistency, notNullValue());
        assertThat(inconsistency.getEffective(), equalTo("1"));
        assertThat(inconsistency.getDesired(), equalTo("2"));

        if (oldEsConfig == null)
            System.clearProperty("es.config");
        else
            System.setProperty("es.config", oldEsConfig);
    }

    protected ReloadSettingsResponse getSettings(String node) {
        ReloadSettingsResponse response = reloadSettingsClient(node).prepareReloadSettings().execute().actionGet();
        assertThat(response, notNullValue());
        return response;
    }

    @Test
    public void testClusterSizeChange() throws Exception {
        File tmp = File.createTempFile("elasticsearch-test-", "-config.yml");
        PrintStream ps = new PrintStream(tmp);
        ps.println("discovery.zen.minimum_master_nodes: 1");
        ps.close();

        String oldEsConfig = System.getProperty("es.config");
        System.setProperty("es.config", tmp.getAbsolutePath());

        logger.info("--> starting 2 nodes");
        startNode("node1", ImmutableSettings.settingsBuilder().put("config.ignore_system_properties", false));
        startNode("node2", ImmutableSettings.settingsBuilder().put("config.ignore_system_properties", false));

        ClusterHealthResponse clusterHealthResponse = client("node1").admin().cluster().prepareHealth().setWaitForGreenStatus().execute().actionGet();
        assertThat(clusterHealthResponse.isTimedOut(), equalTo(false));

        // We should be able to get rid of the following call
        //   System.clearProperty("es.config");
        // But see TransportReloadSettingsAction's call to InternalSettingsPerparer.prepareSettings():
        // the original environment is not preserved.

        ReloadSettingsResponse response = getSettings("node1");
        assertThat(response.getNodes().length, equalTo(2));
        logger.info(response.toString(true));
        assertThat(response.getNodes()[0].getEffectiveSettings().get("discovery.zen.minimum_master_nodes"), equalTo("1"));
        assertThat(response.getNodes()[0].getInconsistentSettings().get("discovery.zen.minimum_master_nodes"), nullValue());
        assertThat(response.getNodes()[1].getEffectiveSettings().get("discovery.zen.minimum_master_nodes"), equalTo("1"));
        assertThat(response.getNodes()[1].getInconsistentSettings().get("discovery.zen.minimum_master_nodes"), nullValue());

        ps = new PrintStream(tmp);
        ps.println("discovery.zen.minimum_master_nodes: 2");
        ps.close();

        response = getSettings("node1");
        assertThat(response.getNodes().length, equalTo(2));
        logger.info(response.toString(true));

        assertThat(response.getNodes()[0].getEffectiveSettings().get("discovery.zen.minimum_master_nodes"), equalTo("1"));
        NodeInconsistency inconsistency = response.getNodes()[0].getInconsistentSettings().get("discovery.zen.minimum_master_nodes");
        assertThat(inconsistency, notNullValue());
        assertThat(inconsistency.getEffective(), equalTo("1"));
        assertThat(inconsistency.getDesired(), equalTo("2"));

        assertThat(response.getNodes()[1].getEffectiveSettings().get("discovery.zen.minimum_master_nodes"), equalTo("1"));
        inconsistency = response.getNodes()[1].getInconsistentSettings().get("discovery.zen.minimum_master_nodes");
        assertThat(inconsistency, notNullValue());
        assertThat(inconsistency.getEffective(), equalTo("1"));
        assertThat(inconsistency.getDesired(), equalTo("2"));

        // Now set cluster-wide setting to fix the inconsistency (in effective settings only, not in initial settings)
        client("node1").admin().cluster().updateSettings(clusterUpdateSettingsRequest().transientSettings("{discovery:{zen:{minimum_master_nodes:2}}}")).actionGet();

        response = getSettings("node1");
        assertThat(response.getNodes().length, equalTo(2));
        logger.info(response.toString(true));
        assertThat(response.getNodes()[0].getEffectiveSettings().get("discovery.zen.minimum_master_nodes"), equalTo("2"));
        assertThat(response.getNodes()[0].getInconsistentSettings().get("discovery.zen.minimum_master_nodes"), nullValue());
        assertThat(response.getNodes()[1].getEffectiveSettings().get("discovery.zen.minimum_master_nodes"), equalTo("2"));
        assertThat(response.getNodes()[1].getInconsistentSettings().get("discovery.zen.minimum_master_nodes"), nullValue());

        if (oldEsConfig == null)
            System.clearProperty("es.config");
        else
            System.setProperty("es.config", oldEsConfig);
    }

}
