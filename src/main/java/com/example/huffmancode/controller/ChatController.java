package com.example.huffmancode.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/send")
    public void sendMessage(@Payload Map<String, Object> payload) {
        String sender = (String) payload.get("sender");
        String message = (String) payload.get("message");
        String receiver = (String) payload.get("receiver");

        Map<String, Object> response = new HashMap<>();
        response.put("sender", sender);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("type", "MESSAGE");

        if (receiver != null && !receiver.isEmpty()) {
            messagingTemplate.convertAndSendToUser(receiver, "/queue/private", response);
        } else {
            messagingTemplate.convertAndSend("/topic/messages", response);
        }
    }

    @MessageMapping("/join")
    public void userJoin(@Payload Map<String, Object> payload) {
        String username = (String) payload.get("username");
        Map<String, Object> response = new HashMap<>();
        response.put("sender", "系统");
        response.put("message", username + " 加入了聊天");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("type", "JOIN");
        messagingTemplate.convertAndSend("/topic/messages", response);
    }

    @MessageMapping("/leave")
    public void userLeave(@Payload Map<String, Object> payload) {
        String username = (String) payload.get("username");
        Map<String, Object> response = new HashMap<>();
        response.put("sender", "系统");
        response.put("message", username + " 离开了聊天");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("type", "LEAVE");
        messagingTemplate.convertAndSend("/topic/messages", response);
    }
}
