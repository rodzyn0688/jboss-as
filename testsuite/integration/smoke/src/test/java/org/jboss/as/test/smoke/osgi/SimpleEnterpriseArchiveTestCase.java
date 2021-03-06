/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.smoke.osgi;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.servlet.Servlet;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import junit.framework.Assert;

import org.apache.commons.collections.bag.HashBag;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.smoke.osgi.bundleA.CollectionsServlet;
import org.jboss.as.test.smoke.osgi.bundleA.SimpleServlet;
import org.jboss.as.test.smoke.osgi.bundleB.Echo;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.osgi.spi.ManifestBuilder;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleReference;

/**
 * Test OSGi bundles contained in EARs
 *
 * @author thomas.diesler@jboss.com
 * @since 07-Jun-2011
 */
@RunAsClient
@RunWith(Arquillian.class)
public class SimpleEnterpriseArchiveTestCase {

    private static final String SIMPLE_EAR = "simple.ear";
    private static final String SIMPLE_WAR = "simple.war";

    private static final String WAR_STRUCTURE_EAR = "war-structure.ear";
    private static final String WAR_STRUCTURE_BUNDLE = "war-structure-bundle.war";

    private static final String LIB_COLLECTIONS_EAR = "lib-collections.ear";
    private static final String LIB_COLLECTIONS_WAR = "lib-collections.war";

    private static final String BUNDLE_COLLECTIONS_EAR = "bundle-collections.ear";
    private static final String BUNDLE_COLLECTIONS_WAR = "bundle-collections.war";

    private static final String ECHO_BUNDLE = "echo-bundle.jar";

    @ArquillianResource
    URL targetURL;

    @Deployment(name = SIMPLE_EAR, testable = false)
    public static Archive<?> getSimpleEar() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, SIMPLE_WAR);
        war.addClasses(SimpleServlet.class, Echo.class);
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, SIMPLE_EAR);
        ear.addAsModule(war);
        return ear;
    }

    @Deployment(name = WAR_STRUCTURE_EAR, testable = false)
    public static Archive<?> getWarStructureEar() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, WAR_STRUCTURE_BUNDLE);
        war.addClasses(SimpleServlet.class);
        war.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(war.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(PostConstruct.class, WebServlet.class);
                builder.addImportPackages(Servlet.class, HttpServlet.class);
                builder.addImportPackages(Echo.class);
                builder.addBundleClasspath("WEB-INF/classes");
                return builder.openStream();
            }
        });

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ECHO_BUNDLE);
        jar.addClasses(Echo.class);
        jar.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(jar.getName());
                builder.addBundleManifestVersion(2);
                builder.addExportPackages(Echo.class);
                return builder.openStream();
            }
        });

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, WAR_STRUCTURE_EAR);
        ear.add(jar, "/", ZipExporter.class);
        ear.addAsModule(war);
        return ear;
    }

    @Deployment(name = LIB_COLLECTIONS_EAR, testable = false)
    public static Archive<?> getLibCollectionsEar() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, LIB_COLLECTIONS_WAR);
        war.addClasses(CollectionsServlet.class);
        war.setManifest(new Asset() {
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                builder.addManifestHeader("Dependencies", "org.jboss.modules,org.osgi.core");
                return builder.openStream();
            }
        });
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, LIB_COLLECTIONS_EAR);
        // Add commons-collections.jar as library
        ear.addAsLibrary(getLibrary("commons-collections.jar"));
        ear.addAsModule(war);
        return ear;
    }

    @Deployment(name = BUNDLE_COLLECTIONS_EAR, testable = false)
    public static Archive<?> getBundleCollectionsEar() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, BUNDLE_COLLECTIONS_WAR);
        war.addClasses(CollectionsServlet.class);
        war.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(war.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(PostConstruct.class, WebServlet.class);
                builder.addImportPackages(Servlet.class, HttpServlet.class);
                builder.addImportPackages(ModuleClassLoader.class, BundleReference.class);
                builder.addImportPackages(HashBag.class);
                builder.addBundleClasspath("WEB-INF/classes");
                return builder.openStream();
            }
        });
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, BUNDLE_COLLECTIONS_EAR);
        // Add commons-collections.jar as bundle (for sub deployment)
        ear.addAsModule(getLibrary("commons-collections.jar"));
        ear.addAsModule(war);
        return ear;
    }

    @Test
    @OperateOnDeployment(SIMPLE_EAR)
    public void testSimpleEar() throws Exception {
        String result = performCall("simple", "Hello");
        Assert.assertEquals("Simple Servlet called with input=Hello", result);
    }

    @Test
    @OperateOnDeployment(WAR_STRUCTURE_EAR)
    public void testWarStructureEar() throws Exception {
        String result = performCall("simple", "Hello");
        Assert.assertEquals("Simple Servlet called with input=Hello", result);
    }

    @Test
    @OperateOnDeployment(LIB_COLLECTIONS_EAR)
    public void testLibCollectionsEar() throws Exception {
        String result = performCall("collections", null);
        Assert.assertEquals("HashBag loaded from deployment.lib-collections.ear:main", result);
    }

    @Test
    @OperateOnDeployment(BUNDLE_COLLECTIONS_EAR)
    public void testBundleCollectionsEar() throws Exception {
        String result = performCall("collections", null);
        Assert.assertEquals("HashBag loaded from org.apache.commons.collections:3.2.1", result);
    }

    private String performCall(String pattern, String param) throws Exception {
        String urlspec = targetURL.toExternalForm();
        URL url = new URL(urlspec + pattern + (param != null ? "?input=" + param : ""));
        return HttpRequest.get(url.toExternalForm(), 10, TimeUnit.SECONDS);
    }

    private static File getLibrary(String filename) {
        String targetdir = System.getProperty("basedir") + File.separatorChar + "target";
        String testdir = targetdir + File.separatorChar + "test-libs";
        File library = new File(testdir, filename);
        Assert.assertTrue("Library exists: " + library, library.exists());
        return library;
    }
}
