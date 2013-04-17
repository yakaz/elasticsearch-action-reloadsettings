package org.elasticsearch.plugin.action.reloadsettings;

import org.elasticsearch.action.ActionModule;
import org.elasticsearch.action.reloadsettings.ReloadSettingsAction;
import org.elasticsearch.action.reloadsettings.ReloadSettingsClusterAction;
import org.elasticsearch.action.reloadsettings.ReloadSettingsClusterModule;
import org.elasticsearch.action.reloadsettings.ReloadSettingsClusterService;
import org.elasticsearch.action.reloadsettings.TransportReloadSettingsAction;
import org.elasticsearch.action.reloadsettings.TransportReloadSettingsClusterAction;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;
import org.elasticsearch.rest.action.reloadsettings.RestReloadSettingsAction;

import java.util.Collection;

public class ReloadSettingsActionPlugin extends AbstractPlugin {

    @Override
    public String name() {
        return "action-reloadsettings";
    }

    @Override
    public String description() {
        return "Update dynamic settings through configuration reloadsettings";
    }

    @Override public void processModule(Module module) {
        if (module instanceof ActionModule) {
            ActionModule actionModule = (ActionModule) module;
            actionModule.registerAction(ReloadSettingsAction.INSTANCE, TransportReloadSettingsAction.class);
            actionModule.registerAction(ReloadSettingsClusterAction.INSTANCE, TransportReloadSettingsClusterAction.class);
        } else if (module instanceof RestModule) {
            RestModule restModule = (RestModule) module;
            restModule.addRestAction(RestReloadSettingsAction.class);
        }
    }

    @Override
    public Collection<Class<? extends Module>> modules() {
        Collection<Class<? extends Module>> services = Lists.newArrayList();
        services.add(ReloadSettingsClusterModule.class);
        return services;
    }

    @Override
    public Collection<Class<? extends LifecycleComponent>> services() {
        Collection<Class<? extends LifecycleComponent>> services = Lists.newArrayList();
        services.add(ReloadSettingsClusterService.class);
        return services;
    }

}
