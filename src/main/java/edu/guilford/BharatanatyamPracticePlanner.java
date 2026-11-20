package edu.guilford;

import java.io.IOException;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class BharatanatyamPracticePlanner {
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
        boolean completed = !item.completed();
        planner.setCompleted(item, completed);
        planner.save();
        System.out.println(item.title() + (completed ? " marked complete." : " marked incomplete."));
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
            System.out.print("Repeat on (for example, MONDAY,WEDNESDAY,FRIDAY; abbreviations also work): ");
            String input = scanner.nextLine().trim();
            Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
            try {
                for (String part : input.split(",")) {
                    String day = part.trim().toUpperCase();
                    days.add(switch (day) {
                        case "MON" -> DayOfWeek.MONDAY;
                        case "TUE" -> DayOfWeek.TUESDAY;
                        case "WED" -> DayOfWeek.WEDNESDAY;
                        case "THU" -> DayOfWeek.THURSDAY;
                        case "FRI" -> DayOfWeek.FRIDAY;
                        case "SAT" -> DayOfWeek.SATURDAY;
                        case "SUN" -> DayOfWeek.SUNDAY;
                        default -> DayOfWeek.valueOf(day);
                    });
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Use full weekday names or abbreviations (for example, MONDAY or MON).");
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

}
