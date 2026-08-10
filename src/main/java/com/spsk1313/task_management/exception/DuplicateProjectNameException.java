package com.spsk1313.task_management.exception;

public class DuplicateProjectNameException extends RuntimeException {
    public DuplicateProjectNameException(String name) {
        super("Project with name \"" + name + "\" already exists.");
    }
}
