package net.distilledcode.tools.osgi;

import org.apache.felix.metatype.MetaData;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.jar.JarFile;

import static java.util.Collections.emptySet;
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
            Comparison comparison = Comparison.compute(className, leftBundleMetadata, rightBundleMetadata);
            comparison.visit(visitor);
        }

        if (!visitor.hasPrintedSomething()) {
            out.append("No differences found between ").append(left.getName()).append(" and ").append(right.getName()).println();
        }

        out.flush();
        out.close();
    }

    private static class BundleMetadata {

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

    private static class Comparison {

        private interface Visitor {
            void enter(String sectionName);
            void leave(String sectionName);
            void added(String name, Object value);
            void changed(String name, Object leftValue, Object rightValue);
            void removed(String name, Object value);
        }

        private String className;
        private final ComponentMetadata leftDS;
        private final ComponentMetadata rightDS;
        private final MetaData leftMT;
        private final MetaData rightMT;

        private Comparison(final String className, final ComponentMetadata leftDS, final ComponentMetadata rightDS, final MetaData leftMT, final MetaData rightMT) {
            this.className = className;
            this.leftDS = leftDS;
            this.rightDS = rightDS;
            this.leftMT = leftMT;
            this.rightMT = rightMT;
        }

        public static Comparison compute(final String className, final BundleMetadata left, final BundleMetadata right) {
            return new Comparison(className,
                    left.getDeclarativeServices(className),
                    right.getDeclarativeServices(className),
                    left.getMetaType(className),
                    right.getMetaType(className));
        }

        public void visit(Visitor visitor) {
            visitor.enter(className);
            visitDS(visitor);
            // TODO - visitMetaType
            visitor.leave(className);
        }

        private void visitDS(final Visitor visitor) {
            visitor.enter("Declarative Services");
            visitAttributes(visitor);
            visitDSProperties(visitor, valueOrNull(leftDS, ComponentMetadata::getProperties), valueOrNull(rightDS, ComponentMetadata::getProperties));
            // TODO
            // - ComponentMetadata::getDependencies
            // - ComponentMetadata::getServiceMetadata
            visitor.leave("Declarative Services");
        }

        private void visitAttributes(final Visitor visitor) {
            visitor.enter("Attributes");
            // visitValue(visitor, "dsVersion", ComponentMetadata::getDSVersion, leftDS, rightDS);
            visitValue(visitor, "name", ComponentMetadata::getName, leftDS, rightDS);
            visitValue(visitor, "enabled", ComponentMetadata::isEnabled, leftDS, rightDS);
            visitValue(visitor, "configurationPid", ComponentMetadata::getConfigurationPid, leftDS, rightDS);
            visitValue(visitor, "configurationPolicy", ComponentMetadata::getConfigurationPolicy, leftDS, rightDS);
            visitValue(visitor, "activate", ComponentMetadata::getActivate, leftDS, rightDS);
            visitValue(visitor, "modified", ComponentMetadata::getModified, leftDS, rightDS);
            visitValue(visitor, "deactivate", ComponentMetadata::getDeactivate, leftDS, rightDS);
            visitValue(visitor, "factoryIdentifier", ComponentMetadata::getFactoryIdentifier, leftDS, rightDS);
            visitValue(visitor, "serviceScope", ComponentMetadata::getServiceScope, leftDS, rightDS);
            visitor.leave("Attributes");
        }

        private void visitValue(final Visitor visitor, String name, final Function<ComponentMetadata, Object> fn, final ComponentMetadata leftDS, final ComponentMetadata rightDS) {
            visitValue(visitor, name, valueOrNull(leftDS, fn), valueOrNull(rightDS, fn));
        }

        @SuppressWarnings("unchecked")
        private <T extends Comparable> void visitValue(final Visitor visitor, final String name, final Collection<T> leftValue, final Collection<T> rightValue) {
            List<T> left = new ArrayList<>(leftValue);
            Collections.sort(left);

            List<T> right = new ArrayList<>(rightValue);
            Collections.sort(right);

            if (!left.equals(right)) {
                visitValue(visitor, name, Arrays.toString(left.toArray()), Arrays.toString(right.toArray()));
            }
        }

        private <T> void visitValue(final Visitor visitor, final String name, final T leftValue, final T rightValue) {
            if (leftValue == null && rightValue == null) {
                return;
            }

            if (leftValue != null && rightValue == null) {
                visitor.removed(name, leftValue);
            } else if (leftValue != null && !leftValue.equals(rightValue)) {
                visitor.changed(name, leftValue, rightValue);
            } else if (leftValue == null) {
                visitor.added(name, rightValue);
            }
        }

        private void visitDSProperties(final Visitor visitor, final Map<String, Object> left, final Map<String, Object> right) {
            visitor.enter("Properties");
            visitValue(visitor, left, right);
            visitor.leave("Properties");
        }

        private void visitValue(final Visitor visitor, final Map<String, Object> left, final Map<String, Object> right) {
            Set<String> union = Sets.union(valueOrDefault(left, Map::keySet, emptySet()), valueOrDefault(right, Map::keySet, emptySet()));
            for (final String key : union) {
                Object leftValue = valueOrNull(left, m -> m.get(key));
                Object rightValue = valueOrNull(right, m -> m.get(key));
                visitValue(visitor, key, leftValue, rightValue);
            }
        }

        private <S, T> T valueOrNull(final S object, final Function<S, T> fn) {
            return object != null ? fn.apply(object) : null;
        }

        private <S, T> T valueOrDefault(final S object, final Function<S, T> fn, T defaultValue) {
            T value = valueOrNull(object, fn);
            return value != null ? value : defaultValue;
        }
    }

    private static class PrintingVisitor implements Comparison.Visitor {

        private static String INDENTATION_WHITESPACE = "                                                ";

        private boolean hasPrintedSomething;

        private Stack<String> sections = new Stack<>();

        private Stack<String> printedSections = new Stack<>();

        private PrintWriter out;

        public PrintingVisitor(final PrintWriter out) {
            this.out = out;
        }

        @Override
        public void enter(final String sectionName) {
            sections.push(sectionName);
        }

        @Override
        public void leave(final String sectionName) {
            if (sections.size() == printedSections.size()) {
                printedSections.pop();
                if (printedSections.empty() && hasPrintedSomething) {
                    out.println();
                }
            }
            sections.pop();
        }

        @Override
        public void added(final String name, final Object value) {
            printSectionHeaderIfNeeded();
            printValue('+', name, value);
        }

        @Override
        public void changed(final String name, final Object leftValue, final Object rightValue) {
            printSectionHeaderIfNeeded();
            removed(name, leftValue);
            added(name, rightValue);
        }

        @Override
        public void removed(final String name, final Object value) {
            printSectionHeaderIfNeeded();
            printValue('-', name, value);
        }

        private void printSectionHeaderIfNeeded() {
            for (int i = printedSections.size(); i < sections.size(); i++) {
                String section = sections.get(i);
                out.append(indent(i)).println(section);
                printedSections.push(section);
                hasPrintedSomething = true;
            }
        }

        private void printValue(final char plusMinus, final String name, final Object value) {
            out.append(indent(sections.size())).append(plusMinus).append(' ').append(name).append(" = ");
            if (value.getClass().isArray()) {
                printArray((Object[]) value);
            } else {
                out.println(value);
            }
        }

        private void printArray(final Object[] values) {
            out.append('[');
            for (int i = 0; i < values.length; i++) {
                if (values.length > 1) {
                    out.println();
                    out.append(indent(sections.size() + 2));
                }
                out.print(values[i]);
                out.append(i == values.length - 1 ? ']' : ',');
            }
            out.println();
        }

        private String indent(final int i) {
            return INDENTATION_WHITESPACE.substring(0, i * 4);
        }

        public boolean hasPrintedSomething() {
            return hasPrintedSomething;
        }
    }
}
