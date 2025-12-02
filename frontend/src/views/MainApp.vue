<template>
  <div class="app-wrapper">
    <!-- 背景层 -->
    <div class="background-layer"></div>

    <!-- 左侧导航栏 -->
    <aside class="sidebar">
      <div class="user-profile">
        <div class="avatar">{{ username.charAt(0).toUpperCase() }}</div>
        <div class="username">{{ username }}</div>
      </div>
      <nav class="navigation">
        <a href="#" :class="['nav-item', { active: activeView === 'encode' }]" @click.prevent="switchView('encode')">
          <span>📝 编码</span>
        </a>
<a href="#" :class="['nav-item', { active: activeView === 'decode' }]" @click.prevent="switchView('decode')">
          <span>🔓 解码</span>
        </a>
<a href="#" :class="['nav-item', { active: activeView === 'tree' }]" @click.prevent="switchView('tree')">
          <span>🌳 哈夫曼树</span>
        </a>
      </nav>
      <button @click="logout" class="logout-button">退出登录</button>
    </aside>

    <!-- 右侧主内容区 -->
    <main class="main-content">
      <header class="content-header">
        <h1>哈夫曼编码/解码器</h1>
        <p>一个用于文本和数据压缩的可视化工具</p>
      </header>

      <!-- 编码视图 -->
      <div v-if="activeView === 'encode'" class="view-content two-column">
        <div class="card input-card">
          <h3>要编码的文本</h3>
          <div class="textarea-wrapper">
            <textarea v-model="textToEncode" placeholder="在此输入文本... "></textarea>
          </div>
          <button @click="handleEncode" class="action-btn" :disabled="isLoading">
            {{ isLoading ? '编码中...' : '生成编码' }}
          </button>
        </div>

        <div class="card result-card">
          <h3>结果展示</h3>
          <div v-if="encodeResult.encodedText" class="result-content">
            <div class="result-header">
              <span class="label">编码后的文本:</span>
              <button class="copy-btn" @click="copyEncodedText">{{ copyEncodedTextButtonText }}</button>
            </div>
            <div class="result-text-box">{{ encodeResult.encodedText }}</div>

            <div class="result-header mt-3">
              <span class="label">哈夫曼编码表:</span>
              <button class="copy-btn" @click="copyCodesAsJson">{{ copyButtonText }}</button>
            </div>
            <div class="data-grid" v-if="encodeResult. codes">
              <div v-for="(code, char) in encodeResult.codes" :key="char" class="data-item">
                <span class="char">'{{ formatChar(char) }}'</span>
                <span class="separator">:</span>
                <span class="value">{{ code }}</span>
              </div>
            </div>

            <div class="result-section mt-3">
              <span class="label">字符频率:</span>
              <div class="data-grid" v-if="encodeResult.frequencies">
                <div v-for="(freq, char) in encodeResult.frequencies" :key="char" class="data-item">
                  <span class="char">'{{ formatChar(char) }}'</span>
                  <span class="separator">:</span>
                  <span class="value">{{ freq }}</span>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="placeholder">
            <span class="placeholder-icon">⌨️</span>
            <p>在左侧输入文本以开始... </p>
          </div>
        </div>
      </div>

      <!-- 解码视图 -->
      <div v-if="activeView === 'decode'" class="view-content two-column">
        <div class="card input-card">
          <h3>解码输入</h3>
          <label for="encoded-text-input">二进制编码 (0和1)</label>
          <textarea id="encoded-text-input" v-model="textToDecode" placeholder="在此输入二进制编码..."></textarea>
          <label for="codes-input" class="mt-2">哈夫曼编码表 (JSON)</label>
          <textarea id="codes-input" v-model="codesForDecode" class="code-input" placeholder='{"a": "01", "b": "11"}'></textarea>
          <button @click="handleDecode" class="action-btn" :disabled="isDecoding">
            {{ isDecoding ? '解码中...' : '执行解码' }}
          </button>
        </div>
        <div class="card result-card">
          <h3>解码结果</h3>
          <div v-if="decodeResult.decodedText" class="result-content">
            <div class="result-section">
              <span class="label">还原的原文:</span>
              <div class="result-text-box success-highlight">{{ decodeResult. decodedText }}</div>
            </div>
          </div>
          <div v-else class="placeholder">
            <span class="placeholder-icon">🔓</span>
            <p>等待解码输入...</p>
          </div>
        </div>
      </div>

      <!-- 哈夫曼树可视化视图 -->
      <div v-if="activeView === 'tree'" class="view-content tree-view">
        <div class="card info-card">
          <h3>文本信息</h3>
          <div v-if="textToEncode && encodeResult.encodedText" class="result-content">
            <label>原始输入</label>
            <div class="readonly-text">{{ textToEncode }}</div>

            <label class="mt-3">编码结果</label>
            <div class="readonly-text code-font">{{ encodeResult.encodedText }}</div>

            <label class="mt-3">压缩信息</label>
            <div class="stats-box">
              <div class="stat-item">
                <span class="stat-label">原始大小:</span>
                <span class="stat-value">{{ textToEncode.length * 8 }} bits</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">压缩后:</span>
                <span class="stat-value">{{ encodeResult.encodedText.length }} bits</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">压缩率:</span>
                <span class="stat-value">{{ compressionRatio }}%</span>
              </div>
            </div>
          </div>
          <div v-else class="placeholder">
            <p>请先在"编码"页面输入文本。</p>
          </div>
        </div>
        <div class="card graph-card">
          <h3>哈夫曼树结构</h3>
          <div v-if="encodeResult.treeDot" id="graph" class="graph-container"></div>
          <div v-else class="placeholder">
            <span class="placeholder-icon">🌳</span>
            <p>暂无数据可视化</p>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, watch, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { graphviz } from 'd3-graphviz';

const router = useRouter();

const username = ref('User');
const activeView = ref('encode');
const isLoading = ref(false);
const isDecoding = ref(false);

const textToEncode = ref('');
const encodeResult = reactive({
  encodedText: '',
  codes: null,
  frequencies: null,
  treeDot: '',
});

const textToDecode = ref('');
const codesForDecode = ref('');
const decodeResult = reactive({
  decodedText: '',
});

const copyButtonText = ref('复制为JSON');
const copyEncodedTextButtonText = ref('复制');

const compressionRatio = computed(() => {
  if (!textToEncode.value || !encodeResult.encodedText) return 0;
  const original = textToEncode.value. length * 8;
  const compressed = encodeResult.encodedText.length;
  return ((1 - compressed / original) * 100).toFixed(1);
});

onMounted(() => {
  const storedUser = localStorage.getItem('user');
  if (storedUser) {
    try {
      const user = JSON.parse(storedUser);
      username.value = user.username || 'User';
    } catch (e) {
      username.value = 'User';
    }
  }
});

function switchView(viewName) {
  activeView. value = viewName;
}

async function handleEncode() {
  if (!textToEncode.value. trim()) {
    alert('请输入要编码的文本！');
    return;
  }

  isLoading.value = true;
  encodeResult.encodedText = '';
  encodeResult.codes = null;
  encodeResult.frequencies = null;
  encodeResult.treeDot = '';

  try {
    const response = await fetch('/api/huffman/process', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text: textToEncode.value }),
    });
    const result = await response.json();

    if (! response.ok) {
      throw new Error(result.message || 'HTTP 错误');
    }

    encodeResult.encodedText = result.encodedText;
    encodeResult.codes = result.codes;
    encodeResult.frequencies = result.frequencies;
    encodeResult.treeDot = result.treeDot;

    copyButtonText.value = '复制为JSON';
    copyEncodedTextButtonText.value = '复制';
  } catch (error) {
    console.error('编码处理时出错:', error);
    alert('处理失败: ' + error.message + '\n(请确保后端服务已启动)');
  } finally {
    isLoading.value = false;
  }
}

async function handleDecode() {
  if (!textToDecode.value.trim() || !codesForDecode. value.trim()) {
    alert('请输入要解码的文本和对应的编码表！');
    return;
  }

  isDecoding.value = true;
  decodeResult.decodedText = '';

  try {
    const codes = JSON.parse(codesForDecode.value);
    const response = await fetch('/api/huffman/decode', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ encodedText: textToDecode.value, codes: codes }),
    });
    const result = await response.json();

    if (!response.ok) {
      throw new Error(result. message || 'HTTP 错误');
    }

    decodeResult.decodedText = result.decodedText;
  } catch (error) {
    console.error('解码处理时出错:', error);
    if (error instanceof SyntaxError) {
      alert('解码失败: 编码表不是有效的JSON格式。');
    } else {
      alert('解码失败: ' + error.message);
    }
  } finally {
    isDecoding.value = false;
  }
}

function logout() {
  localStorage.removeItem('user');
  localStorage.removeItem('username');
  router.push('/login');
}

async function copyEncodedText() {
  if (!encodeResult. encodedText) return;
  try {
    await navigator.clipboard. writeText(encodeResult.encodedText);
    copyEncodedTextButtonText.value = '已复制!';
    setTimeout(() => {
      copyEncodedTextButtonText.value = '复制';
    }, 2000);
  } catch (err) {
    console.error('复制失败:', err);
    alert('复制失败，请手动复制。');
  }
}

async function copyCodesAsJson() {
  if (!encodeResult. codes) {
    alert('没有可复制的编码表！');
    return;
  }
  try {
    const jsonString = JSON.stringify(encodeResult.codes, null, 2);
    await navigator.clipboard. writeText(jsonString);
    copyButtonText.value = '已复制!';
    setTimeout(() => {
      copyButtonText. value = '复制为JSON';
    }, 2000);
  } catch (err) {
    console.error('复制失败:', err);
    alert('复制失败，请检查浏览器权限或手动复制。');
  }
}

function formatChar(char) {
  switch (char) {
    case ' ': return 'Space';
    case '\n': return '\\n';
    case '\t': return '\\t';
    default: return char;
  }
}

function renderGraph() {
  if (encodeResult.treeDot) {
    nextTick(() => {
      const graphContainer = document.querySelector('#graph');
      if (graphContainer) {
        graphContainer.innerHTML = '';
        graphviz(graphContainer, { useWorker: false })
          .width(graphContainer.clientWidth)
          .height(graphContainer.clientHeight)
          . fit(true)
          .zoom(true)
          .renderDot(encodeResult.treeDot);
      }
    });
  }
}

watch(() => encodeResult.treeDot, (newDotValue) => {
  if (newDotValue) renderGraph();
});

watch(activeView, (newView) => {
  if (newView === 'tree') renderGraph();
});
</script>

<style>
html, body, #app {
  margin: 0 ! important;
  padding: 0 !important;
  width: 100%;
  height: 100%;
  overflow: hidden;
}
</style>

<style scoped>
* {
  box-sizing: border-box;
}

.app-wrapper {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  display: flex;
  z-index: 999;
}

.background-layer {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: -1;
  background-image: url('https://images.unsplash.com/photo-1550745165-9bc0b252726a?q=80&w=1920');
  background-size: cover;
  background-position: center;
}

.sidebar {
  width: 220px;
  flex: 0 0 220px;
  background-color: rgba(44, 62, 80, 0.95);
  backdrop-filter: blur(10px);
  color: #ecf0f1;
  display: flex;
  flex-direction: column;
  padding: 24px;
  box-sizing: border-box;
}

.user-profile {
  text-align: center;
  margin-bottom: 32px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3498db, #2980b9);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: bold;
  margin: 0 auto 12px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2);
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
  padding: 12px 16px;
  border-radius: 8px;
  color: #bdc3c7;
  text-decoration: none;
  transition: all 0.2s;
  font-size: 14px;
}

.nav-item:hover {
  background-color: rgba(255, 255, 255, 0.05);
  color: white;
  transform: translateX(4px);
}

.nav-item.active {
  background-color: #3498db;
  color: white;
  font-weight: 500;
}

.logout-button {
  width: 100%;
  padding: 12px;
  border-radius: 8px;
  background-color: rgba(231, 76, 60, 0.1);
  color: #e74c3c;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid rgba(231, 76, 60, 0.2);
}

.logout-button:hover {
  background-color: #e74c3c;
  color: white;
}

.main-content {
  flex: 1;
  padding: 32px;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  background: rgba(244, 247, 246, 0.9);
  backdrop-filter: blur(10px);
}

.content-header {
  text-align: center;
  margin-bottom: 24px;
}

.content-header h1 {
  font-size: 28px;
  margin: 0 0 8px 0;
  color: #2c3e50;
  font-weight: 700;
}

.content-header p {
  font-size: 14px;
  color: #7f8c8d;
  margin: 0;
}

.view-content {
  display: grid;
  gap: 24px;
  flex: 1;
  min-height: 0;
}

.two-column {
  grid-template-columns: 1fr 1fr;
}

.tree-view {
  grid-template-columns: 280px 1fr;
}

.card {
  background: rgba(255, 255, 255, 0.98);
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.card h3 {
  margin: 0 0 16px 0;
  padding-bottom: 12px;
  border-bottom: 2px solid #f1f2f6;
  color: #2c3e50;
  font-size: 16px;
  font-weight: 600;
}

.card label {
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 8px;
  display: block;
  font-size: 14px;
}

.input-card {
  display: flex;
  flex-direction: column;
}

.textarea-wrapper {
  flex: 1;
  min-height: 150px;
  margin-bottom: 16px;
}

.input-card textarea,
.textarea-wrapper textarea {
  width: 100%;
  height: 100%;
  min-height: 120px;
  border-radius: 8px;
  border: 1px solid #dfe6e9;
  padding: 12px;
  font-size: 14px;
  resize: none;
  background: #fafafa;
  transition: border 0.3s, background 0.3s;
  font-family: inherit;
  box-sizing: border-box;
}

.input-card textarea:focus,
.textarea-wrapper textarea:focus {
  outline: none;
  border-color: #3498db;
  background: white;
}

.code-input {
  font-family: 'Consolas', 'Monaco', monospace ! important;
  font-size: 13px !important;
}

.mt-2 {
  margin-top: 12px;
}

.mt-3 {
  margin-top: 16px;
}

.action-btn {
  width: 100%;
  padding: 12px;
  margin-top: auto;
  font-size: 15px;
  font-weight: 600;
  color: white;
  background: linear-gradient(135deg, #27ae60, #2ecc71);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.action-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(39, 174, 96, 0.3);
}

.action-btn:disabled {
  background: #95a5a6;
  cursor: not-allowed;
}

.result-card {
  overflow-y: auto;
}

.result-content {
  flex: 1;
  overflow-y: auto;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.label {
  font-weight: 600;
  color: #2c3e50;
  font-size: 14px;
}

.copy-btn {
  padding: 4px 12px;
  font-size: 12px;
  background-color: #3498db;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.copy-btn:hover {
  background-color: #2980b9;
}

.result-text-box,
.readonly-text {
  background-color: #f8f9fa;
  padding: 12px;
  border-radius: 8px;
  word-break: break-all;
  color: #2d3436;
  max-height: 120px;
  overflow-y: auto;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  border: 1px solid #e9ecef;
  line-height: 1.5;
}

.success-highlight {
  background-color: #d4edda;
  border-color: #c3e6cb;
}

.data-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-height: 150px;
  overflow-y: auto;
  padding: 12px;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 8px;
}

.data-item {
  background: white;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  padding: 4px 8px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  white-space: nowrap;
}

.data-item .char {
  font-weight: bold;
  color: #3498db;
}

.data-item .value {
  color: #e74c3c;
  font-weight: bold;
  margin-left: 4px;
}

.stats-box {
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 12px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  border-bottom: 1px solid #e9ecef;
}

.stat-item:last-child {
  border-bottom: none;
}

.stat-label {
  color: #636e72;
  font-size: 13px;
}

.stat-value {
  color: #2c3e50;
  font-weight: 600;
  font-size: 13px;
}

.placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #b2bec3;
  text-align: center;
  min-height: 200px;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.graph-card {
  min-height: 400px;
}

.graph-container {
  flex: 1;
  min-height: 350px;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e9ecef;
}

@media (max-width: 1024px) {
  .two-column,
  .tree-view {
    grid-template-columns: 1fr;
  }

  .sidebar {
    width: 180px;
    flex: 0 0 180px;
    padding: 16px;
  }
  .main-content {
    padding: 20px;
  }
}
</style>
