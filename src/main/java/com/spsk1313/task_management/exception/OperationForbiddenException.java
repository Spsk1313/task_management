package com.spsk1313.task_management.exception;

public class OperationForbiddenException extends RuntimeException {
    public OperationForbiddenException() {
        super("You are not allowed to perform this operation");
    }
}
