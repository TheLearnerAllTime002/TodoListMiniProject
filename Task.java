package TodoListMiniProject;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class Task {
    private static int idCounter = 1;  // Auto-increment ID
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter VIEW = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private int id;
    private String description;
    private boolean isCompleted;
    private Priority priority;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    // Public constructor for new tasks
    public Task(String description, Priority priority) {
        this.id = idCounter++;
        this.description = description;
        this.priority = priority;
        this.isCompleted = false;
        this.createdAt = LocalDateTime.now();
    }

    // Private constructor for loading from storage
    private Task(int id, String description, Priority priority, boolean isCompleted,
                LocalDateTime createdAt, LocalDateTime completedAt) {
        this.id = id;
        this.description = description;
        this.priority = priority;
        this.isCompleted = isCompleted;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        if (id >= idCounter) idCounter = id + 1; // keep IDs unique across restarts
    }

    // Mark the task as completed
    public void markAsCompleted() {
        if (!this.isCompleted) {
            this.isCompleted = true;
            this.completedAt = LocalDateTime.now();
        }
    }

    // Mutators for editing
    public void setDescription(String newDescription) { this.description = newDescription; }
    public void setPriority(Priority newPriority) { this.priority = newPriority; }

    // Nicely formatted for viewing in a table
    @Override
    public String toString() {
        String status = isCompleted ? "✔ Completed" : "✘ Pending";
        String created = createdAt.format(VIEW);
        String completed = (completedAt != null) ? completedAt.format(VIEW) : "Not completed";
        return String.format("ID: %d | %s | Priority: %s | Status: %s | Created: %s | Completed: %s",
                id, description, priority, status, created, completed);
    }

    // Format: id|base64(description)|priority|isCompleted|createdAtISO|completedAtISO_or_null
    public String toStorageLine() {
        String descB64 = Base64.getEncoder().encodeToString(description.getBytes(StandardCharsets.UTF_8));
        String completedIso = (completedAt == null) ? "null" : completedAt.format(ISO);
        return String.join("|",
                String.valueOf(id),
                descB64,
                priority.name(),
                String.valueOf(isCompleted),
                createdAt.format(ISO),
                completedIso
        );
    }

    public static Task fromStorageLine(String line) {
        try {
            String[] parts = line.split("\\|", -1);
            int id = Integer.parseInt(parts[0]);
            String description = new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Priority priority = Priority.valueOf(parts[2]);
            boolean isCompleted = Boolean.parseBoolean(parts[3]);
            LocalDateTime createdAt = LocalDateTime.parse(parts[4], ISO);
            LocalDateTime completedAt = "null".equals(parts[5]) ? null : LocalDateTime.parse(parts[5], ISO);
            return new Task(id, description, priority, isCompleted, createdAt, completedAt);
        } catch (Exception e) {
            // If a line is corrupted, skip it by returning null (caller should check)
            return null;
        }
    }

    // === Getters ===
    public int getId() { return id; }
    public String getDescription() { return description; }
    public boolean isCompleted() { return isCompleted; }
    public Priority getPriority() { return priority; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }

    // Formatted dates for the table
    public String getCreatedAtFormatted() { return createdAt.format(VIEW); }
    public String getCompletedAtFormatted() { return completedAt == null ? "Not completed" : completedAt.format(VIEW); }

    // Truncate long descriptions for table cells
    public static String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 3)) + "...";
    }
}
