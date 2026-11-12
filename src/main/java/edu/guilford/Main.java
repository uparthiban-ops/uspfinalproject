package edu.guilford;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class Main {
    private static final Path DATA_FILE = Path.of("practice-planner-data.txt");

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            Planner planner = new Planner(DATA_FILE);
            planner.load();
            planner.materializeRecurringThrough(LocalDate.now());
            planner.save();
            runMenu(scanner, planner);
        } catch (IOException e) {
            System.err.println("Could not read or save practice planner data: " + e.getMessage());
        }
    }

    private static void runMenu(Scanner scanner, Planner planner) throws IOException {
        boolean running = true;
        while (running) {
            System.out.println("\n=== Bharatanatyam Practice Planner ===");
            System.out.println("1. Add practice item");
            System.out.println("2. Add recurring practice");
            System.out.println("3. Daily checklist");
            System.out.println("4. Weekly view and progress");
            System.out.println("5. Monthly calendar");
            System.out.println("6. Practice streak");
            System.out.println("0. Save and exit");
            int choice = readInt(scanner, "Choose an option: ", 0, 6);

            switch (choice) {
                case 1 -> addPracticeItem(scanner, planner);
                case 2 -> addRecurringPractice(scanner, planner);
                case 3 -> showDailyChecklist(scanner, planner);
                case 4 -> showWeeklyView(scanner, planner);
                case 5 -> showMonthlyCalendar(scanner, planner);
                case 6 -> showStreak(planner);
                case 0 -> running = false;
                default -> throw new IllegalStateException("Unexpected menu choice");
            }
        }
        planner.save();
        System.out.println("Practice plan saved to " + DATA_FILE.toAbsolutePath());
    }

    private static void addPracticeItem(Scanner scanner, Planner planner) throws IOException {
        String title = readNonBlank(scanner, "Practice item (for example, Tatta Adavu): ");
        PracticeType type = readType(scanner);
        LocalDate date = readDate(scanner, "Date (YYYY-MM-DD): ", LocalDate.now());
        String speed = readNonBlank(scanner, "Speed (for example, slow, medium, fast, or 100 BPM): ");
        int repetitions = readInt(scanner, "Repetition goal: ", 1, Integer.MAX_VALUE);
        planner.addItem(new PracticeItem(title, type, date, speed, repetitions, false));
        planner.save();
        System.out.println("Added to your practice plan.");
    }

    private static void addRecurringPractice(Scanner scanner, Planner planner) throws IOException {
        String title = readNonBlank(scanner, "Practice item (for example, Tatta Adavu): ");
        PracticeType type = readType(scanner);
        String speed = readNonBlank(scanner, "Speed (for example, slow, medium, fast, or 100 BPM): ");
        int repetitions = readInt(scanner, "Repetition goal: ", 1, Integer.MAX_VALUE);
        LocalDate startsOn = readDate(scanner, "Start date (YYYY-MM-DD): ", LocalDate.now());
        Set<DayOfWeek> days = readWeekdays(scanner);
        planner.addRecurrence(new RecurringPractice(title, type, speed, repetitions, startsOn, days));
        planner.materializeRecurringThrough(LocalDate.now());
        planner.save();
        System.out.println("Recurring practice added.");
    }

    private static void showDailyChecklist(Scanner scanner, Planner planner) throws IOException {
        LocalDate date = readDate(scanner, "Checklist date (YYYY-MM-DD): ", LocalDate.now());
        planner.materializeRecurringThrough(date);
        planner.save();
        List<PracticeItem> items = planner.itemsOn(date);
        System.out.println("\nDaily checklist — " + date + " (" + date.getDayOfWeek() + ")");
        if (items.isEmpty()) {
            System.out.println("No practice items planned.");
            return;
        }

        printItems(items);
        System.out.println("Enter an item number to toggle completion, or 0 to return.");
        int selected = readInt(scanner, "Selection: ", 0, items.size());
        if (selected == 0) {
            return;
        }
        PracticeItem item = items.get(selected - 1);
        planner.setCompleted(item, !item.completed());
        planner.save();
        System.out.println(item.title() + (item.completed() ? " marked complete." : " marked incomplete."));
    }

    private static void showWeeklyView(Scanner scanner, Planner planner) throws IOException {
        LocalDate selected = readDate(scanner, "Choose a date in the week (YYYY-MM-DD): ", LocalDate.now());
        LocalDate monday = selected.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        planner.materializeRecurringThrough(sunday);
        planner.save();

        int total = 0;
        int completed = 0;
        System.out.println("\nWeekly practice — " + monday + " to " + sunday);
        for (int offset = 0; offset < 7; offset++) {
            LocalDate date = monday.plusDays(offset);
            List<PracticeItem> items = planner.itemsOn(date);
            int done = countCompleted(items);
            total += items.size();
            completed += done;
            System.out.printf("%s %s  %d/%d complete%n", date, date.getDayOfWeek(), done, items.size());
        }
        System.out.printf("Weekly progress: %d/%d items complete (%d%%)%n",
                completed, total, percentage(completed, total));
    }

    private static void showMonthlyCalendar(Scanner scanner, Planner planner) throws IOException {
        YearMonth month = readMonth(scanner, YearMonth.now());
        LocalDate first = month.atDay(1);
        LocalDate last = month.atEndOfMonth();
        planner.materializeRecurringThrough(last);
        planner.save();

        System.out.println("\n" + month.getMonth() + " " + month.getYear());
        System.out.println(" Mon       Tue       Wed       Thu       Fri       Sat       Sun");
        int leadingSpaces = first.getDayOfWeek().getValue() - 1;
        for (int i = 0; i < leadingSpaces; i++) {
            System.out.print("          ");
        }
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            List<PracticeItem> items = planner.itemsOn(date);
            String marker = items.isEmpty() ? " " : countCompleted(items) == items.size() ? "*" : "+";
            System.out.printf("%2d%s %-6s", day, marker, items.isEmpty() ? "" :
                    countCompleted(items) + "/" + items.size());
            if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                System.out.println();
            } else {
                System.out.print(" ");
            }
        }
        System.out.println("\n* all planned items complete; + practice planned");
    }

    private static void showStreak(Planner planner) {
        Set<LocalDate> practicedDates = new HashSet<>();
        for (PracticeItem item : planner.items()) {
            if (item.completed()) {
                practicedDates.add(item.date());
            }
        }
        LocalDate current = LocalDate.now();
        if (!practicedDates.contains(current)) {
            current = current.minusDays(1);
        }
        int streak = 0;
        while (practicedDates.contains(current)) {
            streak++;
            current = current.minusDays(1);
        }
        System.out.println("Current practice streak: " + streak + (streak == 1 ? " day." : " days."));
    }

    private static void printItems(List<PracticeItem> items) {
        for (int i = 0; i < items.size(); i++) {
            PracticeItem item = items.get(i);
            System.out.printf("%d. [%s] %s — %s, speed %s, goal %d reps%n",
                    i + 1, item.completed() ? "x" : " ", item.title(),
                    item.type().displayName(), item.speed(), item.repetitionGoal());
        }
    }

    private static int countCompleted(List<PracticeItem> items) {
        int completed = 0;
        for (PracticeItem item : items) {
            if (item.completed()) {
                completed++;
            }
        }
        return completed;
    }

    private static int percentage(int completed, int total) {
        return total == 0 ? 0 : (int) Math.round(completed * 100.0 / total);
    }

    private static PracticeType readType(Scanner scanner) {
        PracticeType[] types = PracticeType.values();
        System.out.println("Item type:");
        for (int i = 0; i < types.length; i++) {
            System.out.printf("%d. %s%n", i + 1, types[i].displayName());
        }
        int choice = readInt(scanner, "Type: ", 1, types.length);
        return types[choice - 1];
    }

    private static Set<DayOfWeek> readWeekdays(Scanner scanner) {
        while (true) {
            System.out.print("Repeat on (for example, MON,WED,FRI): ");
            String input = scanner.nextLine().trim();
            Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
            try {
                for (String part : input.split(",")) {
                    days.add(DayOfWeek.valueOf(part.trim().toUpperCase()));
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Use weekday abbreviations MON,TUE,WED,THU,FRI,SAT,SUN.");
                continue;
            }
            if (!days.isEmpty()) {
                return days;
            }
            System.out.println("Enter at least one weekday.");
        }
    }

    private static LocalDate readDate(Scanner scanner, String prompt, LocalDate defaultDate) {
        while (true) {
            System.out.print(prompt + "[" + defaultDate + "]: ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return defaultDate;
            }
            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException e) {
                System.out.println("Enter a valid date in YYYY-MM-DD format.");
            }
        }
    }

    private static YearMonth readMonth(Scanner scanner, YearMonth defaultMonth) {
        while (true) {
            System.out.print("Month (YYYY-MM): [" + defaultMonth + "]: ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return defaultMonth;
            }
            try {
                return YearMonth.parse(input);
            } catch (DateTimeParseException e) {
                System.out.println("Enter a valid month in YYYY-MM format.");
            }
        }
    }

    private static String readNonBlank(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("This value cannot be blank.");
        }
    }

    private static int readInt(Scanner scanner, String prompt, int minimum, int maximum) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= minimum && value <= maximum) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // The prompt below handles non-numeric input.
            }
            System.out.printf("Enter a whole number from %d to %d.%n", minimum, maximum);
        }
    }

    private enum PracticeType {
        ADAVU("Adavu"),
        THALAM("Thalam"),
        PIECE("Piece"),
        TASK("Task");

        private final String displayName;

        PracticeType(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    private record PracticeItem(String title, PracticeType type, LocalDate date,
                                String speed, int repetitionGoal, boolean completed) {
        private PracticeItem {
            if (title.isBlank() || speed.isBlank() || repetitionGoal < 1) {
                throw new IllegalArgumentException("Practice items need a title, speed, and positive repetition goal.");
            }
        }

        private PracticeItem withCompleted(boolean value) {
            return new PracticeItem(title, type, date, speed, repetitionGoal, value);
        }
    }

    private record RecurringPractice(String title, PracticeType type, String speed,
                                     int repetitionGoal, LocalDate startsOn, Set<DayOfWeek> days) {
        private RecurringPractice {
            if (title.isBlank() || speed.isBlank() || repetitionGoal < 1 || days.isEmpty()) {
                throw new IllegalArgumentException("Recurring practice needs a title, speed, goal, and weekdays.");
            }
            days = Set.copyOf(days);
        }
    }

    private static final class Planner {
        private final Path dataFile;
        private final List<PracticeItem> items = new ArrayList<>();
        private final List<RecurringPractice> recurringPractices = new ArrayList<>();

        private Planner(Path dataFile) {
            this.dataFile = dataFile;
        }

        private void load() throws IOException {
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

        private void save() throws IOException {
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

        private void addItem(PracticeItem item) {
            items.add(item);
        }

        private void addRecurrence(RecurringPractice recurrence) {
            recurringPractices.add(recurrence);
        }

        private void setCompleted(PracticeItem item, boolean completed) {
            int index = items.indexOf(item);
            if (index < 0) {
                throw new IllegalStateException("The selected practice item is no longer in the plan.");
            }
            items.set(index, item.withCompleted(completed));
        }

        private List<PracticeItem> itemsOn(LocalDate date) {
            return items.stream().filter(item -> item.date().equals(date)).toList();
        }

        private List<PracticeItem> items() {
            return List.copyOf(items);
        }

        private void materializeRecurringThrough(LocalDate endDate) {
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
}
