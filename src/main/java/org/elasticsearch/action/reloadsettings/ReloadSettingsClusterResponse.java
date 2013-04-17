package org.elasticsearch.action.reloadsettings;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;

public class ReloadSettingsClusterResponse extends ActionResponse implements ToXContent {

    private long version;
    private DateTime timestamp;

    public ReloadSettingsClusterResponse() {
    }

    public ReloadSettingsClusterResponse(long version, DateTime timestamp) {
        this.version = version;
        this.timestamp = timestamp;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(DateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        version = in.readVLong();
        timestamp = (DateTime) in.readGenericValue();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeVLong(version);
        out.writeGenericValue(timestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReloadSettingsClusterResponse that = (ReloadSettingsClusterResponse) o;

        if (version != that.version) return false;
        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (version ^ (version >>> 32));
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        return result;
    }

    public String toString(boolean pretty) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            if (pretty)
                builder.prettyPrint();
            builder.startObject();
            toXContent(builder, ToXContent.EMPTY_PARAMS);
            builder.endObject();
            return builder.string();
        } catch (IOException e) {
            throw new RuntimeException("Could not build string representation of setting", e);
        }
    }

    @Override
    public String toString() {
        return toString(false);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(Fields.VERSION, version);
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

    static final class Fields {
        static final XContentBuilderString VERSION = new XContentBuilderString("version");
        static final XContentBuilderString TIMESTAMP = new XContentBuilderString("timestamp");
        static final XContentBuilderString TIMESTAMP_IN_MILLIS = new XContentBuilderString("timestamp_in_millis");
    }

}
