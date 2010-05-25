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
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;

import org.apache.tomcat.JarScannerCallback;
import org.eclipse.gemini.web.tomcat.internal.BundleDependenciesJarScanner;
import org.eclipse.gemini.web.tomcat.internal.loading.BundleWebappClassLoader;
import org.eclipse.gemini.web.tomcat.internal.support.BundleDependencyDeterminer;
import org.eclipse.gemini.web.tomcat.internal.support.BundleFileResolver;
import org.eclipse.gemini.web.tomcat.spi.ClassLoaderCustomizer;
import org.junit.Test;
import org.osgi.framework.Bundle;



/**
 */
public class BundleDependenciesJarScannerTests {
    
    private final BundleDependencyDeterminer dependencyDeterminer = createMock(BundleDependencyDeterminer.class);
    
    private final BundleFileResolver bundleFileResolver = createMock(BundleFileResolver.class);
    
    private final BundleDependenciesJarScanner scanner = new BundleDependenciesJarScanner(dependencyDeterminer, bundleFileResolver);
    
    private final Bundle bundle = createMock(Bundle.class);        
    
    private final JarScannerCallback callback = createMock(JarScannerCallback.class);
    
    private final ClassLoaderCustomizer classLoaderCustomizer = createNiceMock(ClassLoaderCustomizer.class);
    
    private final Bundle dependency = createMock(Bundle.class);
    
    @Test
    @SuppressWarnings("unchecked")
    public void noDependencies() {
        expect(bundle.getHeaders()).andReturn(new Hashtable());
        expect(dependencyDeterminer.getDependencies(this.bundle)).andReturn(Collections.<Bundle>emptySet());
        
        replay(dependencyDeterminer, bundleFileResolver, bundle, callback);
        
        ClassLoader classLoader = new BundleWebappClassLoader(this.bundle, this.classLoaderCustomizer);
        
        scanner.scan(null, classLoader, callback, null);
        
        verify(dependencyDeterminer, bundleFileResolver, bundle, callback);
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void scanDirectory() throws IOException {
        expect(bundle.getHeaders()).andReturn(new Hashtable());
        expect(dependencyDeterminer.getDependencies(this.bundle)).andReturn(new HashSet<Bundle>(Arrays.asList(this.dependency)));
        
        File dependencyFile = new File("src/test/resources");        
        expect(this.bundleFileResolver.resolve(this.dependency)).andReturn(dependencyFile);
        this.callback.scan(dependencyFile);
        
        replay(dependencyDeterminer, bundleFileResolver, bundle, callback);
        
        ClassLoader classLoader = new BundleWebappClassLoader(this.bundle, this.classLoaderCustomizer);
        
        scanner.scan(null, classLoader, callback, null);
        
        verify(dependencyDeterminer, bundleFileResolver, bundle, callback);
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void scanFile() throws IOException {
        expect(bundle.getHeaders()).andReturn(new Hashtable());
        expect(dependencyDeterminer.getDependencies(this.bundle)).andReturn(new HashSet<Bundle>(Arrays.asList(this.dependency)));
        
        File dependencyFile = new File("");        
        expect(this.bundleFileResolver.resolve(this.dependency)).andReturn(dependencyFile);
        this.callback.scan(isA(JarURLConnection.class));
        
        replay(dependencyDeterminer, bundleFileResolver, bundle, callback);
        
        ClassLoader classLoader = new BundleWebappClassLoader(this.bundle, this.classLoaderCustomizer);
        
        scanner.scan(null, classLoader, callback, null);
        
        verify(dependencyDeterminer, bundleFileResolver, bundle, callback);
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void scanJarUrlConnection() throws IOException {
        expect(bundle.getHeaders()).andReturn(new Hashtable());
        expect(dependencyDeterminer.getDependencies(this.bundle)).andReturn(new HashSet<Bundle>(Arrays.asList(this.dependency)));
        expect(dependency.getLocation()).andReturn("file:src/test/resources/bundle.jar").anyTimes();
            
        expect(this.bundleFileResolver.resolve(this.dependency)).andReturn(null);        
        this.callback.scan(isA(JarURLConnection.class));
        
        replay(dependencyDeterminer, bundleFileResolver, bundle, callback, dependency);
        
        ClassLoader classLoader = new BundleWebappClassLoader(this.bundle, this.classLoaderCustomizer);
        
        scanner.scan(null, classLoader, callback, null);
        
        verify(dependencyDeterminer, bundleFileResolver, bundle, callback);
    }
}