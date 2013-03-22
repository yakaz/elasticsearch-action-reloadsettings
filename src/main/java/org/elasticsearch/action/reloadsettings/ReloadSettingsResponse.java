package org.elasticsearch.action.reloadsettings;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;

import java.io.IOException;

public class ReloadSettingsResponse extends ActionResponse {

    private Settings nodeSettings;
    private Settings transientSettings;
    private Settings persistentSettings;
    private Settings fileSettings;
    private Settings initialSettings;

    public Settings getNodeSettings() {
        return nodeSettings;
    }

    public void setNodeSettings(Settings nodeSettings) {
        this.nodeSettings = nodeSettings;
    }

    public Settings getTransientSettings() {
        return transientSettings;
    }

    public void setTransientSettings(Settings transientSettings) {
        this.transientSettings = transientSettings;
    }

    public Settings getPersistentSettings() {
        return persistentSettings;
    }

    public void setPersistentSettings(Settings persistentSettings) {
        this.persistentSettings = persistentSettings;
    }

    public Settings getFileSettings() {
        return fileSettings;
    }

    public void setFileSettings(Settings fileSettings) {
        this.fileSettings = fileSettings;
    }

    public Settings getInitialSettings() {
        return initialSettings;
    }

    public void setInitialSettings(Settings initialSettings) {
        this.initialSettings = initialSettings;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        nodeSettings = ImmutableSettings.readSettingsFromStream(in);
        transientSettings = ImmutableSettings.readSettingsFromStream(in);
        persistentSettings = ImmutableSettings.readSettingsFromStream(in);
        fileSettings = ImmutableSettings.readSettingsFromStream(in);
        initialSettings = ImmutableSettings.readSettingsFromStream(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        ImmutableSettings.writeSettingsToStream(nodeSettings, out);
        ImmutableSettings.writeSettingsToStream(transientSettings, out);
        ImmutableSettings.writeSettingsToStream(persistentSettings, out);
        ImmutableSettings.writeSettingsToStream(fileSettings, out);
        ImmutableSettings.writeSettingsToStream(initialSettings, out);
    }

}
