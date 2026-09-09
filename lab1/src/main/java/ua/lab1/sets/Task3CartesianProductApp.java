package ua.lab1.sets;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Завдання 3: програма, яка як вхідні дані одержує дві множини A, B
 * і утворює декартові добутки A×B та B×A.
 */
public final class Task3CartesianProductApp {

    private Task3CartesianProductApp() {
    }

    public static void main(String[] args) {
        ConsoleIO.useUtf8Console();
        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            run(scanner);
        }
    }

    static void run(Scanner scanner) {
        System.out.println("=== Завдання 3. Декартів добуток A×B та B×A ===");
        Set<String> a = ConsoleIO.readSet(scanner, "Введіть множину A");
        Set<String> b = ConsoleIO.readSet(scanner, "Введіть множину B");
        ConsoleIO.printSet("A", a);
        ConsoleIO.printSet("B", b);

        List<Pair> aTimesB = SetOperations.cartesianProduct(a, b);
        List<Pair> bTimesA = SetOperations.cartesianProduct(b, a);

        System.out.println("A×B = {" + joinPairs(aTimesB) + "}");
        System.out.println("B×A = {" + joinPairs(bTimesA) + "}");
        System.out.println();
    }

    private static String joinPairs(List<Pair> pairs) {
        return pairs.stream().map(Pair::toString).collect(Collectors.joining(", "));
    }
}
