package com.example.huffmancode.service;

import com.example.huffmancode.model.HuffmanNode;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

@Service
public class HuffmanService {

    // 用于为DOT图中的节点生成唯一ID
    private int nodeIdCounter;

    // 1. 统计字符频率
    public Map<Character, Integer> getFrequencies(String text) {
        Map<Character, Integer> freqMap = new HashMap<>();
        for (char c : text.toCharArray()) {
            freqMap.put(c, freqMap.getOrDefault(c, 0) + 1);
        }
        return freqMap;
    }

    // 2. 构建哈夫曼树
    public HuffmanNode buildTree(Map<Character, Integer> freqMap) {
        PriorityQueue<HuffmanNode> pq = new PriorityQueue<>();
        for (Map.Entry<Character, Integer> entry : freqMap.entrySet()) {
            pq.add(new HuffmanNode(entry.getKey(), entry.getValue()));
        }

        while (pq.size() > 1) {
            HuffmanNode left = pq.poll(); // 取出频率最小的两个节点
            HuffmanNode right = pq.poll();

            HuffmanNode parent = new HuffmanNode(null, left.getFrequency() + right.getFrequency());
            parent.setLeft(left);
            parent.setRight(right);

            pq.add(parent);
        }
        return pq.poll();
    }

    // 3. 生成哈夫曼编码
    public Map<Character, String> generateCodes(HuffmanNode root) {
        Map<Character, String> codeMap = new HashMap<>();
        generateCodesRecursive(root, "", codeMap);
        return codeMap;
    }
    // 辅助方法
    private void generateCodesRecursive(HuffmanNode node, String code, Map<Character, String> codeMap) {
        if (node == null) return;
        if (node.isLeaf()) {
            // 对于只有一个节点的树，其编码是"0"
            codeMap.put(node.getCharacter(), code.isEmpty() ? "0" : code);
            return;
        }
        generateCodesRecursive(node.getLeft(), code + "0", codeMap);
        generateCodesRecursive(node.getRight(), code + "1", codeMap);
    }

    // 4. 对原文进行编码
    public String encode(String text, Map<Character, String> codeMap) {
        StringBuilder encodedText = new StringBuilder();
        for (char c : text.toCharArray()) {
            encodedText.append(codeMap.get(c));
        }
        return encodedText.toString();
    }

    // 5. 对编码进行解码 (使用编码表)
    public String decode(String encodedText, Map<Character, String> codeMap) {
        StringBuilder decodedText = new StringBuilder();
        Map<String, Character> reversedCodes = new java.util.HashMap<>();
        for (Map.Entry<Character, String> entry : codeMap.entrySet()) {
            reversedCodes.put(entry.getValue(), entry.getKey());
        }

        StringBuilder currentCode = new StringBuilder();
        for (char bit : encodedText.toCharArray()) {
            currentCode.append(bit);
            if (reversedCodes.containsKey(currentCode.toString())) { // containsKey是Map的方法，用于检查键是否存在
                decodedText.append(reversedCodes.get(currentCode.toString()));
                currentCode.setLength(0);
            }
        }
        return decodedText.toString();
    }

    // 6. 对编码进行解码 (使用哈夫曼树)
    public String decode(String encodedText, HuffmanNode root) {
        // 如果树为空或只有一个节点但没有子节点（例如，空文本输入），则无法解码
        if (root == null || root.isLeaf()) {
            return root != null && !encodedText.isEmpty() ? String.valueOf(root.getCharacter()).repeat(encodedText.length()) : "";
        }
        StringBuilder decodedText = new StringBuilder();
        HuffmanNode current = root;
        for (char bit : encodedText.toCharArray()) {
            if (bit == '0') {
                current = current.getLeft();
            } else {
                current = current.getRight();
            }

            if (current.isLeaf()) {
                decodedText.append(current.getCharacter());
                current = root;
            }
        }
        return decodedText.toString();
    }

    // --- 新增方法：生成DOT语言格式的树结构字符串 ---
    public String generateDot(HuffmanNode root) {
        this.nodeIdCounter = 0; // 每次调用都重置计数器
        StringBuilder dot = new StringBuilder("digraph HuffmanTree {\n");
        dot.append("    node [shape=record, style=filled, fillcolor=\"#f0f0f0\"];\n");
        dot.append("    edge [fontname=\"helvetica\"];\n");
        if (root != null) {
            generateDotRecursive(root, dot);
        }
        dot.append("}\n");
        return dot.toString();
    }

    private String generateDotRecursive(HuffmanNode node, StringBuilder dot) {
        String myId = "n" + (this.nodeIdCounter++);
        String label;

        if (node.isLeaf()) {
            // 叶子节点：显示字符和频率
            char c = node.getCharacter();
            // 对特殊字符进行转义，以便在DOT语言中正确显示
            String charStr = switch (c) {
                case ' ' -> "Space";
                case '\n' -> "\\\\n";
                case '\t' -> "\\\\t";
                case '"' -> "\\\"";
                case '\\' -> "\\\\";
                default -> String.valueOf(c);
            };
            label = String.format("{<f0> '%s' |<f1> %d}", charStr, node.getFrequency());
            dot.append(String.format("    %s [label=\"%s\", fillcolor=\"#cdeac0\"];\n", myId, label));
        } else {
            // 内部节点：只显示频率
            label = String.format("<f0> %d", node.getFrequency());
            dot.append(String.format("    %s [label=\"%s\"];\n", myId, label));
        }

        if (node.getLeft() != null) {
            String leftId = generateDotRecursive(node.getLeft(), dot);
            dot.append(String.format("    %s:f0 -> %s:f0 [label=\"0\"];\n", myId, leftId));
        }
        if (node.getRight() != null) {
            String rightId = generateDotRecursive(node.getRight(), dot);
            dot.append(String.format("    %s:f0 -> %s:f0 [label=\"1\"];\n", myId, rightId));
        }
        return myId;
    }
}
