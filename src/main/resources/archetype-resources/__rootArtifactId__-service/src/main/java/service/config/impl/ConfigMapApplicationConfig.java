#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.service.config.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import ${package}.service.config.RuntimeConfig;
import ${package}.util.YamlUtils;

/**
 * This configuration is in YAML format and can be used to override spring properties.
 */
@Service("applicationConfig")
@Slf4j
@SuppressWarnings("unused")
public class ConfigMapApplicationConfig implements RuntimeConfig {
    private static final String PROPERTIES_FILE = "application-runtime.yml";
    private static final String CONFIG_VERSION_KEY = "config.version";

    @Value("${symbol_dollar}{configmap.workdir}")
    private String workdir;
    @Autowired
    private Environment environment;
    private final AtomicLong configModifiedTime = new AtomicLong(0L);
    private final AtomicReference<String> configText = new AtomicReference<>("");
    private final AtomicReference<Properties> configProperties =
            new AtomicReference<>(new Properties());
    private final AtomicReference<Map<String, Object>> configMap =
            new AtomicReference<>(new LinkedMap<>());

    @PostConstruct
    public void init() {
        load();
        String version = configProperties.get().getProperty(CONFIG_VERSION_KEY, "0");
        log.info("Config version: {}", version);
    }

    @Override
    public String getContent() {
        return configText.get();
    }

    @Override
    public Properties getProperties() {
        return configProperties.get();
    }

    @Override
    public Map<String, Object> getMap() {
        return configMap.get();
    }

    @Override
    public <T> T getObject() {
        String text = configText.get();
        if (StringUtils.isBlank(text)) {
            return null;
        }
        return YamlUtils.toObject(text);
    }

    @Override
    public String get(String key) {
        // Prioritize fetching from the config map; if it does not exist, return the spring property.
        return configProperties.get().getProperty(key, environment.getProperty(key));
    }

    @Scheduled(fixedDelay = 5000L)
    public void load() {
        Path configPath = Paths.get(workdir, PROPERTIES_FILE);
        try {
            if (Files.exists(configPath)) {
                long lastModifiedTime = Files.getLastModifiedTime(configPath).toMillis();
                if (lastModifiedTime <= configModifiedTime.get()) {
                    // no changes
                    return;
                }

                String runtimeConf = Files.readString(configPath);
                if (StringUtils.isBlank(runtimeConf)) {
                    log.warn("Fetch a blank string from {}! Ignore it.", configPath);
                    return;
                }
                configModifiedTime.set(lastModifiedTime);
                configText.set(runtimeConf);
                configProperties.set(YamlUtils.toProperties(runtimeConf));
                configMap.set(YamlUtils.toObject(runtimeConf));
            }
        } catch (Exception e) {
            log.error("Failed to load config from {}", configPath, e);
        }
    }
}
