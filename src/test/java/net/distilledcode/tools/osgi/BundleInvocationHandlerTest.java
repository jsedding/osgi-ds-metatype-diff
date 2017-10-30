package net.distilledcode.tools.osgi;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.JarFile;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class BundleInvocationHandlerTest {

    public static final String BUNDLE_FILE_NAME = "oak-core-1.7.8.jar";

    public static final String BUNDLE_RELATIVE_PATH = "target/test-classes/" + BUNDLE_FILE_NAME;

    private Bundle bundle;

    @Before
    public void setUp() throws IOException, URISyntaxException {
        JarFile jarFile = TestUtil.getJarFile(BUNDLE_FILE_NAME);
        bundle = BundleInvocationHandler.getJarFileAsBundle(jarFile);
    }
    
    @Test
    public void getLocation() throws Exception {
        assertThat("getLocation", bundle.getLocation(), endsWith(BUNDLE_RELATIVE_PATH));
    }

    @Test
    public void getExistingEntry() {
        String path = "OSGI-INF/org.apache.jackrabbit.oak.query.QueryEngineSettingsService.xml";
        URL entryUrl = bundle.getEntry(path);
        assertThat("getEntry", entryUrl, notNullValue());
        assertThat("getEntry().getProtocol()", entryUrl.getProtocol(), equalTo("jar"));
        assertThat("getEntry().getPath()", entryUrl.getPath(), endsWith(BUNDLE_RELATIVE_PATH + "!/" + path));
    }

    @Test
    public void getNonExistantEntry() {
        String path = "OSGI-INF/unavailable.xml";
        URL entryUrl = bundle.getEntry(path);
        assertThat("getEntry", entryUrl, nullValue());
    }
}
