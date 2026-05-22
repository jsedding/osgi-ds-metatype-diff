package net.distilledcode.tools.osgi;

import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.xml.XmlHandler;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.distilledcode.tools.osgi.InvocationHandlers.getJarFileAsBundle;

public class DeclarativeServices {

    private static final Logger LOG = LoggerFactory.getLogger(DeclarativeServices.class);

    private static final String SERVICE_COMPONENT = "Service-Component";

    public static Map<String, ComponentMetadata> readComponentMetadata(JarFile jarFile) throws IOException {
        Manifest manifest = jarFile.getManifest();
        if (manifest == null) {
            throw new IOException("Jar at " + jarFile.getName() + " does not contain the mandatory manifest file");
        }
        String serviceComponents = manifest.getMainAttributes().getValue(SERVICE_COMPONENT);
        if (serviceComponents == null) {
            LOG.warn("The bundle '{}' does not contain a 'Service-Component' header in its manifest. Therefore no comparison can be performed.", jarFile.getName());
            return Collections.emptyMap();
        }
        return Arrays.stream(serviceComponents.split(","))
                .map(jarFile::getJarEntry)
                .map(toComponentMetadata(jarFile))
                .filter(Predicate.isEqual(null).negate())
                .flatMap(Collection::stream)
                .peek(ComponentMetadata::validate)
                .flatMap(component -> generateNames(component).map(p -> new AbstractMap.SimpleEntry<>(p, component)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @CheckForNull
    private static Function<JarEntry, List<ComponentMetadata>> toComponentMetadata(final JarFile jarFile) {
        return jarEntry -> {
            try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                Bundle bundle = getJarFileAsBundle(jarFile);
                BundleContext bundleContext = bundle.getBundleContext();
                XmlHandler handler = new XmlHandler(
                        bundle,
                        new SCRLogger(LOG),
                        false,
                        false,
                        null);
                final SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setNamespaceAware(true);
                final SAXParser parser = factory.newSAXParser();
                parser.parse(inputStream, handler);
                return handler.getComponentMetadataList();
            } catch (Exception e) {
                LOG.warn("Error parsing '{}' in '{}'", jarEntry.getName(), jarFile.getName(), e);
                return null;
            }
        };
    }

    private static Stream<? extends String> generateNames(ComponentMetadata component) {
        return component.isConfigurationPidDeclared()
                ? component.getConfigurationPid().stream()
                : Stream.of(component.getName());
    }
}
