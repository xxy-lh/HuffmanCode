package com.example.huffmancode.controller;

import com.example.huffmancode.model.ChatMessage;
import com.example.huffmancode.repository.ChatMessageRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final ChatMessageRepository chatMessageRepository;

    public MessageController(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    @GetMapping("/history/{username}")
    public ResponseEntity<List<ChatMessage>> getMessageHistory(@PathVariable String username) { // 从URL路径中获取用户名
        List<ChatMessage> messages = chatMessageRepository.findByUsername(username);
        return ResponseEntity.ok(messages);
    }
}
