package org.elasticsearch.action.reloadsettings.inconsistencies;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Inconsistency implements ToXContent {

    private final String key;
    private final boolean updatable;
    private final Map<String, String> values;

    public Inconsistency(String key, boolean updatable) {
        this.key = key;
        this.updatable = updatable;
        this.values = new HashMap<String, String>();
    }

    public String getKey() {
        return key;
    }

    public boolean isUpdatable() {
        return updatable;
    }

    protected void add(String source, String value) {
        values.put(source, value);
    }

    public String get(String source) {
        return values.get(source);
    }

    public boolean has(String source) {
        return values.containsKey(source);
    }

    public Set<String> keySet() {
        return values.keySet();
    }

    public Set<Map.Entry<String, String>> entrySet() {
        return values.entrySet();
    }

    public Collection<String> values() {
        return values.values();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Inconsistency))
            return false;

        Inconsistency that = (Inconsistency) o;

        if (updatable != that.updatable)
            return false;
        if (key != null ? !key.equals(that.key) : that.key != null)
            return false;
        if (!values.equals(that.values))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (updatable ? 1 : 0);
        result = 31 * result + values.hashCode();
        return result;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            builder.field(entry.getKey(), entry.getValue());
        }
        builder.field(Fields.UPDATABLE, updatable);
        builder.endObject();
        return builder;
    }

    public static final class Fields {
        public static final XContentBuilderString UPDATABLE = new XContentBuilderString("_updatable");
    }

}
