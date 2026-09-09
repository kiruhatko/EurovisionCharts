package ua.lab1.sets;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetOperationsTest {

    private static final Set<String> A = Set.of("1", "2", "3", "4");
    private static final Set<String> B = Set.of("3", "4", "5", "6");
    private static final Set<String> UNIVERSE = Set.of("1", "2", "3", "4", "5", "6", "7", "8");

    @Test
    void unionContainsElementsOfBothSets() {
        assertEquals(Set.of("1", "2", "3", "4", "5", "6"), SetOperations.union(A, B));
    }

    @Test
    void intersectionContainsOnlyCommonElements() {
        assertEquals(Set.of("3", "4"), SetOperations.intersection(A, B));
    }

    @Test
    void differenceRemovesElementsOfSecondSet() {
        assertEquals(Set.of("1", "2"), SetOperations.difference(A, B));
    }

    @Test
    void symmetricDifferenceExcludesCommonElements() {
        assertEquals(Set.of("1", "2", "5", "6"), SetOperations.symmetricDifference(A, B));
    }

    @Test
    void complementIsUniverseMinusSet() {
        assertEquals(Set.of("5", "6", "7", "8"), SetOperations.complement(A, UNIVERSE));
    }

    @Test
    void classifyDetectsEquality() {
        assertEquals(SetRelation.EQUAL, SetRelation.classify(A, A));
    }

    @Test
    void classifyDetectsProperSubset() {
        Set<String> subset = Set.of("1", "2");
        assertEquals(SetRelation.PROPER_SUBSET, SetRelation.classify(subset, A));
        assertEquals(SetRelation.PROPER_SUPERSET, SetRelation.classify(A, subset));
    }

    @Test
    void classifyDetectsNeitherWhenSetsAreIncomparable() {
        assertEquals(SetRelation.NEITHER, SetRelation.classify(A, B));
    }

    @Test
    void isSubsetMatchesContainsAll() {
        assertTrue(SetOperations.isSubset(Set.of("1", "2"), A));
        assertFalse(SetOperations.isSubset(A, B));
    }

    @Test
    void powerSetHasTwoToThePowerOfNElements() {
        Set<String> set = Set.of("a", "b", "c");
        List<Set<String>> powerSet = SetOperations.powerSet(set);
        assertEquals(8, powerSet.size());
        assertTrue(powerSet.contains(Set.of()));
        assertTrue(powerSet.contains(set));
        assertTrue(powerSet.contains(Set.of("a", "b")));
    }

    @Test
    void cartesianProductHasSizeEqualToProductOfSizes() {
        Set<String> x = Set.of("a", "b");
        Set<String> y = Set.of("1", "2", "3");
        List<Pair> product = SetOperations.cartesianProduct(x, y);
        assertEquals(6, product.size());
        assertTrue(product.contains(new Pair("a", "1")));
        assertTrue(product.contains(new Pair("b", "3")));
    }
}
