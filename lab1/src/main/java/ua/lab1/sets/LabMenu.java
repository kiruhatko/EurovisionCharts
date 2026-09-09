package ua.lab1.sets;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/** Головне меню лабораторної роботи — запускає завдання 1-4. */
public final class LabMenu {

    private LabMenu() {
    }

    public static void main(String[] args) {
        ConsoleIO.useUtf8Console();
        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            boolean running = true;
            while (running) {
                printMenu();
                String choice = scanner.nextLine().trim();
                switch (choice) {
                    case "1" -> Task1RelationApp.run(scanner);
                    case "2" -> Task2PowerSetApp.run(scanner);
                    case "3" -> Task3CartesianProductApp.run(scanner);
                    case "4" -> Task4SetOperationsGui.launch();
                    case "0" -> running = false;
                    default -> System.out.println("Невірний вибір, спробуйте ще раз.\n");
                }
            }
        }
        System.out.println("Роботу завершено.");
    }

    private static void printMenu() {
        System.out.print("""
                Лабораторна робота №1. Операції над множинами
                1 - Завдання 1: рівність / підмножина
                2 - Завдання 2: побудова булеану P(A)
                3 - Завдання 3: декартів добуток A×B, B×A
                4 - Завдання 4: графічний режим (Swing GUI)
                0 - Вихід
                Ваш вибір: \
                """);
    }
}
