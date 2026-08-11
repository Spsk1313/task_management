package com.spsk1313.task_management.exception;

public class DuplicateTaskTagException extends RuntimeException {

    public DuplicateTaskTagException(Long taskId, String tagName) {
        super(
                "Tag \"" + tagName +
                        "\" is already attached to task " + taskId
        );
    }
}