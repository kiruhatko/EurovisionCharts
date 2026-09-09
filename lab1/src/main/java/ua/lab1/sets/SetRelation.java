package ua.lab1.sets;

import java.util.Set;

/** Результат порівняння двох множин: рівність або відношення підмножини. */
public enum SetRelation {

    EQUAL("A = B (множини рівні)"),
    PROPER_SUBSET("A ⊂ B (A є власною підмножиною B)"),
    PROPER_SUPERSET("A ⊃ B (B є власною підмножиною A)"),
    NEITHER("жодна з множин не є підмножиною іншої");

    private final String description;

    SetRelation(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static SetRelation classify(Set<String> a, Set<String> b) {
        boolean aSubB = b.containsAll(a);
        boolean bSubA = a.containsAll(b);
        if (aSubB && bSubA) {
            return EQUAL;
        } else if (aSubB) {
            return PROPER_SUBSET;
        } else if (bSubA) {
            return PROPER_SUPERSET;
        } else {
            return NEITHER;
        }
    }
}
