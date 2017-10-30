package net.distilledcode.tools.osgi;

import org.apache.felix.metatype.Designate;
import org.apache.felix.metatype.MetaData;
import org.apache.felix.metatype.MetaDataReader;
import org.apache.felix.metatype.OCD;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MetaType {

    private static MetaDataReader metaDataReader = new MetaDataReader();

    @SuppressWarnings("unchecked")
    public static Map<String, MetaData> readMetaData(final JarFile jarFile) {
        return findEntries(jarFile, "OSGI-INF/metatype", "xml")
                .map(toInputStream(jarFile).andThen(toMetaData(metaDataReader)))
                .collect(Collectors.toMap(metaData -> {
                    List<Designate> designates = (List<Designate>)metaData.getDesignates();
                    if (designates == null) {
                        return ((Map<String, OCD>)metaData.getObjectClassDefinitions()).keySet().iterator().next();
                    } else if (designates.size() == 1) {
                        Designate designate = designates.get(0);
                        String pid = designate.getPid();
                        if (pid == null) {
                            pid = designate.getFactoryPid();
                        }
                        return pid;
                    } else {
                        throw new RuntimeException("Too many designates " + designates.size());
                    }
                }, Function.identity()));
    }

    private static Function<InputStream, MetaData> toMetaData(final MetaDataReader metaDataReader) {
        return inputStream -> {
            try {
                return metaDataReader.parse(inputStream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private static Stream<JarEntry> findEntries(final JarFile jarFile, final String path, final String extension) {
        return jarFile.stream()
                .filter(entry -> {
                    String name = entry.getName();
                    return name.startsWith(path) && name.endsWith(extension);
                });
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
}
