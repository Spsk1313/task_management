package com.spsk1313.task_management.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "tasks")
public class Task {

    private static final int MAX_TITLE_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = MAX_TITLE_LENGTH)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "task_priority")
    private TaskPriority priority;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "task_status")
    private TaskStatus status;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected Task() {
    }

    public Task(
            String title,
            String description,
            Project project,
            TaskPriority priority,
            TaskStatus status,
            LocalDate dueDate
    ) {
        changeTitle(title);
        changeDescription(description);
        changeProject(project);
        changePriority(priority);
        changeStatus(status);
        changeDueDate(dueDate);
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void changeTitle(String title) {
        validateTitle(title);
        this.title = title.trim();
    }

    public void changeDescription(String description) {
        this.description = description;
    }

    public void changeProject(Project project) {
        validateProject(project);
        this.project = project;
    }

    public void changePriority(TaskPriority priority) {
        validatePriority(priority);
        this.priority = priority;
    }

    public void changeStatus(TaskStatus status) {
        validateStatus(status);

        this.status = status;

        if (status == TaskStatus.DONE) {
            if (this.completedAt == null) {
                this.completedAt = Instant.now();
            }
        } else {
            this.completedAt = null;
        }
    }

    public void changeDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Title cannot be null or blank"
            );
        }

        if (title.trim().length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                    "Title cannot exceed "
                            + MAX_TITLE_LENGTH
                            + " characters"
            );
        }
    }

    private static void validateProject(Project project) {
        if (project == null) {
            throw new IllegalArgumentException(
                    "Project cannot be null"
            );
        }
    }

    private static void validatePriority(TaskPriority priority) {
        if (priority == null) {
            throw new IllegalArgumentException(
                    "Priority cannot be null"
            );
        }
    }

    private static void validateStatus(TaskStatus status) {
        if (status == null) {
            throw new IllegalArgumentException(
                    "Status cannot be null"
            );
        }
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Project getProject() {
        return project;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}