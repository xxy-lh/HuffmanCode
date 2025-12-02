package com.example.huffmancode.controller;

import com.example.huffmancode.service.HuffmanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController // 标记为REST控制器 组合注解，相当于@Controller + @ResponseBody
@RequestMapping("/api/huffman") // 基础路径
public class HuffmanController {

    private final HuffmanService huffmanService; // 使用final 表示依赖不可变
    // 构造器注入 - 推荐方式
    public HuffmanController(HuffmanService huffmanService) {
        this.huffmanService = huffmanService;
    }

    // 编码接口
    @PostMapping("/process")
    // ResponseEntity是Spring提供的一个通用响应类型，允许我们自定义HTTP状态码、头信息和响应体
    public ResponseEntity<?> encode(@RequestBody EncodeRequest request) { // @RequestBody将JSON转换为Java对象
        String text = request.getText();
        if (text == null || text.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Text cannot be empty."));
        }

        Map<Character, Integer> frequencies = huffmanService.getFrequencies(text);
        // --- 情况1: 输入文本只有一个唯一字符 ---
        if (frequencies.size() <= 1) {
            // 对于单个字符或空文本，没有树可以生成
            Map<Character, String> codes = Map.of();
            String encodedText = "";
            if (frequencies.size() == 1) {
                char singleChar = frequencies.keySet().iterator().next(); // keySet()获取键的集合，iterator()获取迭代器，next()获取第一个元素
                codes = Map.of(singleChar, "0");
                encodedText = "0".repeat(text.length());
            }
            EncodeResponse response = new EncodeResponse(text, encodedText, codes, frequencies);
            // treeDot 保持为 null，因为没有树
            return ResponseEntity.ok(response);
        }

        // 1. 构建哈夫曼树并保存根节点
        com.example.huffmancode.model.HuffmanNode root = huffmanService.buildTree(frequencies);

        // 2. 使用树生成哈夫曼编码
        Map<Character, String> codes = huffmanService.generateCodes(root);

        // 3. 对原文进行编码
        String encodedText = huffmanService.encode(text, codes);

        // 4. 【关键】使用同一棵树生成用于可视化的DOT字符串
        String treeDot = huffmanService.generateDot(root);

        // 5. 创建响应对象
        EncodeResponse response = new EncodeResponse(text, encodedText, codes, frequencies);

        // 6. 【关键】将DOT字符串设置到响应中
        response.setTreeDot(treeDot);

        return ResponseEntity.ok(response);

    }

    // 解码接口
    @PostMapping("/decode")
    public ResponseEntity<?> decode(@RequestBody DecodeRequest request) {
        if (request.getEncodedText() == null || request.getCodes() == null || request.getCodes().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Encoded text and codes cannot be empty."));
        }

        String decodedText = huffmanService.decode(request.getEncodedText(), request.getCodes());
        DecodeResponse response = new DecodeResponse(decodedText);
        return ResponseEntity.ok(response);
    }

    // --- 请求和响应的数据类 (DTOs) ---

    // 编码请求体
    static class EncodeRequest {
        private String text;
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    // 编码响应体
    static class EncodeResponse {
        private final String originalText;
        private final String encodedText;
        private final Map<Character, String> codes;
        private final Map<Character, Integer> frequencies; // 新增字段
        private String treeDot;

        public EncodeResponse(String originalText, String encodedText, Map<Character, String> codes, Map<Character, Integer> frequencies) {
            this.originalText = originalText;
            this.encodedText = encodedText;
            this.codes = codes;
            this.frequencies = frequencies;
        }

        // Getters and Setters
        public String getOriginalText() { return originalText; }
        public String getEncodedText() { return encodedText; }
        public Map<Character, String> getCodes() { return codes; }
        public Map<Character, Integer> getFrequencies() { return frequencies; } // 新增Getter
        public String getTreeDot() { return treeDot; }
        public void setTreeDot(String treeDot) { this.treeDot = treeDot; }
    }

    // 解码请求体
    static class DecodeRequest {
        private String encodedText;
        private Map<Character, String> codes;

        public String getEncodedText() { return encodedText; }
        public void setEncodedText(String encodedText) { this.encodedText = encodedText; }
        public Map<Character, String> getCodes() { return codes; }
        public void setCodes(Map<Character, String> codes) { this.codes = codes; }
    }

    // 解码响应体
    static class DecodeResponse {
        private final String decodedText;

        public DecodeResponse(String decodedText) {
            this.decodedText = decodedText;
        }

        public String getDecodedText() { return decodedText; }
    }
}
