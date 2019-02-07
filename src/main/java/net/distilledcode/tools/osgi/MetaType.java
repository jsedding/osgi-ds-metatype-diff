package net.distilledcode.tools.osgi;

import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.felix.metatype.Designate;
import org.apache.felix.metatype.MetaData;
import org.apache.felix.metatype.MetaDataReader;

public class MetaType {

    private static MetaDataReader metaDataReader = new MetaDataReader();

    public static Map<String, MetaData> readMetaData(final JarFile jarFile) {
        return findEntries(jarFile, "OSGI-INF/metatype", "xml")
                .map(toMetaData(jarFile, metaDataReader))
                .flatMap(metaData -> designatesStream(metaData)
                           .map(MetaType::getDesignatePidOrFactoryPid)
                           .map(pid -> new SimpleEntry<>(pid, metaData)))
                .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    }

    @SuppressWarnings("unchecked")
    public static Stream<Designate> designatesStream(MetaData metaData) {
        List<Designate> designates = (List<Designate>) metaData.getDesignates();
        return designates == null ? Stream.empty() : designates.stream();
    }

    public static String getDesignatePidOrFactoryPid(Designate designate) {
        String pid = designate.getPid();
        if (pid == null) {
            pid = designate.getFactoryPid();
        }
        return pid;
    }

    public static Map<String, Properties> readLocalizationProperties(final JarFile jarFile, Map<String, MetaData> metaDataMap) {
        return  metaDataMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, getProperties(jarFile)));
    }

    private static Function<JarEntry, MetaData> toMetaData(final JarFile jarFile, MetaDataReader metaDataReader) {
        return jarEntry -> {
            try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                return metaDataReader.parse(inputStream);
            } catch (IOException e) {
                throw new RuntimeException("Error handling " + jarEntry.getName() + " in '" + jarFile.getName() + "'", e);
            }
        };
    }

    // from the referenced "localization" attribute try to load the properties file
    private static Function<Map.Entry<String, MetaData>, Properties> getProperties(JarFile jarFile) {
        return entry -> {
            Properties properties = new Properties();
            String path = entry.getValue().getLocalePrefix();
            if (path != null) {
                path = path + ".properties";
                JarEntry jarEntry = jarFile.getJarEntry(path);
                if (jarEntry == null) {
                    return properties;
                }
                try (InputStream input = jarFile.getInputStream(jarEntry)) {
                    properties.load(input);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return properties;
        };
    }
   
    private static Stream<JarEntry> findEntries(final JarFile jarFile, final String path, final String extension) {
        return jarFile.stream()
                .filter(entry -> {
                    String name = entry.getName();
                    return name.startsWith(path) && name.endsWith(extension);
                });
    }
}
