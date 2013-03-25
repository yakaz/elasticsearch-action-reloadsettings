package org.elasticsearch.action.reloadsettings.inconsistencies;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class InconsistentSettings<E extends Inconsistency> implements ToXContent {

    private final Map<String, E> map;

    public InconsistentSettings() {
        map = new HashMap<String, E>();
    }

    public void add(E inconsistency) {
        map.put(inconsistency.getKey(), inconsistency);
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public E get(Object key) {
        return map.get(key);
    }

    public E remove(Object key) {
        return map.remove(key);
    }

    public void clear() {
        map.clear();
    }

    public Set<String> keySet() {
        return map.keySet();
    }

    public Collection<E> values() {
        return map.values();
    }

    public Set<Map.Entry<String,E>> entrySet() {
        return map.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InconsistentSettings that = (InconsistentSettings) o;

        if (!map.equals(that.map))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        for (Map.Entry<String, E> entry : map.entrySet()) {
            builder.field(entry.getKey());
            entry.getValue().toXContent(builder, params);
        }
        builder.endObject();
        return builder;
    }

}
