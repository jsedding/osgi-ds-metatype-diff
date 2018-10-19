package net.distilledcode.tools.osgi;

import org.apache.felix.metatype.AD;
import org.apache.felix.metatype.Designate;
import org.apache.felix.metatype.MetaData;
import org.apache.felix.metatype.OCD;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;

import javax.annotation.Nonnull;
import java.beans.Introspector;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Comparison {

    public interface Visitor {
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

    public static Comparison create(final String className, final MetadataDiff.BundleMetadata left, final MetadataDiff.BundleMetadata right) {
        return new Comparison(className,
                left.getDeclarativeServices(className),
                right.getDeclarativeServices(className),
                left.getMetaType(className),
                right.getMetaType(className));
    }

    public void visit(Visitor visitor) {
        visitor.enter(className);
        visitDS(visitor);
        visitMetaType(visitor);
        visitor.leave(className);
    }

    @SuppressWarnings("unchecked")
    private void visitMetaType(final Visitor visitor) {
        visitor.enter("MetaType");
        visitMetaTypeAttributes(visitor, leftMT, rightMT);
        visitAsMap(visitor, "Designates", leftMT, rightMT,
                Comparison.<MetaData, List<Designate>>tryAll(MetaData::getDesignates, m -> Collections.emptyList())
                    .andThen(fromCollectionToMap(tryAll(Designate::getPid, Designate::getFactoryPid))),
                Comparison::visitDesignate);
        visitAsMap(visitor, "ObjectClassDefinitions", leftMT, rightMT,
                Comparison::toOCDMap,
                Comparison::visitOCD);
        visitor.leave("MetaType");

    }

    private static void visitMetaTypeAttributes(final Visitor visitor, final MetaData leftMT, final MetaData rightMT) {
        visitor.enter("Attributes");
        visitValue(visitor, "namespace", MetaData::getNamespace, leftMT, rightMT);
        // visitValue(visitor, "localePrefix", MetaData::getLocalePrefix, leftMT, rightMT);
        visitor.leave("Attributes");
    }

    private static void visitDesignate(final Visitor visitor, final String name, final Designate left, final Designate right) {
        visitor.enter(name);
        visitValue(visitor, "pid", Designate::getPid, left, right);
        visitValue(visitor, "factoryPid", Designate::getFactoryPid, left, right);
        visitValue(visitor, "bundleLocation", Designate::getBundleLocation, left, right);
        visitValue(visitor, "merge", Designate::isMerge, left, right);
        visitValue(visitor, "optional", Designate::isOptional, left, right);
        visitValue(visitor, "ocdRef", d -> d.getObject().getOcdRef(), left, right);
        visitor.leave(name);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, OCD> toOCDMap(MetaData metaData) {
        Map<String, OCD> ocds = metaData.getObjectClassDefinitions();
        // if OCDs without AD children in metatype < 1.3 then ocds = null
        if (ocds == null) {
            return Collections.emptyMap();
        }
        List<Designate> designates = metaData.getDesignates();
        Stream<Designate> stream = designates == null ? Stream.empty() : designates.stream();
        Function<Designate, String> designateStringFunction = d -> d.getObject().getOcdRef();
        Map<String, String> ocdRefToImplClass = stream.collect(Collectors.toMap(designateStringFunction, tryAll(Designate::getPid, Designate::getFactoryPid)));
        return fromCollectionToMap((Function<OCD, String>) ocd -> ocdRefToImplClass.get(ocd.getID())).apply(ocds.values());
    }

    @SuppressWarnings("unchecked")
    private static void visitOCD(final Visitor visitor, final String name, final OCD left, final OCD right) {
        visitor.enter(name);
        visitValue(visitor, "id", OCD::getID, left, right);
        visitValue(visitor, "name", OCD::getName, left, right);
        visitValue(visitor, "description", OCD::getDescription, left, right);
        visitAsMap(visitor, "Attribute Definitions", left, right,
                ocd -> (Map<String, AD>)ocd.getAttributeDefinitions(),
                Comparison::visitAttributeDefinition);
        visitor.leave(name);
    }
    private static void visitAttributeDefinition(final Visitor visitor, final String name, final AD left, final AD right) {
        visitor.enter(name);
        visitValue(visitor, "id", AD::getID, left, right);
        visitValue(visitor, "name", AD::getName, left, right);
        visitValue(visitor, "description", AD::getDescription, left, right);
        visitValue(visitor, "type", AD::getType, left, right);
        visitValue(visitor, "cardinality", AD::getCardinality, left, right);
        visitValue(visitor, "defaultValue", AD::getDefaultValue, left, right);
        visitValue(visitor, "min", AD::getMin, left, right);
        visitValue(visitor, "max", AD::getMax, left, right);
        visitValue(visitor, "optionLabels", AD::getOptionLabels, left, right);
        visitValue(visitor, "optionValues", AD::getOptionValues, left, right);
        visitor.leave(name);
    }

    private void visitDS(final Visitor visitor) {
        visitor.enter("Declarative Services");
        visitDSAttributes(visitor, leftDS, rightDS);
        visitAsMap(visitor, "Properties", leftDS, rightDS, ComponentMetadata::getProperties, Comparison::visitValue);
        visitAsMap(visitor, "References", leftDS, rightDS,
                Comparison.<ComponentMetadata, List<ReferenceMetadata>>tryAll(ComponentMetadata::getDependencies, m -> Collections.emptyList())
                        .andThen(fromCollectionToMap(rm -> Introspector.decapitalize(rm.getName()))),
                Comparison::visitDSReference);
        visitor.leave("Declarative Services");
    }

    private void visitDSAttributes(final Visitor visitor, final ComponentMetadata leftDS, final ComponentMetadata rightDS) {
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

    private static void visitDSReference(final Visitor visitor, String name, final ReferenceMetadata left, final ReferenceMetadata right) {
        visitor.enter(name);
        visitValue(visitor, "name", ReferenceMetadata::getName, left, right);
        visitValue(visitor, "interface", ReferenceMetadata::getInterface, left, right);
        visitValue(visitor, "cardinality", ReferenceMetadata::getCardinality, left, right);
        visitValue(visitor, "bind", ReferenceMetadata::getBind, left, right);
        visitValue(visitor, "unbind", ReferenceMetadata::getUnbind, left, right);
        visitValue(visitor, "updated", ReferenceMetadata::getUpdated, left, right);
        visitValue(visitor, "field", ReferenceMetadata::getField, left, right);
        visitValue(visitor, "field-option", ReferenceMetadata::getFieldOption, left, right);
        visitValue(visitor, "field-collection", ReferenceMetadata::getFieldCollectionType, left, right);
        visitor.leave(name);
    }

    private static <S, T> void visitValue(final Visitor visitor, String name, final Function<S, T> fn, final S left, final S right) {
        visitValue(visitor, name, valueOrNull(left, fn), valueOrNull(right, fn));
    }

    private static <T> void visitValue(final Visitor visitor, final String name, final T leftValue, final T rightValue) {
        if (leftValue == null && rightValue == null) {
            return;
        }
        if (leftValue != null && rightValue == null) {
            visitor.removed(name, leftValue);
        } else if (leftValue != null && !equalValue(leftValue, rightValue)) {
            visitor.changed(name, leftValue, rightValue);
        } else if (leftValue == null) {
            visitor.added(name, rightValue);
        }
    }

    private static <T> boolean equalValue(@Nonnull final T leftValue, @Nonnull final T rightValue) {
        if (leftValue.getClass().isArray() && rightValue.getClass().isArray()) {
            return Arrays.equals((Object[])leftValue, (Object[])rightValue);
        }
        return leftValue.equals(rightValue);
    }

    private static <S, T> void visitAsMap(final Visitor visitor, final String sectionName, final S left, final S right, final Function<S, Map<String, T>> toMap, final VisitorFunction<T> visitEntry) {
        Map<String, T> leftMap = valueOrDefault(left, toMap, Collections.emptyMap());
        Map<String, T> rightMap = valueOrDefault(right, toMap, Collections.emptyMap());
        visitor.enter(sectionName);
        for (final String name : Sets.union(leftMap.keySet(), rightMap.keySet())) {
            visitEntry.apply(visitor, name, valueOrNull(leftMap, m -> m.get(name)), valueOrNull(rightMap, m -> m.get(name)));
        }
        visitor.leave(sectionName);
    }

    @FunctionalInterface
    public interface VisitorFunction<T> {
        void apply(Visitor visitor, String name, T left, T right);
    }

    private static <S, T> T valueOrNull(final S object, final Function<S, T> fn) {
        return nullSafe(fn).apply(object);
    }

    private static <S, T> T valueOrDefault(final S object, final Function<S, T> fn, final T defaultValue) {
        return tryAll(nullSafe(fn), s -> defaultValue).apply(object);
    }

    @SafeVarargs
    private static <S, T> Function<S, T> tryAll(Function<S, T>... functions) {
        return s -> Arrays.stream(functions)
                .flatMap(fn -> {
                    T result = fn.apply(s);
                    return result != null ? Stream.of(result) : Stream.empty();
                })
                .findFirst()
                .orElse(null);
    }

    private static <S, T> Function<S, T> nullSafe(final Function<S, T> fn) {
        return s -> s != null ? fn.apply(s) : null;
    }

    private static <T> Function<Collection<T>, Map<String, T>> fromCollectionToMap(final Function<T, String> keyMapper) {
        return list -> list.stream().collect(Collectors.toMap(keyMapper, Function.identity()));
    }
}
