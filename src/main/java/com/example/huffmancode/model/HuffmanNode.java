package com.example.huffmancode.model;
// 哈夫曼树节点
public class HuffmanNode implements Comparable<HuffmanNode> {
    private Character character; // 字符
    private int frequency;     // 频率
    private HuffmanNode left;  // 左子节点
    private HuffmanNode right; // 右子节点

    public HuffmanNode(Character character, int frequency) {
        this.character = character;
        this.frequency = frequency;
    }
    // 辅助方法
    // Getters and Setters
    public Character getCharacter() {
        return character;
    }
    public void setCharacter(Character character) {
        this.character = character;
    }

    public int getFrequency() {
        return frequency;
    }
    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public HuffmanNode getLeft() {
        return left;
    }
    public void setLeft(HuffmanNode left) {
        this.left = left;
    }

    public HuffmanNode getRight() {
        return right;
    }
    public void setRight(HuffmanNode right) {
        this.right = right;
    }

    // 节点按频率排序，用于构建优先队列
    @Override
    public int compareTo(HuffmanNode other) {
        return this.frequency - other.frequency;
    }

    // 判断是否是叶子节点
    public boolean isLeaf() {
        return this.left == null && this.right == null;
    }
}
