package org.elasticsearch.action.reloadsettings;

import org.elasticsearch.action.reloadsettings.inconsistencies.ClusterInconsistency;
import org.elasticsearch.action.reloadsettings.inconsistencies.InconsistentSettings;
import org.elasticsearch.action.reloadsettings.inconsistencies.NodeInconsistency;
import org.elasticsearch.action.support.nodes.NodesOperationResponse;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.settings.DynamicSettings;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ReloadSettingsResponse extends NodesOperationResponse<ReloadSettings> implements ToXContent {

    private static final String INCONSISTENCY = new String(); // unique pointer

    private final DynamicSettings dynamicSettings;

    private ReloadSettings.Cluster clusterSettings;
    private Settings consistentEffectiveSettings;
    private Settings consistentInitialSettings;
    private Settings consistentDesiredSettings;
    private InconsistentSettings<ClusterInconsistency> inconsistentEffectiveSettings;
    private InconsistentSettings<ClusterInconsistency> inconsistentInitialSettings;
    private InconsistentSettings<ClusterInconsistency> inconsistentDesiredSettings;

    public ReloadSettingsResponse() {
        dynamicSettings = null;
    }

    public ReloadSettingsResponse(ClusterName clusterName, ReloadSettings.Cluster clusterSettings, ReloadSettings[] nodeSettings, DynamicSettings dynamicSettings) {
        super(clusterName, nodeSettings);
        this.clusterSettings = clusterSettings;
        this.dynamicSettings = dynamicSettings;
        calculateMetaSettings();
    }

    public ReloadSettings.Cluster getClusterSettings() {
        return clusterSettings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        ReloadSettingsResponse that = (ReloadSettingsResponse) o;

        if (!getNodesMap().equals(that.getNodesMap()))
            return false;
        if (clusterSettings != null ? !clusterSettings.equals(that.clusterSettings) : that.clusterSettings != null)
            return false;
        if (consistentDesiredSettings != null ? !consistentDesiredSettings.equals(that.consistentDesiredSettings) : that.consistentDesiredSettings != null)
            return false;
        if (consistentEffectiveSettings != null ? !consistentEffectiveSettings.equals(that.consistentEffectiveSettings) : that.consistentEffectiveSettings != null)
            return false;
        if (consistentInitialSettings != null ? !consistentInitialSettings.equals(that.consistentInitialSettings) : that.consistentInitialSettings != null)
            return false;
        if (inconsistentDesiredSettings != null ? !inconsistentDesiredSettings.equals(that.inconsistentDesiredSettings) : that.inconsistentDesiredSettings != null)
            return false;
        if (inconsistentEffectiveSettings != null ? !inconsistentEffectiveSettings.equals(that.inconsistentEffectiveSettings) : that.inconsistentEffectiveSettings != null)
            return false;
        if (inconsistentInitialSettings != null ? !inconsistentInitialSettings.equals(that.inconsistentInitialSettings) : that.inconsistentInitialSettings != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(getNodes());
        result = 31 * result + (clusterSettings != null ? clusterSettings.hashCode() : 0);
        result = 31 * result + (consistentEffectiveSettings != null ? consistentEffectiveSettings.hashCode() : 0);
        result = 31 * result + (consistentInitialSettings != null ? consistentInitialSettings.hashCode() : 0);
        result = 31 * result + (consistentDesiredSettings != null ? consistentDesiredSettings.hashCode() : 0);
        result = 31 * result + (inconsistentEffectiveSettings != null ? inconsistentEffectiveSettings.hashCode() : 0);
        result = 31 * result + (inconsistentInitialSettings != null ? inconsistentInitialSettings.hashCode() : 0);
        result = 31 * result + (inconsistentDesiredSettings != null ? inconsistentDesiredSettings.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            toXContent(builder, ToXContent.EMPTY_PARAMS);
            builder.endObject();
            return builder.string();
        } catch (IOException e) {
            throw new RuntimeException("Could not build string representation of setting", e);
        }
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(Fields.SETTINGS);

        builder.field(Fields.CLUSTER);
        if (clusterSettings == null) {
            builder.startObject().endObject();
        } else {
            clusterSettings.toXContent(builder, params);
        }

        builder.startObject(Fields.NODES);
        for (Map.Entry<String, ReloadSettings> entry : getNodesMap().entrySet()) {
            builder.field(entry.getKey());
            entry.getValue().toXContent(builder, params);
        }
        builder.endObject();

        builder.startObject(Fields.CONSISTENCIES);
        if (consistentEffectiveSettings != null)
            builder.field(Fields.EFFECTIVE, consistentEffectiveSettings.getAsMap());
        if (consistentInitialSettings != null)
            builder.field(Fields.INITIAL, consistentInitialSettings.getAsMap());
        if (consistentDesiredSettings != null)
            builder.field(Fields.DESIRED, consistentDesiredSettings.getAsMap());
        builder.endObject();

        builder.startObject(Fields.INCONSISTENCIES);
        if (inconsistentEffectiveSettings != null) {
            builder.field(Fields.EFFECTIVE);
            inconsistentEffectiveSettings.toXContent(builder, params);
        }
        if (inconsistentInitialSettings != null) {
            builder.field(Fields.INITIAL);
            inconsistentInitialSettings.toXContent(builder, params);
        }
        if (inconsistentDesiredSettings != null) {
            builder.field(Fields.DESIRED);
            inconsistentDesiredSettings.toXContent(builder, params);
        }
        builder.endObject();

        builder.endObject();

        return builder;
    }

    private void calculateMetaSettings() {
        Map<String, String> effectiveSettingsConsistency = new HashMap<String, String>();
        Map<String, String> initialSettingsConsistency = new HashMap<String, String>();
        Map<String, String> desiredSettingsConsistency = new HashMap<String, String>();
        Map<String, Settings> effectiveSettingsPerNode = new HashMap<String, Settings>();
        Map<String, Settings> initialSettingsPerNode = new HashMap<String, Settings>();
        Map<String, Settings> desiredSettingsPerNode = new HashMap<String, Settings>();

        boolean first = true;
        for (Map.Entry<String, ReloadSettings> entry : getNodesMap().entrySet()) {
            String nodeId = entry.getKey();
            ReloadSettings reloadSettings = entry.getValue();

            Settings effective = effectiveSettings(reloadSettings.getInitialSettings());
            Settings desired = desiredSettings(reloadSettings.getFileSettings(), reloadSettings.getEnvironmentSettings());

            effectiveSettingsPerNode.put(nodeId, effective);
            initialSettingsPerNode.put(nodeId, reloadSettings.getInitialSettings());
            desiredSettingsPerNode.put(nodeId, desired);

            noteInconsistencies(effectiveSettingsConsistency, effective, first);
            noteInconsistencies(initialSettingsConsistency, reloadSettings.getInitialSettings(), first);
            noteInconsistencies(desiredSettingsConsistency, desired, first);

            InconsistentSettings<NodeInconsistency> inconsistencies = new InconsistentSettings<NodeInconsistency>();
            for (String key : settingsDifference(effective, desired)) {
                inconsistencies.add(new NodeInconsistency(key, dynamicSettings.hasDynamicSetting(key), effective.get(key), desired.get(key)));
            }

            reloadSettings.setEffectiveSettings(effective);
            reloadSettings.setDesiredSettings(desired);
            reloadSettings.setInconsistentSettings(inconsistencies);

            first = false;
        }

        consistentEffectiveSettings = extractConsistencies(effectiveSettingsConsistency, true);
        consistentInitialSettings = extractConsistencies(initialSettingsConsistency, true);
        consistentDesiredSettings = extractConsistencies(desiredSettingsConsistency, true);

        inconsistentEffectiveSettings = buildInconsistency(effectiveSettingsConsistency, effectiveSettingsPerNode);
        inconsistentInitialSettings = buildInconsistency(initialSettingsConsistency, initialSettingsPerNode);
        inconsistentDesiredSettings = buildInconsistency(desiredSettingsConsistency, desiredSettingsPerNode);
    }

    private static void noteInconsistencies(Map<String, String> referent, Settings version, boolean isFirst) {
        for (Map.Entry<String, String> setting : version.getAsMap().entrySet()) {
            String key = setting.getKey();
            String value = setting.getValue();
            if (referent.containsKey(key)) {
                if (value == null && referent.get(key) != null
                        || value != null && !value.equals(referent.get(key))) {
                    referent.put(key, INCONSISTENCY);
                }
            } else if (isFirst) {
                referent.put(key, value);
            } else {
                referent.put(key, INCONSISTENCY);
            }
        }
    }

    private static Settings extractConsistencies(Map<String, String> referent, boolean returnConsistencies) {
        ImmutableSettings.Builder builder = ImmutableSettings.builder();
        for (Map.Entry<String, String> entry : referent.entrySet())
            if (isConsistent(entry.getValue()) == returnConsistencies)
                builder.put(entry.getKey(), entry.getValue());
        return builder.build();
    }

    private static boolean isConsistent(String value) {
        // use pointer comparison here
        return value != INCONSISTENCY;
    }

    private InconsistentSettings<ClusterInconsistency> buildInconsistency(Map<String, String> reference, Map<String, Settings> settingsPerNode) {
        InconsistentSettings<ClusterInconsistency> rtn = new InconsistentSettings<ClusterInconsistency>();
        Settings inconsistencies = extractConsistencies(reference, false);
        for (String inconsistentKey : inconsistencies.getAsMap().keySet()) {
            ClusterInconsistency inconsistency = new ClusterInconsistency(inconsistentKey, dynamicSettings.hasDynamicSetting(inconsistentKey));
            for (Map.Entry<String, Settings> entry : settingsPerNode.entrySet()) {
                inconsistency.add(entry.getKey(), entry.getValue().get(inconsistentKey));
            }
            rtn.add(inconsistency);
        }
        return rtn;
    }

    public Settings effectiveSettingsForNode(String nodeId) {
        return effectiveSettings(getNodesMap().get(nodeId).getInitialSettings());
    }

    public Settings effectiveSettings(Settings initialSettings) {
        return effectiveSettings(clusterSettings.getEffectiveSettings(), initialSettings);
    }

    public static Settings effectiveSettings(Settings nodeSettings, Settings initialSettings) {
        return ImmutableSettings.builder()
                .put(initialSettings)
                .put(nodeSettings)
                .build();
    }

    public static Settings desiredSettings(Settings fileSettings, Settings environmentSettings) {
        return ImmutableSettings.builder()
                .put(environmentSettings)
                .put(fileSettings)
                .build();
    }

    public Set<String> settingsDifference(Settings effectiveSettings, Settings desiredSettings) {
        Set<String> rtn = new HashSet<String>();
        for (Map.Entry<String, String> desiredEntry : desiredSettings.getAsMap().entrySet()) {
            String key = desiredEntry.getKey();
            String desired = desiredEntry.getValue();
            Object effective = effectiveSettings.get(key);
            if (desired == null // unspecified values should not be listed at all
                    || TransportReloadSettingsAction.RANDOM_VALUE_AT_STARTUP.equals(desired) // filter special value
                    || desired.equals(effective)) // no change
                continue;
            rtn.add(key);
        }
        return rtn;
    }

    static final class Fields {
        static final XContentBuilderString SETTINGS = new XContentBuilderString("settings");
        static final XContentBuilderString CLUSTER = new XContentBuilderString("cluster");
        static final XContentBuilderString NODES = new XContentBuilderString("nodes");
        static final XContentBuilderString EFFECTIVE = ReloadSettings.Fields.EFFECTIVE;
        static final XContentBuilderString INITIAL = ReloadSettings.Fields.INITIAL;
        static final XContentBuilderString DESIRED = ReloadSettings.Fields.DESIRED;
        static final XContentBuilderString CONSISTENCIES = new XContentBuilderString("consistencies");
        static final XContentBuilderString INCONSISTENCIES = ReloadSettings.Fields.INCONSISTENCIES;
    }

}
