package ua.lab1.sets;

/** Впорядкована пара елементів декартового добутку (a, b). */
public record Pair(String first, String second) {

    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }
}
