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

package org.eclipse.gemini.web.test.tomcat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Set;

import javax.servlet.ServletContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

import org.eclipse.gemini.web.core.spi.ServletContainer;
import org.eclipse.gemini.web.core.spi.WebApplicationHandle;
import org.eclipse.virgo.test.framework.OsgiTestRunner;
import org.eclipse.virgo.test.framework.TestFrameworkUtils;
import org.eclipse.virgo.util.io.PathReference;
import org.eclipse.virgo.util.io.ZipUtils;

@RunWith(OsgiTestRunner.class)
public class TomcatServletContainerTests {

    private static final String PATH_WAR_WITH_TLD_WAR = "../org.eclipse.gemini.web.test/src/test/resources/war-with-tld.war?Web-ContextPath=/war-with-tld";

    private static final String PATH_WAR_WITH_SERVLET = "../org.eclipse.gemini.web.test/src/test/resources/war-with-servlet.war?Web-ContextPath=/war-with-servlet";

    private static final String LOCATION_PREFIX = "webbundle:file:";

    private static final String LOCATION_SIMPLE_WAR = LOCATION_PREFIX
        + "../org.eclipse.gemini.web.core/src/test/resources/simple-war.war?Web-ContextPath=/simple-war";

    private static final String LOCATION_WAR_WITH_SERVLET = "webbundle:file:" + PATH_WAR_WITH_SERVLET;

    private static final String LOCATION_WAR_WITH_JSP = LOCATION_PREFIX
        + "../org.eclipse.gemini.web.test/src/test/resources/war-with-jsp.war?Web-ContextPath=/war-with-jsp";

    private static final String LOCATION_WAR_WITH_TLD = LOCATION_PREFIX + PATH_WAR_WITH_TLD_WAR;

    private static final String LOCATION_WAR_WITH_TLD_FROM_DEPENDENCY = LOCATION_PREFIX
        + "../org.eclipse.gemini.web.test/src/test/resources/war-with-tld-from-dependency.war?Web-ContextPath=/war-with-tld-from-dependency";

    private static final String LOCATION_WAR_WITH_TLD_IMPORT_SYSTEM_PACKAGES = LOCATION_PREFIX
        + "../org.eclipse.gemini.web.test/src/test/resources/war-with-tld-import-system-packages.war?Web-ContextPath=/war-with-tld-import-system-packages";

    private BundleContext bundleContext;

    private ServletContainer container;

    @Before
    public void before() throws Exception {
        this.bundleContext = TestFrameworkUtils.getBundleContextForTestClass(getClass());
        ServiceReference ref = bundleContext.getServiceReference(ServletContainer.class.getName());
        this.container = (ServletContainer) bundleContext.getService(ref);
    }

    @Test
    public void testServletContainerAvailable() {
        assertNotNull(this.container);
        try {
            new Socket("localhost", 8080);
        } catch (UnknownHostException e) {
            fail("Unable to connect");
        } catch (IOException e) {
            fail("Unable to connect");
        }
    }

    @Test
    public void testInstallSimpleWar() throws Exception {
        String location = LOCATION_SIMPLE_WAR;
        Bundle bundle = this.bundleContext.installBundle(location);
        bundle.start();

        validateNotFound("http://localhost:8080/test/index.html");

        WebApplicationHandle handle = this.container.createWebApplication("/test", bundle);
        this.container.startWebApplication(handle);
        assertNotNull(handle);

        validateURL("http://localhost:8080/test/index.html");

        this.container.stopWebApplication(handle);

        validateNotFound("http://localhost:8080/test/index.html");

    }

    @Test
    public void testWarWithServlet() throws Exception {
        String location = LOCATION_WAR_WITH_SERVLET;
        Bundle bundle = this.bundleContext.installBundle(location);
        bundle.start();

        WebApplicationHandle handle = this.container.createWebApplication("/war-with-servlet", bundle);
        this.container.startWebApplication(handle);
        try {
            validateURL("http://localhost:8080/war-with-servlet/test");
        } finally {
            this.container.stopWebApplication(handle);
        }
    }

    @Test
    public void testWarWithBasicJSP() throws Exception {
        String location = LOCATION_WAR_WITH_JSP;
        Bundle bundle = this.bundleContext.installBundle(location);
        bundle.start();

        WebApplicationHandle handle = this.container.createWebApplication("/war-with-jsp", bundle);
        this.container.startWebApplication(handle);
        try {
            validateURL("http://localhost:8080/war-with-jsp/index.jsp");
        } finally {
            this.container.stopWebApplication(handle);
        }
    }

    @Test
    public void testWarWithJSTL() throws Exception {
        testWarWithJSTL("");
    }

    private void testWarWithJSTL(String addtionalUrlSuffix) throws MalformedURLException, IOException, BundleException {
        String location = LOCATION_WAR_WITH_TLD + addtionalUrlSuffix;
        Bundle bundle = this.bundleContext.installBundle(location);
        bundle.start();

        WebApplicationHandle handle = this.container.createWebApplication("/war-with-tld", bundle);
        this.container.startWebApplication(handle);
        try {
            String realPath = handle.getServletContext().getRealPath("/");
            System.out.println(realPath);
            validateURL("http://localhost:8080/war-with-tld/test.jsp");
        } finally {
            this.container.stopWebApplication(handle);
            bundle.uninstall();
        }
    }

    @Test
    public void testWarWithJSTLFromDependency() throws MalformedURLException, IOException, BundleException {
        String jstlLocation = "file:../ivy-cache/repository/javax.servlet/com.springsource.javax.servlet.jsp.jstl/1.2.0/com.springsource.javax.servlet.jsp.jstl-1.2.0.jar";
        Bundle jstlBundle = this.bundleContext.installBundle(jstlLocation);

        Bundle bundle = this.bundleContext.installBundle(LOCATION_WAR_WITH_TLD_FROM_DEPENDENCY);
        bundle.start();

        WebApplicationHandle handle = this.container.createWebApplication("/war-with-tld-from-dependency", bundle);
        this.container.startWebApplication(handle);
        try {
            String realPath = handle.getServletContext().getRealPath("/");
            System.out.println(realPath);
            validateURL("http://localhost:8080/war-with-tld-from-dependency/test.jsp");
        } finally {
            this.container.stopWebApplication(handle);
            bundle.uninstall();
            jstlBundle.uninstall();
        }
    }

    @Test
    public void testWarWithJSTLFromExplodedDependency() throws MalformedURLException, IOException, BundleException {
        String jstlPath = "../ivy-cache/repository/javax.servlet/com.springsource.javax.servlet.jsp.jstl/1.2.0/com.springsource.javax.servlet.jsp.jstl-1.2.0.jar";
        PathReference jstl = new PathReference(jstlPath);
        PathReference unzippedJstl = explode(jstl);

        String jstlLocation = "file:" + unzippedJstl.getAbsolutePath();
        Bundle jstlBundle = this.bundleContext.installBundle(jstlLocation);

        Bundle bundle = this.bundleContext.installBundle(LOCATION_WAR_WITH_TLD_FROM_DEPENDENCY);
        bundle.start();

        WebApplicationHandle handle = this.container.createWebApplication("/war-with-tld-from-dependency", bundle);
        this.container.startWebApplication(handle);
        try {
            validateURL("http://localhost:8080/war-with-tld-from-dependency/test.jsp");
        } finally {
            this.container.stopWebApplication(handle);
            bundle.uninstall();
            jstlBundle.uninstall();
            unzippedJstl.delete(true);
        }
    }

    @Test
    public void testWarWithJSTLThatImportsSystemPackages() throws MalformedURLException, IOException, BundleException {
        String location = LOCATION_WAR_WITH_TLD_IMPORT_SYSTEM_PACKAGES;
        Bundle bundle = this.bundleContext.installBundle(location);
        bundle.start();

        WebApplicationHandle handle = this.container.createWebApplication("/war-with-tld", bundle);
        this.container.startWebApplication(handle);
        try {
            String realPath = handle.getServletContext().getRealPath("/");
            System.out.println(realPath);
            validateURL("http://localhost:8080/war-with-tld/test.jsp");
        } finally {
            this.container.stopWebApplication(handle);
            bundle.uninstall();
        }
    }

    @Test
    public void testGetRealPathWithJarBundle() throws Exception {
        String location = LOCATION_WAR_WITH_SERVLET;
        Bundle bundle = this.bundleContext.installBundle(location);
        bundle.start();

        WebApplicationHandle handle = this.container.createWebApplication("/war-with-servlet", bundle);
        this.container.startWebApplication(handle);
        try {
            ServletContext context = handle.getServletContext();
            assertNotNull(context);

            String path = context.getRealPath("/WEB-INF/web.xml");
            assertNull(path);
        } finally {
            this.container.stopWebApplication(handle);
        }
    }

    @Test
    public void testServletContextResourceLookup() throws Exception {
        String location = LOCATION_WAR_WITH_SERVLET;
        Bundle bundle = this.bundleContext.installBundle(location);
        bundle.start();

        WebApplicationHandle handle = this.container.createWebApplication("/war-with-servlet", bundle);
        this.container.startWebApplication(handle);
        try {
            ServletContext context = handle.getServletContext();
            assertNotNull(context);

            URL resource = context.getResource("/WEB-INF/web.xml");
            assertNotNull(resource);

            URLConnection connection = resource.openConnection();
            assertNotNull(connection);

            Set<?> paths = context.getResourcePaths("/WEB-INF");
            assertNotNull(paths);
            assertEquals(3, paths.size());

        } finally {
            this.container.stopWebApplication(handle);
        }
    }

    @Test
    public void rootContextPath() throws Exception {
        String location = LOCATION_WAR_WITH_SERVLET;
        Bundle bundle = this.bundleContext.installBundle(location);
        bundle.start();

        WebApplicationHandle handle = this.container.createWebApplication("", bundle);
        this.container.startWebApplication(handle);
        try {
            ServletContext context = handle.getServletContext();
            assertEquals("", context.getContextPath());
        } finally {
            this.container.stopWebApplication(handle);
        }
    }

    private void validateURL(String path) throws MalformedURLException, IOException {
        URL url = new URL(path);
        InputStream stream = url.openConnection().getInputStream();
        assertNotNull(stream);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line = null;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
    }

    private void validateNotFound(String path) throws Exception {
        URL url = new URL(path);
        try {
            url.openConnection().getInputStream();
        } catch (IOException e) {
            assertTrue("success case", true);
            return;
        }
        fail("URL '" + path + "' is still deployed");
    }

    private PathReference explode(PathReference packed) throws IOException {
        PathReference target = new PathReference("target");
        return ZipUtils.unzipTo(packed, target);
    }

    @Test
    public void testLastModified() throws Exception {
        String location = LOCATION_WAR_WITH_SERVLET;
        Bundle bundle = this.bundleContext.installBundle(location);
        bundle.start();

        WebApplicationHandle handle = this.container.createWebApplication("", bundle);
        this.container.startWebApplication(handle);
        try {
            ServletContext context = handle.getServletContext();
            long lm = context.getResource("/META-INF/").openConnection().getLastModified();
            assertTrue(lm != 0);
        } finally {
            this.container.stopWebApplication(handle);
        }
    }
}