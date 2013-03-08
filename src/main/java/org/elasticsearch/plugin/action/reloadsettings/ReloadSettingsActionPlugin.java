package org.elasticsearch.plugin.action.reloadsettings;

import org.elasticsearch.action.ActionModule;
import org.elasticsearch.action.reloadsettings.ReloadSettingsAction;
import org.elasticsearch.action.reloadsettings.TransportReloadSettingsAction;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;
import org.elasticsearch.rest.action.reloadsettings.RestReloadSettingsAction;

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
        } else if (module instanceof RestModule) {
            RestModule restModule = (RestModule) module;
            restModule.addRestAction(RestReloadSettingsAction.class);
        }
    }

}
