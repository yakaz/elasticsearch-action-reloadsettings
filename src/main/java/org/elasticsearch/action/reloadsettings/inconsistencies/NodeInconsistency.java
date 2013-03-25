package org.elasticsearch.action.reloadsettings.inconsistencies;

import org.elasticsearch.action.reloadsettings.ReloadSettings;

public class NodeInconsistency extends Inconsistency {

    public static final String SOURCE_EFFECTIVE = ReloadSettings.Fields.EFFECTIVE.underscore().getValue();
    public static final String SOURCE_DESIRED = ReloadSettings.Fields.DESIRED.underscore().getValue();

    public NodeInconsistency(String key, boolean updatable, String effectiveValue, String desiredValue) {
        super(key, updatable);
        add(SOURCE_EFFECTIVE, effectiveValue);
        add(SOURCE_DESIRED, desiredValue);
    }

    public String getEffective() {
        return get(SOURCE_EFFECTIVE);
    }

    public String getDesired() {
        return get(SOURCE_DESIRED);
    }

}
