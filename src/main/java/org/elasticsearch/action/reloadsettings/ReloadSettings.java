package org.elasticsearch.action.reloadsettings;

import org.elasticsearch.action.reloadsettings.inconsistencies.InconsistentSettings;
import org.elasticsearch.action.reloadsettings.inconsistencies.NodeInconsistency;
import org.elasticsearch.action.support.nodes.NodeOperationResponse;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;

import java.io.IOException;

public class ReloadSettings extends NodeOperationResponse implements ToXContent {

    @Nullable
    private Settings effectiveSettings; // calculated in ReloadSettingsResponse

    private Settings initialSettings;

    @Nullable
    private Settings desiredSettings; // calculated in ReloadSettingsResponse

    private Settings fileSettings;

    private Settings environmentSettings;

    @Nullable
    private DateTime fileTimestamp;

    @Nullable
    private InconsistentSettings<NodeInconsistency> inconsistentSettings; // calculated in ReloadSettingsResponse

    public ReloadSettings(DiscoveryNode node) {
        super(node);
    }

    public Settings getEffectiveSettings() {
        return effectiveSettings;
    }

    public void setEffectiveSettings(Settings effectiveSettings) {
        this.effectiveSettings = effectiveSettings;
    }

    public Settings getInitialSettings() {
        return initialSettings;
    }

    public void setInitialSettings(Settings initialSettings) {
        this.initialSettings = initialSettings;
    }

    public Settings getDesiredSettings() {
        return desiredSettings;
    }

    public void setDesiredSettings(Settings desiredSettings) {
        this.desiredSettings = desiredSettings;
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

    public DateTime getFileTimestamp() {
        return fileTimestamp;
    }

    public void setFileTimestamp(DateTime fileTimestamp) {
        this.fileTimestamp = fileTimestamp;
    }

    public InconsistentSettings<NodeInconsistency> getInconsistentSettings() {
        return inconsistentSettings;
    }

    public void setInconsistentSettings(InconsistentSettings<NodeInconsistency> inconsistentSettings) {
        this.inconsistentSettings = inconsistentSettings;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        fileSettings = ImmutableSettings.readSettingsFromStream(in);
        environmentSettings = ImmutableSettings.readSettingsFromStream(in);
        fileTimestamp = (DateTime) in.readGenericValue();
        initialSettings = ImmutableSettings.readSettingsFromStream(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        ImmutableSettings.writeSettingsToStream(fileSettings, out);
        ImmutableSettings.writeSettingsToStream(environmentSettings, out);
        out.writeGenericValue(fileTimestamp);
        ImmutableSettings.writeSettingsToStream(initialSettings, out);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.startObject();
        if (effectiveSettings != null)
            builder.field(Fields.EFFECTIVE, effectiveSettings.getAsMap());
        builder.field(Fields.INITIAL, initialSettings.getAsMap());
        if (desiredSettings != null)
            builder.field(Fields.DESIRED, desiredSettings.getAsMap());
        builder.field(Fields.FILE, fileSettings.getAsMap());
        builder.field(Fields.ENVIRONMENT, environmentSettings.getAsMap());
        if (fileTimestamp != null) {
            builder.field(Fields.FILE_TIMESTAMP, fileTimestamp.toString(ISODateTimeFormat.dateTime()));
            builder.field(Fields.FILE_TIMESTAMP_IN_MILLIS, fileTimestamp.getMillis());
        } else {
            builder.nullField(Fields.FILE_TIMESTAMP);
            builder.nullField(Fields.FILE_TIMESTAMP_IN_MILLIS);
        }
        if (inconsistentSettings != null) {
            builder.field(Fields.INCONSISTENCIES);
            inconsistentSettings.toXContent(builder, params);
        }
        builder.endObject();
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReloadSettings that = (ReloadSettings) o;

        if (initialSettings != null ? !initialSettings.equals(that.initialSettings) : that.initialSettings != null)
            return false;
        if (fileSettings != null ? !fileSettings.equals(that.fileSettings) : that.fileSettings != null)
            return false;
        if (environmentSettings != null ? !environmentSettings.equals(that.environmentSettings) : that.environmentSettings != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = initialSettings != null ? initialSettings.hashCode() : 0;
        result = 31 * result + (fileSettings != null ? fileSettings.hashCode() : 0);
        result = 31 * result + (environmentSettings != null ? environmentSettings.hashCode() : 0);
        return result;
    }

    public static class Cluster extends NodeOperationResponse implements ToXContent {

        private Settings effectiveSettings;

        private Settings transientSettings;

        private Settings persistentSettings;

        private DateTime timestamp;
        private long version;

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

        public DateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(DateTime timestamp) {
            this.timestamp = timestamp;
        }

        public long getVersion() {
            return version;
        }

        public void setVersion(long version) {
            this.version = version;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            effectiveSettings = ImmutableSettings.readSettingsFromStream(in);
            transientSettings = ImmutableSettings.readSettingsFromStream(in);
            persistentSettings = ImmutableSettings.readSettingsFromStream(in);
            timestamp = (DateTime) in.readGenericValue();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            ImmutableSettings.writeSettingsToStream(effectiveSettings, out);
            ImmutableSettings.writeSettingsToStream(transientSettings, out);
            ImmutableSettings.writeSettingsToStream(persistentSettings, out);
            out.writeGenericValue(timestamp);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
            builder.startObject();
            builder.field(Fields.EFFECTIVE, effectiveSettings.getAsMap());
            builder.field(Fields.TRANSIENT, transientSettings.getAsMap());
            builder.field(Fields.PERSISTENT, persistentSettings.getAsMap());
            if (timestamp == null) {
                builder.nullField(Fields.TIMESTAMP);
                builder.nullField(Fields.TIMESTAMP_IN_MILLIS);
            } else {
                builder.field(Fields.TIMESTAMP, timestamp.toString(ISODateTimeFormat.dateTime()));
                builder.field(Fields.TIMESTAMP_IN_MILLIS, timestamp.getMillis());
            }
            builder.endObject();
            return builder;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Cluster cluster = (Cluster) o;

            if (effectiveSettings != null ? !effectiveSettings.equals(cluster.effectiveSettings) : cluster.effectiveSettings != null)
                return false;
            if (persistentSettings != null ? !persistentSettings.equals(cluster.persistentSettings) : cluster.persistentSettings != null)
                return false;
            if (transientSettings != null ? !transientSettings.equals(cluster.transientSettings) : cluster.transientSettings != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = effectiveSettings != null ? effectiveSettings.hashCode() : 0;
            result = 31 * result + (transientSettings != null ? transientSettings.hashCode() : 0);
            result = 31 * result + (persistentSettings != null ? persistentSettings.hashCode() : 0);
            return result;
        }

        public static final class Fields {
            public static final XContentBuilderString EFFECTIVE = ReloadSettings.Fields.EFFECTIVE;
            public static final XContentBuilderString TRANSIENT = new XContentBuilderString("transient");
            public static final XContentBuilderString PERSISTENT = new XContentBuilderString("persistent");
            public static final XContentBuilderString TIMESTAMP = new XContentBuilderString("timestamp");
            public static final XContentBuilderString TIMESTAMP_IN_MILLIS = new XContentBuilderString("timestamp_in_millis");
        }

    }

    public static final class Fields {
        public static final XContentBuilderString EFFECTIVE = new XContentBuilderString("effective");
        public static final XContentBuilderString DESIRED = new XContentBuilderString("desired");
        public static final XContentBuilderString INITIAL = new XContentBuilderString("initial");
        public static final XContentBuilderString FILE = new XContentBuilderString("file");
        public static final XContentBuilderString ENVIRONMENT = new XContentBuilderString("environment");
        public static final XContentBuilderString FILE_TIMESTAMP = new XContentBuilderString("file_timestamp");
        public static final XContentBuilderString FILE_TIMESTAMP_IN_MILLIS = new XContentBuilderString("file_timestamp_in_millis");
        public static final XContentBuilderString INCONSISTENCIES = new XContentBuilderString("inconsistencies");
    }

}
