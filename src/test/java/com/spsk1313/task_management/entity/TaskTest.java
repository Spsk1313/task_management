package com.spsk1313.task_management.entity;

import com.spsk1313.task_management.exception.InvalidTaskStatusTransitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaskTest {

    private Task createTask() {
        User owner = new User("Sahil", "sahil@example.com");
        Project project = new Project("Project", "Description", owner);

        return new Task(
                "Task",
                "Description",
                project,
                TaskPriority.HIGH,
                null
        );
    }

    @Test
    void newTask_shouldStartWithTodoStatus() {
        Task task = createTask();

        assertEquals(TaskStatus.TODO, task.getStatus());
        assertNull(task.getCompletedAt());
    }

    @Test
    void changeStatus_shouldAllowTodoToInProgress() {
        Task task = createTask();

        task.changeStatus(TaskStatus.IN_PROGRESS);

        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
        assertNull(task.getCompletedAt());
    }

    @Test
    void changeStatus_shouldAllowTodoToDone_andSetCompletedAt() {
        Task task = createTask();

        task.changeStatus(TaskStatus.DONE);

        assertEquals(TaskStatus.DONE, task.getStatus());
        assertNotNull(task.getCompletedAt());
    }

    @Test
    void changeStatus_shouldRejectTodoToBlocked() {
        Task task = createTask();

        assertThrows(
                InvalidTaskStatusTransitionException.class,
                () -> task.changeStatus(TaskStatus.BLOCKED)
        );

        assertEquals(TaskStatus.TODO, task.getStatus());
    }

    @Test
    void changeStatus_shouldAllowInProgressToBlocked() {
        Task task = createTask();
        task.changeStatus(TaskStatus.IN_PROGRESS);

        task.changeStatus(TaskStatus.BLOCKED);

        assertEquals(TaskStatus.BLOCKED, task.getStatus());
    }

    @Test
    void changeStatus_shouldRejectBlockedToDone() {
        Task task = createTask();
        task.changeStatus(TaskStatus.IN_PROGRESS);
        task.changeStatus(TaskStatus.BLOCKED);

        assertThrows(
                InvalidTaskStatusTransitionException.class,
                () -> task.changeStatus(TaskStatus.DONE)
        );

        assertEquals(TaskStatus.BLOCKED, task.getStatus());
    }

    @Test
    void changeStatus_shouldAllowDoneToInProgress_andClearCompletedAt() {
        Task task = createTask();
        task.changeStatus(TaskStatus.DONE);

        assertNotNull(task.getCompletedAt());

        task.changeStatus(TaskStatus.IN_PROGRESS);

        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
        assertNull(task.getCompletedAt());
    }

    @Test
    void changeStatus_shouldDoNothing_whenStatusIsUnchanged() {
        Task task = createTask();

        task.changeStatus(TaskStatus.TODO);

        assertEquals(TaskStatus.TODO, task.getStatus());
        assertNull(task.getCompletedAt());
    }
}