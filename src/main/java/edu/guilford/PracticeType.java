package edu.guilford;

public enum PracticeType {
    ADAVU("Adavu"),
    THALAM("Thalam"),
    PIECE("Piece"),
    TASK("Other");

    private final String displayName;

    PracticeType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
