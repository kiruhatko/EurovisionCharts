package ua.lab1.sets;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * Завдання 2: програма, яка як вхідні дані одержує множину і утворює
 * список усіх можливих підмножин даної множини (булеан P(A)).
 */
public final class Task2PowerSetApp {

    private Task2PowerSetApp() {
    }

    public static void main(String[] args) {
        ConsoleIO.useUtf8Console();
        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            run(scanner);
        }
    }

    static void run(Scanner scanner) {
        System.out.println("=== Завдання 2. Побудова булеану множини P(A) ===");
        Set<String> a = ConsoleIO.readSet(scanner, "Введіть множину A");
        ConsoleIO.printSet("A", a);

        List<Set<String>> powerSet = SetOperations.powerSet(a);
        System.out.println("P(A) містить " + powerSet.size() + " (2^" + a.size() + ") підмножин:");
        for (Set<String> subset : powerSet) {
            System.out.println("  " + ConsoleIO.formatSet(subset));
        }
        System.out.println();
    }
}
