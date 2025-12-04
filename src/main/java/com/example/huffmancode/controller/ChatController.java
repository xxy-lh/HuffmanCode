package com.example.huffmancode.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
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
        Boolean encoded = (Boolean) payload.get("encoded");

        Map<String, Object> response = Map.of(
                "sender", sender,
                "message", message,  // 确保字段名是 message
                "timestamp", LocalDateTime.now().toString(),
                "type", receiver != null && !receiver.isEmpty() ? "PRIVATE" : "MESSAGE",
                "encoded", encoded != null && encoded
        );

        if (receiver != null && !receiver.isEmpty()) {
            // 私信
            messagingTemplate.convertAndSendToUser(receiver, "/queue/private", response);
        } else {
            // 群发
            messagingTemplate.convertAndSend("/topic/messages", response);
        }
    }

    @MessageMapping("/join")
    public void userJoin(@Payload Map<String, Object> payload) {
        String username = (String) payload.get("username");
        Map<String, Object> response = Map.of(
                "sender", "系统",
                "message", username + " 加入了聊天",
                "timestamp", LocalDateTime.now().toString(),
                "type", "JOIN"
        );
        messagingTemplate.convertAndSend("/topic/messages", response);
    }

    @MessageMapping("/leave")
    public void userLeave(@Payload Map<String, Object> payload) {
        String username = (String) payload.get("username");
        Map<String, Object> response = Map.of(
                "sender", "系统",
                "message", username + " 离开了聊天",
                "timestamp", LocalDateTime.now().toString(),
                "type", "LEAVE"
        );
        messagingTemplate.convertAndSend("/topic/messages", response);
    }
}
