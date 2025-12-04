<template>
  <div class="app-layout">
    <!-- 左侧导航栏 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <span class="logo-icon">🛒</span>
        <h1 class="logo-text">哈夫曼工具箱</h1>
      </div>
      <nav class="navigation">
        <a href="#"
           :class="['nav-item', { active: currentPage === 'huffman' }]"
           @click.prevent="currentPage = 'huffman'">
          <span class="nav-icon">🔐</span>
          <span>编码译码</span>
        </a>
        <a href="#"
           :class="['nav-item', { active: currentPage === 'send' }]"
           @click.prevent="currentPage = 'send'">
          <span class="nav-icon">📤</span>
          <span>消息通信</span>
        </a>
        <a href="#"
           :class="['nav-item', { active: currentPage === 'history' }]"
           @click.prevent="currentPage = 'history'">
          <span class="nav-icon">📋</span>
          <span>操作历史</span>
        </a>
      </nav>
    </aside>

    <!-- 右侧主内容区 -->
    <div class="main-wrapper">
      <!-- 顶部信息栏 -->
      <header class="top-bar">
        <h2 class="page-title">
          {{ currentPage === 'huffman' ? '哈夫曼编码/解码器' : (currentPage === 'send' ? 'Socket 消息通信' : '历史记录') }}
        </h2>
        <div class="user-info">
          <div class="avatar">{{ username.charAt(0).toUpperCase() }}</div>
          <span class="username">{{ username }}</span>
          <button @click="logout" class="logout-button" title="退出登录">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
          </button>
        </div>
      </header>

      <!-- 内容面板 -->
      <main class="main-content">
        <!-- 发送页面 -->
        <div v-if="currentPage === 'send'" class="content-card">
          <div class="send-panel">
            <div class="connection-status">
              <div :class="['status-dot', { connected: isConnected }]"></div>
              <span>{{ isConnected ? '已连接' : '未连接' }}</span>
              <button v-if="!isConnected" @click="connectWebSocket" class="connect-btn">连接</button>
              <button v-else @click="disconnectWebSocket" class="disconnect-btn">断开</button>
              <button @click="loadMessageHistory" class="history-btn">加载历史</button>
            </div>
            <div class="message-section">
              <div class="input-area">
                <h2 class="section-title">发送消息</h2>
                <div class="receiver-input">
                  <label>接收者 (留空则广播):</label>
                  <input v-model="messageReceiver" placeholder="输入接收者用户名..." />
                </div>
                <textarea v-model="messageToSend" placeholder="在此输入要发送的消息..."></textarea>
                <div class="send-options">
                  <label>
                    <input type="checkbox" v-model="encodeBeforeSend" />
                    发送前进行哈夫曼编码
                  </label>
                </div>
                <button @click="sendMessage" class="action-button primary" :disabled="!isConnected || !messageToSend.trim()">
                  发送消息
                </button>
              </div>
              <div class="received-area">
                <h2 class="section-title">消息记录</h2>
                <div class="message-tabs">
                  <button :class="{ active: messageTab === 'all' }" @click="messageTab = 'all'">全部</button>
                  <button :class="{ active: messageTab === 'sent' }" @click="messageTab = 'sent'">已发送</button>
                  <button :class="{ active: messageTab === 'received' }" @click="messageTab = 'received'">已接收</button>
                </div>
                <div class="messages-list">
                  <div v-if="filteredMessages.length === 0" class="placeholder">
                    <div class="placeholder-icon">💬</div>
                    <p>暂无消息</p>
                  </div>
                  <div v-else v-for="msg in filteredMessages" :key="msg.id" :class="['message-item', getMsgClass(msg)]">
                    <div class="message-header">
                      <span class="message-sender">{{ msg.sender }}</span>
                      <span class="message-time">{{ formatTime(msg.timestamp) }}</span>
                    </div>
                    <div class="message-content">{{ msg.message }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 历史页面 -->
        <div v-else-if="currentPage === 'history'" class="content-card">
          <div class="history-panel-header">
            <h2 class="section-title">操作历史记录</h2>
            <button v-if="historyList.length > 0" @click="clearHistory" class="clear-btn">
              清空历史
            </button>
          </div>
          <div class="history-panel">
            <div v-if="historyList.length === 0" class="placeholder">
              <div class="placeholder-icon">📋</div>
              <p>暂无历史记录</p>
            </div>
            <div v-else class="history-list">
              <div v-for="(item, index) in historyList" :key="index" class="history-item">
                <div class="history-header">
                  <span class="history-type">{{ item.type }}</span>
                  <span class="history-time">{{ item.time }}</span>
                </div>
                <div class="history-content">
                  <div class="history-original"><strong>原文:</strong> {{ item.original }}</div>
                  <div class="history-encoded"><strong>结果:</strong> {{ item.encoded }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 哈夫曼编码译码页面 -->
        <div v-else class="content-card">
          <div class="tool-tabs">
            <button @click="activeTab = 'encode'" :class="{ active: activeTab === 'encode' }" class="tab-button">编码</button>
            <button @click="activeTab = 'decode'" :class="{ active: activeTab === 'decode' }" class="tab-button">解码</button>
            <button @click="activeTab = 'tree'" :class="{ active: activeTab === 'tree' }" class="tab-button tree-tab" :disabled="!encodeResult">查看哈夫曼树</button>
          </div>
          <div v-if="activeTab === 'encode'" class="coder-panel">
            <div class="input-section">
              <h2 class="section-title">输入文本</h2>
              <div class="textarea-wrapper">
                <textarea v-model="textToEncode" placeholder="在此输入要编码的文本..."></textarea>
              </div>
              <button @click="handleEncode" class="action-button primary" :disabled="isLoading">
                {{ isLoading ? '编码中...' : '开始编码' }}
              </button>
            </div>
            <div class="output-section">
              <h2 class="section-title">编码结果</h2>
              <div class="result-content">
                <div v-if="!encodeResult" class="placeholder">
                  <div class="placeholder-icon">📊</div>
                  <p>编码结果将在这里显示</p>
                </div>
                <div v-else class="result-display">
                  <div class="output-tabs">
                    <button :class="{ active: outputTab === 'codes' }" @click="outputTab = 'codes'">编码表</button>
                    <button :class="{ active: outputTab === 'encoded' }" @click="outputTab = 'encoded'">编码结果</button>
                    <button :class="{ active: outputTab === 'freq' }" @click="outputTab = 'freq'">字符频率</button>
                  </div>
                  <div class="output-content">
                    <div v-if="outputTab === 'codes'" class="result-item">
                      <div class="result-header">
                        <h3>哈夫曼编码表</h3>
                        <div class="btn-group">
                          <button class="copy-btn" @click="copyToClipboard(JSON.stringify(encodeResult.codes))">复制JSON</button>
                        </div>
                      </div>
                      <pre class="code-box">{{ formatCodes(encodeResult.codes) }}</pre>
                    </div>
                    <div v-if="outputTab === 'encoded'" class="result-item">
                      <div class="result-header">
                        <h3>编码后的文本</h3>
                        <button class="copy-btn" @click="copyToClipboard(encodeResult.encodedText)">复制</button>
                      </div>
                      <pre class="code-box">{{ encodeResult.encodedText }}</pre>
                    </div>
                    <div v-if="outputTab === 'freq'" class="result-item">
                      <div class="result-header">
                        <h3>字符频率统计</h3>
                      </div>
                      <pre class="code-box">{{ formatFrequencies(encodeResult.frequencies) }}</pre>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div v-if="activeTab === 'decode'" class="coder-panel">
            <div class="input-section">
              <h2 class="section-title">输入编码</h2>
              <div class="textarea-wrapper">
                <textarea v-model="textToDecode" placeholder="在此输入要解码的二进制文本..."></textarea>
              </div>
              <div class="codes-input">
                <h3>编码表 (JSON格式)</h3>
                <textarea v-model="codesForDecode" placeholder='{"a": "00", "b": "01", ...}'></textarea>
              </div>
              <button @click="handleDecode" class="action-button primary" :disabled="isDecoding">
                {{ isDecoding ? '解码中...' : '开始解码' }}
              </button>
            </div>
            <div class="output-section">
              <h2 class="section-title">解码结果</h2>
              <div class="result-content">
                <div v-if="!decodeResult" class="placeholder">
                  <div class="placeholder-icon">📝</div>
                  <p>解码结果将在这里显示</p>
                </div>
                <div v-else class="result-display">
                  <div class="result-item">
                    <div class="result-header">
                      <h3>解码后的文本</h3>
                      <button class="copy-btn" @click="copyToClipboard(decodeResult)">复制</button>
                    </div>
                    <pre class="code-box">{{ decodeResult }}</pre>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-if="activeTab === 'tree'" class="tree-panel">
            <div class="tree-toolbar">
              <button class="zoom-btn" @click="zoomOut">-</button>
              <span>缩放: {{ Math.round(scale * 100) }}%</span>
              <button class="zoom-btn" @click="zoomIn">+</button>
              <button class="zoom-btn" @click="resetZoom">重置</button>
              <button class="retry-btn" @click="retryRender">重新渲染</button>
            </div>
            <div class="tree-container">
              <div v-if="isTreeLoading" class="tree-loading">
                <div class="loading-spinner"></div>
                <p>正在渲染哈夫曼树...</p>
              </div>
              <div v-else-if="renderError" class="error-msg">
                <p><strong>渲染失败:</strong></p>
                <p>{{ renderError }}</p>
              </div>
              <div v-else-if="svgContent"
                   ref="graphContainer"
                   class="graph-container"
                   @wheel.prevent="handleWheel"
                   @mousedown="startDrag"
                   @mousemove="onDrag"
                   @mouseup="endDrag"
                   @mouseleave="endDrag">
                <div ref="svgWrapper" class="svg-wrapper" :style="transformStyle" v-html="svgContent"></div>
              </div>
              <div v-else class="placeholder">
                <div class="placeholder-icon">🌳</div>
                <p>请先进行编码操作</p>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const router = useRouter();

// --- 页面和标签状态 ---
const currentPage = ref('huffman');
const activeTab = ref('encode');
const outputTab = ref('codes');
const username = ref('User');

// --- 编码相关状态 ---
const textToEncode = ref('');
const isLoading = ref(false);
const encodeResult = ref(null);

// --- 解码相关状态 ---
const textToDecode = ref('');
const codesForDecode = ref('');
const isDecoding = ref(false);
const decodeResult = ref(null);

// --- WebSocket相关状态 ---
const isConnected = ref(false);
const messageToSend = ref('');
const messageReceiver = ref('');
const encodeBeforeSend = ref(false);
const receivedMessages = ref([]);
const messageTab = ref('all');
let stompClient = null;

// --- 历史记录 ---
const historyList = ref([]);

// --- 树可视化 ---
const graphContainer = ref(null);
const svgWrapper = ref(null);
const renderError = ref('');
const isTreeLoading = ref(false);
const svgContent = ref('');

// 缩放和拖拽状态
const scale = ref(1);
const translateX = ref(0);
const translateY = ref(0);
const isDragging = ref(false);
const dragStartX = ref(0);
const dragStartY = ref(0);
const lastTranslateX = ref(0);
const lastTranslateY = ref(0);

let vizInstance = null;

const transformStyle = computed(() => ({
  transform: `translate(${translateX.value}px, ${translateY.value}px) scale(${scale.value})`,
  transformOrigin: 'center center'
}));

const filteredMessages = computed(() => {
  if (messageTab.value === 'sent') {
    return receivedMessages.value.filter(msg => msg.sender === username.value);
  } else if (messageTab.value === 'received') {
    return receivedMessages.value.filter(msg => msg.sender !== username.value && msg.type !== 'JOIN' && msg.type !== 'LEAVE');
  }
  return receivedMessages.value;
});

const getMsgClass = (msg) => {
  if (msg.type === 'JOIN' || msg.type === 'LEAVE') {
    return 'system';
  }
  if (msg.sender === username.value) {
    return 'sent';
  }
  return 'received';
};

const formatTime = (timestamp) => {
  if (!timestamp) return '';
  try {
    const date = new Date(timestamp);
    return date.toLocaleString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  } catch {
    return timestamp;
  }
};

// --- 生命周期钩子 ---
onMounted(async () => {
  const storedUsername = localStorage.getItem('username');
  if (storedUsername) {
    username.value = storedUsername;
  } else {
    router.push('/login'); // 如果没有用户名，则重定向到登录页
    return;
  }

  const savedHistory = localStorage.getItem('huffmanHistory');
  if (savedHistory) {
    historyList.value = JSON.parse(savedHistory);
  }

  await initViz();
});

onUnmounted(() => {
  if (stompClient) {
    stompClient.deactivate();
  }
});

const initViz = async () => {
  try {
    const { instance } = await import('@viz-js/viz');
    vizInstance = await instance();
  } catch (error) {
    console.error('Viz.js 初始化失败:', error);
  }
};

watch([activeTab, encodeResult], async ([newTab, newResult]) => {
  if (newTab === 'tree' && newResult && newResult.treeDot) {
    renderError.value = '';
    await nextTick();
    renderTree(newResult.treeDot);
  }
});

const renderTree = async (dotString) => {
  if (!dotString) {
    renderError.value = '没有可渲染的数据';
    return;
  }

  isTreeLoading.value = true;
  renderError.value = '';
  svgContent.value = '';

  try {
    if (!vizInstance) {
      await initViz();
    }

    if (!vizInstance) {
      throw new Error('Viz.js 未能初始化');
    }

    const svg = vizInstance.renderSVGElement(dotString);
    svg.setAttribute('width', '100%');
    svg.setAttribute('height', '100%');
    svg.style.maxWidth = '100%';
    svg.style.maxHeight = '100%';

    svgContent.value = svg.outerHTML;
    resetZoom();
  } catch (error) {
    console.error('渲染失败:', error);
    renderError.value = error.message || '渲染失败，请重试';
  } finally {
    isTreeLoading.value = false;
  }
};

const retryRender = () => {
  if (encodeResult.value && encodeResult.value.treeDot) {
    renderTree(encodeResult.value.treeDot);
  } else {
    alert("没有可渲染的数据，请先进行编码");
  }
};

const handleWheel = (event) => {
  const delta = event.deltaY > 0 ? -0.1 : 0.1;
  const newScale = Math.max(0.1, Math.min(3, scale.value + delta));
  scale.value = newScale;
};

const zoomIn = () => {
  scale.value = Math.min(3, scale.value + 0.2);
};

const zoomOut = () => {
  scale.value = Math.max(0.1, scale.value - 0.2);
};

const resetZoom = () => {
  scale.value = 1;
  translateX.value = 0;
  translateY.value = 0;
};

const startDrag = (event) => {
  isDragging.value = true;
  dragStartX.value = event.clientX;
  dragStartY.value = event.clientY;
  lastTranslateX.value = translateX.value;
  lastTranslateY.value = translateY.value;
};

const onDrag = (event) => {
  if (!isDragging.value) return;
  const deltaX = event.clientX - dragStartX.value;
  const deltaY = event.clientY - dragStartY.value;
  translateX.value = lastTranslateX.value + deltaX;
  translateY.value = lastTranslateY.value + deltaY;
};

const endDrag = () => {
  isDragging.value = false;
};

const copyToClipboard = async (text) => {
  try {
    await navigator.clipboard.writeText(text);
    alert('已复制到剪贴板');
  } catch (err) {
    console.error('复制失败:', err);
    alert('复制失败');
  }
};

const handleEncode = async () => {
  if (!textToEncode.value.trim()) {
    alert('请输入要编码的文本！');
    return;
  }
  isLoading.value = true;
  encodeResult.value = null;
  decodeResult.value = null;
  outputTab.value = 'codes';

  try {
    const response = await axios.post('/api/huffman/process', {
      text: textToEncode.value
    });
    encodeResult.value = response.data;
    addToHistory('编码', textToEncode.value, response.data.encodedText);
  } catch (error) {
    console.error('编码失败:', error);
    alert('编码失败，请检查后端服务或网络连接。');
  } finally {
    isLoading.value = false;
  }
};

const handleDecode = async () => {
  if (!textToDecode.value.trim() || !codesForDecode.value.trim()) {
    alert('请输入编码文本和编码表！');
    return;
  }

  let codes;
  try {
    codes = JSON.parse(codesForDecode.value);
  } catch (e) {
    alert('编码表格式错误，请输入有效的JSON格式！');
    return;
  }

  isDecoding.value = true;
  decodeResult.value = null;

  try {
    const response = await axios.post('/api/huffman/decode', {
      encodedText: textToDecode.value,
      codes: codes
    });
    decodeResult.value = response.data.decodedText;
    addToHistory('解码', textToDecode.value, response.data.decodedText);
  } catch (error) {
    console.error('解码失败:', error);
    alert('解码失败，请检查后端服务或输入内容。');
  } finally {
    isDecoding.value = false;
  }
};

// --- WebSocket方法 ---
const connectWebSocket = () => {
  const socket = new SockJS('http://localhost:8080/ws');
  stompClient = new Client({
    webSocketFactory: () => socket,
    reconnectDelay: 5000,
    onConnect: () => {
      isConnected.value = true;
      stompClient.subscribe('/topic/messages', (message) => {
        const msg = JSON.parse(message.body);
        if (msg.sender !== username.value || msg.type === 'JOIN' || msg.type === 'LEAVE') {
          receivedMessages.value.unshift({
            id: msg.id || Date.now(),
            sender: msg.sender,
            message: msg.message,
            timestamp: msg.timestamp,
            type: msg.type || 'MESSAGE'
          });
        }
      });
      stompClient.subscribe(`/user/${username.value}/queue/private`, (message) => {
        const msg = JSON.parse(message.body);
        receivedMessages.value.unshift({
          id: msg.id || Date.now(),
          sender: msg.sender,
          message: msg.message,
          timestamp: msg.timestamp,
          type: 'PRIVATE'
        });
      });
      stompClient.publish({
        destination: '/app/join',
        body: JSON.stringify({ username: username.value })
      });
    },
    onDisconnect: () => {
      isConnected.value = false;
    },
    onStompError: (frame) => {
      console.error('STOMP 错误:', frame);
      isConnected.value = false;
    }
  });
  stompClient.activate();
};

const disconnectWebSocket = () => {
  if (stompClient) {
    stompClient.publish({
      destination: '/app/leave',
      body: JSON.stringify({ username: username.value })
    });
    stompClient.deactivate();
  }
};

const sendMessage = async () => {
  if (!messageToSend.value.trim() || !isConnected.value) return;

  let messageContent = messageToSend.value;
  let originalMessage = messageToSend.value;

  if (encodeBeforeSend.value) {
    try {
      const response = await axios.post('/api/huffman/process', {
        text: messageToSend.value
      });
      messageContent = response.data.encodedText;
      addToHistory('发送(编码)', originalMessage, messageContent);
    } catch (error) {
      console.error('编码失败:', error);
      alert('编码失败');
      return;
    }
  } else {
    addToHistory('发送', originalMessage, originalMessage);
  }

  stompClient.publish({
    destination: '/app/send',
    body: JSON.stringify({
      message: messageContent,
      sender: username.value,
      receiver: messageReceiver.value || null,
      encoded: encodeBeforeSend.value
    })
  });

  receivedMessages.value.unshift({
    id: Date.now(),
    sender: username.value,
    message: messageContent,
    timestamp: new Date().toISOString(),
    type: messageReceiver.value ? 'PRIVATE' : 'MESSAGE'
  });

  messageToSend.value = '';
};

const loadMessageHistory = async () => {
  try {
    const response = await axios.get(`/api/messages/history/${username.value}`);
    const existingIds = new Set(receivedMessages.value.map(m => m.id));
    const newMessages = response.data
      .filter(m => !existingIds.has(m.id))
      .map(m => ({
        id: m.id,
        sender: m.sender,
        message: m.content,
        timestamp: m.createdAt,
        type: m.messageType
      }));
    receivedMessages.value = [...newMessages, ...receivedMessages.value].sort((a, b) => {
      return new Date(b.timestamp) - new Date(a.timestamp);
    });
  } catch (error) {
    console.error('加载历史失败:', error);
    alert('加载消息历史失败');
  }
};

const addToHistory = (type, original, processed) => {
  const item = {
    type,
    original: original.substring(0, 100) + (original.length > 100 ? '...' : ''),
    encoded: processed.substring(0, 100) + (processed.length > 100 ? '...' : ''),
    time: new Date().toLocaleString()
  };
  historyList.value.unshift(item);
  if (historyList.value.length > 50) {
    historyList.value = historyList.value.slice(0, 50);
  }
  localStorage.setItem('huffmanHistory', JSON.stringify(historyList.value));
};

const clearHistory = () => {
  if (confirm('确定要清空所有历史记录吗？')) {
    historyList.value = [];
    localStorage.removeItem('huffmanHistory');
  }
};

const formatFrequencies = (frequencies) => {
  if (!frequencies) return '';
  return Object.entries(frequencies)
    .map(([char, freq]) => `'${char}': ${freq}`)
    .join(',\n');
};

const formatCodes = (codes) => {
  if (!codes) return '';
  return Object.entries(codes)
    .map(([char, code]) => `'${char}': "${code}"`)
    .join(',\n');
};

const logout = () => {
  localStorage.removeItem('username');
  localStorage.removeItem('user');
  if (stompClient) {
    stompClient.deactivate();
  }
  router.push('/login');
};
</script>

<style>
/* 全局样式重置 */
body, html {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background-color: #f4f7f6; /* 页面背景色 */
  color: #333;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}
</style>

<style scoped>
/* 主布局 */
.app-layout {
  display: flex;
  width: 100vw;
  height: 100vh;
}

/* 侧边栏 */
.sidebar {
  width: 220px;
  flex-shrink: 0;
  background-color: #2c3e50; /* 深蓝色背景 */
  color: #ecf0f1;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 5px rgba(0,0,0,0.1);
  z-index: 100;
}

.sidebar-header {
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-icon {
  font-size: 24px;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  color: #fff;
}

.navigation {
  flex-grow: 1;
  padding: 10px 0;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  margin: 4px 10px;
  border-radius: 6px;
  color: #bdc3c7;
  text-decoration: none;
  transition: all 0.3s;
  font-size: 15px;
  border-left: 3px solid transparent;
}

.nav-icon {
  font-size: 18px;
}

.nav-item:hover {
  background-color: #34495e;
  color: #fff;
}

.nav-item.active {
  background-color: #1abc9c;
  color: #fff;
  font-weight: 500;
}

/* 右侧主内容区 */
.main-wrapper {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 顶部栏 */
.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 60px;
  background-color: #fff;
  border-bottom: 1px solid #e0e0e0;
  flex-shrink: 0;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #1abc9c;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: bold;
}

.username {
  font-size: 15px;
  font-weight: 500;
  color: #555;
}

.logout-button {
  background: none;
  border: none;
  cursor: pointer;
  color: #7f8c8d;
  padding: 6px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.logout-button:hover {
  color: #e74c3c;
  background-color: #fbeeee;
}

/* 内容面板 */
.main-content {
  flex-grow: 1;
  padding: 24px;
  overflow-y: auto;
  background-color: #f4f7f6;
}

.content-card {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  padding: 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.tool-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  border-bottom: 1px solid #eee;
}

.tab-button {
  padding: 10px 20px;
  border: none;
  border-bottom: 3px solid transparent;
  background-color: transparent;
  cursor: pointer;
  font-size: 15px;
  color: #7f8c8d;
  transition: all 0.3s;
  font-weight: 500;
}

.tab-button:hover:not(:disabled) {
  color: #1abc9c;
}

.tab-button.active {
  color: #1abc9c;
  border-bottom-color: #1abc9c;
}

.tab-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.coder-panel, .send-panel {
  flex-grow: 1;
  display: flex;
  gap: 24px;
  min-height: 0;
}

.input-section, .output-section, .input-area, .received-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.section-title {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  padding-bottom: 12px;
  border-bottom: 1px solid #eee;
}

.textarea-wrapper {
  flex-grow: 1;
  margin-bottom: 16px;
  min-height: 150px;
  display: flex;
}

textarea {
  width: 100%;
  height: 100%;
  border: 1px solid #ddd;
  border-radius: 6px;
  padding: 12px;
  font-size: 14px;
  resize: none;
  outline: none;
  transition: border-color 0.3s;
  background-color: #fdfdfd;
  color: #333;
  box-sizing: border-box;
  font-family: inherit;
  line-height: 1.6;
}

textarea:focus {
  border-color: #1abc9c;
}

.codes-input {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  margin-bottom: 16px;
  min-height: 120px;
}

.codes-input h3 {
  font-size: 14px;
  color: #7f8c8d;
  margin: 0 0 8px 0;
}

.action-button {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 6px;
  color: white;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  flex-shrink: 0;
}

.action-button.primary {
  background-color: #1abc9c;
}

.action-button:hover:not(:disabled) {
  opacity: 0.9;
}

.action-button:disabled {
  background-color: #bdc3c7;
  cursor: not-allowed;
}

.result-content, .messages-list, .history-list {
  flex-grow: 1;
  overflow-y: auto;
  min-height: 0;
}

.placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #95a5a6;
  text-align: center;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.result-display {
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex-grow: 1;
  overflow-y: auto;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.result-item h3 {
  font-size: 12px;
  color: #7f8c8d;
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.copy-btn {
  background-color: #ecf0f1;
  border: 1px solid #ddd;
  color: #7f8c8d;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.copy-btn:hover {
  background-color: #bdc3c7;
  color: white;
}

.code-box {
  background-color: #f8f9fa;
  padding: 14px;
  border-radius: 6px;
  border: 1px solid #eee;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  color: #2c3e50;
  max-height: 180px;
  overflow-y: auto;
  word-break: break-all;
  white-space: pre-wrap;
  line-height: 1.6;
}

.output-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  border-bottom: 1px solid #eee;
  flex-shrink: 0;
}

.output-tabs button {
  padding: 8px 16px;
  border: none;
  background-color: transparent;
  color: #7f8c8d;
  cursor: pointer;
  transition: all 0.3s;
  border-bottom: 2px solid transparent;
  font-size: 14px;
}

.output-tabs button.active {
  color: #1abc9c;
  border-bottom-color: #1abc9c;
}

.output-content {
  flex-grow: 1;
  min-height: 0;
  overflow-y: auto;
}

/* 树 */
.tree-panel { flex-grow: 1; display: flex; flex-direction: column; height: 100%; overflow: hidden; }
.tree-toolbar { text-align: center; padding: 8px; color: #7f8c8d; font-size: 12px; display: flex; justify-content: center; gap: 12px; align-items: center; }
.retry-btn, .zoom-btn { background: #fff; border: 1px solid #ddd; color: #555; cursor: pointer; padding: 4px 12px; border-radius: 4px; font-size: 12px; transition: all 0.2s; }
.retry-btn:hover, .zoom-btn:hover { color: #1abc9c; border-color: #1abc9c; }
.tree-container { flex-grow: 1; background-color: #f8f9fa; border-radius: 8px; border: 1px solid #eee; display: flex; align-items: center; justify-content: center; position: relative; overflow: hidden; }
.graph-container { width: 100%; height: 100%; overflow: hidden; cursor: grab; display: flex; align-items: center; justify-content: center; }
.graph-container:active { cursor: grabbing; }
.svg-wrapper { transition: transform 0.1s ease-out; display: flex; align-items: center; justify-content: center; }
.tree-loading { display: flex; flex-direction: column; align-items: center; justify-content: center; color: #7f8c8d; }
.error-msg { color: #e74c3c; background: #fbeeee; padding: 20px; border-radius: 8px; border: 1px solid #f5c6cb; max-width: 80%; }
.loading-spinner { margin: 20px auto; border: 3px solid #eee; border-top: 3px solid #1abc9c; border-radius: 50%; width: 30px; height: 30px; animation: spin 1s linear infinite; }
@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }

/* 消息通信 */
.connection-status { display: flex; align-items: center; gap: 12px; padding: 12px; background-color: #f8f9fa; border-radius: 8px; flex-shrink: 0; margin-bottom: 24px; border: 1px solid #eee; }
.status-dot { width: 10px; height: 10px; border-radius: 50%; background-color: #e74c3c; }
.status-dot.connected { background-color: #2ecc71; }
.connect-btn, .disconnect-btn, .history-btn { margin-left: auto; padding: 6px 16px; border: 1px solid; border-radius: 6px; cursor: pointer; font-size: 13px; transition: all 0.3s; }
.connect-btn { background-color: #1abc9c; color: white; border-color: #1abc9c; }
.disconnect-btn { background-color: transparent; color: #e74c3c; border-color: #e74c3c; }
.history-btn { background-color: transparent; color: #3498db; border-color: #3498db; margin-left: 12px; }
.message-section { flex-grow: 1; display: flex; gap: 24px; min-height: 0; }
.send-options { margin-bottom: 16px; color: #555; }
.send-options label { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.send-options input[type="checkbox"] { width: 16px; height: 16px; accent-color: #1abc9c; }
.receiver-input { margin-bottom: 12px; }
.receiver-input label { display: block; margin-bottom: 6px; color: #555; font-size: 13px; }
.receiver-input input { width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px; outline: none; transition: border-color 0.3s; box-sizing: border-box; }
.receiver-input input:focus { border-color: #1abc9c; }
.message-tabs { display: flex; gap: 8px; margin-bottom: 16px; border-bottom: 1px solid #eee; padding-bottom: 12px; }
.message-tabs button { padding: 6px 14px; border: none; background: transparent; color: #7f8c8d; cursor: pointer; font-size: 13px; border-radius: 4px; transition: all 0.3s; }
.message-tabs button.active { background: #e8f8f5; color: #1abc9c; }
.messages-list { display: flex; flex-direction: column; gap: 12px; padding-right: 10px; }
.message-item { padding: 10px 14px; border-radius: 12px; max-width: 80%; line-height: 1.5; }
.message-item.sent { background-color: #1abc9c; color: white; margin-left: auto; }
.message-item.received { background-color: #ecf0f1; color: #34495e; margin-right: auto; }
.message-item.system { background-color: #fef9e7; text-align: center; max-width: 100%; color: #f39c12; font-size: 12px; }
.message-header { display: flex; justify-content: space-between; margin-bottom: 4px; font-size: 12px; opacity: 0.8; }
.message-sender { font-weight: 600; }
.message-content { word-break: break-word; }

/* 历史记录 */
.history-panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding-bottom: 16px; border-bottom: 1px solid #eee; }
.history-panel { flex-grow: 1; overflow-y: auto; }
.history-list { display: flex; flex-direction: column; gap: 12px; }
.history-item { background-color: #f8f9fa; padding: 16px; border-radius: 8px; border: 1px solid #eee; }
.history-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.history-type { background-color: #3498db; color: white; padding: 3px 10px; border-radius: 12px; font-size: 12px; font-weight: 500; }
.history-time { color: #95a5a6; font-size: 12px; }
.history-content { display: flex; flex-direction: column; gap: 8px; font-size: 14px; }
.history-original, .history-encoded { color: #555; word-break: break-all; }
.history-original strong, .history-encoded strong { color: #2c3e50; }
.clear-btn { padding: 8px 16px; border: 1px solid #e74c3c; border-radius: 6px; background-color: transparent; color: #e74c3c; cursor: pointer; transition: all 0.3s; font-size: 13px; font-weight: 500; }
.clear-btn:hover { background-color: #e74c3c; color: white; }

/* Viz.js SVG 样式覆盖 */
.svg-wrapper :deep(svg) { max-width: 100%; max-height: 100%; }
.svg-wrapper :deep(.node text) { fill: #333 !important; }
.svg-wrapper :deep(.edge text) { fill: #777 !important; }
.svg-wrapper :deep(.node polygon),
.svg-wrapper :deep(.node ellipse) { fill: #fff !important; stroke: #1abc9c !important; stroke-width: 2px !important; }
.svg-wrapper :deep(.edge path) { stroke: #bdc3c7 !important; }
.svg-wrapper :deep(.graph > polygon) { fill: transparent !important; stroke: none !important; }
</style>
