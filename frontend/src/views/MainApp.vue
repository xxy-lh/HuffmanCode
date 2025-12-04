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
          <span class="nav-icon">📝</span>
          <span>编码/解码</span>
        </a>
        <a href="#"
           :class="['nav-item', { active: currentPage === 'send' }]"
           @click.prevent="switchToSendMessage">
          <span class="nav-icon">💬</span>
          <span>消息通信</span>
        </a>
        <a href="#"
           :class="['nav-item', { active: currentPage === 'history' }]"
           @click.prevent="currentPage = 'history'">
          <span class="nav-icon">📋</span>
          <span>历史记录</span>
        </a>
      </nav>
    </aside>

    <!-- 右侧主内容区 -->
    <div class="main-wrapper">
      <!-- 顶部信息栏 -->
      <header class="top-bar">
        <h2 class="page-title">{{ pageTitle }}</h2>
        <div class="user-info">
          <div class="avatar">{{ username.charAt(0).toUpperCase() }}</div>
          <span class="username">{{ username }}</span>
          <button class="logout-button" @click="logout" title="退出登录">🚪</button>
        </div>
      </header>

      <!-- 内容面板 -->
      <main class="main-content">
        <!-- 发送页面 -->
        <div v-if="currentPage === 'send'" class="content-card chat-layout">
          <div class="chat-toolbar">
            <div class="connection-status">
              <span class="status-dot" :class="{ connected: isConnected }"></span>
              <span class="status-text">{{ isConnected ? '已连接' : '未连接' }}</span>
              <button v-if="!isConnected" class="connect-btn small" @click="connectWebSocket">连接</button>
              <button v-else class="disconnect-btn small" @click="disconnectWebSocket">断开</button>
            </div>
            <div class="receiver-input-wrapper">
              <label>接收者:</label>
              <input type="text" v-model="messageReceiver" placeholder="留空则群发" list="user-list" />
              <datalist id="user-list">
                <option v-for="user in userList" :key="user.id" :value="user.username"></option>
              </datalist>
            </div>
            <button class="history-btn small" @click="loadMessageHistory">加载历史</button>
          </div>

          <div class="messages-container" ref="messagesContainerRef">
            <div v-if="sortedMessages.length === 0" class="empty-chat">暂无消息，快来发一条吧！</div>
            <div v-for="(msg, index) in sortedMessages" :key="index" :class="['message-row', getMsgClass(msg)]">
              <template v-if="getMsgClass(msg) === 'system'">
                <div class="system-msg-bubble">{{ msg.message }}</div>
              </template>
              <template v-else>
                <div class="msg-avatar">{{ msg.sender ? msg.sender.charAt(0).toUpperCase() : '?' }}</div>
                <div class="msg-bubble-wrapper">
                  <div class="msg-info">
                    <span class="msg-sender">{{ msg.sender }}</span>
                    <span class="msg-time">{{ formatTime(msg.timestamp) }}</span>
                  </div>
                  <div class="msg-content">{{ msg.message }}</div>
                </div>
              </template>
            </div>
          </div>

          <div class="chat-input-area">
            <textarea v-model="messageToSend" placeholder="输入消息内容..." @keyup.enter.ctrl="sendMessage"></textarea>
            <div class="send-options">
              <input type="checkbox" id="encodeCheck" v-model="encodeBeforeSend" />
              <label for="encodeCheck">发送前编码</label>
            </div>
            <button class="send-btn" @click="sendMessage" :disabled="!isConnected || !messageToSend.trim()">发送</button>
          </div>
        </div>

        <!-- 历史页面 -->
        <div v-else-if="currentPage === 'history'" class="content-card">
          <div class="history-panel-header">
            <h3 class="section-title" style="border: none; margin: 0; padding: 0;">操作历史</h3>
            <button class="clear-btn" @click="clearHistory">清空历史</button>
          </div>
          <div class="history-panel">
            <div v-if="historyList.length === 0" class="placeholder">
              <span class="placeholder-icon">📭</span>
              <p>暂无历史记录</p>
            </div>
            <div v-else class="history-list">
              <div v-for="(item, index) in historyList" :key="index" class="history-item">
                <div class="history-header">
                  <span class="history-type">{{ item.type }}</span>
                  <span class="history-time">{{ item.time }}</span>
                </div>
                <div class="history-content">
                  <p class="history-original"><strong>原文:</strong> {{ item.original }}</p>
                  <p class="history-encoded"><strong>结果:</strong> {{ item.encoded }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 哈夫曼编码译码页面 -->
        <div v-else class="content-card">
          <div class="tool-tabs">
            <button :class="['tab-button', { active: activeTab === 'encode' }]" @click="activeTab = 'encode'">编码</button>
            <button :class="['tab-button', { active: activeTab === 'decode' }]" @click="activeTab = 'decode'">解码</button>
            <button :class="['tab-button', { active: activeTab === 'tree' }]" @click="activeTab = 'tree'" :disabled="!encodeResult">哈夫曼树</button>
          </div>

          <!-- 编码面板 -->
          <div v-if="activeTab === 'encode'" class="coder-panel">
            <div class="input-section">
              <h3 class="section-title">输入文本</h3>
              <div class="textarea-wrapper">
                <textarea v-model="textToEncode" placeholder="请输入要编码的文本..."></textarea>
              </div>
              <button class="action-button primary" @click="handleEncode" :disabled="isLoading">
                {{ isLoading ? '编码中...' : '执行哈夫曼编码' }}
              </button>
            </div>
            <div class="output-section">
              <h3 class="section-title">编码结果</h3>
              <div v-if="!encodeResult" class="placeholder">
                <span class="placeholder-icon">📊</span>
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
                      <h3>编码表</h3>
                      <button class="copy-btn" @click="copyToClipboard(JSON.stringify(encodeResult.codes))">复制JSON</button>
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
                      <h3>字符频率</h3>
                    </div>
                    <pre class="code-box">{{ formatFrequencies(encodeResult.frequencies) }}</pre>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 解码面板 -->
          <div v-if="activeTab === 'decode'" class="coder-panel">
            <div class="input-section">
              <h3 class="section-title">输入编码</h3>
              <div class="textarea-wrapper">
                <textarea v-model="textToDecode" placeholder="请输入要解码的二进制文本..."></textarea>
              </div>
              <div class="codes-input">
                <h3>编码表 (JSON格式)</h3>
                <textarea v-model="codesForDecode" placeholder='例如: {"a":"00","b":"01","c":"1"}'></textarea>
              </div>
              <button class="action-button primary" @click="handleDecode" :disabled="isDecoding">
                {{ isDecoding ? '解码中...' : '执行哈夫曼解码' }}
              </button>
            </div>
            <div class="output-section">
              <h3 class="section-title">解码结果</h3>
              <div v-if="!decodeResult" class="placeholder">
                <span class="placeholder-icon">📄</span>
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

          <!-- 哈夫曼树面板 -->
          <div v-if="activeTab === 'tree'" class="tree-panel">
            <div class="tree-toolbar">
              <button class="retry-btn" @click="retryRender">重新渲染</button>
              <button class="zoom-btn" @click="zoomIn">放大</button>
              <button class="zoom-btn" @click="zoomOut">缩小</button>
              <button class="zoom-btn" @click="resetZoom">重置</button>
            </div>
            <div class="tree-container" ref="graphContainer" @wheel.prevent="handleWheel">
              <div v-if="isTreeLoading" class="tree-loading">
                <div class="loading-spinner"></div>
                <p>正在渲染哈夫曼树...</p>
              </div>
              <div v-else-if="renderError" class="error-msg">{{ renderError }}</div>
              <div v-else class="graph-container"
                   @mousedown="startDrag"
                   @mousemove="onDrag"
                   @mouseup="endDrag"
                   @mouseleave="endDrag">
                <div class="svg-wrapper" ref="svgWrapper" :style="transformStyle" v-html="svgContent"></div>
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
const userList = ref([]);
const messagesContainerRef = ref(null);
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

const pageTitle = computed(() => {
  switch (currentPage.value) {
    case 'huffman': return '哈夫曼编码/解码器';
    case 'send': return 'Socket 消息通信';
    case 'history': return '历史记录';
    default: return '哈夫曼工具箱';
  }
});

const transformStyle = computed(() => ({
  transform: `translate(${translateX.value}px, ${translateY.value}px) scale(${scale.value})`,
  transformOrigin: 'center center'
}));

const sortedMessages = computed(() => {
  return [...receivedMessages.value].sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));
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
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  } catch {
    return timestamp;
  }
};

const scrollToBottom = () => {
  nextTick(() => {
    const container = messagesContainerRef.value;
    if (container) {
      container.scrollTop = container.scrollHeight;
    }
  });
};

watch(receivedMessages, () => {
  scrollToBottom();
}, { deep: true });

// --- 生命周期钩子 ---
onMounted(async () => {
  const storedUsername = localStorage.getItem('username');
  if (storedUsername) {
    username.value = storedUsername;
  } else {
    router.push('/login');
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

const fetchUsers = async () => {
  try {
    const response = await axios.get('/api/users');
    userList.value = response.data;
  } catch (error) {
    console.error('获取用户列表失败:', error);
  }
};

const switchToSendMessage = () => {
  currentPage.value = 'send';
  fetchUsers();
};

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
      renderError.value = 'Viz.js 初始化失败';
      return;
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

const zoomIn = () => { scale.value = Math.min(3, scale.value + 0.2); };
const zoomOut = () => { scale.value = Math.max(0.1, scale.value - 0.2); };
const resetZoom = () => { scale.value = 1; translateX.value = 0; translateY.value = 0; };

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

const endDrag = () => { isDragging.value = false; };

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
        receivedMessages.value.push(msg);
      });
      stompClient.subscribe(`/topic/private.${username.value}`, (message) => {
        const msg = JSON.parse(message.body);
        receivedMessages.value.push(msg);
      });
      stompClient.publish({
        destination: '/app/join',
        body: JSON.stringify({ username: username.value })
      });
    },
    onDisconnect: () => { isConnected.value = false; },
    onStompError: (frame) => { console.error('STOMP 错误:', frame); isConnected.value = false; }
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
      const response = await axios.post('/api/huffman/process', { text: messageToSend.value });
      messageContent = response.data.encodedText;
      addToHistory('发送(编码)', originalMessage, messageContent);
    } catch (error) {
      console.error('编码失败:', error);
      alert('编码失败，消息将以原文发送');
    }
  }

  const messagePayload = {
    message: messageContent,
    originalMessage: originalMessage,
    sender: username.value,
    receiver: messageReceiver.value || null,
    encoded: encodeBeforeSend.value
  };

  stompClient.publish({
    destination: '/app/send',
    body: JSON.stringify(messagePayload)
  });

  if (messageReceiver.value) {
    receivedMessages.value.push({
      sender: username.value,
      message: messageContent,
      originalMessage: originalMessage,
      timestamp: new Date().toISOString(),
      type: 'PRIVATE',
      encoded: encodeBeforeSend.value
    });
  }

  messageToSend.value = '';
};


const loadMessageHistory = async () => {
  try {
    const response = await axios.get(`/api/messages/history/${username.value}`);
    const existingIds = new Set(receivedMessages.value.map(m => m.id));
    const newMessages = response.data.filter(m => !existingIds.has(m.id));
    receivedMessages.value = [...newMessages, ...receivedMessages.value];
    alert(`成功加载 ${newMessages.length} 条历史消息。`);
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
  return Object.entries(frequencies).map(([char, freq]) => `'${char}': ${freq}`).join(',\n');
};

const formatCodes = (codes) => {
  if (!codes) return '';
  return Object.entries(codes).map(([char, code]) => `'${char}': "${code}"`).join(',\n');
};

const logout = () => {
  localStorage.removeItem('username');
  //localStorage.removeItem('user');
  if (stompClient) {
    stompClient.deactivate();
  }
  router.push('/login');
};
</script>

<style>
/* 全局样式重置 */
*, *::before, *::after {
  box-sizing: border-box;
}

body, html {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background-color: #f4f7f6;
  color: #333;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

#app {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
}
</style>

<style scoped>
/* 主布局 */
.app-layout {
  display: flex;
  width: 100vw;
  height: 100vh;
  margin: 0;
  padding: 0;
  position: absolute;
  top: 0;
  left: 0;
}

/* 侧边栏 */
.sidebar { width: 220px; flex-shrink: 0; background-color: #2c3e50; color: #ecf0f1; display: flex; flex-direction: column; box-shadow: 2px 0 5px rgba(0,0,0,0.1); z-index: 100; }
.sidebar-header { padding: 20px; display: flex; align-items: center; gap: 12px; border-bottom: 1px solid rgba(255, 255, 255, 0.1); }
.logo-icon { font-size: 24px; }
.logo-text { font-size: 18px; font-weight: 600; margin: 0; color: #fff; }
.navigation { flex-grow: 1; padding: 10px 0; }
.nav-item { display: flex; align-items: center; gap: 12px; padding: 14px 20px; margin: 4px 10px; border-radius: 6px; color: #bdc3c7; text-decoration: none; transition: all 0.3s; font-size: 15px; border-left: 3px solid transparent; }
.nav-icon { font-size: 18px; }
.nav-item:hover { background-color: #34495e; color: #fff; }
.nav-item.active { background-color: #1abc9c; color: #fff; font-weight: 500; }

/* 右侧主内容区 */
.main-wrapper { flex-grow: 1; display: flex; flex-direction: column; overflow: hidden; }
.top-bar { display: flex; justify-content: space-between; align-items: center; padding: 0 24px; height: 60px; background-color: #fff; border-bottom: 1px solid #e0e0e0; flex-shrink: 0; }
.page-title { font-size: 20px; font-weight: 600; color: #2c3e50; margin: 0; }
.user-info { display: flex; align-items: center; gap: 12px; }
.avatar { width: 36px; height: 36px; border-radius: 50%; background-color: #1abc9c; color: white; display: flex; align-items: center; justify-content: center; font-size: 16px; font-weight: bold; }
.username { font-size: 15px; font-weight: 500; color: #555; }
.logout-button { background: none; border: none; cursor: pointer; color: #7f8c8d; padding: 6px; border-radius: 50%; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
.logout-button:hover { color: #e74c3c; background-color: #fbeeee; }

/* 内容面板 */
.main-content { flex-grow: 1; padding: 24px; overflow-y: auto; background-color: #f4f7f6; }
.content-card { background-color: #fff; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.05); padding: 24px; height: 100%; display: flex; flex-direction: column; box-sizing: border-box; overflow: hidden; }

/* --- 消息通信页样式 --- */
.chat-layout { padding: 0; overflow: hidden; }
.chat-toolbar { display: flex; align-items: center; gap: 16px; padding: 12px 20px; border-bottom: 1px solid #eee; flex-shrink: 0; }
.connection-status { display: flex; align-items: center; gap: 8px; font-size: 14px; }
.status-dot { width: 10px; height: 10px; border-radius: 50%; background-color: #e74c3c; transition: background-color 0.3s; }
.status-dot.connected { background-color: #2ecc71; }
.status-text { color: #333; font-weight: 600; margin-right: 4px; }

.connect-btn.small, .disconnect-btn.small, .history-btn.small { padding: 4px 12px; font-size: 13px; margin-left: 4px; }
.connect-btn { background-color: #1abc9c; color: white; border: 1px solid #1abc9c; border-radius: 4px; cursor: pointer; }
.disconnect-btn { background-color: transparent; color: #e74c3c; border: 1px solid #e74c3c; border-radius: 4px; cursor: pointer; }
.history-btn { background-color: transparent; color: #3498db; border: 1px solid #3498db; border-radius: 4px; cursor: pointer; margin-left: auto; }

.receiver-input-wrapper { display: flex; align-items: center; gap: 8px; font-size: 14px; }
.receiver-input-wrapper label { color: #555; }
.receiver-input-wrapper input { border: 1px solid #ddd; border-radius: 4px; padding: 6px 10px; font-size: 14px; outline: none; width: 180px; }
.receiver-input-wrapper input:focus { border-color: #1abc9c; }

.messages-container {
  flex-grow: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  background-color: #f8f9fa;
}

.empty-chat { text-align: center; color: #bbb; margin-top: 40px; font-size: 14px; }

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  max-width: 85%;
}

.message-row.received { align-self: flex-start; }
.message-row.received .msg-content { background-color: #fff; color: #333; border: 1px solid #e6e6e6; border-top-left-radius: 2px; }
.message-row.received .msg-avatar { background-color: #95a5a6; }

.message-row.sent { align-self: flex-end; flex-direction: row-reverse; text-align: right; }
.message-row.sent .msg-info { justify-content: flex-end; }
.message-row.sent .msg-content { background-color: #1abc9c; color: white; border-top-right-radius: 2px; text-align: left; box-shadow: 0 2px 5px rgba(26, 188, 156, 0.2); }
.message-row.sent .msg-avatar { background-color: #16a085; }

.message-row.system { align-self: center; max-width: 100%; }
.system-msg-bubble { background-color: rgba(0,0,0,0.05); color: #aaa; padding: 4px 12px; border-radius: 20px; font-size: 12px; text-align: center; }

.msg-avatar { width: 36px; height: 36px; border-radius: 50%; color: white; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 14px; flex-shrink: 0; }
.msg-bubble-wrapper { display: flex; flex-direction: column; max-width: 100%; }
.msg-info { display: flex; gap: 8px; margin-bottom: 4px; font-size: 11px; color: #888; }
.msg-content { padding: 10px 14px; border-radius: 12px; line-height: 1.5; word-break: break-all; font-size: 14px; box-shadow: 0 1px 2px rgba(0,0,0,0.05); }

.chat-input-area { border-top: 1px solid #eee; padding: 16px 20px; display: flex; flex-direction: column; gap: 10px; flex-shrink: 0; background-color: #fff; }
.chat-input-area textarea { width: 100%; height: 80px; border: 1px solid #ddd; border-radius: 6px; padding: 10px; font-size: 14px; resize: none; outline: none; transition: border-color 0.3s; box-sizing: border-box; }
.chat-input-area textarea:focus { border-color: #1abc9c; }
.send-options { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #555; }
.send-options input[type="checkbox"] { accent-color: #1abc9c; }
.send-btn { align-self: flex-end; padding: 8px 24px; border: none; border-radius: 6px; background-color: #1abc9c; color: white; font-size: 15px; cursor: pointer; transition: opacity 0.3s; }
.send-btn:disabled { background-color: #bdc3c7; cursor: not-allowed; }

/* --- 编码/解码页样式 --- */
.tool-tabs { display: flex; gap: 12px; margin-bottom: 24px; border-bottom: 1px solid #eee; flex-shrink: 0; }
.tab-button { padding: 10px 20px; border: none; border-bottom: 3px solid transparent; background-color: transparent; cursor: pointer; font-size: 15px; color: #7f8c8d; transition: all 0.3s; font-weight: 500; }
.tab-button:hover:not(:disabled) { color: #1abc9c; }
.tab-button.active { color: #1abc9c; border-bottom-color: #1abc9c; }
.tab-button:disabled { opacity: 0.5; cursor: not-allowed; }

.coder-panel {
  flex-grow: 1;
  display: flex;
  gap: 24px;
  min-height: 0;
  overflow: hidden;
}

.input-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.output-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.section-title { margin: 0 0 16px 0; font-size: 16px; font-weight: 600; color: #2c3e50; padding-bottom: 12px; border-bottom: 1px solid #eee; flex-shrink: 0; }

.textarea-wrapper {
  flex: 1;
  margin-bottom: 16px;
  min-height: 100px;
  max-height: 200px;
  display: flex;
}

textarea { width: 100%; height: 100%; border: 1px solid #ddd; border-radius: 6px; padding: 12px; font-size: 14px; resize: none; outline: none; transition: border-color 0.3s; background-color: #fdfdfd; color: #333; box-sizing: border-box; font-family: inherit; line-height: 1.6; }
textarea:focus { border-color: #1abc9c; }

.codes-input {
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  margin-bottom: 16px;
  min-height: 80px;
  max-height: 120px;
}

.codes-input h3 { font-size: 14px; color: #7f8c8d; margin: 0 0 8px 0; flex-shrink: 0; }
.codes-input textarea { flex: 1; min-height: 60px; }

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
  margin-top: auto;
}

.action-button.primary { background-color: #1abc9c; }
.action-button:hover:not(:disabled) { opacity: 0.9; }
.action-button:disabled { background-color: #bdc3c7; cursor: not-allowed; }

.placeholder { height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #95a5a6; text-align: center; }
.placeholder-icon { font-size: 48px; margin-bottom: 16px; opacity: 0.5; }

.result-display { display: flex; flex-direction: column; gap: 16px; flex-grow: 1; overflow-y: auto; }
.result-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.result-item h3 { font-size: 12px; color: #7f8c8d; margin: 0; text-transform: uppercase; letter-spacing: 1px; }
.copy-btn { background-color: #ecf0f1; border: 1px solid #ddd; color: #7f8c8d; padding: 4px 10px; border-radius: 4px; font-size: 12px; cursor: pointer; transition: all 0.2s; }
.copy-btn:hover { background-color: #bdc3c7; color: white; }
.code-box { background-color: #f8f9fa; padding: 14px; border-radius: 6px; border: 1px solid #eee; font-family: 'Consolas', 'Monaco', monospace; font-size: 13px; color: #2c3e50; max-height: 180px; overflow-y: auto; word-break: break-all; white-space: pre-wrap; line-height: 1.6; }

.output-tabs { display: flex; gap: 8px; margin-bottom: 16px; border-bottom: 1px solid #eee; flex-shrink: 0; }
.output-tabs button { padding: 8px 16px; border: none; background-color: transparent; color: #7f8c8d; cursor: pointer; transition: all 0.3s; border-bottom: 2px solid transparent; font-size: 14px; }
.output-tabs button.active { color: #1abc9c; border-bottom-color: #1abc9c; }
.output-content { flex-grow: 1; min-height: 0; overflow-y: auto; }

/* 树 */
.tree-panel { flex-grow: 1; display: flex; flex-direction: column; height: 100%; overflow: hidden; }
.tree-toolbar { text-align: center; padding: 8px; color: #7f8c8d; font-size: 12px; display: flex; justify-content: center; gap: 12px; align-items: center; flex-shrink: 0; }
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

/* 历史记录 */
.history-panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding-bottom: 16px; border-bottom: 1px solid #eee; flex-shrink: 0; }
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
