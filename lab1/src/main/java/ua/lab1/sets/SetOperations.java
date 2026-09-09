package ua.lab1.sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Базові теоретико-множинні операції над {@code Set<String>}, які
 * використовуються у всіх чотирьох завданнях лабораторної роботи.
 */
public final class SetOperations {

    private SetOperations() {
    }

    public static boolean isEqual(Set<String> a, Set<String> b) {
        return a.equals(b);
    }

    /** Чи є {@code sub} підмножиною {@code sup}, тобто sub &sube; sup. */
    public static boolean isSubset(Set<String> sub, Set<String> sup) {
        return sup.containsAll(sub);
    }

    public static Set<String> union(Set<String> a, Set<String> b) {
        Set<String> result = new TreeSet<>(a);
        result.addAll(b);
        return result;
    }

    public static Set<String> intersection(Set<String> a, Set<String> b) {
        Set<String> result = new TreeSet<>(a);
        result.retainAll(b);
        return result;
    }

    /** Різниця A \ B: елементи A, яких немає в B. */
    public static Set<String> difference(Set<String> a, Set<String> b) {
        Set<String> result = new TreeSet<>(a);
        result.removeAll(b);
        return result;
    }

    /** Диз'юнктивна сума (симетрична різниця) A &oplus; B. */
    public static Set<String> symmetricDifference(Set<String> a, Set<String> b) {
        Set<String> result = union(a, b);
        result.removeAll(intersection(a, b));
        return result;
    }

    /** Доповнення множини {@code a} відносно універсуму {@code universe}. */
    public static Set<String> complement(Set<String> a, Set<String> universe) {
        return difference(universe, a);
    }

    /** Булеан P(A) — список усіх підмножин множини {@code set}, разом з &empty; та самою A. */
    public static List<Set<String>> powerSet(Set<String> set) {
        List<String> elements = new ArrayList<>(set);
        int n = elements.size();
        if (n > 20) {
            throw new IllegalArgumentException(
                    "Множина занадто велика для побудови булеану (2^" + n + " підмножин)");
        }
        int subsetCount = 1 << n;
        List<Set<String>> result = new ArrayList<>(subsetCount);
        for (int mask = 0; mask < subsetCount; mask++) {
            Set<String> subset = new TreeSet<>();
            for (int bit = 0; bit < n; bit++) {
                if ((mask & (1 << bit)) != 0) {
                    subset.add(elements.get(bit));
                }
            }
            result.add(subset);
        }
        return result;
    }

    /** Декартів добуток A&times;B як список впорядкованих пар. */
    public static List<Pair> cartesianProduct(Set<String> a, Set<String> b) {
        List<Pair> result = new ArrayList<>(a.size() * b.size());
        for (String x : a) {
            for (String y : b) {
                result.add(new Pair(x, y));
            }
        }
        return result;
    }
}
