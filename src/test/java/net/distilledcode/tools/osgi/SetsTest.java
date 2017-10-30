package net.distilledcode.tools.osgi;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertThat;

public class SetsTest {

    public static final Set<String> SET_A = asSet("a", "c", "e", "f");

    public static final Set<String> SET_B = asSet("b", "d", "e", "f");

    @Test
    public void union() throws Exception {
        Set<String> union = Sets.union(SET_A, SET_B);
        assertThat(union, hasItems("a", "b", "c", "d", "e", "f"));
    }

    @Test
    public void difference() throws Exception {
        Set<String> diff1 = Sets.difference(SET_A, SET_B);
        assertThat(diff1, hasItems("a", "c"));
        assertThat(diff1, not(hasItems("b", "d", "e", "f")));


        Set<String> diff2 = Sets.difference(SET_B, SET_A);
        assertThat(diff2, hasItems("b", "d"));
        assertThat(diff2, not(hasItems("a", "c", "e", "f")));
    }

    @Test
    public void symmetricDifference() throws Exception {
        Set<String> diff1 = Sets.symmetricDifference(SET_A, SET_B);
        assertThat(diff1, hasItems("a", "c", "b", "d"));
        assertThat(diff1, not(hasItems("e", "f")));

        Set<String> diff2 = Sets.symmetricDifference(SET_B, SET_A);
        assertThat(diff2, equalTo(diff1));
    }

    @Test
    public void intersection() throws Exception {
        Set<String> intersection = Sets.intersection(SET_A, SET_B);
        assertThat(intersection, hasItems("e", "f"));
        assertThat(intersection, not(hasItems("a", "b", "c", "d")));
    }

    @SafeVarargs
    private static <T> Set<T> asSet(final T... elements) {
        return Collections.unmodifiableSet(new HashSet<>(asList(elements)));
    }
}