package org.elasticsearch.action.reloadsettings;

import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

public class ReloadSettingsNodeService extends AbstractComponent {

    private ESLogger logger = Loggers.getLogger(ReloadSettingsNodeService.class);

    @Inject
    public ReloadSettingsNodeService(Settings settings) {
        super(settings);
    }

    public DateTime getLastFileTimestamp(Settings pSettings) {
        URL fileConf = ESInternalSettingsPerparer.getConfigurationURL(pSettings);
        if (fileConf == null || !"file".equals(fileConf.getProtocol()))
            return null;
        try {
            File conf = new File(fileConf.toURI());
            return new DateTime(conf.lastModified());
        } catch (URISyntaxException e) {
            logger.warn("Could not open configuration file", e);
            return null;
        }
    }

}
