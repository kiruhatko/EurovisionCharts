package ua.lab1.sets;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

/** Читання множин з консолі та їх форматований вивід. */
public final class ConsoleIO {

    private ConsoleIO() {
    }

    /**
     * Примушує stdout/stderr використовувати UTF-8, незалежно від локалі системи
     * (без цього кирилиця може виводитись як "?" у консолях з не-UTF-8 кодуванням).
     */
    public static void useUtf8Console() {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
    }

    public static Set<String> readSet(Scanner scanner, String prompt) {
        System.out.print(prompt + " (елементи через кому або пробіл): ");
        String line = scanner.nextLine();
        Set<String> set = new TreeSet<>();
        for (String token : line.trim().split("[,\\s]+")) {
            if (!token.isEmpty()) {
                set.add(token);
            }
        }
        return set;
    }

    public static void printSet(String name, Set<String> set) {
        System.out.println(name + " = " + formatSet(set));
    }

    public static String formatSet(Set<String> set) {
        return set.isEmpty() ? "∅" : "{" + String.join(", ", set) + "}";
    }
}
