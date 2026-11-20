package edu.guilford;

import java.io.IOException;
import java.nio.file.Path;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Planner planner = new Planner(Path.of("practice-planner-data.txt"));
                planner.load();
                new PracticePlannerFrame(planner).setVisible(true);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null,
                        "Could not read or save practice planner data: " + e.getMessage(),
                        "Practice Planner Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
