package org.elasticsearch.plugin.action.reload;

import org.elasticsearch.action.ActionModule;
import org.elasticsearch.action.reload.ReloadAction;
import org.elasticsearch.action.reload.TransportReloadAction;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;
import org.elasticsearch.rest.action.reload.RestReloadAction;

public class ReloadActionPlugin extends AbstractPlugin {

    @Override
    public String name() {
        return "action-reload";
    }

    @Override
    public String description() {
        return "Update dynamic settings through configuration reload";
    }

    @Override public void processModule(Module module) {
        if (module instanceof ActionModule) {
            ActionModule actionModule = (ActionModule) module;
            actionModule.registerAction(ReloadAction.INSTANCE, TransportReloadAction.class);
        } else if (module instanceof RestModule) {
            RestModule restModule = (RestModule) module;
            restModule.addRestAction(RestReloadAction.class);
        }
    }

}
