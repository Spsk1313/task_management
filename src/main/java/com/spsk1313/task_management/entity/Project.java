package com.spsk1313.task_management.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "projects")
public class Project {
    private static final int MAX_NAME_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    protected Project() {
    }

    public Project(String name, String description, User owner) {
        changeName(name);
        changeDescription(description);
        validateOwner(owner);
        this.owner = owner;
    }

    public void changeName(String name) {
        validateName(name);
        this.name = name.trim();
    }

    public void changeDescription(String description) {
        this.description = description;
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }

        if (name.trim().length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Name cannot exceed " + MAX_NAME_LENGTH + " characters"
            );
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public User getOwner() {
        return owner;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static void validateOwner(User owner) {
        if (owner == null) throw new IllegalArgumentException("Owner cannot be null");
    }
}
