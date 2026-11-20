package edu.guilford;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class Planner {
    private final Path dataFile;
    private final List<PracticeItem> items = new ArrayList<>();
    private final List<RecurringPractice> recurringPractices = new ArrayList<>();

    public Planner(Path dataFile) {
        this.dataFile = dataFile;
    }

    public void load() throws IOException {
        if (!Files.exists(dataFile)) {
            return;
        }
        List<String> lines = Files.readAllLines(dataFile, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
            String[] fields = lines.get(i).split("\\|", -1);
            try {
                if (fields[0].equals("I") && fields.length == 7) {
                    items.add(new PracticeItem(decode(fields[1]), PracticeType.valueOf(fields[2]),
                            LocalDate.parse(fields[3]), decode(fields[4]),
                            Integer.parseInt(fields[5]), Boolean.parseBoolean(fields[6])));
                } else if (fields[0].equals("R") && fields.length == 7) {
                    Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
                    for (String day : fields[6].split(",")) {
                        days.add(DayOfWeek.valueOf(day));
                    }
                    recurringPractices.add(new RecurringPractice(decode(fields[1]),
                            PracticeType.valueOf(fields[2]), decode(fields[3]),
                            Integer.parseInt(fields[4]), LocalDate.parse(fields[5]), days));
                } else {
                    throw new IllegalArgumentException("Unrecognized data row");
                }
            } catch (IllegalArgumentException e) {
                throw new IOException("Invalid planner data on line " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
    }

    public void save() throws IOException {
        List<String> lines = new ArrayList<>();
        for (PracticeItem item : items) {
            lines.add(String.join("|", "I", encode(item.title()), item.type().name(),
                    item.date().toString(), encode(item.speed()),
                    Integer.toString(item.repetitionGoal()), Boolean.toString(item.completed())));
        }
        for (RecurringPractice rule : recurringPractices) {
            String days = rule.days().stream().sorted()
                    .map(DayOfWeek::name).reduce((left, right) -> left + "," + right).orElse("");
            lines.add(String.join("|", "R", encode(rule.title()), rule.type().name(),
                    encode(rule.speed()), Integer.toString(rule.repetitionGoal()),
                    rule.startsOn().toString(), days));
        }
        Files.write(dataFile, lines, StandardCharsets.UTF_8);
    }

    public void addItem(PracticeItem item) {
        items.add(item);
    }

    public void addRecurrence(RecurringPractice recurrence) {
        recurringPractices.add(recurrence);
    }

    public void setCompleted(PracticeItem item, boolean completed) {
        int index = items.indexOf(item);
        if (index < 0) {
            throw new IllegalStateException("The selected practice item is no longer in the plan.");
        }
        items.set(index, item.withCompleted(completed));
    }

    public List<PracticeItem> itemsOn(LocalDate date) {
        return items.stream().filter(item -> item.date().equals(date)).toList();
    }

    public List<PracticeItem> items() {
        return List.copyOf(items);
    }

    public void materializeRecurringThrough(LocalDate endDate) {
        for (RecurringPractice recurrence : recurringPractices) {
            LocalDate date = recurrence.startsOn();
            while (!date.isAfter(endDate)) {
                if (recurrence.days().contains(date.getDayOfWeek())) {
                    PracticeItem occurrence = new PracticeItem(recurrence.title(), recurrence.type(),
                            date, recurrence.speed(), recurrence.repetitionGoal(), false);
                    boolean alreadyScheduled = items.stream().anyMatch(item ->
                            item.title().equals(occurrence.title())
                                    && item.type() == occurrence.type()
                                    && item.date().equals(occurrence.date())
                                    && item.speed().equals(occurrence.speed())
                                    && item.repetitionGoal() == occurrence.repetitionGoal());
                    if (!alreadyScheduled) {
                        items.add(occurrence);
                    }
                }
                date = date.plusDays(1);
            }
        }
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
