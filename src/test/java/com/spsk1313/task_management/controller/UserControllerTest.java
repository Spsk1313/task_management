package com.spsk1313.task_management.controller;

import com.spsk1313.task_management.dto.CreateUserRequest;
import com.spsk1313.task_management.dto.UpdateUserRequest;
import com.spsk1313.task_management.dto.UserResponse;
import com.spsk1313.task_management.exception.DuplicateEmailException;
import com.spsk1313.task_management.exception.UserNotFoundException;
import com.spsk1313.task_management.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserControllerTest {

    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);

        UserController userController = new UserController(userService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getUserById_shouldReturn200_whenUserExists() throws Exception {
        UserResponse response = new UserResponse(1L, "Sahil", "sahil@example.com");

        when(userService.getUserById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Sahil"))
                .andExpect(jsonPath("$.email").value("sahil@example.com"));
    }

    @Test
    void getUserById_shouldReturn404_whenUserDoesNotExist() throws Exception {
        when(userService.getUserById(999L)).thenThrow(new UserNotFoundException(999L));

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void createUser_shouldReturn201_whenRequestIsValid() throws Exception {
        UserResponse response =
                new UserResponse(1L, "Sahil", "sahil@example.com");

        when(userService.createUser(any(CreateUserRequest.class)))
                .thenReturn(response);

        String json = """
                {
                  "name": "Sahil",
                  "email": "sahil@example.com"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Sahil"))
                .andExpect(jsonPath("$.email").value("sahil@example.com"));
    }

    @Test
    void createUser_shouldReturn400_whenRequestIsInvalid() throws Exception {
        String json = """
                {
                  "name": "",
                  "email": "banana"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void createUser_shouldReturn409_whenEmailAlreadyExists() throws Exception {
        when(userService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new DuplicateEmailException());

        String json = """
                {
                  "name": "Sahil",
                  "email": "sahil@example.com"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void updateUser_shouldReturn200_whenRequestIsValid() throws Exception {
        UserResponse response =
                new UserResponse(1L, "Sahilpreet", "new@example.com");

        when(userService.updateUser(
                eq(1L),
                any(UpdateUserRequest.class)
        )).thenReturn(response);

        String json = """
                {
                  "name": "Sahilpreet",
                  "email": "new@example.com"
                }
                """;

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Sahilpreet"))
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }

    @Test
    void updateUser_shouldReturn404_whenUserDoesNotExist() throws Exception {
        when(userService.updateUser(
                eq(999L),
                any(UpdateUserRequest.class)
        )).thenThrow(new UserNotFoundException(999L));

        String json = """
                {
                  "name": "Sahil",
                  "email": "sahil@example.com"
                }
                """;

        mockMvc.perform(put("/api/users/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void updateUser_shouldReturn409_whenEmailAlreadyExists() throws Exception {
        when(userService.updateUser(
                eq(1L),
                any(UpdateUserRequest.class)
        )).thenThrow(new DuplicateEmailException());

        String json = """
                {
                  "name": "Sahil",
                  "email": "taken@example.com"
                }
                """;

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }
}
