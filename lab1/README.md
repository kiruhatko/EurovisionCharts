# Лабораторна робота №1. Операції над множинами

Реалізація на Java всіх чотирьох пунктів «Порядку виконання роботи»:

1. **Task1RelationApp** — приймає дві множини й визначає, чи вони рівні,
   чи одна є підмножиною іншої.
2. **Task2PowerSetApp** — приймає множину й будує список усіх її
   підмножин (булеан P(A)).
3. **Task3CartesianProductApp** — приймає дві множини A, B і будує
   декартові добутки A×B та B×A.
4. **Task4SetOperationsGui** — Swing-застосунок, що в графічному режимі
   моделює операції A∪B, A∩B, A\B, A⊕B, Ā на діаграмі Ейлера — Венна
   (зафарбовує потрібну область і водночас показує сам результат-множину).

Спільна логіка операцій над множинами винесена у клас `SetOperations`
(union/intersection/difference/symmetricDifference/complement/powerSet/
cartesianProduct), а порівняння множин — у `SetRelation`.

## Структура проєкту

```
lab1/
├── pom.xml
└── src/
    ├── main/java/ua/lab1/sets/
    │   ├── SetOperations.java          — базові операції над множинами
    │   ├── SetRelation.java            — рівність / підмножина
    │   ├── Pair.java                   — пара для декартового добутку
    │   ├── ConsoleIO.java              — читання/друк множин у консолі
    │   ├── Task1RelationApp.java
    │   ├── Task2PowerSetApp.java
    │   ├── Task3CartesianProductApp.java
    │   ├── Task4SetOperationsGui.java  — графічний режим (Swing)
    │   └── LabMenu.java                — меню для запуску всіх завдань
    └── test/java/ua/lab1/sets/
        └── SetOperationsTest.java      — JUnit 5 тести операцій
```

## Збірка та запуск

Проєкт збирається Maven'ом (`maven.compiler.release` — 21, без будь-яких
версійно-специфічних конструкцій, тож так само компілюється й новішим
JDK, включно з Java 26):

```bash
cd lab1
mvn clean test          # компіляція + 11 юніт-тестів
mvn clean package        # збирає виконуваний lab1-sets-operations.jar
java -jar target/lab1-sets-operations.jar
```

Без Maven — напряму через `javac`/`java`:

```bash
cd lab1
javac -d out $(find src/main/java -name "*.java")
java -cp out ua.lab1.sets.LabMenu
```

`LabMenu` виводить меню, що дозволяє послідовно запустити будь-яке з
чотирьох завдань. Кожне завдання також має власний клас з `main`, тож
його можна запускати окремо, наприклад:

```bash
java -cp out ua.lab1.sets.Task1RelationApp
java -cp out ua.lab1.sets.Task4SetOperationsGui
```

Формат введення множини в консольних завданнях — елементи через кому
або пробіл, наприклад: `1, 2, 3, 4` або `a b c`.

Завдання 4 (Swing GUI) вимагає графічного дисплея; при спробі запустити
його в headless-середовищі програма коректно повідомляє про це замість
падіння з винятком.

## Приклад роботи (завдання 1)

```
Введіть множину A: 1, 2
Введіть множину B: 1, 2, 3
A = {1, 2}
B = {1, 2, 3}
Результат: A ⊂ B (A є власною підмножиною B)
```
