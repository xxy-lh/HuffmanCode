package com.example.huffmancode.controller;

import com.example.huffmancode.model.User;
import com.example.huffmancode.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<String> getAllUsernames() {
        return userRepository.findAll()
                .stream()
                .map(User::getUsername)
                .collect(Collectors.toList());
    }
}
