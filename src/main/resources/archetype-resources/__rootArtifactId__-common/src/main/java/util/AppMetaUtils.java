#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

@SuppressWarnings("unused")
public class AppMetaUtils {
    private static final Logger logger = LoggerFactory.getLogger(AppMetaUtils.class);

    private static final String PROPERTIES_FILE = "application-version.properties";
    private static final String NAME_KEY = "app.name";
    private static final String VERSION_KEY = "app.version";
    private static final String BUILD_TIMESTAMP_KEY = "app.build.timestamp";
    private static final String BUILD_NUMBER_KEY = "app.build.number";
    private static final String SCM_URL_KEY = "app.scm.url";
    private static final String URL_KEY = "app.url";

    private static String appName;
    private static String appVersion;
    private static String appBuildTimestamp;
    private static String appBuildNumber;
    private static String appScmUrl;
    private static String appUrl;

    public static String getName() {
        if (Objects.isNull(appName)) {
            synchronized (AppMetaUtils.class) {
                if (Objects.isNull(appName)) {
                    appName = getProperty(NAME_KEY);
                }
            }
        }
        return appName;
    }

    public static String getVersion() {
        if (Objects.isNull(appVersion)) {
            synchronized (AppMetaUtils.class) {
                if (Objects.isNull(appVersion)) {
                    appVersion = getProperty(VERSION_KEY);
                }
            }
        }
        return appVersion;
    }

    public static String getBuildTimestamp() {
        if (Objects.isNull(appBuildTimestamp)) {
            synchronized (AppMetaUtils.class) {
                if (Objects.isNull(appBuildTimestamp)) {
                    appBuildTimestamp = getProperty(BUILD_TIMESTAMP_KEY);
                }
            }
        }
        return appBuildTimestamp;
    }

    public static String getBuildNumber() {
        if (Objects.isNull(appBuildNumber)) {
            synchronized (AppMetaUtils.class) {
                if (Objects.isNull(appBuildNumber)) {
                    appBuildNumber = getProperty(BUILD_NUMBER_KEY);
                }
            }
        }
        return appBuildNumber;
    }

    public static String getScmUrl() {
        if (Objects.isNull(appScmUrl)) {
            synchronized (AppMetaUtils.class) {
                if (Objects.isNull(appScmUrl)) {
                    appScmUrl = getProperty(SCM_URL_KEY);
                }
            }
        }
        return appScmUrl;
    }

    public static String getUrl() {
        if (Objects.isNull(appUrl)) {
            synchronized (AppMetaUtils.class) {
                appUrl = getProperty(URL_KEY);
            }
        }
        return appUrl;
    }

    private static String getProperty(String name) {
        Properties properties = new Properties();
        try {
            properties.load(AppMetaUtils.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE));
            if (!properties.isEmpty()) {
                return properties.getProperty(name);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        logger.info(AppMetaUtils.getName());
        logger.info(AppMetaUtils.getVersion());
        logger.info(AppMetaUtils.getBuildTimestamp());
        logger.info(AppMetaUtils.getBuildNumber());
        logger.info(AppMetaUtils.getScmUrl());
        logger.info(AppMetaUtils.getUrl());
    }
}