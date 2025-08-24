package TodoListMiniProject;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

// Main Application
public class TodoListApp {
    private final ArrayList<Task> tasks = new ArrayList<>();
    private final Scanner scanner = new Scanner(System.in);
    private final Path storagePath = Paths.get("tasks.txt"); // simple local persistence

    // ====== Startup / Shutdown ======
    public TodoListApp() {
        loadFromFile();
    }

    // ====== Input Utilities ======
    private String prompt(String msg) {
        System.out.print(msg);
        return scanner.nextLine();
    }

    private Optional<Integer> promptInt(String msg) {
        String raw = prompt(msg).trim();
        try {
            return Optional.of(Integer.parseInt(raw));
        } catch (NumberFormatException e) {
            System.out.println("Invalid number. Try again.");
            return Optional.empty();
        }
    }

    private Priority parsePriorityOrDefault(String raw, Priority def) {
        try {
            return Priority.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid priority! Defaulting to " + def);
            return def;
        }
    }

    // ====== Core Operations ======
    public void addTask() {
        String description = prompt("Enter task description: ").trim();
        if (description.isEmpty()) {
            System.out.println("Task description cannot be empty!");
            return;
        }
        String pIn = prompt("Enter priority (LOW, MEDIUM, HIGH): ");
        Priority priority = parsePriorityOrDefault(pIn, Priority.MEDIUM);

        Task task = new Task(description, priority);
        tasks.add(task);
        saveToFile();
        System.out.println("✅ Task added successfully!");
    }

    public void listTasks(List<Task> list) {
        if (list.isEmpty()) {
            System.out.println("📭 No tasks to show.");
            return;
        }

        System.out.println("\n------------------------- TASK LIST -------------------------");
        System.out.printf("%-5s %-28s %-8s %-12s %-19s %-19s%n",
                "ID", "Description", "Pri", "Status", "Created", "Completed");
        System.out.println("--------------------------------------------------------------------------" +
                           "----------------------");

        for (Task t : list) {
            String status = t.isCompleted() ? "✔ Completed" : "✘ Pending";
            String desc = Task.truncate(t.getDescription(), 28);
            System.out.printf("%-5d %-28s %-8s %-12s %-19s %-19s%n",
                    t.getId(), desc, t.getPriority(), status, t.getCreatedAtFormatted(), t.getCompletedAtFormatted());
        }
    }

    public void listAllTasks() {
        listTasks(tasks);
    }

    public void markTaskAsCompleted() {
        Optional<Integer> maybeId = promptInt("Enter task ID to mark as completed: ");
        if (maybeId.isEmpty()) return;
        int id = maybeId.get();

        for (Task task : tasks) {
            if (task.getId() == id && !task.isCompleted()) {
                task.markAsCompleted();
                saveToFile();
                System.out.println("Task marked as completed!");
                return;
            }
        }
        System.out.println("Task not found or already completed.");
    }

    public void deleteTask() {
        Optional<Integer> maybeId = promptInt("Enter task ID to delete: ");
        if (maybeId.isEmpty()) return;
        int id = maybeId.get();

        boolean removed = tasks.removeIf(t -> t.getId() == id);
        if (removed) {
            saveToFile();
            System.out.println("Task deleted.");
        } else {
            System.out.println("Task not found.");
        }
    }

    public void editTaskDescription() {
        Optional<Integer> maybeId = promptInt("Enter task ID to edit description: ");
        if (maybeId.isEmpty()) return;
        int id = maybeId.get();

        for (Task t : tasks) {
            if (t.getId() == id) {
                String newDesc = prompt("Enter new description: ").trim();
                if (newDesc.isEmpty()) {
                    System.out.println("Description cannot be empty.");
                    return;
                }
                t.setDescription(newDesc);
                saveToFile();
                System.out.println("Description updated.");
                return;
            }
        }
        System.out.println("Task not found.");
    }

    public void changeTaskPriority() {
        Optional<Integer> maybeId = promptInt("Enter task ID to change priority: ");
        if (maybeId.isEmpty()) return;
        int id = maybeId.get();

        for (Task t : tasks) {
            if (t.getId() == id) {
                String pIn = prompt("Enter new priority (LOW, MEDIUM, HIGH): ");
                Priority newP = parsePriorityOrDefault(pIn, t.getPriority());
                t.setPriority(newP);
                saveToFile();
                System.out.println("Priority updated.");
                return;
            }
        }
        System.out.println("Task not found.");
    }

    // ====== Filtering ======
    public void listCompletedTasks() {
        List<Task> done = tasks.stream().filter(Task::isCompleted).collect(Collectors.toList());
        listTasks(done);
    }

    public void listPendingTasks() {
        List<Task> pending = tasks.stream().filter(t -> !t.isCompleted()).collect(Collectors.toList());
        listTasks(pending);
    }

    // ====== Sorting ======
    public void sortMenu() {
        System.out.println("\n-- Sort Options --");
        System.out.println("1. By Priority (HIGH -> LOW)");
        System.out.println("2. By Created Date (Newest first)");
        System.out.println("3. By Created Date (Oldest first)");
        System.out.println("4. By Status (Completed first)");
        System.out.println("5. Back");
        var choice = prompt("Choose: ").trim();

        switch (choice) {
            case "1" -> tasks.sort(Comparator.comparingInt((Task t) -> t.getPriority().rank()).reversed()
                    .thenComparing(Task::getCreatedAt).reversed());
            case "2" -> tasks.sort(Comparator.comparing(Task::getCreatedAt).reversed());
            case "3" -> tasks.sort(Comparator.comparing(Task::getCreatedAt));
            case "4" -> tasks.sort(Comparator.comparing(Task::isCompleted).reversed()
                    .thenComparing(Task::getCreatedAt).reversed());
            case "5" -> { return; }
            default -> { System.out.println("Invalid choice."); return; }
        }
        listAllTasks();
        saveToFile();
    }

    public void filterMenu() {
        System.out.println("\n-- Filter Options --");
        System.out.println("1. Show Completed");
        System.out.println("2. Show Pending");
        System.out.println("3. Back");
        var choice = prompt("Choose: ").trim();

        switch (choice) {
            case "1" -> listCompletedTasks();
            case "2" -> listPendingTasks();
            case "3" -> { /* back */ }
            default -> System.out.println("Invalid choice.");
        }
    }

    // ====== Persistence ======
    private void loadFromFile() {
        if (!Files.exists(storagePath)) return;
        try {
            List<String> lines = Files.readAllLines(storagePath);
            for (String line : lines) {
                if (line.isBlank()) continue;
                Task t = Task.fromStorageLine(line);
                if (t != null) tasks.add(t); // skip corrupted lines
            }
        } catch (IOException e) {
            System.out.println("⚠️  Could not read tasks.txt. Starting fresh.");
        }
    }

    private void saveToFile() {
        List<String> lines = tasks.stream().map(Task::toStorageLine).toList();
        try {
            Files.write(storagePath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException e) {
            System.out.println("Could not save tasks to file.");
        }
    }

    // ====== Menu ======
    public void showMenu() {
        while (true) {
            System.out.println("\n=== TO-DO LIST MENU ===");
            System.out.println("1. Add Task");
            System.out.println("2. List All Tasks");
            System.out.println("3. Mark Task as Completed");
            System.out.println("4. Delete Task");
            System.out.println("5. Edit Task Description");
            System.out.println("6. Change Task Priority");
            System.out.println("7. Filter Tasks");
            System.out.println("8. Sort Tasks");
            System.out.println("9. Exit");
            String choice = prompt("Choose an option: ").trim();

            switch (choice) {
                case "1" -> addTask();
                case "2" -> listAllTasks();
                case "3" -> markTaskAsCompleted();
                case "4" -> deleteTask();
                case "5" -> editTaskDescription();
                case "6" -> changeTaskPriority();
                case "7" -> filterMenu();
                case "8" -> sortMenu();
                case "9" -> {
                    String confirm = prompt("Are you sure you want to exit? (y/n): ").trim().toLowerCase();
                    if (confirm.equals("y") || confirm.equals("yes")) {
                        System.out.println("👋 Exiting... Goodbye!");
                        return;
                    }
                }
                default -> System.out.println("❌ Invalid choice. Try again.");
            }
        }
    }

    public static void main(String[] args) {
        new TodoListApp().showMenu();
    }
}
