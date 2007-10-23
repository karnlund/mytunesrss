/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.catalina.startup;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;


/**
 * Utility class to read the bootstrap Catalina configuration.
 *
 * @author Remy Maucherat
 * @version $Revision: 466608 $ $Date: 2006-10-21 17:10:15 -0600 (Sat, 21 Oct 2006) $
 */

public class CatalinaProperties {


    // ------------------------------------------------------- Static Variables

    private static org.apache.commons.logging.Log log=
        org.apache.commons.logging.LogFactory.getLog( CatalinaProperties.class );

    private static Properties properties = null;


    static {

        loadProperties();

    }


    // --------------------------------------------------------- Public Methods


    /**
     * Return specified property value.
     */
    public static String getProperty(String name) {

        return properties.getProperty(name);

    }


    /**
     * Return specified property value.
     */
    public static String getProperty(String name, String defaultValue) {

        return properties.getProperty(name, defaultValue);

    }


    // --------------------------------------------------------- Public Methods


    /**
     * Load properties.
     */
    private static void loadProperties() {

        InputStream is = null;
        Throwable error = null;

        try {
          if (log.isDebugEnabled()) {
            log.debug("trying config url");
          }
            String configUrl = getConfigUrl();
            if (configUrl != null) {
                is = (new URL(configUrl)).openStream();
            }
        } catch (Throwable t) {
          if (log.isErrorEnabled()) {
            log.error(t);
          }
        }

        if (is == null) {
            try {
              if (log.isDebugEnabled()) {
                log.debug("trying conf dir");
              }
                File home = new File(getCatalinaBase());
                File conf = new File(home, "conf");
                File properties = new File(conf, "catalina.properties");
                is = new FileInputStream(properties);
            } catch (Throwable t) {
              if (log.isErrorEnabled()) {
                log.error(t);
              }
            }
        }

        if (is == null) {
            try {
              if (log.isDebugEnabled()) {
                log.debug("trying from jar");
              }
                is = CatalinaProperties.class.getResourceAsStream
                    ("/org/apache/catalina/startup/catalina.properties");
            } catch (Throwable t) {
              if (log.isErrorEnabled()) {
                log.error(t);
              }
            }
        }

        if (is != null) {
            try {
              if (log.isDebugEnabled()) {
                log.debug("loading props");
              }
                properties = new Properties();
                properties.load(is);
                is.close();
            } catch (Throwable t) {
              if (log.isErrorEnabled()) {
                log.error(t);
              }
                error = t;
            }
        }

        if ((is == null) || (error != null)) {
            // Do something
            log.warn("Failed to load catalina.properties", error);
            // That's fine - we have reasonable defaults.
            properties=new Properties();
        }

        // Register the properties as system properties
        Enumeration enumeration = properties.propertyNames();
        while (enumeration.hasMoreElements()) {
            String name = (String) enumeration.nextElement();
            String value = properties.getProperty(name);
            if (value != null) {
                System.setProperty(name, value);
            }
        }

    }


    /**
     * Get the value of the catalina.home environment variable.
     */
    private static String getCatalinaHome() {
        return System.getProperty("catalina.home",
                                  System.getProperty("user.dir"));
    }


    /**
     * Get the value of the catalina.base environment variable.
     */
    private static String getCatalinaBase() {
        return System.getProperty("catalina.base", getCatalinaHome());
    }


    /**
     * Get the value of the configuration URL.
     */
    private static String getConfigUrl() {
        return System.getProperty("catalina.config");
    }


}
