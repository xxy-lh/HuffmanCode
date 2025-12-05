package com.example.huffmancode.model;

import jakarta.persistence.*; // 导入所有JPA注解

@Entity // 标记是一个JPA实体
@Table(name = "users") // 映射到数据库中的users表
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 主键自动生成，数据库自增策略
    private Long id;

    @Column(unique = true, nullable = false) // 用户名唯一且不能为空
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "plain_password") // 用于存储明文密码
    private String plainPassword;

    public User() {
    } // 无参构造，JPA必需

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPlainPassword() {
        return plainPassword;
    }

    public void setPlainPassword(String plainPassword) {
        this.plainPassword = plainPassword;
    }
}