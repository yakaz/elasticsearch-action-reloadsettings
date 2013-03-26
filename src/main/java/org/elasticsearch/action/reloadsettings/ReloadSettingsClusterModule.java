package org.elasticsearch.action.reloadsettings;

import org.elasticsearch.common.inject.AbstractModule;

public class ReloadSettingsClusterModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ReloadSettingsClusterService.class).asEagerSingleton();
    }

}
