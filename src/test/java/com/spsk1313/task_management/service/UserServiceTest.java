package com.spsk1313.task_management.service;

import com.spsk1313.task_management.dto.CreateUserRequest;
import com.spsk1313.task_management.dto.UpdateUserRequest;
import com.spsk1313.task_management.dto.UserResponse;
import com.spsk1313.task_management.entity.User;
import com.spsk1313.task_management.exception.DuplicateEmailException;
import com.spsk1313.task_management.exception.UserNotFoundException;
import com.spsk1313.task_management.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        userService = new UserService(userRepository);
    }

    @Test
    void getUserById_shouldReturnUser_whenUserExists() {
        User user = new User("Sahil", "Sahil@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(1L);

        assertEquals("Sahil", response.name());
        assertEquals("sahil@example.com", response.email());
    }

    @Test
    void getUserById_shouldThrow_whenUserDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserById(1L));
    }

    @Test
    void createUser_shouldCreateUser_whenEmailIsAvailable() {
        CreateUserRequest req = new CreateUserRequest("Sahil", "Sahil@example.com");

        when(userRepository.existsByEmail("sahil@example.com")).thenReturn(false);

        UserResponse response = userService.createUser(req);

        assertEquals("Sahil", response.name());
        assertEquals("sahil@example.com", response.email());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_shouldThrow_whenEmailAlreadyExists() {
        CreateUserRequest req = new CreateUserRequest("Sahil", "Sahil@example.com");

        when(userRepository.existsByEmail("sahil@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> userService.createUser(req));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_shouldUpdateUser_whenRequestIsValid() {
        User user = new User("Sahil", "old@example.com");

        UpdateUserRequest req = new UpdateUserRequest("Sahilpreet", "NEW@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(userRepository.existsByEmailAndIdNot("new@example.com", 1L)).thenReturn(false);

        UserResponse response = userService.updateUser(1L, req);

        assertEquals("Sahilpreet", response.name());
        assertEquals("new@example.com", response.email());
    }

    @Test
    void updateUser_shouldThrow_whenUserDoesNotExist() {
        UpdateUserRequest req = new UpdateUserRequest("Sahil", "new@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.updateUser(1L, req));
    }

    @Test
    void updateUser_shouldThrow_whenEmailBelongsToAnotherUser() {
        User user = new User("Sahil", "old@example.com");
        UpdateUserRequest req = new UpdateUserRequest("Sahilpreet", "TAKEN@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(userRepository.existsByEmailAndIdNot("taken@example.com", 1L)).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> userService.updateUser(1L, req));

        assertEquals("Sahil", user.getName());
        assertEquals("old@example.com", user.getEmail());
    }
}

