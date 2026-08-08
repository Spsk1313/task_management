package com.spsk1313.task_management.exception;

public class DuplicateEmailException extends RuntimeException{
    public DuplicateEmailException() {
        super("This email is already in use");
    }
}
