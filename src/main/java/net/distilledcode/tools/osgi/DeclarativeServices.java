package net.distilledcode.tools.osgi;

import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.parser.KXml2SAXParser;
import org.apache.felix.scr.impl.xml.XmlHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static net.distilledcode.tools.osgi.BundleInvocationHandler.getJarFileAsBundle;

public class DeclarativeServices {

    private static final Logger LOG = LoggerFactory.getLogger(DeclarativeServices.class);
    public static final SCRLogger SCR_LOGGER = new SCRLogger(LOG);

    private static final String SERVICE_COMPONENT = "Service-Component";

    public static Map<String, ComponentMetadata> readComponentMetadata(JarFile jarFile) throws IOException {
        String serviceComponents = jarFile.getManifest().getMainAttributes().getValue(SERVICE_COMPONENT);
        return Arrays.stream(serviceComponents.split(","))
                .map(jarFile::getEntry)
                .map(toInputStream(jarFile).andThen(toComponentMetadata(jarFile)))
                .flatMap(Collection::stream)
                .peek(m -> m.validate(SCR_LOGGER))
                .flatMap(component -> generateNames(component).map(p -> new AbstractMap.SimpleEntry<>(p, component)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Function<InputStream, List<ComponentMetadata>> toComponentMetadata(final JarFile jarFile) {
        return stream -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                XmlHandler handler = new XmlHandler(
                        getJarFileAsBundle(jarFile),
                        SCR_LOGGER,
                        false,
                        false);
                KXml2SAXParser parser;

                parser = new KXml2SAXParser(in);
                parser.parseXML(handler);
                return handler.getComponentMetadataList();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    stream.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };

    }

    private static Function<ZipEntry, InputStream> toInputStream(final ZipFile zipFile) {
        return jarEntry -> {
            try {
                return zipFile.getInputStream(jarEntry);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static Stream<? extends String> generateNames(ComponentMetadata component) {
        return component.isConfigurationPidDeclared()
                ? component.getConfigurationPid().stream()
                : Stream.of(component.getName());
    }
}
