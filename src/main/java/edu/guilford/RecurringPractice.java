package edu.guilford;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

public record RecurringPractice(String title, PracticeType type, String speed,
                                int repetitionGoal, LocalDate startsOn, Set<DayOfWeek> days) {
    public RecurringPractice {
        if (title.isBlank() || speed.isBlank() || repetitionGoal < 1 || days.isEmpty()) {
            throw new IllegalArgumentException("Recurring practice needs a title, speed, goal, and weekdays.");
        }
        days = Set.copyOf(days);
    }
}
