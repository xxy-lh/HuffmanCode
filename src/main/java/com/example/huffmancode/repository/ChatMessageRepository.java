package com.example.huffmancode.repository;

import com.example.huffmancode.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 查询用户的所有消息（发送和接收）
    @Query("SELECT m FROM ChatMessage m WHERE m.sender = :username OR m.receiver = :username ORDER BY m.createdAt DESC")
    List<ChatMessage> findByUsername(@Param("username") String username);

    // 查询两个用户之间的消息
    @Query("SELECT m FROM ChatMessage m WHERE (m.sender = :user1 AND m.receiver = :user2) OR (m.sender = :user2 AND m.receiver = :user1) ORDER BY m.createdAt ASC")
    List<ChatMessage> findConversation(@Param("user1") String user1, @Param("user2") String user2);
}
