    package com.example.huffmancode.repository;

    import com.example.huffmancode.model.User;
    import org.springframework.data.jpa.repository.JpaRepository; // Spring Data JPA 提供的基础仓库接口
    import java.util.Optional; // 用于处理可能为空的返回值
    // 数据访问层DAO接口
    public interface UserRepository extends JpaRepository<User, Long> {
        Optional<User> findByUsername(String username);
    }