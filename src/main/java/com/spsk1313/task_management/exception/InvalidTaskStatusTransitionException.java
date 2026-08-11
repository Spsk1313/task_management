package com.spsk1313.task_management.exception;

import com.spsk1313.task_management.entity.TaskStatus;

public class InvalidTaskStatusTransitionException extends RuntimeException {
    public InvalidTaskStatusTransitionException(TaskStatus current, TaskStatus next) {
        super("Cannot transition from " + current.name() + " to " + next.name());
    }
}
