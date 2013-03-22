package org.elasticsearch.action.reloadsettings;

import org.elasticsearch.action.support.nodes.NodeOperationResponse;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

public class ReloadSettings extends NodeOperationResponse implements ToXContent {

    // Cluster level settings

    @Nullable
    private Settings nodeSettings;

    @Nullable
    private Settings transientSettings;

    @Nullable
    private Settings persistentSettings;

    // Node level settings

    private Settings initialSettings;

    private Settings fileSettings;

    private Settings environmentSettings;

    public ReloadSettings() {
    }

    public ReloadSettings(DiscoveryNode node) {
        super(node);
    }

    public ReloadSettings(DiscoveryNode node,
                          @Nullable Settings nodeSettings,
                          @Nullable Settings transientSettings,
                          @Nullable Settings persistentSettings,
                          Settings initialSettings,
                          Settings fileSettings,
                          Settings environmentSettings) {
        super(node);
        this.nodeSettings = nodeSettings;
        this.transientSettings = transientSettings;
        this.persistentSettings = persistentSettings;
        this.fileSettings = fileSettings;
        this.environmentSettings = environmentSettings;
        this.initialSettings = initialSettings;
    }

    public Settings getNodeSettings() {
        return nodeSettings;
    }

    public void setNodeSettings(Settings nodeSettings) {
        this.nodeSettings = nodeSettings;
    }

    public Settings getTransientSettings() {
        return transientSettings;
    }

    public void setTransientSettings(Settings transientSettings) {
        this.transientSettings = transientSettings;
    }

    public Settings getPersistentSettings() {
        return persistentSettings;
    }

    public void setPersistentSettings(Settings persistentSettings) {
        this.persistentSettings = persistentSettings;
    }

    public Settings getInitialSettings() {
        return initialSettings;
    }

    public void setInitialSettings(Settings initialSettings) {
        this.initialSettings = initialSettings;
    }

    public Settings getFileSettings() {
        return fileSettings;
    }

    public void setFileSettings(Settings fileSettings) {
        this.fileSettings = fileSettings;
    }

    public Settings getEnvironmentSettings() {
        return environmentSettings;
    }

    public void setEnvironmentSettings(Settings environmentSettings) {
        this.environmentSettings = environmentSettings;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        boolean cluster = in.readBoolean();
        if (cluster) {
            nodeSettings = ImmutableSettings.readSettingsFromStream(in);
            transientSettings = ImmutableSettings.readSettingsFromStream(in);
            persistentSettings = ImmutableSettings.readSettingsFromStream(in);
        }
        fileSettings = ImmutableSettings.readSettingsFromStream(in);
        environmentSettings = ImmutableSettings.readSettingsFromStream(in);
        initialSettings = ImmutableSettings.readSettingsFromStream(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        if (nodeSettings != null) {
            out.writeBoolean(true);
            ImmutableSettings.writeSettingsToStream(nodeSettings, out);
            ImmutableSettings.writeSettingsToStream(transientSettings, out);
            ImmutableSettings.writeSettingsToStream(persistentSettings, out);
        } else {
            out.writeBoolean(false);
        }
        ImmutableSettings.writeSettingsToStream(fileSettings, out);
        ImmutableSettings.writeSettingsToStream(environmentSettings, out);
        ImmutableSettings.writeSettingsToStream(initialSettings, out);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        boolean wrapObject = params.paramAsBoolean("wrap-object", true);
        boolean cluster = params.paramAsBoolean("cluster", false);
        if (wrapObject)
            builder.startObject();
        if (cluster) {
            builder.field("node", nodeSettings.getAsMap());
            builder.field("transient", transientSettings.getAsMap());
            builder.field("persistent", persistentSettings.getAsMap());
        } else {
            builder.field("initial", initialSettings.getAsMap());
            builder.field("file", fileSettings.getAsMap());
            builder.field("environment", environmentSettings.getAsMap());
        }
        if (wrapObject)
            builder.endObject();
        return builder;
    }

}
