package org.elasticsearch.action.reloadsettings;

import org.elasticsearch.common.inject.AbstractModule;

public class ReloadSettingsNodeModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ReloadSettingsNodeService.class).asEagerSingleton();
    }

}
