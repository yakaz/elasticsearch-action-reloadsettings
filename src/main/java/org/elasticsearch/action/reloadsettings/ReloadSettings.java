package org.elasticsearch.action.reloadsettings;

import org.elasticsearch.action.support.nodes.NodeOperationResponse;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;

import java.io.IOException;

public class ReloadSettings extends NodeOperationResponse implements ToXContent {

    public static final String TOXCONTENT_PARAM_WRAP_OBJECT = "wrap_object";

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
        boolean wrapObject = params.paramAsBoolean(TOXCONTENT_PARAM_WRAP_OBJECT, true);
        if (wrapObject)
            builder.startObject();
        builder.field(Fields.INITIAL, initialSettings.getAsMap());
        builder.field(Fields.FILE, fileSettings.getAsMap());
        builder.field(Fields.ENVIRONMENT, environmentSettings.getAsMap());
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
            boolean wrapObject = params.paramAsBoolean(TOXCONTENT_PARAM_WRAP_OBJECT, true);
            if (wrapObject)
                builder.startObject();
            builder.field(Fields.EFFECTIVE, effectiveSettings.getAsMap());
            builder.field(Fields.TRANSIENT, transientSettings.getAsMap());
            builder.field(Fields.PERSISTENT, persistentSettings.getAsMap());
            if (wrapObject)
                builder.endObject();
            return builder;
        }

    }

    static final class Fields {
        static final XContentBuilderString EFFECTIVE = new XContentBuilderString("effective");
        static final XContentBuilderString TRANSIENT = new XContentBuilderString("transient");
        static final XContentBuilderString PERSISTENT = new XContentBuilderString("persistent");
        static final XContentBuilderString INITIAL = new XContentBuilderString("initial");
        static final XContentBuilderString FILE = new XContentBuilderString("file");
        static final XContentBuilderString ENVIRONMENT = new XContentBuilderString("environment");
    }

}
