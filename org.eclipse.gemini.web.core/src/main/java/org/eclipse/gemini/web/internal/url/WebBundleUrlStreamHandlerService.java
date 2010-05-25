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

package org.eclipse.gemini.web.internal.url;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.service.url.URLStreamHandlerService;


import org.eclipse.gemini.web.core.InstallationOptions;
import org.eclipse.gemini.web.core.WebBundleManifestTransformer;
import org.eclipse.gemini.web.internal.WebContainerUtils;
import org.eclipse.virgo.util.io.JarTransformer;
import org.eclipse.virgo.util.io.JarTransformingURLConnection;
import org.eclipse.virgo.util.io.JarTransformer.JarTransformerCallback;
import org.eclipse.virgo.util.osgi.manifest.BundleManifest;
import org.eclipse.virgo.util.osgi.manifest.BundleManifestFactory;

/**
 * {@link URLStreamHandlerService} that transforms bundles installed with the <code>war:</code> protocol.
 * <p/>
 * Transformations are applied using the {@link WebBundleManifestTransformer}.
 * 
 * @see WebBundleManifestTransformer
 */
public final class WebBundleUrlStreamHandlerService extends AbstractURLStreamHandlerService {

    private final WebBundleManifestTransformer transformer;

    public WebBundleUrlStreamHandlerService(WebBundleManifestTransformer transformer) {
        this.transformer = transformer;
    }

    @Override
    public URLConnection openConnection(URL u) throws IOException {
        WebBundleUrl url = new WebBundleUrl(u);
        URL actualUrl = new URL(url.getLocation());

        JarTransformer jarTransformer = new JarTransformer(new Callback(actualUrl, url, this.transformer));
        return new JarTransformingURLConnection(actualUrl, jarTransformer, true);
    }

    private static final class Callback implements JarTransformerCallback {

        private final WebBundleManifestTransformer transformer;

        private final URL sourceURL;

        private final WebBundleUrl webBundleUrl;

        public Callback(URL sourceURL, WebBundleUrl url, WebBundleManifestTransformer transformer) {
            this.sourceURL = sourceURL;
            this.webBundleUrl = url;
            this.transformer = transformer;
        }

        public boolean transformEntry(String entryName, InputStream is, JarOutputStream jos) throws IOException {
            if (JarFile.MANIFEST_NAME.equals(entryName)) {
                jos.putNextEntry(new ZipEntry(entryName));
                InputStreamReader reader = new InputStreamReader(is);
                BundleManifest manifest = BundleManifestFactory.createBundleManifest(reader);
                InstallationOptions options = new InstallationOptions(this.webBundleUrl.getOptions());
                if (manifest.getHeader(WebContainerUtils.HEADER_SPRINGSOURCE_DEFAULT_WAB_HEADERS) != null) {
                    options.setDefaultWABHeaders(true);
                }

                boolean webBundle = WebContainerUtils.isWebApplicationBundle(manifest);
                this.transformer.transform(manifest, sourceURL, options, webBundle);

                toManifest(manifest.toDictionary()).write(jos);
                jos.closeEntry();
                return true;
            }

            // Delete signature files. Should be generalised into another transformer type.
            return isSignatureFile(entryName);
        }

        private boolean isSignatureFile(String entryName) {
            String[] entryNameComponents = entryName.split("/");
            if (entryNameComponents.length == 2) {
                if ("META-INF".equals(entryNameComponents[0])) {
                    String entryFileName = entryNameComponents[1];
                    if (entryFileName.endsWith(".SF") || entryFileName.endsWith(".DSA") || entryFileName.endsWith(".RSA")) {
                        return true;
                    }
                }
            }
            return false;
        }

        private static Manifest toManifest(Dictionary<String, String> headers) {
            Manifest manifest = new Manifest();
            Attributes attributes = manifest.getMainAttributes();
            Enumeration<String> names = headers.keys();

            while (names.hasMoreElements()) {
                String name = names.nextElement();
                String value = headers.get(name);

                attributes.putValue(name, value);
            }
            return manifest;
        }

    }

}