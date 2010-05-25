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
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.tomcat.JarScanner;
import org.apache.tomcat.JarScannerCallback;
import org.eclipse.gemini.web.tomcat.internal.loading.BundleWebappClassLoader;
import org.eclipse.gemini.web.tomcat.internal.support.BundleDependencyDeterminer;
import org.eclipse.gemini.web.tomcat.internal.support.BundleFileResolver;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A <code>JarScanner</code> implementation that passes each of the
 * {@link Bundle}'s dependencies to the {@link JarScannerCallback}.
 * 
 * <p />
 *
 * <strong>Concurrent Semantics</strong><br />
 *
 * Thread-safe.
 *
 */
final class BundleDependenciesJarScanner implements JarScanner {
    
    private static final String JAR_URL_SUFFIX = "!/";

    private static final String JAR_URL_PREFIX = "jar:";    
    
    private static final Logger LOGGER = LoggerFactory.getLogger(BundleDependenciesJarScanner.class);
    
    private final BundleDependencyDeterminer bundleDependencyDeterminer;
    
    private final BundleFileResolver bundleFileResolver;
    
    public BundleDependenciesJarScanner(BundleDependencyDeterminer bundleDependencyDeterminer, BundleFileResolver bundleFileResolver) {
        this.bundleDependencyDeterminer = bundleDependencyDeterminer;
        this.bundleFileResolver = bundleFileResolver;
    }

    public void scan(ServletContext context, ClassLoader classLoader, JarScannerCallback callback, Set<String> jarsToSkip) {
        if (classLoader instanceof BundleWebappClassLoader) {
            Bundle bundle = ((BundleWebappClassLoader)classLoader).getBundle();
            scanDependentBundles(bundle, callback);
        }
    }

    private void scanDependentBundles(Bundle rootBundle, JarScannerCallback callback) { 
        Set<Bundle> dependencies = this.bundleDependencyDeterminer.getDependencies(rootBundle);
        
        for (Bundle bundle : dependencies) {
            scanBundle(bundle, callback);
        }
    }
    
    private void scanBundle(Bundle bundle, JarScannerCallback callback) {
        File bundleFile = this.bundleFileResolver.resolve(bundle);
        if (bundleFile != null) {
            scanBundleFile(bundleFile, callback);
        } else {
            scanJarUrlConnection(bundle, callback);
        }
    }
    
    private void scanJarUrlConnection(Bundle bundle, JarScannerCallback callback) {
        URL bundleUrl;
        try {
            bundleUrl = new URL(JAR_URL_PREFIX + bundle.getLocation() + JAR_URL_SUFFIX);
        } catch (MalformedURLException e) {
            LOGGER.warn("Failed to create jar: url for bundle location " + bundle.getLocation());
            return;
        }

        scanBundleUrl(bundleUrl, callback); 
    }   
    
    private void scanBundleFile(File bundleFile, JarScannerCallback callback) {
        if (bundleFile.isDirectory()) {
            try {
                callback.scan(bundleFile);
            } catch (IOException e) {
                LOGGER.warn("Failure when attempting to scan bundle file '" + bundleFile + "'.", e);
            }
        } else {
            URL bundleUrl;
            try {
                bundleUrl = new URL(JAR_URL_PREFIX + bundleFile.toURI().toURL() + JAR_URL_SUFFIX);
            } catch (MalformedURLException e) {
                LOGGER.warn("Failed to create jar: url for bundle file " + bundleFile);
                return;
            }
            scanBundleUrl(bundleUrl, callback);
        }
    }
    
    private void scanBundleUrl(URL url, JarScannerCallback callback) {
        try {
            URLConnection connection = url.openConnection();
            
            if (connection instanceof JarURLConnection) {
                callback.scan((JarURLConnection)connection);
            }       
        } catch (IOException e) {
            LOGGER.warn("Failure when attempting to scan bundle via jar URL '" + url + "'.", e);
        }
    }
}