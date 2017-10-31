package net.distilledcode.tools.osgi;

import org.apache.felix.metatype.MetaData;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import static net.distilledcode.tools.osgi.DeclarativeServices.readComponentMetadata;
import static net.distilledcode.tools.osgi.MetaType.readMetaData;

public class MetadataDiff {

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.exit(1);
        }
        String jar1 = args[0];
        String jar2 = args[1];
        JarFile jarFile1 = new JarFile(jar1);
        JarFile jarFile2 = new JarFile(jar2);
        diff(jarFile1, jarFile2);
    }

    public static void diff(JarFile left, JarFile right) throws IOException {
        BundleMetadata leftBundleMetadata = new BundleMetadata(left);
        BundleMetadata rightBundleMetadata = new BundleMetadata(right);
        Set<String> allClasses = Sets.union(
                leftBundleMetadata.getAllClasses(),
                rightBundleMetadata.getAllClasses()
        );

        PrintWriter out = new PrintWriter(System.out);
        PrintingVisitor visitor = new PrintingVisitor(out);

        for (final String className : allClasses) {
            Comparison comparison = Comparison.create(className, leftBundleMetadata, rightBundleMetadata);
            comparison.visit(visitor);
        }

        if (!visitor.hasPrintedSomething()) {
            out.append("No differences found between ").append(left.getName()).append(" and ").append(right.getName()).println();
        }

        out.flush();
        out.close();
    }

    public static class BundleMetadata {

        private final Map<String, ComponentMetadata> declarativeServices;

        private final Map<String, MetaData> metaType;

        BundleMetadata(JarFile jarFile) throws IOException {
            declarativeServices = readComponentMetadata(jarFile);
            metaType = readMetaData(jarFile);
        }

        public Set<String> getAllClasses() {
            return Sets.union(declarativeServices.keySet(), metaType.keySet());
        }

        public ComponentMetadata getDeclarativeServices(String className) {
            return declarativeServices.get(className);
        }

        public MetaData getMetaType(String className) {
            return metaType.get(className);
        }
    }

}
