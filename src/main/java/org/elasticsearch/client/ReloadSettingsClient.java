package org.elasticsearch.client;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.reloadsettings.ReloadSettingsRequest;
import org.elasticsearch.action.reloadsettings.ReloadSettingsRequestBuilder;
import org.elasticsearch.action.reloadsettings.ReloadSettingsResponse;

public interface ReloadSettingsClient {

    /**
     * Reload the dynamic settings.
     *
     * @param request The reloadsettings request.
     * @param listener A listener that notifies the caller when the reloadsettings operation has completed
     */
    void reloadSettings(ReloadSettingsRequest request, ActionListener<ReloadSettingsResponse> listener);

    /**
     * Performs the same action as in {@link #reloadSettings(org.elasticsearch.action.reloadsettings.ReloadSettingsRequest,
     * org.elasticsearch.action.ActionListener)}, but works with an {@link ActionFuture} instead of a {@link ActionListener}.
     *
     * @param request The reloadsettings request
     * @return The result future
     */
    ActionFuture<ReloadSettingsResponse> reloadSettings(ReloadSettingsRequest request);

    /**
     * Prepares a reloadsettings of dynamic settings.
     *
     * @return a builder instance
     */
    ReloadSettingsRequestBuilder prepareReloadSettings();

}
