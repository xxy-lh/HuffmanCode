package com.example.huffmancode.controller;

import com.example.huffmancode.model.ChatMessage;
import com.example.huffmancode.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RestController
@RequestMapping("/api/messages")
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;

    @Autowired
    public WebSocketController(SimpMessagingTemplate messagingTemplate,
                               ChatMessageRepository chatMessageRepository) {
        this.messagingTemplate = messagingTemplate;
        this.chatMessageRepository = chatMessageRepository;
    }

    // 处理发送消息
    @MessageMapping("/send")
    public void sendMessage(@Payload MessageDTO messageDTO) {
        // 保存发送方的消息记录
        ChatMessage senderMessage = new ChatMessage();
        senderMessage.setSender(messageDTO.getSender());
        senderMessage.setReceiver(messageDTO.getReceiver() != null ? messageDTO.getReceiver() : "broadcast");
        senderMessage.setContent(messageDTO.getMessage());
        senderMessage.setMessageType("SEND");
        chatMessageRepository.save(senderMessage);

        // 构建响应消息
        MessageResponse response = new MessageResponse();
        response.setId(senderMessage.getId());
        response.setSender(messageDTO.getSender());
        response.setMessage(messageDTO.getMessage());
        response.setTimestamp(LocalDateTime.now().toString());
        response.setType("MESSAGE");

        // 如果有指定接收者，发送私信
        if (messageDTO.getReceiver() != null && !messageDTO.getReceiver().isEmpty()) {
            // 保存接收方的消息记录
            ChatMessage receiverMessage = new ChatMessage();
            receiverMessage.setSender(messageDTO.getSender());
            receiverMessage.setReceiver(messageDTO.getReceiver());
            receiverMessage.setContent(messageDTO.getMessage());
            receiverMessage.setMessageType("RECEIVE");
            chatMessageRepository.save(receiverMessage);

            // 发送给指定用户
            messagingTemplate.convertAndSendToUser(
                    messageDTO.getReceiver(),
                    "/queue/private",
                    response
            );
            // 也发送给发送者确认
            messagingTemplate.convertAndSendToUser(
                    messageDTO.getSender(),
                    "/queue/private",
                    response
            );
        } else {
            // 广播给所有用户
            messagingTemplate.convertAndSend("/topic/messages", response);
        }
    }

    // 用户加入通知
    @MessageMapping("/join")
    public void userJoin(@Payload Map<String, String> payload) {
        String username = payload.get("username");
        MessageResponse response = new MessageResponse();
        response.setSender("系统");
        response.setMessage(username + " 加入了聊天室");
        response.setTimestamp(LocalDateTime.now().toString());
        response.setType("JOIN");

        messagingTemplate.convertAndSend("/topic/messages", response);
    }

    // 用户离开通知
    @MessageMapping("/leave")
    public void userLeave(@Payload Map<String, String> payload) {
        String username = payload.get("username");
        MessageResponse response = new MessageResponse();
        response.setSender("系统");
        response.setMessage(username + " 离开了聊天室");
        response.setTimestamp(LocalDateTime.now().toString());
        response.setType("LEAVE");

        messagingTemplate.convertAndSend("/topic/messages", response);
    }

    // REST API: 获取用户的消息历史
    @GetMapping("/history/{username}")
    public ResponseEntity<List<ChatMessage>> getMessageHistory(@PathVariable String username) {
        List<ChatMessage> messages = chatMessageRepository.findByUsername(username);
        return ResponseEntity.ok(messages);
    }

    // REST API: 获取两个用户之间的对话
    @GetMapping("/conversation")
    public ResponseEntity<List<ChatMessage>> getConversation(
            @RequestParam String user1,
            @RequestParam String user2) {
        List<ChatMessage> messages = chatMessageRepository.findConversation(user1, user2);
        return ResponseEntity.ok(messages);
    }

    // DTO 类
    public static class MessageDTO {
        private String sender;
        private String receiver;
        private String message;
        private boolean encoded;

        public String getSender() { return sender; }
        public void setSender(String sender) { this.sender = sender; }
        public String getReceiver() { return receiver; }
        public void setReceiver(String receiver) { this.receiver = receiver; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public boolean isEncoded() { return encoded; }
        public void setEncoded(boolean encoded) { this.encoded = encoded; }
    }

    public static class MessageResponse {
        private Long id;
        private String sender;
        private String message;
        private String timestamp;
        private String type;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getSender() { return sender; }
        public void setSender(String sender) { this.sender = sender; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
}
