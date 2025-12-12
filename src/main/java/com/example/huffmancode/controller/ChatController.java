package com.example.huffmancode.controller;

import com.example.huffmancode.model.ChatMessage;
import com.example.huffmancode.repository.ChatMessageRepository;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload; // 用于将消息体绑定到方法参数
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller // 用于传统 MVC 和 WebSocket 消息处理
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;

    public ChatController(SimpMessagingTemplate messagingTemplate,
                          ChatMessageRepository chatMessageRepository) {
        this.messagingTemplate = messagingTemplate;
        this.chatMessageRepository = chatMessageRepository;
    }

    @MessageMapping("/send")
    public void sendMessage(@Payload Map<String, Object> payload) {
        String sender = (String) payload.get("sender");
        String message = (String) payload.get("message");
        String originalMessage = (String) payload.get("originalMessage");
        String receiver = (String) payload.get("receiver");
        Boolean encoded = (Boolean) payload.get("encoded");

        // 保存消息到数据库
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSender(sender);
        chatMessage.setReceiver(receiver);
        chatMessage.setContent(message);
        chatMessage.setMessageType(encoded != null && encoded ? "ENCODED" : "SEND");
        chatMessageRepository.save(chatMessage);

        // 构建响应消息
        Map<String, Object> response = new HashMap<>();
        response.put("id", chatMessage.getId());
        response.put("sender", sender);
        response.put("message", message);
        response.put("originalMessage", originalMessage);
        response.put("timestamp", chatMessage.getCreatedAt().toString());
        response.put("type", "MESSAGE");
        response.put("encoded", encoded != null && encoded);

        if (receiver != null && !receiver.isEmpty()) {
            response.put("receiver", receiver);
            response.put("isPrivate", true);
            messagingTemplate.convertAndSend("/topic/private." + receiver, response);
        } else {
            response.put("isPrivate", false);
            messagingTemplate.convertAndSend("/topic/messages", response);
        }
    }

    @MessageMapping("/join")
    public void userJoin(@Payload Map<String, Object> payload) { // Payload提取WebSocket消息体，自动将JSON转化为Map
        String username = (String) payload.get("username");
        Map<String, Object> response = new HashMap<>();
        response.put("sender", "系统");
        response.put("message", username + " 加入了聊天");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("type", "JOIN");
        messagingTemplate.convertAndSend("/topic/messages", response); // 广播消息到指定主题
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
