package net.distilledcode.tools.osgi;

import org.junit.Test;

import java.util.jar.JarFile;

import static net.distilledcode.tools.osgi.TestUtil.getJarFile;

public class MetadataDiffTest {

    @Test // No assertions yet, mainly for fast semi-manual testing at this point
    public void testDiff() throws Exception {
        JarFile oakCore1 = getJarFile("oak-core-1.7.8.jar");
        JarFile oakCore2 = getJarFile("oak-core-1.7.9.jar");
        MetadataDiff.diff(oakCore1, oakCore2, true);
    }
}