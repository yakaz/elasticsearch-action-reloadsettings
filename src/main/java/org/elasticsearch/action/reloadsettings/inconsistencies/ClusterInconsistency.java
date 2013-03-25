package org.elasticsearch.action.reloadsettings.inconsistencies;

public class ClusterInconsistency extends Inconsistency {

    public ClusterInconsistency(String key, boolean updatable) {
        super(key, updatable);
    }

    @Override
    public void add(String source, String value) {
        super.add(source, value);
    }

}
