/*******************************************************************************
 * Copyright (c) 2009, 2010 VMware Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution. 
 * The Eclipse Public License is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 * and the Apache License v2.0 is available at 
 *   http://www.opensource.org/licenses/apache2.0.php.
 * You may elect to redistribute this code under either of these licenses.  
 *
 * Contributors:
 *   VMware Inc. - initial contribution
 *******************************************************************************/

package org.eclipse.gemini.web.tomcat.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for loading the master Tomcat configuration.
 * <p/>
 * The location algorithm is as follows:
 * <ol>
 * <li>Check for <code>config/tomcat-server.xml</code> in the current working directory, use if found</code></li>
 * <li>Check this bundle and attached fragments for <code>/META-INF/tomcat/server.xml, use if found</code></li>
 * <li>Check this bundle for <code>/META-INF/tomcat/default-server.xml, use if found</code></li>
 * <li>Throw {@link IllegalStateException} if no configuration is found</li>
 * </ol>
 * 
 * 
 */
final class TomcatConfigLocator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TomcatConfigLocator.class);

    static final String CONFIG_PATH_FRAMEWORK_PROPERTY = "org.eclipse.gemini.web.tomcat.config.path";
    
    static final String DEFAULT_CONFIG_FILE_PATH = "config" + File.separator + "tomcat-server.xml";

    static final String CONFIG_PATH = "META-INF/tomcat";

    static final String DEFAULT_CONFIG_PATH = CONFIG_PATH + "/default-server.xml";

    static final String USER_CONFIG_PATH = "server.xml";

    public static InputStream resolveConfigFile(BundleContext context) throws BundleException {
        Bundle bundle = context.getBundle();

        InputStream is = lookupConfigInFileSystem(context);

        if (is == null) {
            is = lookupConfigInBundle(bundle);
        }
        return is;
    }

    private static InputStream lookupConfigInFileSystem(BundleContext context) {
        InputStream result = null;

        String path = context.getProperty(CONFIG_PATH_FRAMEWORK_PROPERTY);
        if(path != null) {
            result = tryGetStreamForFilePath(path);
        }
        
        if(result == null) {
            result = tryGetStreamForFilePath(DEFAULT_CONFIG_FILE_PATH);
        }
        return result;
    }

    private static InputStream tryGetStreamForFilePath(String filePath) {
        File configFile = new File(filePath);
        if (configFile.exists()) {

            try {
                FileInputStream fis = new FileInputStream(configFile);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Configuring Tomcat from file '" + configFile + "'");
                }
                return fis;
            } catch (FileNotFoundException e) {
                LOGGER.warn("Found config file on disk but then received FileNotFoundException when trying to access", e);
            }
        }
        return null;
    }

    private static InputStream lookupConfigInBundle(Bundle bundle) throws BundleException {
        URL entry = null;
        Enumeration<?> entries = bundle.findEntries(CONFIG_PATH, USER_CONFIG_PATH, false);
        if (entries != null && entries.hasMoreElements()) {
            entry = (URL) entries.nextElement();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Configuring Tomcat from fragment entry '" + entry + "'");
            }
        } else {
            entry = bundle.getEntry(DEFAULT_CONFIG_PATH);
            if (entry == null) {
                throw new IllegalStateException("Unable to locate default Tomcat configuration. Is the '" + bundle + "' bundle corrupt?");
            } else if(LOGGER.isInfoEnabled()) {
                LOGGER.info("Configuring Tomcat from default config file");
            }
        }

        try {
            return entry.openStream();
        } catch (IOException e) {
            throw new BundleException("Unable to open Tomcat configuration at '" + entry + "'");
        }
    }
}
