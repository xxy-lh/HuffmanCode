package com.example.huffmancode.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @GetMapping("/history/{username}")
    public ResponseEntity<List<Object>> getMessageHistory(@PathVariable String username) {
        // 目前返回空列表，如需持久化可集成数据库
        return ResponseEntity.ok(new ArrayList<>());
    }
}
