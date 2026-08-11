package com.spsk1313.task_management.entity;

import jakarta.persistence.*;

import java.util.Locale;

@Entity
@Table(name = "tags")
public class Tag {

    private static final int MAX_NAME_LENGTH = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = MAX_NAME_LENGTH, unique = true)
    private String name;

    protected Tag() {
    }

    public Tag(String name) {
        changeName(name);
    }

    public void changeName(String name) {
        validateName(name);

        this.name = name
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Name cannot be null or blank"
            );
        }

        if (name.trim().length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Name cannot exceed " +
                            MAX_NAME_LENGTH +
                            " characters"
            );
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
