<template>
  <!-- 最外层容器：强制占满全屏 -->
  <div class="main-app-container">
    <!-- 左侧导航栏 (固定宽度) -->
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

    <!-- 右侧主内容区 -->
    <main class="main-content">
      <!-- 发送页面 -->
      <div v-if="currentPage === 'send'" class="page-content">
        <header class="content-header">
          <h1>Socket 消息发送</h1>
          <p>通过 WebSocket 发送哈夫曼编码数据</p>
        </header>
        <div class="send-panel">
          <div class="connection-status">
            <div :class="['status-dot', { connected: isConnected }]"></div>
            <span>{{ isConnected ? '已连接' : '未连接' }}</span>
            <button v-if="! isConnected" @click="connectWebSocket" class="connect-btn">连接</button>
            <button v-else @click="disconnectWebSocket" class="disconnect-btn">断开</button>
          </div>
          <div class="message-section">
            <div class="input-area">
              <h2>发送消息</h2>
              <textarea v-model="messageToSend" placeholder="在此输入要发送的消息..."></textarea>
              <div class="send-options">
                <label>
                  <input type="checkbox" v-model="encodeBeforeSend">
                  发送前进行哈夫曼编码
                </label>
              </div>
              <button @click="sendMessage" :disabled="!isConnected || ! messageToSend.trim()" class="action-button">发送</button>
            </div>
            <div class="received-area">
              <h2>接收到的消息</h2>
              <div v-if="receivedMessages.length === 0" class="placeholder">
                <span class="placeholder-icon">📥</span>
                <span>等待接收消息...</span>
              </div>
              <div v-else class="messages-list">
                <div v-for="(msg, index) in receivedMessages" :key="index" class="message-item">
                  <div class="message-time">{{ msg.time }} (来自: {{ msg.sender }})</div>
                  <div class="message-content">{{ msg.content }}</div>
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
            <span class="placeholder-icon">📂</span>
            <span>暂无历史记录</span>
          </div>
          <div v-else class="history-list">
            <div v-for="(item, index) in historyList" :key="index" class="history-item">
              <div class="history-header">
                <span class="history-type">{{ item.type }}</span>
                <span class="history-time">{{ item.time }}</span>
              </div>
              <div class="history-content">
                <div class="history-original"><strong>原文:</strong> {{ item.original }}</div>
                <div class="history-encoded"><strong>处理后:</strong> {{ item.encoded }}</div>
              </div>
            </div>
          </div>
          <button v-if="historyList.length > 0" @click="clearHistory" class="clear-btn">
            清空历史记录
          </button>
        </div>
      </div>

      <!-- 哈夫曼编码译码页面 (默认) -->
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
            <h2>输入</h2>
            <div class="textarea-wrapper">
              <textarea v-model="textToEncode" placeholder="在此输入要编码的文本..."></textarea>
            </div>
            <button @click="handleEncode" :disabled="isLoading" class="action-button">
              {{ isLoading ?  '编码中...' : '执行编码' }}
            </button>
          </div>
          <div class="output-section">
            <h2>输出</h2>
            <div v-if="isLoading" class="loading-spinner"></div>
            <div v-else-if="! encodeResult" class="placeholder">
              <span class="placeholder-icon">📊</span>
              <span>等待编码结果...</span>
            </div>
            <div v-else class="result-content">
              <!-- 输出区导航栏 -->
              <div class="output-tabs">
                <button @click="outputTab = 'codes'" :class="{ active: outputTab === 'codes' }">哈夫曼编码</button>
                <button @click="outputTab = 'freq'" :class="{ active: outputTab === 'freq' }">字符频率</button>
              </div>
              <!-- 输出区内容 -->
              <div class="output-content">
                <div v-if="outputTab === 'codes'" class="result-display">
                  <div class="result-item">
                    <div class="result-header">
                      <h3>编码后的文本</h3>
                      <div class="btn-group">
                        <button @click="copyToClipboard(encodeResult.encodedText)" class="copy-btn primary">复制</button>
                      </div>
                    </div>
                    <pre class="code-box">{{ encodeResult.encodedText }}</pre>
                  </div>
                  <div class="result-item">
                    <div class="result-header">
                      <h3>哈夫曼编码表 (JSON)</h3>
                      <div class="btn-group">
                        <button @click="copyToClipboard(JSON.stringify(encodeResult.codes, null, 2))" class="copy-btn">复制</button>
                      </div>
                    </div>
                    <pre class="code-box">{{ formatCodes(encodeResult.codes) }}</pre>
                  </div>
                </div>
                <div v-if="outputTab === 'freq'" class="result-display">
                  <div class="result-item">
                    <div class="result-header">
                      <h3>字符频率</h3>
                      <div class="btn-group">
                        <button @click="copyToClipboard(JSON.stringify(encodeResult.frequencies, null, 2))" class="copy-btn">复制</button>
                      </div>
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
            <h2>输入</h2>
            <div class="textarea-wrapper">
              <textarea v-model="textToDecode" placeholder="在此输入要解码的二进制字符串..."></textarea>
            </div>
            <!-- 修复：将 textarea 和 button 分开，并让 textarea 自动填充空间 -->
            <div class="codes-input">
              <h3>哈夫曼编码表 (JSON格式)</h3>
              <textarea v-model="codesForDecode" placeholder='例如：&#10;{&#10;  "a": "01",&#10;  "b": "11",&#10;  "c": "001"&#10;}'></textarea>
            </div>
            <button @click="handleDecode" :disabled="isDecoding" class="action-button">
              {{ isDecoding ? '解码中...' : '执行解码' }}
            </button>
          </div>
          <div class="output-section">
            <h2>输出</h2>
            <div v-if="isDecoding" class="loading-spinner"></div>
            <div v-else-if="!decodeResult" class="placeholder">
              <span class="placeholder-icon">📜</span>
              <span>等待解码结果...</span>
            </div>
            <div v-else class="result-content">
              <div class="result-display">
                <div class="result-item">
                  <div class="result-header">
                    <h3>解码后的文本</h3>
                    <div class="btn-group">
                      <button @click="copyToClipboard(decodeResult)" class="copy-btn primary">复制</button>
                    </div>
                  </div>
                  <pre class="code-box">{{ decodeResult }}</pre>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 树可视化页面 -->
        <div v-if="activeTab === 'tree'" class="tree-panel">
          <div class="tree-toolbar">
            <span>使用鼠标滚轮缩放，拖动平移</span>
            <div v-if="renderError">
              <button @click="retryRender" class="retry-btn">重试</button>
            </div>
          </div>
          <div class="tree-container">
            <!-- 加载状态 -->
            <div v-if="isTreeLoading" class="tree-loading">
              <div class="loading-spinner"></div>
              <p>正在初始化图形引擎...</p>
            </div>
            <!-- 错误状态 -->
            <div v-else-if="renderError" class="error-msg">
              <p><strong>图形渲染遇到问题:</strong></p>
              <p>{{ renderError }}</p>
            </div>
            <!-- 图形容器 -->
            <div ref="graphContainer" class="graph-container"></div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue';
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
const encodeBeforeSend = ref(false);
const receivedMessages = ref([]);
let stompClient = null;

// --- 历史记录 ---
const historyList = ref([]);

// --- 树可视化 ---
const graphContainer = ref(null);
const renderError = ref('');
const isTreeLoading = ref(false);

// Graphviz 实例缓存
let graphvizModule = null;
let d3Module = null;

// --- 生命周期钩子 ---
onMounted(() => {
  const storedUsername = localStorage.getItem('username');
  if (storedUsername) {
    username.value = storedUsername;
  }

  const savedHistory = localStorage.getItem('huffmanHistory');
  if (savedHistory) {
    historyList.value = JSON.parse(savedHistory);
  }
});

onUnmounted(() => {
  if (stompClient) {
    stompClient.deactivate();
  }
});

// 监听标签页切换
watch([activeTab, encodeResult], async ([newTab, newResult]) => {
  if (newTab === 'tree' && newResult && newResult.treeDot) {
    renderError.value = '';
    await nextTick();
    renderTree(newResult.treeDot);
  }
});

const retryRender = () => {
  if (encodeResult.value && encodeResult.value.treeDot) {
    renderTree(encodeResult.value.treeDot);
  } else {
    alert("没有可渲染的数据，请先进行编码");
  }
}

// --- 复制到剪贴板方法 ---
const copyToClipboard = async (text) => {
  try {
    await navigator.clipboard.writeText(text);
    alert('已复制到剪贴板');
  } catch (err) {
    console.error('复制失败:', err);
    alert('复制失败，您的浏览器可能不支持或未授权。');
  }
};

// --- 编码方法 ---
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
      text: textToEncode.value,
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

// --- 解码方法 ---
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
      codes: codes,
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
      stompClient.subscribe('/topic/messages', (message) => {
        const msg = JSON.parse(message.body);
        receivedMessages.value.unshift({
          content: msg.message,
          sender: msg.sender,
          time: new Date().toLocaleTimeString()
        });
      });
    },
    onDisconnect: () => {
      isConnected.value = false;
      console.log('WebSocket 已断开');
    },
    onStompError: (frame) => {
      console.error('STOMP 错误:', frame);
    }
  });
  stompClient.activate();
};

const disconnectWebSocket = () => {
  if (stompClient) {
    stompClient.deactivate();
  }
};

const sendMessage = async () => {
  if (! messageToSend.value.trim() || !isConnected.value) return;

  let messageContent = messageToSend.value;
  let originalMessage = messageToSend.value;

  if (encodeBeforeSend.value) {
    try {
      const response = await axios.post('/api/huffman/process', { text: originalMessage });
      messageContent = `[哈夫曼编码]: ${response.data.encodedText}`;
      addToHistory('发送 (编码)', originalMessage, response.data.encodedText);
    } catch (error) {
      alert('发送前编码失败，将以原文发送。');
      addToHistory('发送', originalMessage, originalMessage);
    }
  } else {
    addToHistory('发送', originalMessage, originalMessage);
  }

  stompClient.publish({
    destination: '/app/send',
    body: JSON.stringify({
      message: messageContent,
      sender: username.value
    })
  });

  messageToSend.value = '';
};

// --- 历史记录方法 ---
const addToHistory = (type, original, processed) => {
  const item = {
    type,
    original: original.substring(0, 100) + (original.length > 100 ? '...' : ''),
    encoded: processed.substring(0, 100) + (processed.length > 100 ?  '...' : ''),
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

// --- 格式化方法 ---
const formatFrequencies = (frequencies) => {
  if (!frequencies) return '';
  return Object.entries(frequencies)
    .map(([char, freq]) => `'${char}': ${freq}`)
    .join(',\n');
};

const formatCodes = (codes) => {
  if (! codes) return '';
  return Object.entries(codes)
    .map(([char, code]) => `'${char}': "${code}"`)
    .join(',\n');
};

// --- 树可视化方法 (修复版 - 使用动态导入和正确初始化) ---
const renderTree = async (dotString) => {
  if (!graphContainer.value) {
    console.warn("渲染容器未找到");
    return;
  }

  renderError.value = '';
  isTreeLoading.value = true;

  try {
    // 动态导入模块（只在需要时加载）
    if (! d3Module) {
      d3Module = await import('d3');
    }
    if (!graphvizModule) {
      const graphvizLib = await import('d3-graphviz');
      graphvizModule = graphvizLib.graphviz;
    }

    await nextTick();

    // 清空容器
    d3Module.select(graphContainer.value).selectAll('*').remove();

    // 创建 graphviz 实例
    const graph = graphvizModule(graphContainer.value, {
      useWorker: false,  // 禁用 Worker，避免 WASM 加载问题
      zoom: true         // 启用缩放
    });

    // 设置渲染完成和错误的回调
    graph
      .width(graphContainer.value.clientWidth || 800)
      .height(graphContainer.value.clientHeight || 500)
      .fit(true)
      .scale(0.8)
      .on('initEnd', () => {
        console.log('Graphviz 初始化完成');
        isTreeLoading.value = false;
      })
      .on('renderEnd', () => {
        console.log('渲染完成');
        isTreeLoading.value = false;
      })
      .on('error', (err) => {
        console.error('Graphviz 渲染错误:', err);
        renderError.value = String(err);
        isTreeLoading.value = false;
      })
      .renderDot(dotString);

  } catch (error) {
    console.error('启动渲染失败:', error);
    renderError.value = '启动渲染失败: ' + (error.message || String(error));
    isTreeLoading.value = false;
  }
};

// --- 登出方法 ---
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
/* 强制全局重置 */
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
/* 1.主容器布局 */
.main-app-container {
  width: 100vw;
  height: 100vh;
  display: flex;
}

/* 2. 左侧导航栏 */
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

/* 3.右侧主内容区 */
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

/* 头部样式 */
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

/* 标签页样式 */
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

.tab-button:hover {
  border-color: #667eea;
  color: #667eea;
}

.tab-button.active {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-color: transparent;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.tab-button.tree-tab {
  padding: 12px 24px;
}

.tab-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 编码器/发送/历史 面板布局 */
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

/* 修复：解码页面的输入布局 */
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

.codes-input textarea {
  min-height: 80px;
  flex-grow: 1;
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

/* 结果区样式 */
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
.copy-btn.primary:hover {
  background-color: #34495e;
}

.code-box {
  background-color: #1a1a2e;
  padding: 14px;
  border-radius: 10px;
  border: 1px solid #333;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  color: #4ecca3;
  display: block;
  flex: none;
  max-height: 180px;
  overflow-y: auto;
  word-break: break-all;
  white-space: pre-wrap;
  line-height: 1.6;
}

/* 自定义滚动条样式 */
.code-box::-webkit-scrollbar, .result-display::-webkit-scrollbar, .output-content::-webkit-scrollbar {
  width: 8px;
}
.code-box::-webkit-scrollbar-track, .result-display::-webkit-scrollbar-track, .output-content::-webkit-scrollbar-track {
  background: transparent;
}
.code-box::-webkit-scrollbar-thumb, .result-display::-webkit-scrollbar-thumb, .output-content::-webkit-scrollbar-thumb {
  background: #333;
  border-radius: 4px;
}
.code-box::-webkit-scrollbar-thumb:hover, .result-display::-webkit-scrollbar-thumb:hover, .output-content::-webkit-scrollbar-thumb:hover {
  background: #555;
}

/* 输出区导航栏样式 */
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

/* 树面板 */
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
  gap: 20px;
  align-items: center;
}

.retry-btn {
  background: transparent;
  border: 1px solid #444;
  color: #888;
  cursor: pointer;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}
.retry-btn:hover {
  color: white;
  border-color: #666;
}

.tree-container {
  flex-grow: 1;
  background-color: #242444;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

/* Graph Container */
.graph-container {
  width: 100%;
  height: 100%;
  overflow: hidden;
}

/* 树加载状态 */
.tree-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #888;
}

.tree-loading p {
  margin-top: 16px;
  font-size: 14px;
}

.error-msg {
  color: #e74c3c;
  background: rgba(231, 76, 60, 0.1);
  padding: 20px;
  border-radius: 8px;
  border: 1px solid rgba(231, 76, 60, 0.3);
  max-width: 80%;
  text-align: left;
  word-break: break-word;
}
.error-msg p {
  margin: 0;
}

/* 发送页面样式 */
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

.connection-status span {
  color: #fff;
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

.messages-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-item {
  background-color: #1a1a2e;
  padding: 12px 16px;
  border-radius: 10px;
  border: 1px solid #333;
}

.message-time {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.message-content {
  color: #4ecca3;
  font-family: 'Consolas', monospace;
  word-break: break-all;
}

/* 历史页面样式 */
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

/* 加载动画 */
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

/* SVG 样式（用于 graphviz 渲染的图形）*/
.graph-container :deep(svg) {
  width: 100%;
  height: 100%;
  max-width: 100%;
  max-height: 100%;
}

.graph-container :deep(.node text) {
  fill: #e0e0e0;
}

.graph-container :deep(.edge text) {
  fill: #888;
}

.graph-container :deep(.node polygon),
.graph-container :deep(.node ellipse) {
  fill: #2a2a4a;
  stroke: #667eea;
}

.graph-container :deep(.edge path) {
  stroke: #555;
}
</style>
