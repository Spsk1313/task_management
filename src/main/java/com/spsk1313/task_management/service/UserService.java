package com.spsk1313.task_management.service;

import com.spsk1313.task_management.dto.CreateUserRequest;
import com.spsk1313.task_management.dto.UpdateUserRequest;
import com.spsk1313.task_management.dto.UserResponse;
import com.spsk1313.task_management.entity.User;
import com.spsk1313.task_management.exception.DuplicateEmailException;
import com.spsk1313.task_management.exception.UserNotFoundException;
import com.spsk1313.task_management.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        return toResponse(user);
    }

    public UserResponse createUser(CreateUserRequest req) {
        User user = toEntity(req);
        if(userRepository.existsByEmail(user.getEmail())) throw new DuplicateEmailException();
        userRepository.save(user);
        return toResponse(user);
    }

    public UserResponse updateUser(Long id, UpdateUserRequest req) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        String normalizedEmail = req.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailAndIdNot(normalizedEmail, id)) {
            throw new DuplicateEmailException();
        }

        user.changeName(req.name());
        user.changeEmail(req.email());
        return toResponse(user);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

    private User toEntity(CreateUserRequest req) {
        return new User(req.name(), req.email());
    }
}
