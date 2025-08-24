package TodoListMiniProject;

// Enum for task priority levels
public enum Priority {
    LOW, MEDIUM, HIGH;

    // For sorting: HIGH > MEDIUM > LOW
    public int rank() {
        return switch (this) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }
}
