package edu.guilford;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.locks.LockSupport;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.JDialog;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

public final class PracticePlannerFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final String[] WEEKDAY_HEADERS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    private static final Color BURGUNDY = new Color(104, 18, 52);
    private static final Color DEEP_BURGUNDY = new Color(62, 10, 32);
    private static final Color LIGHT_BURGUNDY = new Color(132, 43, 75);
    private static final Color GOLD = new Color(212, 175, 55);
    private static final Color SOFT_GOLD = new Color(238, 205, 112);
    private static final Color CREAM = new Color(255, 248, 235);
    private static final Font DISPLAY_FONT = new Font("Palatino Linotype", Font.BOLD, 18);
    private static final Font SERIF_FONT = new Font("Nirmala UI", Font.PLAIN, 14);
    private static final Font SMALL_SERIF_FONT = new Font("Georgia", Font.PLAIN, 12);
    private static final Border DAY_BORDER = BorderFactory.createLineBorder(GOLD, 1);
    private static final Border SELECTED_DAY_BORDER = BorderFactory.createLineBorder(
            SOFT_GOLD, 3);
    private static final Border ORNATE_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GOLD, 2),
            BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(SOFT_GOLD, 1),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)));

    private final transient Planner planner;
    private final JPanel calendarPanel = new JPanel(new GridLayout(0, 7, 4, 4));
    private final JPanel checklistPanel = new JPanel();
    private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel selectedDateLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel weeklyProgressLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel streakLabel = new JLabel("", SwingConstants.CENTER);
    private YearMonth displayedMonth = YearMonth.now();
    private LocalDate selectedDate = LocalDate.now();

    public PracticePlannerFrame(Planner planner) throws IOException {
        super("Bharatanatyam Practice Planner");
        this.planner = planner;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 650));
        setSize(1100, 780);
        setLocationRelativeTo(null);

        add(createToolbar(), BorderLayout.NORTH);
        add(createMainPanel(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);
        refreshCalendar();
        applyTheme();
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(8, 8));
        JLabel ornament = new JLabel("✦  ◇  PRACTICE WITH GRACE  ◇  ✦", SwingConstants.CENTER);
        ornament.setFont(DISPLAY_FONT);
        ornament.setForeground(SOFT_GOLD);
        ornament.setBorder(BorderFactory.createEmptyBorder(4, 0, 6, 0));
        JPanel navigation = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton previous = new JButton("<");
        previous.setToolTipText("Previous month");
        previous.addActionListener(event -> changeMonth(-1));
        JButton next = new JButton(">");
        next.setToolTipText("Next month");
        next.addActionListener(event -> changeMonth(1));
        JButton today = new JButton("Today");
        today.addActionListener(event -> {
            displayedMonth = YearMonth.now();
            selectedDate = LocalDate.now();
            refreshSafely();
        });
        navigation.add(previous);
        navigation.add(monthLabel);
        navigation.add(next);
        navigation.add(today);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton adavuLibrary = new JButton("Adavu Library");
        adavuLibrary.addActionListener(event -> openAdavuLibrary());
        JButton addPractice = new JButton("Add Practice");
        addPractice.addActionListener(event -> addPracticeItem());
        JButton addRecurring = new JButton("Add Recurring Practice");
        addRecurring.addActionListener(event -> addRecurringPractice());
        actions.add(adavuLibrary);
        actions.add(addPractice);
        actions.add(addRecurring);

        toolbar.setBorder(BorderFactory.createCompoundBorder(
                ORNATE_BORDER, BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        toolbar.add(ornament, BorderLayout.NORTH);
        toolbar.add(navigation, BorderLayout.WEST);
        toolbar.add(actions, BorderLayout.EAST);
        return toolbar;
    }

    private JPanel createMainPanel() {
        JPanel calendarContainer = new JPanel(new BorderLayout());
        calendarContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD, 1),
                BorderFactory.createEmptyBorder(5, 8, 5, 5)));
        calendarContainer.add(calendarPanel, BorderLayout.CENTER);
        calendarContainer.setPreferredSize(new Dimension(740, 600));

        JPanel checklistContainer = new JPanel(new BorderLayout(4, 4));
        checklistContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 8)));
        checklistPanel.setLayout(new BoxLayout(checklistPanel, BoxLayout.Y_AXIS));
        selectedDateLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));
        checklistContainer.add(selectedDateLabel, BorderLayout.NORTH);
        checklistContainer.add(new JScrollPane(checklistPanel), BorderLayout.CENTER);
        checklistContainer.setPreferredSize(new Dimension(340, 600));

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(calendarContainer, BorderLayout.CENTER);
        mainPanel.add(checklistContainer, BorderLayout.EAST);
        return mainPanel;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new GridLayout(1, 2, 8, 0));
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, GOLD),
                BorderFactory.createEmptyBorder(8, 8, 10, 8)));
        footer.add(weeklyProgressLabel);
        footer.add(streakLabel);
        return footer;
    }

    private void changeMonth(int offset) {
        displayedMonth = displayedMonth.plusMonths(offset);
        int dayOfMonth = Math.min(selectedDate.getDayOfMonth(), displayedMonth.lengthOfMonth());
        selectedDate = displayedMonth.atDay(dayOfMonth);
        refreshSafely();
    }

    private void refreshSafely() {
        try {
            refreshCalendar();
        } catch (IOException e) {
            showError("Could not save practice planner data: " + e.getMessage());
        }
    }

    record Adavu(String name, String setName, String jathi, String notes) {
    }

    final class ThalamTrainerPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private static final int[] NADAIS = {3, 4, 5, 7, 9};
        private static final String[] NADA_NAMES = {"Tisra", "Chatusra", "Khanda", "Misra", "Sankirna"};
        private static final String[] SOLKATTU = {
                "Ta - Ki - Ta",
                "Ta - Ka - Di - Mi",
                "Ta - Ka - Ta - Ki - Ta",
                "Ta - Ki - Ta - Ta - Ka - Di - Mi",
                "Ta - Ka - Di - Mi - Ta - Ka - Ta - Ki - Ta"
        };
        private static final int[] JATI_COUNTS = {3, 4, 5, 7, 9};
        private static final String[] KALAM_OPTIONS = {"First (1x)", "Second (2x)", "Third (4x)"};
        private static final TalaPattern[] TALA_PATTERNS = {
                new TalaPattern("Adi Tala (8)", new TalaGesture[]{TalaGesture.CLAP,
                        TalaGesture.FINGER, TalaGesture.FINGER, TalaGesture.FINGER,
                        TalaGesture.CLAP, TalaGesture.WAVE, TalaGesture.CLAP, TalaGesture.WAVE}),
                new TalaPattern("Rupaka Tala (3)", new TalaGesture[]{TalaGesture.CLAP,
                        TalaGesture.FINGER, TalaGesture.WAVE}),
                new TalaPattern("Misra Chapu (7)", new TalaGesture[]{TalaGesture.CLAP,
                        TalaGesture.WAVE, TalaGesture.FINGER, TalaGesture.CLAP,
                        TalaGesture.WAVE, TalaGesture.FINGER, TalaGesture.WAVE}),
                new TalaPattern("Khanda Chapu (5)", new TalaGesture[]{TalaGesture.CLAP,
                        TalaGesture.WAVE, TalaGesture.FINGER, TalaGesture.WAVE, TalaGesture.FINGER}),
                new TalaPattern("Custom", new TalaGesture[0])
        };

        private final JComboBox<String> nadaiField = new JComboBox<>(NADA_NAMES);
        private final JComboBox<String> kalamField = new JComboBox<>(KALAM_OPTIONS);
        private final JComboBox<TalaPattern> talaField = new JComboBox<>(TALA_PATTERNS);
        private final JComboBox<String> jatiField = new JComboBox<>(
                new String[]{"Tisra Jati (3)", "Chatusra Jati (4)", "Khanda Jati (5)",
                        "Misra Jati (7)", "Sankirna Jati (9)"});
        private final JSpinner bpmField = new JSpinner(new SpinnerNumberModel(60, 30, 240, 1));
        private final JSpinner cycleBeatsField = new JSpinner(new SpinnerNumberModel(8, 1, 32, 1));
        private final JCheckBox loopField = new JCheckBox("Loop cycle", true);
        private final JLabel beatLabel = new JLabel("Ready", SwingConstants.CENTER);
        private final JLabel cycleLabel = new JLabel("Cycle: 0 / 8", SwingConstants.CENTER);
        private final JLabel syllableLabel = new JLabel("Nadai: Ta - Ka - Di - Mi", SwingConstants.CENTER);
        private final JLabel audioStatus = new JLabel(" ");
        private final JPanel beatPanel = new JPanel(new GridLayout(0, 8, 4, 4));
        private final JPanel subdivisionPanel = new JPanel(new GridLayout(0, 9, 3, 3));
        private final JButton startButton = new JButton("Start");
        private final JButton stopButton = new JButton("Stop");
        private final Timer visualTimer;
        private transient Clip samamClip;
        private transient Clip clapClip;
        private transient Clip knockClip;
        private transient Clip subTickClip;
        private transient Clip subAccentClip;
        private volatile boolean running;
        private transient Thread audioThread;
        private long playbackStartNanos;
        private int subdivisionsPerBeat;
        private boolean updatingTalaControls;

        ThalamTrainerPanel(String practiceTitle) {
            super(new BorderLayout(12, 12));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(GOLD, 2),
                    BorderFactory.createEmptyBorder(12, 14, 12, 14)));
            setBackground(DEEP_BURGUNDY);

            JLabel title = new JLabel(practiceTitle + " — Thalam Trainer", SwingConstants.CENTER);
            title.setFont(new Font("Palatino Linotype", Font.BOLD, 20));
            title.setForeground(GOLD);

            JPanel controls = new JPanel(new GridLayout(2, 5, 8, 6));
            controls.setOpaque(false);
            addControl(controls, "Tempo (BPM)", bpmField);
            addControl(controls, "Tala", talaField);
            addControl(controls, "Beats per cycle", cycleBeatsField);
            addControl(controls, "Jati (accent group)", jatiField);
            addControl(controls, "Nadai (subdivision)", nadaiField);
            addControl(controls, "Kalam", kalamField);
            controls.add(loopField);
            controls.add(startButton);
            controls.add(stopButton);
            controls.add(new JLabel());

            JPanel visual = new JPanel(new BorderLayout(6, 8));
            visual.setOpaque(false);
            beatPanel.setOpaque(false);
            subdivisionPanel.setOpaque(false);
            beatLabel.setFont(new Font("Georgia", Font.BOLD, 30));
            beatLabel.setForeground(SOFT_GOLD);
            cycleLabel.setForeground(CREAM);
            cycleLabel.setFont(SERIF_FONT);
            syllableLabel.setForeground(GOLD);
            syllableLabel.setFont(SERIF_FONT);
            visual.add(beatLabel, BorderLayout.NORTH);
            visual.add(beatPanel, BorderLayout.CENTER);
            visual.add(subdivisionPanel, BorderLayout.SOUTH);

            JPanel footer = new JPanel(new BorderLayout());
            footer.setOpaque(false);
            audioStatus.setForeground(SOFT_GOLD);
            footer.add(syllableLabel, BorderLayout.CENTER);
            footer.add(audioStatus, BorderLayout.SOUTH);

            JPanel mainVisual = new JPanel(new BorderLayout(4, 4));
            mainVisual.setOpaque(false);
            mainVisual.add(cycleLabel, BorderLayout.NORTH);
            mainVisual.add(visual, BorderLayout.CENTER);

            JPanel top = new JPanel(new BorderLayout(6, 8));
            top.setOpaque(false);
            top.add(title, BorderLayout.NORTH);
            top.add(controls, BorderLayout.SOUTH);
            add(top, BorderLayout.NORTH);
            add(mainVisual, BorderLayout.CENTER);
            add(footer, BorderLayout.SOUTH);

            nadaiField.setSelectedIndex(1);
            nadaiField.addActionListener(event -> settingsChanged());
            kalamField.addActionListener(event -> settingsChanged());
            jatiField.addActionListener(event -> settingsChanged());
            talaField.addActionListener(event -> talaChanged());
            cycleBeatsField.addChangeListener(event -> {
                if (!updatingTalaControls) {
                    settingsChanged();
                }
            });
            bpmField.addChangeListener(event -> settingsChanged());
            startButton.addActionListener(event -> start());
            stopButton.addActionListener(event -> stop());
            loopField.addActionListener(event -> settingsChanged());
            loopField.setOpaque(false);
            loopField.setForeground(CREAM);
            styleTrainerCombo(talaField);
            styleTrainerCombo(jatiField);
            styleTrainerCombo(nadaiField);
            styleTrainerCombo(kalamField);
            bpmField.setBackground(CREAM);
            bpmField.setForeground(DEEP_BURGUNDY);
            cycleBeatsField.setBackground(CREAM);
            cycleBeatsField.setForeground(DEEP_BURGUNDY);
            cycleBeatsField.setEnabled(false);
            styleTrainerButton(startButton);
            styleTrainerButton(stopButton);
            visualTimer = new Timer(15, event -> updateVisualFromClock());
            visualTimer.setCoalesce(true);
            initializeAudio();
            settingsChanged();
        }

        private void styleTrainerCombo(JComboBox<?> comboBox) {
            comboBox.setBackground(CREAM);
            comboBox.setForeground(DEEP_BURGUNDY);
            comboBox.setFont(SERIF_FONT);
        }

        private void styleTrainerButton(JButton button) {
            button.setBackground(LIGHT_BURGUNDY);
            button.setForeground(GOLD);
            button.setFont(SERIF_FONT);
            button.setOpaque(true);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(GOLD, 1),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        }

        private void addControl(JPanel panel, String labelText, java.awt.Component control) {
            JPanel fieldPanel = new JPanel(new BorderLayout(2, 2));
            fieldPanel.setOpaque(false);
            JLabel label = new JLabel(labelText);
            label.setForeground(CREAM);
            label.setFont(SMALL_SERIF_FONT);
            fieldPanel.add(label, BorderLayout.NORTH);
            fieldPanel.add(control, BorderLayout.CENTER);
            panel.add(fieldPanel);
        }

        private void initializeAudio() {
            try {
                samamClip = createSoundClip(1080, 220, 10000, true);
                clapClip = createCymbalClip();
                knockClip = createSoundClip(520, 70, 5500);
                subTickClip = createSoundClip(820, 28, 3000);
                subAccentClip = createSoundClip(940, 34, 5000);
                audioStatus.setText("Audio ready");
            } catch (LineUnavailableException | IllegalArgumentException e) {
                closeClips();
                audioStatus.setText("Audio unavailable: " + e.getMessage());
            }
        }

        private Clip createSoundClip(int frequency, int durationMs, int amplitude)
                throws LineUnavailableException {
            return createSoundClip(frequency, durationMs, amplitude, false);
        }

        private Clip createSoundClip(int frequency, int durationMs, int amplitude, boolean resonant)
                throws LineUnavailableException {
            int sampleRate = 22050;
            int sampleCount = sampleRate * durationMs / 1000;
            byte[] data = new byte[sampleCount * 2];
            for (int i = 0; i < sampleCount; i++) {
                double envelope = 1.0 - (double) i / sampleCount;
                double phase = 2 * Math.PI * frequency * i / sampleRate;
                double waveform = resonant
                        ? Math.sin(phase) * 0.72 + Math.sin(phase * 2.73) * 0.28
                        : Math.sin(phase) * 0.72 + Math.sin(phase * 1.91) * 0.28;
                short sample = (short) (waveform * envelope * amplitude);
                data[i * 2] = (byte) (sample & 0xff);
                data[i * 2 + 1] = (byte) ((sample >> 8) & 0xff);
            }
            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            Clip clip = AudioSystem.getClip();
            clip.open(format, data, 0, data.length);
            return clip;
        }

        private Clip createCymbalClip() throws LineUnavailableException {
            int sampleRate = 22050;
            int sampleCount = sampleRate * 65 / 1000;
            byte[] data = new byte[sampleCount * 2];
            int noiseState = 0x1f123bb5;
            for (int i = 0; i < sampleCount; i++) {
                noiseState ^= noiseState << 13;
                noiseState ^= noiseState >>> 17;
                noiseState ^= noiseState << 5;
                double noise = ((noiseState & 0xffff) / 32767.5) - 1.0;
                double phase = 2 * Math.PI * i / sampleRate;
                double metallic = Math.sin(phase * 2310) * 0.55
                        + Math.sin(phase * 3170) * 0.3 + Math.sin(phase * 4110) * 0.15;
                double envelope = Math.exp(-i / (sampleRate * 0.018));
                short sample = (short) ((noise * 0.45 + metallic * 0.55) * envelope * 15000);
                data[i * 2] = (byte) (sample & 0xff);
                data[i * 2 + 1] = (byte) ((sample >> 8) & 0xff);
            }
            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            Clip clip = AudioSystem.getClip();
            clip.open(format, data, 0, data.length);
            return clip;
        }

        private void settingsChanged() {
            subdivisionsPerBeat = selectedNadaiPulses() * selectedKalamMultiplier();
            syllableLabel.setText("Nadai: " + selectedNadaiName() + " — "
                    + selectedSolkattu() + "    |    " + subdivisionsPerBeat + " pulses per beat");
            if (running) {
                start();
            }
            updateBeatIndicators();
            updateSubdivisionIndicators(-1);
        }

        private void talaChanged() {
            TalaPattern selected = (TalaPattern) talaField.getSelectedItem();
            if (selected == null) {
                return;
            }
            boolean custom = selected.gestures().length == 0;
            cycleBeatsField.setEnabled(custom);
            if (!custom && (Integer) cycleBeatsField.getValue() != selected.gestures().length) {
                updatingTalaControls = true;
                cycleBeatsField.setValue(selected.gestures().length);
                updatingTalaControls = false;
            }
            settingsChanged();
        }

        private SchedulerSettings schedulerSettings() {
            TalaPattern selected = (TalaPattern) talaField.getSelectedItem();
            int beatsPerCycle = (Integer) cycleBeatsField.getValue();
            TalaGesture[] gestures = selected.gestures();
            if (gestures.length == 0) {
                gestures = new TalaGesture[beatsPerCycle];
                for (int beat = 0; beat < beatsPerCycle; beat++) {
                    gestures[beat] = beat == 0 || beat % 4 == 0 ? TalaGesture.CLAP
                            : beat % 2 == 0 ? TalaGesture.FINGER : TalaGesture.WAVE;
                }
            }
            return new SchedulerSettings((Integer) bpmField.getValue(), beatsPerCycle,
                    selectedNadaiPulses(), selectedKalamMultiplier(), selectedJatiCount(),
                    selectedNadaiName(), selectedSolkattu(), gestures, loopField.isSelected());
        }

        private void updateBeatIndicators() {
            int beatsPerCycle = (Integer) cycleBeatsField.getValue();
            int jati = selectedJatiCount();
            beatPanel.removeAll();
            for (int index = 0; index < beatsPerCycle; index++) {
                JLabel indicator = new JLabel(Integer.toString(index + 1), SwingConstants.CENTER);
                indicator.setOpaque(true);
                indicator.setFont(SMALL_SERIF_FONT);
                indicator.setBorder(BorderFactory.createLineBorder(GOLD, 1));
                indicator.setBackground(index == 0 ? SOFT_GOLD : CREAM);
                indicator.setForeground(DEEP_BURGUNDY);
                indicator.setToolTipText("Beat " + (index + 1)
                        + (index == 0 ? " — Samam" : index % jati == 0 ? " — Jati accent" : ""));
                beatPanel.add(indicator);
            }
            cycleLabel.setText("Cycle: 0 / " + beatsPerCycle + " beats    |    Jati accent every "
                    + jati + " beats");
            beatPanel.revalidate();
            beatPanel.repaint();
        }

        private void updateSubdivisionIndicators(int activeSubdivision) {
            subdivisionPanel.removeAll();
            for (int index = 0; index < subdivisionsPerBeat; index++) {
                JLabel indicator = new JLabel(Integer.toString(index + 1), SwingConstants.CENTER);
                indicator.setOpaque(true);
                indicator.setFont(SMALL_SERIF_FONT);
                indicator.setBorder(BorderFactory.createLineBorder(GOLD, 1));
                indicator.setBackground(index == activeSubdivision ? GOLD : CREAM);
                indicator.setForeground(DEEP_BURGUNDY);
                subdivisionPanel.add(indicator);
            }
            subdivisionPanel.revalidate();
            subdivisionPanel.repaint();
        }

        private void start() {
            stop();
            SchedulerSettings settings = schedulerSettings();
            subdivisionsPerBeat = settings.nadaiPulses() * settings.kalamMultiplier();
            playbackStartNanos = System.nanoTime() + 100_000_000L;
            running = true;
            startButton.setEnabled(false);
            visualTimer.start();
            audioThread = new Thread(() -> runAudioScheduler(settings, playbackStartNanos),
                    "carnatic-metronome-audio");
            audioThread.setDaemon(true);
            audioThread.start();
        }

        private void runAudioScheduler(SchedulerSettings settings, long startNanos) {
            try {
                long beatDurationNanos = 60_000_000_000L / settings.bpm();
                int pulsesPerBeat = settings.nadaiPulses() * settings.kalamMultiplier();
                for (long pulseIndex = 0; running; pulseIndex++) {
                    long absoluteBeat = pulseIndex / pulsesPerBeat;
                    int subPulseIndex = (int) (pulseIndex % pulsesPerBeat);
                    long beatStart = startNanos + absoluteBeat * beatDurationNanos;
                    long deadline = beatStart + beatDurationNanos * subPulseIndex / pulsesPerBeat;
                    waitUntil(deadline);
                    if (!running) {
                        break;
                    }

                    int cycleBeat = (int) (absoluteBeat % settings.beatsPerCycle());
                    if (subPulseIndex == 0) {
                        playTalaGesture(cycleBeat, settings.gestures());
                    } else {
                        int nadaiPulse = subPulseIndex % settings.nadaiPulses();
                        playSubPulse(isNadaiAccent(nadaiPulse, settings.nadaiPulses()));
                    }

                    boolean lastPulse = cycleBeat == settings.beatsPerCycle() - 1
                            && subPulseIndex == pulsesPerBeat - 1;
                    if (lastPulse && !settings.loop()) {
                        running = false;
                        SwingUtilities.invokeLater(this::finishPlayback);
                        break;
                    }
                }
            } catch (IllegalStateException e) {
                running = false;
                SwingUtilities.invokeLater(() -> {
                    finishPlayback();
                    audioStatus.setText("Audio playback failed: " + e.getMessage());
                });
            }
        }

        private void waitUntil(long deadlineNanos) {
            while (running) {
                long remaining = deadlineNanos - System.nanoTime();
                if (remaining <= 0) {
                    return;
                }
                if (remaining > 200_000L) {
                    LockSupport.parkNanos(remaining - 100_000L);
                } else {
                    Thread.onSpinWait();
                }
            }
        }

        private void playTalaGesture(int beatIndex, TalaGesture[] gestures) {
            if (beatIndex == 0) {
                playClip(samamClip);
            } else if (gestures[beatIndex] == TalaGesture.CLAP) {
                playClip(clapClip);
            } else {
                playClip(knockClip);
            }
        }

        private void playSubPulse(boolean accent) {
            playClip(accent ? subAccentClip : subTickClip);
        }

        private void playClip(Clip clip) {
            if (clip == null) {
                return;
            }
            clip.stop();
            clip.setFramePosition(0);
            clip.start();
        }

        private boolean isNadaiAccent(int pulseIndex, int nadaiPulses) {
            return switch (nadaiPulses) {
                case 4, 5 -> pulseIndex == 2;
                case 7 -> pulseIndex == 3;
                case 9 -> pulseIndex == 4;
                default -> false;
            };
        }

        private void updateVisualFromClock() {
            if (!running) {
                return;
            }
            if (System.nanoTime() < playbackStartNanos) {
                beatLabel.setText("Starting…");
                return;
            }
            SchedulerSettings settings = schedulerSettings();
            long beatDurationNanos = 60_000_000_000L / settings.bpm();
            long elapsedNanos = Math.max(0, System.nanoTime() - playbackStartNanos);
            long absoluteBeat = elapsedNanos / beatDurationNanos;
            long withinBeatNanos = elapsedNanos % beatDurationNanos;
            int beatIndex = (int) (absoluteBeat % settings.beatsPerCycle());
            int subPulseIndex = (int) (withinBeatNanos * subdivisionsPerBeat / beatDurationNanos);
            subPulseIndex = Math.min(subPulseIndex, subdivisionsPerBeat - 1);

            highlightBeat(beatIndex);
            updateSubdivisionIndicators(subPulseIndex);
            cycleLabel.setText("Cycle: " + (beatIndex + 1) + " / " + settings.beatsPerCycle()
                    + " beats    |    Jati accent every " + settings.jatiCount() + " beats");
            String[] syllables = settings.solkattu().split(" - ");
            if (subPulseIndex == 0) {
                beatLabel.setText("Beat " + (beatIndex + 1));
            } else {
                beatLabel.setText("Beat " + (beatIndex + 1) + " — Sub-pulse "
                        + (subPulseIndex + 1) + " — "
                        + syllables[subPulseIndex % settings.nadaiPulses()]);
            }
        }

        private void highlightBeat(int selectedBeat) {
            int jati = selectedJatiCount();
            for (int i = 0; i < beatPanel.getComponentCount(); i++) {
                JLabel indicator = (JLabel) beatPanel.getComponent(i);
                if (i == selectedBeat) {
                    indicator.setBackground(GOLD);
                } else if (i == 0) {
                    indicator.setBackground(SOFT_GOLD);
                } else if (i % jati == 0) {
                    indicator.setBackground(new Color(246, 224, 166));
                } else {
                    indicator.setBackground(CREAM);
                }
            }
        }

        private void finishPlayback() {
            visualTimer.stop();
            running = false;
            audioThread = null;
            startButton.setEnabled(true);
            beatLabel.setText("Cycle complete");
            updateSubdivisionIndicators(-1);
        }

        private void stop() {
            running = false;
            visualTimer.stop();
            Thread thread = audioThread;
            if (thread != null && thread != Thread.currentThread()) {
                thread.interrupt();
                try {
                    thread.join(250);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            audioThread = null;
            startButton.setEnabled(true);
            beatLabel.setText("Ready");
            updateBeatIndicators();
            updateSubdivisionIndicators(-1);
        }

        private int selectedNadaiPulses() {
            return NADAIS[nadaiField.getSelectedIndex()];
        }

        private String selectedNadaiName() {
            return NADA_NAMES[nadaiField.getSelectedIndex()];
        }

        private String selectedSolkattu() {
            return SOLKATTU[nadaiField.getSelectedIndex()];
        }

        private int selectedJatiCount() {
            return JATI_COUNTS[jatiField.getSelectedIndex()];
        }

        private int selectedKalamMultiplier() {
            return switch (kalamField.getSelectedIndex()) {
                case 1 -> 2;
                case 2 -> 4;
                default -> 1;
            };
        }

        private void closeClips() {
            closeClip(samamClip);
            closeClip(clapClip);
            closeClip(knockClip);
            closeClip(subTickClip);
            closeClip(subAccentClip);
            samamClip = null;
            clapClip = null;
            knockClip = null;
            subTickClip = null;
            subAccentClip = null;
        }

        private void closeClip(Clip clip) {
            if (clip != null) {
                clip.close();
            }
        }

        void disposeTrainer() {
            stop();
            closeClips();
        }

        private record SchedulerSettings(int bpm, int beatsPerCycle, int nadaiPulses,
                                         int kalamMultiplier, int jatiCount,
                                         String nadaiName, String solkattu,
                                         TalaGesture[] gestures, boolean loop) {
        }

        private enum TalaGesture {
            CLAP,
            FINGER,
            WAVE
        }

        private record TalaPattern(String name, TalaGesture[] gestures) {
            @Override
            public String toString() {
                return name;
            }
        }
    }

    private void refreshCalendar() throws IOException {
        LocalDate weekEnd = selectedDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDate through = displayedMonth.atEndOfMonth().isAfter(weekEnd)
                ? displayedMonth.atEndOfMonth() : weekEnd;
        planner.materializeRecurringThrough(through);
        planner.save();

        monthLabel.setText("◆  " + displayedMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault())
                + " " + displayedMonth.getYear() + "  ◆");
        rebuildCalendar();
        rebuildChecklist();
        updateProgress();
        applyTheme();
        revalidate();
        repaint();
    }

    private void rebuildCalendar() {
        calendarPanel.removeAll();
        for (String weekday : WEEKDAY_HEADERS) {
            JLabel header = new JLabel(weekday, SwingConstants.CENTER);
            header.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
            header.setForeground(GOLD);
            header.setFont(SERIF_FONT);
            calendarPanel.add(header);
        }

        int leadingDays = displayedMonth.atDay(1).getDayOfWeek().getValue() - 1;
        int cellCount = 42;
        for (int cell = 0; cell < cellCount; cell++) {
            int dayNumber = cell - leadingDays + 1;
            if (dayNumber < 1 || dayNumber > displayedMonth.lengthOfMonth()) {
                calendarPanel.add(new JLabel());
                continue;
            }

            LocalDate date = displayedMonth.atDay(dayNumber);
            List<PracticeItem> items = planner.itemsOn(date);
            int completed = countCompleted(items);
            String itemSummary = items.isEmpty() ? " " : completed + "/" + items.size() + " practice";
            JButton dayButton = new JButton("<html><center><b>" + dayNumber + "</b><br>"
                    + itemSummary + "</center></html>");
            dayButton.setHorizontalAlignment(SwingConstants.CENTER);
            dayButton.setFocusPainted(false);
            dayButton.setFont(SERIF_FONT);
            dayButton.setBorder(date.equals(selectedDate) ? SELECTED_DAY_BORDER : DAY_BORDER);
            dayButton.setToolTipText(items.isEmpty() ? date.toString()
                    : date + ": " + completed + " of " + items.size() + " complete");
            dayButton.addActionListener(event -> {
                selectedDate = date;
                refreshSafely();
            });
            calendarPanel.add(dayButton);
        }
        calendarPanel.revalidate();
        calendarPanel.repaint();
    }

    private void rebuildChecklist() {
        selectedDateLabel.setText("<html><center>Practice checklist<br>"
                + selectedDate + " (" + selectedDate.getDayOfWeek() + ")</center></html>");
        checklistPanel.removeAll();
        List<PracticeItem> items = planner.itemsOn(selectedDate);
        if (items.isEmpty()) {
            JLabel emptyMessage = new JLabel("No practice planned for this day.");
            emptyMessage.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            checklistPanel.add(emptyMessage);
        } else {
            for (PracticeItem item : items) {
                JCheckBox checkBox = new JCheckBox(item.title() + " - " + item.type().displayName()
                        + " | " + item.speed() + " | " + item.repetitionGoal() + " reps",
                        item.completed());
                checkBox.setAlignmentX(LEFT_ALIGNMENT);
                checkBox.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
                checkBox.addActionListener(event -> {
                    planner.setCompleted(item, checkBox.isSelected());
                    refreshSafely();
                });
                JButton practiceButton = new JButton("Open Practice");
                practiceButton.setAlignmentX(LEFT_ALIGNMENT);
                practiceButton.addActionListener(event -> openPracticeScreen(item));
                JPanel itemPanel = new JPanel();
                itemPanel.setLayout(new BoxLayout(itemPanel, BoxLayout.Y_AXIS));
                itemPanel.add(checkBox);
                itemPanel.add(practiceButton);
                checklistPanel.add(itemPanel);
                checklistPanel.add(Box.createVerticalStrut(4));
            }
        }
        checklistPanel.revalidate();
        checklistPanel.repaint();
    }

    private void updateProgress() {
        LocalDate monday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int total = 0;
        int completed = 0;
        for (int offset = 0; offset < 7; offset++) {
            List<PracticeItem> items = planner.itemsOn(monday.plusDays(offset));
            total += items.size();
            completed += countCompleted(items);
        }
        int percentage = total == 0 ? 0 : (int) Math.round(completed * 100.0 / total);
        weeklyProgressLabel.setText("Week of " + monday + ": " + completed + "/" + total
                + " complete (" + percentage + "%)");

        Set<LocalDate> completedDates = new HashSet<>();
        for (PracticeItem item : planner.items()) {
            if (item.completed()) {
                completedDates.add(item.date());
            }
        }
        LocalDate streakDate = LocalDate.now();
        if (!completedDates.contains(streakDate)) {
            streakDate = streakDate.minusDays(1);
        }
        int streak = 0;
        while (completedDates.contains(streakDate)) {
            streak++;
            streakDate = streakDate.minusDays(1);
        }
        streakLabel.setText("Current practice streak: " + streak + (streak == 1 ? " day" : " days"));
    }

    private void addPracticeItem() {
        JTextField titleField = new JTextField();
        JComboBox<PracticeType> typeField = new JComboBox<>(PracticeType.values());
        JTextField dateField = new JTextField(selectedDate.toString());
        JTextField speedField = new JTextField();
        JSpinner repetitionsField = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        JPanel fields = new JPanel(new GridLayout(0, 2, 6, 6));
        addFormRow(fields, "Practice item", titleField);
        addFormRow(fields, "Type", typeField);
        addFormRow(fields, "Date (YYYY-MM-DD)", dateField);
        addFormRow(fields, "Speed", speedField);
        addFormRow(fields, "Repetition goal", repetitionsField);
        applyThemeToComponent(fields);

        while (JOptionPane.showConfirmDialog(this, fields, "Add Practice",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            String title = titleField.getText().trim();
            String speed = speedField.getText().trim();
            try {
                if (title.isEmpty() || speed.isEmpty()) {
                    throw new IllegalArgumentException("Enter a practice item and speed.");
                }
                PracticeItem item = new PracticeItem(title, (PracticeType) typeField.getSelectedItem(),
                        LocalDate.parse(dateField.getText().trim()), speed,
                        (Integer) repetitionsField.getValue(), false);
                planner.addItem(item);
                refreshSafely();
                if (!item.date().equals(selectedDate)) {
                    selectedDate = item.date();
                    displayedMonth = YearMonth.from(selectedDate);
                    refreshSafely();
                }
                return;
            } catch (DateTimeParseException | IllegalArgumentException e) {
                showError("Enter a valid date, practice item, and speed.");
            }
        }
    }

    private void addRecurringPractice() {
        JTextField titleField = new JTextField();
        JComboBox<PracticeType> typeField = new JComboBox<>(PracticeType.values());
        JTextField startDateField = new JTextField(selectedDate.toString());
        JTextField speedField = new JTextField();
        JSpinner repetitionsField = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        JCheckBox[] weekdayFields = new JCheckBox[DayOfWeek.values().length];
        JPanel fields = new JPanel(new GridLayout(0, 2, 6, 6));
        addFormRow(fields, "Practice item", titleField);
        addFormRow(fields, "Type", typeField);
        addFormRow(fields, "Start date (YYYY-MM-DD)", startDateField);
        addFormRow(fields, "Speed", speedField);
        addFormRow(fields, "Repetition goal", repetitionsField);
        fields.add(new JLabel("Repeat on"));
        JPanel weekdays = new JPanel(new GridLayout(0, 2));
        DayOfWeek[] daysOfWeek = DayOfWeek.values();
        for (int i = 0; i < daysOfWeek.length; i++) {
            DayOfWeek day = daysOfWeek[i];
            weekdayFields[i] = new JCheckBox(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()));
            weekdays.add(weekdayFields[i]);
        }
        fields.add(weekdays);
        applyThemeToComponent(fields);

        while (JOptionPane.showConfirmDialog(this, fields, "Add Recurring Practice",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            String title = titleField.getText().trim();
            String speed = speedField.getText().trim();
            Set<DayOfWeek> selectedDays = EnumSet.noneOf(DayOfWeek.class);
            for (int i = 0; i < weekdayFields.length; i++) {
                if (weekdayFields[i].isSelected()) {
                    selectedDays.add(daysOfWeek[i]);
                }
            }
            try {
                if (title.isEmpty() || speed.isEmpty() || selectedDays.isEmpty()) {
                    throw new IllegalArgumentException("Enter a practice item, speed, and at least one weekday.");
                }
                RecurringPractice recurrence = new RecurringPractice(title,
                        (PracticeType) typeField.getSelectedItem(), speed,
                        (Integer) repetitionsField.getValue(),
                        LocalDate.parse(startDateField.getText().trim()), selectedDays);
                planner.addRecurrence(recurrence);
                refreshSafely();
                return;
            } catch (DateTimeParseException | IllegalArgumentException e) {
                showError("Enter a valid start date, practice item, speed, and at least one weekday.");
            }
        }
    }

    private static void addFormRow(JPanel panel, String label, java.awt.Component field) {
        panel.add(new JLabel(label));
        panel.add(field);
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

    private void openAdavuLibrary() {
        JDialog dialog = new JDialog(this, "Adavu Library", true);
        dialog.setSize(760, 540);
        dialog.setLocationRelativeTo(this);

        List<Adavu> adavus = new ArrayList<>();
        adavus.add(new Adavu("Tatta Adavu 1", "Tatta",
                "Tai ya tai hi",
                "Flat tap with the right foot (tai ya), then the left (tai hi). One tap on each side counts as one step.\n\nLift the foot toward the inner thigh before each tap, strike with the whole sole, keep palms at the waist facing outward, and hold araimandi."));
        adavus.add(new Adavu("Tatta Adavu 2", "Tatta",
                "Tai ya tai hi Tai ya tai hi",
                "Two flat taps with the right foot, then two with the left. Four taps in total count as one side.\n\nUse the same flat-foot rules as Tatta 1."));
        adavus.add(new Adavu("Tatta Adavu 3", "Tatta",
                "Tai Tai Tam",
                "Three flat taps with the right foot (Tai Tai Tam), then three flat taps with the left (Tai Tai Tam)."));
        adavus.add(new Adavu("Tatta Adavu 4", "Tatta",
                "Tai Tai Tai Ya Tam",
                "Stamp the feet alternately four times."));
        adavus.add(new Adavu("Tatta Adavu 5", "Tatta",
                "Tai ya Tai ya Tai Tai Tam",
                "The first two taps are slow, with two syllables per tap; the final Tai, Tai, Tam are quick, with one syllable per tap.\n\nKeep the quick taps as flat and clear as the slow ones."));
        adavus.add(new Adavu("Tatta Adavu 6", "Tatta",
                "Tai Tai Tam Tai Tai Tam",
                "Stamp the feet three times, pause slightly, then stamp three times again."));
        adavus.add(new Adavu("Tatta Adavu 7", "Tatta",
                "Tai Tai Tat Tat Tai Tai Tam",
                "Stamp twice, then the other foot once, then the original feet three more times for seven beats."));

        adavus.add(new Adavu("Natta Adavu 1", "Natta",
                "Tai yum tat tat, tai yum tam",
                "Stretch the leg to the side, strike the heel, and return to araimandi.\n\nThe standing leg holds araimandi and the torso does not lean."));
        adavus.add(new Adavu("Natta Adavu 2", "Natta",
                "Tai yum tat tat, tai yum tam, Tai yum tat tat, tai yum tam",
                "Repeat the Natta Adavu 1 phrase twice consecutively on each side."));
        adavus.add(new Adavu("Natta Adavu 3", "Natta",
                "Tai yum tat tat, tai yum tam, Tai yum tat tat, tai yum tam",
                "Stretch one leg to the side and strike the heel, then repeat with the other leg. Finish with a back tap such as dit dit thai."));
        adavus.add(new Adavu("Natta Adavu 4", "Natta",
                "Tai yum tat tat, tai yum tam, Tai yum tat tat, tai yum tam, Tai yum tat tat, tai yum tam, Tai yum tat tat, tai yum tam",
                "Double each movement from Natta Adavu 3. Hold the standing leg in araimandi throughout."));

        adavus.add(new Adavu("Visharu Adavu 1", "Visharu",
                "Ta tai tai ta (Tai ha tai hi)",
                "Cross the feet into the swastika position and sweep side to side.\n\nKeep the torso centered while the feet travel. Keep soft and loud sounds distinct."));

        adavus.add(new Adavu("Tatti Mettu Adavu", "Tatti Mettu",
                "Tat tai ta ha, dhit tai ta ha",
                "Combine a flat tap with a heel strike. Count both halves evenly and keep heel contact clean and crisp."));
        adavus.add(new Adavu("Kuditta Mettu Adavu", "Kuditta Mettu",
                "Tai hat tai hi",
                "Jump onto the toes, then strike the heels down while the toes remain in contact with the floor.\n\nKeep araimandi on every landing and make jumps small and controlled."));
        adavus.add(new Adavu("Tat Tai Ta Ha Adavu", "Tat Tai Ta Ha",
                "Tai tai taa haa",
                "A bridge between tapping and jumping adavus. Keep the four syllables even, with the final two held longer."));
        adavus.add(new Adavu("Sarikkal Adavu", "Sarikkal",
                "Tai kat tai hi",
                "Slide or glide smoothly without hopping or bouncing. Transfer weight within araimandi and keep both knees turned out."));
        adavus.add(new Adavu("Mandi Adavu", "Mandi",
                "Tat tai taa",
                "Rise and sit smoothly in the full-sitting (muzhumandi) position. Keep the spine straight and do not sit back on the heels."));
        adavus.add(new Adavu("Small Teermanam", "Teermanam",
                "Tari kita tom",
                "A concluding adavu repeated three times. The final landing of the third repeat must fall exactly on sam."));
        adavus.add(new Adavu("Big Teermanam", "Teermanam",
                "Tari kita tom",
                "A big teermanam joins several units. Work out the count first and finish the three repeats exactly on sam."));

        JComboBox<String> setField = new JComboBox<>(adavus.stream()
                .map(Adavu::setName).distinct().toArray(String[]::new));
        java.awt.List adavuList = new java.awt.List();
        JTextArea details = new JTextArea();
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setFont(SERIF_FONT);
        JLabel tattaVisual = new JLabel("", SwingConstants.CENTER);
        tattaVisual.setVisible(false);
        java.net.URL tattaImageUrl = getClass().getResource("/images/tatta-adavu.png");
        if (tattaImageUrl != null) {
            ImageIcon tattaIcon = new ImageIcon(tattaImageUrl);
            java.awt.Image scaled = tattaIcon.getImage().getScaledInstance(480, 217, java.awt.Image.SCALE_SMOOTH);
            tattaVisual.setIcon(new ImageIcon(scaled));
        }
        JLabel nattaVisual = new JLabel("", SwingConstants.CENTER);
        nattaVisual.setVisible(false);
        java.net.URL nattaImageUrl = getClass().getResource("/images/natta-adavu.png");
        if (nattaImageUrl != null) {
            ImageIcon nattaIcon = new ImageIcon(nattaImageUrl);
            java.awt.Image scaled = nattaIcon.getImage().getScaledInstance(480, 217, java.awt.Image.SCALE_SMOOTH);
            nattaVisual.setIcon(new ImageIcon(scaled));
        }
        JLabel visharuVisual = new JLabel("", SwingConstants.CENTER);
        java.net.URL visharuImageUrl = getClass().getResource("/images/visharu-adavu.png");
        if (visharuImageUrl != null) {
            ImageIcon visharuIcon = new ImageIcon(visharuImageUrl);
            java.awt.Image scaled = visharuIcon.getImage().getScaledInstance(
                    480, 217, java.awt.Image.SCALE_SMOOTH);
            visharuVisual.setIcon(new ImageIcon(scaled));
        }
        java.awt.CardLayout visualLayout = new java.awt.CardLayout();
        JPanel visualArea = new JPanel(visualLayout);
        visualArea.add(tattaVisual, "Tatta");
        visualArea.add(nattaVisual, "Natta");
        visualArea.add(visharuVisual, "Visharu");

        Runnable refreshList = () -> {
            adavuList.removeAll();
            for (Adavu adavu : adavus) {
                if (adavu.setName().equals(setField.getSelectedItem())) {
                    adavuList.add(adavu.name());
                }
            }
            if (adavuCount(adavuList) > 0) {
                adavuList.select(0);
            }
            showAdavuDetails(adavuList, adavus, details, visualLayout, visualArea);
        };
        setField.addActionListener(event -> refreshList.run());
        adavuList.addItemListener(event -> showAdavuDetails(
                adavuList, adavus, details, visualLayout, visualArea));
        refreshList.run();

        JTextField dateField = new JTextField(LocalDate.now().toString());
        JComboBox<String> speedField = new JComboBox<>(new String[]{"First kala", "Second kala", "Third kala"});
        JSpinner repetitionsField = new JSpinner(new SpinnerNumberModel(8, 1, 999, 1));
        JButton addToCalendar = new JButton("Add to Calendar");
        addToCalendar.addActionListener(event -> {
            Adavu selected = findAdavu(adavuList.getSelectedItem(), adavus);
            if (selected == null) {
                showError("Select an adavu first.");
                return;
            }
            try {
                PracticeItem item = new PracticeItem(selected.name(), PracticeType.ADAVU,
                        LocalDate.parse(dateField.getText().trim()),
                        (String) speedField.getSelectedItem(),
                        (Integer) repetitionsField.getValue(), false);
                planner.addItem(item);
                refreshSafely();
                dialog.dispose();
            } catch (DateTimeParseException | IllegalArgumentException e) {
                showError("Enter a valid date and repetition goal.");
            }
        });

        JPanel left = new JPanel(new BorderLayout());
        left.add(setField, BorderLayout.NORTH);
        left.add(new JScrollPane(adavuList), BorderLayout.CENTER);
        left.setBorder(BorderFactory.createLineBorder(GOLD, 1));
        JPanel right = new JPanel(new BorderLayout(6, 6));
        JPanel detailContent = new JPanel(new BorderLayout(4, 4));
        detailContent.add(new JScrollPane(details), BorderLayout.CENTER);
        detailContent.add(visualArea, BorderLayout.SOUTH);
        detailContent.setBorder(BorderFactory.createLineBorder(GOLD, 1));
        right.add(detailContent, BorderLayout.CENTER);
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Date (YYYY-MM-DD)"));
        form.add(dateField);
        form.add(new JLabel("Speed"));
        form.add(speedField);
        form.add(new JLabel("Repetition goal"));
        form.add(repetitionsField);
        form.add(new JLabel());
        form.add(addToCalendar);
        right.add(form, BorderLayout.SOUTH);

        dialog.setLayout(new BorderLayout(8, 8));
        dialog.add(left, BorderLayout.WEST);
        dialog.add(right, BorderLayout.CENTER);
        dialog.setBackground(DEEP_BURGUNDY);
        dialog.getContentPane().setBackground(DEEP_BURGUNDY);
        themeAdavuLibrary(dialog, setField, adavuList, details, visualArea);
        dialog.setVisible(true);
    }

    private static int adavuCount(java.awt.List list) {
        return list.getItemCount();
    }

    private static void showAdavuDetails(java.awt.List list, List<Adavu> adavus,
                                         JTextArea details, java.awt.CardLayout visualLayout,
                                         JPanel visualArea) {
        Adavu selected = findAdavu(list.getSelectedItem(), adavus);
        if (selected != null) {
            details.setText("Jathi syllables\n" + selected.jathi()
                    + "\n\nNotes\n" + selected.notes());
            if (selected.setName().equals("Tatta") || selected.setName().equals("Natta")
                    || selected.setName().equals("Visharu")) {
                visualLayout.show(visualArea, selected.setName());
                visualArea.setVisible(true);
            } else {
                visualArea.setVisible(false);
            }
        }
    }

    private static Adavu findAdavu(String name, List<Adavu> adavus) {
        if (name == null) {
            return null;
        }
        for (Adavu adavu : adavus) {
            if (adavu.name().equals(name)) {
                return adavu;
            }
        }
        return null;
    }

    private void themeAdavuLibrary(JDialog dialog, JComboBox<String> setField,
                                   java.awt.List adavuList, JTextArea details,
                                   JPanel visualArea) {
        dialog.setBackground(DEEP_BURGUNDY);
        setField.setBackground(CREAM);
        setField.setForeground(DEEP_BURGUNDY);
        setField.setFont(SERIF_FONT);
        adavuList.setBackground(BURGUNDY);
        adavuList.setForeground(CREAM);
        adavuList.setFont(SERIF_FONT);
        details.setBackground(CREAM);
        details.setForeground(DEEP_BURGUNDY);
        details.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD, 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        visualArea.setBackground(BURGUNDY);
        visualArea.setOpaque(true);
        applyThemeToComponent(dialog);
        for (java.awt.Component component : dialog.getContentPane().getComponents()) {
            if (component instanceof JPanel panel) {
                panel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(GOLD, 1),
                        BorderFactory.createEmptyBorder(4, 4, 4, 4)));
            }
        }
    }

    private void openPracticeScreen(PracticeItem item) {
        JDialog dialog = new JDialog(this, "Practice - " + item.title(), true);
        dialog.setSize(950, 620);
        dialog.setLocationRelativeTo(this);
        ThalamTrainerPanel trainer = new ThalamTrainerPanel(item.title());
        dialog.add(trainer, BorderLayout.CENTER);
        try {
            dialog.setVisible(true);
        } finally {
            trainer.disposeTrainer();
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Practice Planner Error", JOptionPane.ERROR_MESSAGE);
    }

    private void applyTheme() {
        getContentPane().setBackground(DEEP_BURGUNDY);
        applyThemeToComponent(getContentPane());
        monthLabel.setForeground(GOLD);
        monthLabel.setFont(DISPLAY_FONT);
        weeklyProgressLabel.setForeground(SOFT_GOLD);
        weeklyProgressLabel.setFont(SERIF_FONT);
        streakLabel.setForeground(SOFT_GOLD);
        streakLabel.setFont(SERIF_FONT);
        selectedDateLabel.setForeground(GOLD);
        selectedDateLabel.setFont(DISPLAY_FONT);
    }

    private void applyThemeToComponent(java.awt.Component component) {
        if (component instanceof JPanel panel) {
            panel.setBackground(panel == getContentPane() ? DEEP_BURGUNDY : BURGUNDY);
            panel.setOpaque(true);
        } else if (component instanceof JLabel label) {
            label.setForeground(CREAM);
            if (label != monthLabel && label != weeklyProgressLabel
                    && label != streakLabel && label != selectedDateLabel) {
                label.setFont(SMALL_SERIF_FONT);
            }
        } else if (component instanceof JButton button) {
            button.setBackground(LIGHT_BURGUNDY);
            button.setForeground(GOLD);
            button.setOpaque(true);
            button.setFont(SERIF_FONT);
            if (button.getBorder() != DAY_BORDER && button.getBorder() != SELECTED_DAY_BORDER) {
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(GOLD, 1),
                        BorderFactory.createEmptyBorder(4, 8, 4, 8)));
            }
        } else if (component instanceof JCheckBox checkBox) {
            checkBox.setBackground(BURGUNDY);
            checkBox.setForeground(CREAM);
            checkBox.setFont(SERIF_FONT);
        } else if (component instanceof JTextField textField) {
            textField.setBackground(CREAM);
            textField.setForeground(DEEP_BURGUNDY);
            textField.setCaretColor(DEEP_BURGUNDY);
            textField.setFont(SERIF_FONT);
        } else if (component instanceof JTextArea textArea) {
            textArea.setBackground(CREAM);
            textArea.setForeground(DEEP_BURGUNDY);
            textArea.setCaretColor(DEEP_BURGUNDY);
            textArea.setFont(SERIF_FONT);
        } else if (component instanceof JComboBox<?> comboBox) {
            comboBox.setBackground(CREAM);
            comboBox.setForeground(DEEP_BURGUNDY);
            comboBox.setFont(SERIF_FONT);
        } else if (component instanceof JSpinner spinner) {
            spinner.setBackground(CREAM);
            spinner.setForeground(DEEP_BURGUNDY);
            spinner.setFont(SERIF_FONT);
        } else if (component instanceof JScrollPane scrollPane) {
            scrollPane.getViewport().setBackground(BURGUNDY);
            scrollPane.setBorder(BorderFactory.createLineBorder(GOLD, 1));
        }

        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                applyThemeToComponent(child);
            }
        }
    }
}
