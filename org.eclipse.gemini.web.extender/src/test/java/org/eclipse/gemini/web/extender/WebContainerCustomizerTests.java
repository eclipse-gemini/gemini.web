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

package org.eclipse.gemini.web.extender;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.servlet.ServletContext;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import org.eclipse.gemini.web.core.WebApplication;
import org.eclipse.gemini.web.core.WebContainer;
import org.eclipse.gemini.web.extender.WebContainerBundleCustomizer;
import org.eclipse.virgo.teststubs.osgi.framework.StubBundle;


/**
 */
public class WebContainerCustomizerTests {
    
    private final StubBundle extenderBundle = new StubBundle();
    
    private final WebContainer container = new StubWebContainer();
    
    private final WebContainerBundleCustomizer customizer = new WebContainerBundleCustomizer(container, this.extenderBundle);

    @Test
    public void testAddWebBundle() {        
        StubBundle bundle = new StubBundle();
        bundle.addHeader("Web-ContextPath", "foo");

        Object result = customizer.addingBundle(bundle, null);
        assertTrue(result instanceof WebApplication);

        StubWebApplication wa = (StubWebApplication) result;
        assertEquals(wa.bundle, bundle);
        assertTrue(wa.started);
    }

    @Test
    public void testAddNonWebBundle() {
        StubBundle bundle = new StubBundle();

        Object result = customizer.addingBundle(bundle, null);
        assertNull(result);
    }

    @Test
    public void testRemoveWebBundle() throws BundleException {
        StubBundle bundle = new StubBundle();
        bundle.addHeader("Web-ContextPath", "foo");

        StubWebApplication wa = (StubWebApplication) container.createWebApplication(bundle, this.extenderBundle);

        customizer.removedBundle(bundle,null, wa);
        assertFalse(wa.started);
    }

    private static class StubWebContainer implements WebContainer {

        public WebApplication createWebApplication(Bundle bundle) throws BundleException {
            return new StubWebApplication(bundle);
        }
        
        public WebApplication createWebApplication(Bundle bundle, Bundle extenderBundle) throws BundleException {
            return new StubWebApplication(bundle);
        }

        public boolean isWebBundle(Bundle bundle) {
            return bundle.getHeaders().get("Web-ContextPath") != null;
        }

        public void halt() {
        }
    }

    private static class StubWebApplication implements WebApplication {

        final Bundle bundle;

        boolean started = false;

        public StubWebApplication(Bundle bundle) {
            this.bundle = bundle;
        }

        public ServletContext getServletContext() {
            return null;
        }

        public void start() {
            this.started = true;
        }

        public void stop() {
            this.started = false;
        }

        public ClassLoader getClassLoader() {
            return null;
        }
    }
}