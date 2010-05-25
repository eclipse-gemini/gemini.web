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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.net.URL;
import java.util.Vector;

import org.eclipse.gemini.web.tomcat.internal.TomcatConfigLocator;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;


public class TomcatConfigLocatorTests {

    @Test
    public void testStartAndStop() throws Exception {
        Bundle mockBundle = createMock(Bundle.class);
        expect(mockBundle.findEntries(TomcatConfigLocator.CONFIG_PATH, TomcatConfigLocator.USER_CONFIG_PATH, false)).andReturn(null);
        expect(mockBundle.getEntry(TomcatConfigLocator.DEFAULT_CONFIG_PATH)).andReturn(new URL("file:src/test/resources/server.xml"));

        BundleContext mockContext = createMockBundleContext(mockBundle);

        replay(mockBundle, mockContext);

        TomcatConfigLocator.resolveConfigFile(mockContext);

        verify(mockBundle, mockContext);
    }

    @Test
    public void testStartAndStopWithUserConfig() throws Exception {
        URL url = new URL("file:src/test/resources/server.xml");
        Vector<URL> v = new Vector<URL>();
        v.add(url);
        
        Bundle mockBundle = createMock(Bundle.class);
        expect(mockBundle.findEntries(TomcatConfigLocator.CONFIG_PATH, TomcatConfigLocator.USER_CONFIG_PATH, false)).andReturn(v.elements());

        BundleContext mockContext = createMockBundleContext(mockBundle);
        replay(mockBundle, mockContext);

        TomcatConfigLocator.resolveConfigFile(mockContext);

        verify(mockBundle, mockContext);
    }

    @Test(expected = IllegalStateException.class)
    public void testStartWithNoConfig() throws Exception {
        Bundle mockBundle = createMock(Bundle.class);
        expect(mockBundle.findEntries(TomcatConfigLocator.CONFIG_PATH, TomcatConfigLocator.USER_CONFIG_PATH, false)).andReturn(null);
        expect(mockBundle.getEntry(TomcatConfigLocator.DEFAULT_CONFIG_PATH)).andReturn(null);

        BundleContext mockContext = createNiceMock(BundleContext.class);
        expect(mockContext.getBundle()).andReturn(mockBundle).anyTimes();

        replay(mockBundle, mockContext);

        TomcatConfigLocator.resolveConfigFile(mockContext);
    }

    private BundleContext createMockBundleContext(Bundle mockBundle) {
        BundleContext mockContext = createNiceMock(BundleContext.class);
        expect(mockContext.getBundle()).andReturn(mockBundle);
        return mockContext;
    }
}