package org.elasticsearch.action.reloadsettings;

import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

public class ReloadSettingsNodeUtil {

    private static ESLogger logger = Loggers.getLogger(ReloadSettingsNodeUtil.class);

    public static DateTime getLastFileTimestamp(Settings pSettings) {
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
