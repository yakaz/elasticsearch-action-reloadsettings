package org.elasticsearch.action.reloadsettings;

import org.elasticsearch.action.support.nodes.NodeOperationResponse;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

public class ReloadSettings extends NodeOperationResponse implements ToXContent {

    private Settings initialSettings;

    private Settings fileSettings;

    private Settings environmentSettings;

    public ReloadSettings(DiscoveryNode node) {
        super(node);
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
        fileSettings = ImmutableSettings.readSettingsFromStream(in);
        environmentSettings = ImmutableSettings.readSettingsFromStream(in);
        initialSettings = ImmutableSettings.readSettingsFromStream(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        ImmutableSettings.writeSettingsToStream(fileSettings, out);
        ImmutableSettings.writeSettingsToStream(environmentSettings, out);
        ImmutableSettings.writeSettingsToStream(initialSettings, out);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        boolean wrapObject = params.paramAsBoolean("wrap-object", true);
        if (wrapObject)
            builder.startObject();
        builder.field("initial", initialSettings.getAsMap());
        builder.field("file", fileSettings.getAsMap());
        builder.field("environment", environmentSettings.getAsMap());
        if (wrapObject)
            builder.endObject();
        return builder;
    }



    public static class Cluster extends NodeOperationResponse implements ToXContent {

        private Settings effectiveSettings;

        private Settings transientSettings;

        private Settings persistentSettings;

        public Settings getEffectiveSettings() {
            return effectiveSettings;
        }

        public void setEffectiveSettings(Settings effectiveSettings) {
            this.effectiveSettings = effectiveSettings;
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

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            effectiveSettings = ImmutableSettings.readSettingsFromStream(in);
            transientSettings = ImmutableSettings.readSettingsFromStream(in);
            persistentSettings = ImmutableSettings.readSettingsFromStream(in);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            ImmutableSettings.writeSettingsToStream(effectiveSettings, out);
            ImmutableSettings.writeSettingsToStream(transientSettings, out);
            ImmutableSettings.writeSettingsToStream(persistentSettings, out);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
            boolean wrapObject = params.paramAsBoolean("wrap-object", true);
            if (wrapObject)
                builder.startObject();
            builder.field("effective", effectiveSettings.getAsMap());
            builder.field("transient", transientSettings.getAsMap());
            builder.field("persistent", persistentSettings.getAsMap());
            if (wrapObject)
                builder.endObject();
            return builder;
        }

    }

}
