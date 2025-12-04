<template>
  <div class="main-app-container">
    <aside class="sidebar">
      <div class="user-profile">
        <div class="avatar">{{ username.charAt(0).toUpperCase() }}</div>
        <div class="username">{{ username }}</div>
      </div>
      <nav class="navigation">
        <a href="#"
           :class="['nav-item', { active: currentPage === 'huffman' }]"
           @click.prevent="currentPage = 'huffman'">
          <span>🔐</span>
          <span>哈夫曼编码译码</span>
        </a>
        <a href="#"
           :class="['nav-item', { active: currentPage === 'send' }]"
           @click.prevent="currentPage = 'send'">
          <span>📤</span>
          <span>发送</span>
        </a>
        <a href="#"
           :class="['nav-item', { active: currentPage === 'history' }]"
           @click.prevent="currentPage = 'history'">
          <span>📋</span>
          <span>历史</span>
        </a>
      </nav>
      <button @click="logout" class="logout-button">退出登录</button>
    </aside>

    <main class="main-content">
      <!-- 发送页面 -->
      <div v-if="currentPage === 'send'" class="page-content">
        <header class="content-header">
          <h1>Socket 消息通信</h1>
          <p>通过 WebSocket 实时发送和接收消息</p>
        </header>
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
              <h2>发送消息</h2>
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
              <button @click="sendMessage" class="action-button" :disabled="!isConnected || !messageToSend.trim()">
                发送消息
              </button>
            </div>
            <div class="received-area">
              <h2>消息记录</h2>
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
      <div v-else-if="currentPage === 'history'" class="page-content">
        <header class="content-header">
          <h1>历史记录</h1>
          <p>查看编码和发送的历史记录</p>
        </header>
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
          <button v-if="historyList.length > 0" @click="clearHistory" class="clear-btn">
            清空历史
          </button>
        </div>
      </div>

      <!-- 哈夫曼编码译码页面 -->
      <div v-else class="page-content">
        <header class="content-header">
          <h1>哈夫曼编码/解码器</h1>
          <p>一个用于文本和数据压缩的可视化工具</p>
        </header>
        <div class="tool-tabs">
          <button @click="activeTab = 'encode'" :class="{ active: activeTab === 'encode' }" class="tab-button">编码</button>
          <button @click="activeTab = 'decode'" :class="{ active: activeTab === 'decode' }" class="tab-button">解码</button>
          <button @click="activeTab = 'tree'" :class="{ active: activeTab === 'tree' }" class="tab-button tree-tab" :disabled="!encodeResult">查看哈夫曼树</button>
        </div>
        <div v-if="activeTab === 'encode'" class="coder-panel">
          <div class="input-section">
            <h2>输入文本</h2>
            <div class="textarea-wrapper">
              <textarea v-model="textToEncode" placeholder="在此输入要编码的文本..."></textarea>
            </div>
            <button @click="handleEncode" class="action-button" :disabled="isLoading">
              {{ isLoading ? '编码中...' : '开始编码' }}
            </button>
          </div>
          <div class="output-section">
            <h2>编码结果</h2>
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
                        <button class="copy-btn primary" @click="copyToClipboard(JSON.stringify(encodeResult.codes))">复制JSON</button>
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
            <h2>输入编码</h2>
            <div class="textarea-wrapper">
              <textarea v-model="textToDecode" placeholder="在此输入要解码的二进制文本..."></textarea>
            </div>
            <div class="codes-input">
              <h3>编码表 (JSON格式)</h3>
              <textarea v-model="codesForDecode" placeholder='{"a": "00", "b": "01", ...}'></textarea>
            </div>
            <button @click="handleDecode" class="action-button" :disabled="isDecoding">
              {{ isDecoding ? '解码中...' : '开始解码' }}
            </button>
          </div>
          <div class="output-section">
            <h2>解码结果</h2>
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
  const storedUser = localStorage.getItem('user');

  if (storedUsername) {
    username.value = storedUsername;
  } else if (storedUser) {
    try {
      const userObj = JSON.parse(storedUser);
      username.value = userObj.username || userObj.name || 'User';
      localStorage.setItem('username', username.value);
    } catch (e) {
      username.value = storedUser;
      localStorage.setItem('username', username.value);
    }
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
    console.log('Viz.js 初始化成功');
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
      console.log('WebSocket 已连接');

      // 订阅公共消息
      stompClient.subscribe('/topic/messages', (message) => {
        const msg = JSON.parse(message.body);
        console.log('收到公共消息:', msg);
        // 避免重复添加自己发送的消息
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

      // 订阅私人消息
      stompClient.subscribe(`/user/${username.value}/queue/private`, (message) => {
        const msg = JSON.parse(message.body);
        console.log('收到私人消息:', msg);
        receivedMessages.value.unshift({
          id: msg.id || Date.now(),
          sender: msg.sender,
          message: msg.message,
          timestamp: msg.timestamp,
          type: 'PRIVATE'
        });
      });

      // 通知服务器用户加入
      stompClient.publish({
        destination: '/app/join',
        body: JSON.stringify({ username: username.value })
      });
    },
    onDisconnect: () => {
      isConnected.value = false;
      console.log('WebSocket 已断开');
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

  // 本地立即添加已发送的消息
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
body, html {
  margin: 0 !important;
  padding: 0 !important;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background-color: #1a1a2e;
  color: #ecf0f1;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}
</style>

<style scoped>
.main-app-container {
  width: 100vw;
  height: 100vh;
  display: flex;
}

.sidebar {
  width: 220px;
  flex-shrink: 0;
  background-color: #16213e;
  color: #ecf0f1;
  display: flex;
  flex-direction: column;
  padding: 24px 16px;
  box-sizing: border-box;
  box-shadow: 4px 0 10px rgba(0,0,0,0.2);
  z-index: 10;
}

.user-profile {
  text-align: center;
  margin-bottom: 32px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: bold;
  margin: 0 auto 12px;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.username {
  font-size: 16px;
  font-weight: 600;
}

.navigation {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 10px;
  color: #a0a0a0;
  text-decoration: none;
  transition: all 0.3s;
  font-size: 14px;
}

.nav-item:hover {
  background-color: rgba(255,255,255,0.05);
  color: white;
}

.nav-item.active {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  font-weight: 500;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}

.logout-button {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 10px;
  background-color: rgba(231, 76, 60, 0.15);
  color: #e74c3c;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid rgba(231, 76, 60, 0.3);
}

.logout-button:hover {
  background-color: #e74c3c;
  color: white;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  box-sizing: border-box;
  min-width: 0;
  background-color: #1a1a2e;
}

.page-content {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  padding: 32px 40px;
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  box-sizing: border-box;
}

.content-header {
  text-align: center;
  margin-bottom: 24px;
  flex-shrink: 0;
}

.content-header h1 {
  font-size: 32px;
  margin: 0 0 8px 0;
  color: #ffffff;
  font-weight: 700;
}

.content-header p {
  font-size: 14px;
  color: #888;
  margin: 0;
}

.tool-tabs {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 24px;
  flex-shrink: 0;
}

.tab-button {
  padding: 12px 32px;
  border: 2px solid #333;
  border-radius: 25px;
  background-color: transparent;
  cursor: pointer;
  font-size: 15px;
  color: #888;
  transition: all 0.3s;
  font-weight: 500;
}

.tab-button:hover:not(:disabled) {
  border-color: #667eea;
  color: #667eea;
}

.tab-button.active {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-color: transparent;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.tab-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.coder-panel, .send-panel, .history-panel {
  flex-grow: 1;
  display: flex;
  gap: 24px;
  min-height: 0;
  width: 100%;
}

.coder-panel, .send-panel {
  flex-direction: row;
}

.history-panel {
  flex-direction: column;
  background-color: #242444;
  border-radius: 16px;
  padding: 24px;
}

.input-section, .output-section, .input-area, .received-area {
  flex: 1;
  background-color: #242444;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.input-section h2, .output-section h2, .input-area h2, .received-area h2 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
  flex-shrink: 0;
}

.textarea-wrapper {
  flex-grow: 1;
  margin-bottom: 16px;
  min-height: 150px;
  display: flex;
}

.input-section textarea,
.codes-input textarea,
.input-area textarea {
  width: 100%;
  height: 100%;
  border: 1px solid #333;
  border-radius: 12px;
  padding: 16px;
  font-size: 14px;
  resize: none;
  outline: none;
  transition: border-color 0.3s;
  background-color: #1a1a2e;
  color: #fff;
  box-sizing: border-box;
  font-family: inherit;
  line-height: 1.6;
}

.input-area textarea {
  flex-grow: 1;
  margin-bottom: 16px;
}

.input-section textarea:focus,
.codes-input textarea:focus,
.input-area textarea:focus {
  border-color: #667eea;
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
  color: #888;
  margin: 0 0 8px 0;
  flex-shrink: 0;
}

.action-button {
  width: 100%;
  padding: 14px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #27ae60, #2ecc71);
  color: white;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 15px rgba(39, 174, 96, 0.3);
  flex-shrink: 0;
}

.action-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(39, 174, 96, 0.4);
}

.action-button:disabled {
  background: #555;
  cursor: not-allowed;
  box-shadow: none;
}

.result-content {
  flex-grow: 1;
  overflow-y: auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.messages-list, .history-list {
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
  color: #666;
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
  color: #888;
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.btn-group {
  display: flex;
  gap: 8px;
}

.copy-btn {
  background-color: #333;
  border: none;
  color: #aaa;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.copy-btn:hover {
  background-color: #555;
  color: white;
}

.copy-btn.primary {
  background-color: #2c3e50;
  color: #667eea;
}

.code-box {
  background-color: #1a1a2e;
  padding: 14px;
  border-radius: 10px;
  border: 1px solid #333;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  color: #4ecca3;
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
  border-bottom: 1px solid #333;
  flex-shrink: 0;
}

.output-tabs button {
  padding: 8px 16px;
  border: none;
  background-color: transparent;
  color: #888;
  cursor: pointer;
  transition: all 0.3s;
  border-bottom: 2px solid transparent;
  font-size: 14px;
}

.output-tabs button.active {
  color: #667eea;
  border-bottom-color: #667eea;
}

.output-content {
  flex-grow: 1;
  min-height: 0;
  overflow-y: auto;
}

.tree-panel {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.tree-toolbar {
  text-align: center;
  padding: 8px;
  color: #666;
  font-size: 12px;
  display: flex;
  justify-content: center;
  gap: 12px;
  align-items: center;
}

.retry-btn, .zoom-btn {
  background: transparent;
  border: 1px solid #444;
  color: #888;
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  transition: all 0.2s;
}

.retry-btn:hover, .zoom-btn:hover {
  color: white;
  border-color: #667eea;
  background-color: rgba(102, 126, 234, 0.1);
}

.tree-container {
  flex-grow: 1;
  background-color: #242444;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.graph-container {
  width: 100%;
  height: 100%;
  overflow: hidden;
  cursor: grab;
  display: flex;
  align-items: center;
  justify-content: center;
}

.graph-container:active {
  cursor: grabbing;
}

.svg-wrapper {
  transition: transform 0.1s ease-out;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tree-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #888;
}

.error-msg {
  color: #e74c3c;
  background: rgba(231, 76, 60, 0.1);
  padding: 20px;
  border-radius: 8px;
  border: 1px solid rgba(231, 76, 60, 0.3);
  max-width: 80%;
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;
  background-color: #242444;
  border-radius: 12px;
  flex-shrink: 0;
  margin-bottom: 24px;
}

.status-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: #e74c3c;
}

.status-dot.connected {
  background-color: #27ae60;
  box-shadow: 0 0 10px rgba(39, 174, 96, 0.5);
}

.connect-btn, .disconnect-btn {
  margin-left: auto;
  padding: 8px 20px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.connect-btn {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.disconnect-btn {
  background-color: rgba(231, 76, 60, 0.2);
  color: #e74c3c;
  border: 1px solid rgba(231, 76, 60, 0.3);
}

.message-section {
  flex-grow: 1;
  display: flex;
  gap: 24px;
  min-height: 0;
}

.send-options {
  margin-bottom: 16px;
  color: #888;
  flex-shrink: 0;
}

.send-options label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.send-options input[type="checkbox"] {
  width: 18px;
  height: 18px;
  accent-color: #667eea;
}

.receiver-input {
  margin-bottom: 12px;
}

.receiver-input label {
  display: block;
  margin-bottom: 6px;
  color: #888;
  font-size: 13px;
}

.receiver-input input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #333;
  border-radius: 8px;
  background-color: #1a1a2e;
  color: #fff;
  font-size: 14px;
  outline: none;
  transition: border-color 0.3s;
  box-sizing: border-box;
}

.receiver-input input:focus {
  border-color: #667eea;
}

.history-btn {
  margin-left: 12px;
  padding: 8px 16px;
  border: 1px solid #667eea;
  border-radius: 8px;
  background: transparent;
  color: #667eea;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.3s;
}

.history-btn:hover {
  background: rgba(102, 126, 234, 0.1);
}

.message-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  border-bottom: 1px solid #333;
  padding-bottom: 12px;
}

.message-tabs button {
  padding: 6px 14px;
  border: none;
  background: transparent;
  color: #888;
  cursor: pointer;
  font-size: 13px;
  border-radius: 4px;
  transition: all 0.3s;
}

.message-tabs button.active {
  background: rgba(102, 126, 234, 0.2);
  color: #667eea;
}

.messages-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-item {
  padding: 12px 16px;
  border-radius: 12px;
  margin-bottom: 10px;
  max-width: 85%;
}

.message-item.sent {
  background: linear-gradient(135deg, #667eea, #764ba2);
  margin-left: auto;
  color: white;
}

.message-item.received {
  background-color: #2a2a4a;
  margin-right: auto;
}

.message-item.system {
  background-color: rgba(255, 193, 7, 0.1);
  border: 1px solid rgba(255, 193, 7, 0.3);
  text-align: center;
  max-width: 100%;
  color: #ffc107;
  font-size: 12px;
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 12px;
}

.message-sender {
  font-weight: 600;
}

.message-item.sent .message-sender,
.message-item.sent .message-time {
  color: rgba(255, 255, 255, 0.8);
}

.message-item.received .message-sender {
  color: #667eea;
}

.message-item.received .message-time {
  color: #666;
}

.message-content {
  word-break: break-word;
  line-height: 1.5;
}

.message-item.received .message-content {
  color: #e0e0e0;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.history-item {
  background-color: #1a1a2e;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid #333;
}

.history-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.history-type {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
}

.history-time {
  color: #666;
  font-size: 12px;
}

.history-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.history-original, .history-encoded {
  color: #888;
  font-size: 13px;
  word-break: break-all;
}

.history-original strong, .history-encoded strong {
  color: #c0c0c0;
}

.clear-btn {
  align-self: flex-end;
  padding: 10px 24px;
  border: 1px solid rgba(231, 76, 60, 0.3);
  border-radius: 8px;
  background-color: rgba(231, 76, 60, 0.1);
  color: #e74c3c;
  cursor: pointer;
  transition: all 0.3s;
  flex-shrink: 0;
}

.clear-btn:hover {
  background-color: #e74c3c;
  color: white;
}

.loading-spinner {
  margin: 40px auto;
  border: 3px solid #333;
  border-top: 3px solid #667eea;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.svg-wrapper :deep(svg) {
  max-width: 100%;
  max-height: 100%;
}

.svg-wrapper :deep(.node text) {
  fill: #e0e0e0 !important;
}

.svg-wrapper :deep(.edge text) {
  fill: #aaa !important;
}

.svg-wrapper :deep(.node polygon),
.svg-wrapper :deep(.node ellipse) {
  fill: #2a2a4a !important;
  stroke: #667eea !important;
}

.svg-wrapper :deep(.edge path) {
  stroke: #888 !important;
}

.svg-wrapper :deep(.graph > polygon) {
  fill: transparent !important;
  stroke: none !important;
}
</style>
