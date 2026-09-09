package ua.lab1.sets;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.Set;

/**
 * Завдання 1: програма, яка як вхідні дані одержує дві множини і визначає,
 * чи рівні ці множини, чи є одна з них підмножиною іншої.
 */
public final class Task1RelationApp {

    private Task1RelationApp() {
    }

    public static void main(String[] args) {
        ConsoleIO.useUtf8Console();
        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            run(scanner);
        }
    }

    static void run(Scanner scanner) {
        System.out.println("=== Завдання 1. Рівність множин і відношення підмножини ===");
        Set<String> a = ConsoleIO.readSet(scanner, "Введіть множину A");
        Set<String> b = ConsoleIO.readSet(scanner, "Введіть множину B");

        ConsoleIO.printSet("A", a);
        ConsoleIO.printSet("B", b);

        SetRelation relation = SetRelation.classify(a, b);
        System.out.println("Результат: " + relation.getDescription());
        System.out.println();
    }
}
