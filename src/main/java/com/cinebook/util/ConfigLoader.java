package com.cinebook.util;

import java.io.InputStream;
import java.util.Properties;

/**
 * ConfigLoader provides application-wide configuration loading capabilities.
 */
public final class ConfigLoader {

    private ConfigLoader() {
    }

    /**
     * Loads a properties file from the classpath.
     *
     * @param fileName resource file name
     * @return loaded Properties or empty Properties if not found
     */
    public static Properties loadProperties(String fileName) {
        Properties properties = new Properties();
        try (InputStream stream = ConfigLoader.class.getClassLoader().getResourceAsStream(fileName)) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (Exception e) {
            System.err.println("Warning: Unable to load " + fileName + ": " + e.getMessage());
        }
        return properties;
    }
}
