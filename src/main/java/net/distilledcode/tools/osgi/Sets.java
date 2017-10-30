package net.distilledcode.tools.osgi;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Sets {

    public static <T> Set<T> union(final Set<T> set1, final Set<T> set2) {
        HashSet<T> union = new HashSet<>();
        union.addAll(set1);
        union.addAll(set2);
        return Collections.unmodifiableSet(union);
    }

    public static <T> Set<T> difference(final Set<T> set1, final Set<T> set2) {
        HashSet<T> difference = new HashSet<>();
        difference.addAll(set1);
        difference.removeAll(set2);
        return Collections.unmodifiableSet(difference);
    }

    public static <T> Set<T> symmetricDifference(final Set<T> set1, final Set<T> set2) {
        return union(difference(set1, set2), difference(set2, set1));
    }

    public static <T> Set<T> intersection(final Set<T> set1, final Set<T> set2) {
        return difference(union(set1, set2), symmetricDifference(set1, set2));
    }
}
