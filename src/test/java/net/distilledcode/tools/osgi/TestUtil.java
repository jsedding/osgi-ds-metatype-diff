package net.distilledcode.tools.osgi;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.JarFile;

public class TestUtil {
    public static JarFile getJarFile(final String name) throws URISyntaxException, IOException {
        URL url = MetadataDiffTest.class.getClassLoader().getResource(name);
        if (url == null) {
            throw new IllegalArgumentException("No jar file named '" + name + "' could be found.");
        }
        File file = new File(url.toURI());
        return new JarFile(file);
    }
}
