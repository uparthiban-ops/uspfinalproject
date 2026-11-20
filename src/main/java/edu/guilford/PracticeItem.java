package edu.guilford;

import java.time.LocalDate;

public record PracticeItem(String title, PracticeType type, LocalDate date,
                           String speed, int repetitionGoal, boolean completed) {
    public PracticeItem {
        if (title.isBlank() || speed.isBlank() || repetitionGoal < 1) {
            throw new IllegalArgumentException("Practice items need a title, speed, and positive repetition goal.");
        }
    }

    PracticeItem withCompleted(boolean value) {
        return new PracticeItem(title, type, date, speed, repetitionGoal, value);
    }
}
