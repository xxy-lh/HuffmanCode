<template>
  <!-- 最外层容器：强制占满全屏 -->
  <div class="main-app-container">
    <!-- 左侧导航栏 (固定 25vw) -->
    <aside class="sidebar">
      <div class="user-profile">
        <div class="avatar">{{ username.charAt(0).toUpperCase() }}</div>
        <div class="username">{{ username }}</div>
      </div>
      <nav class="navigation">
        <a href="#" class="nav-item active"><span>编码</span></a>
        <a href="#" class="nav-item"><span>发送</span></a>
        <a href="#" class="nav-item"><span>历史</span></a>
      </nav>
      <button @click="logout" class="logout-button">退出登录</button>
    </aside>

    <!-- 右侧主内容区 (固定 75vw) -->
    <main class="main-content">
      <header class="content-header">
        <h1>哈夫曼编码/解码器</h1>
        <p>一个用于文本和数据压缩的可视化工具</p>
      </header>

      <div class="tool-tabs">
        <button :class="{ active: activeTab === 'encode' }" class="tab-button" @click="activeTab = 'encode'">编码</button>
        <button :class="{ active: activeTab === 'decode' }" class="tab-button" @click="activeTab = 'decode'">解码</button>
        <button :class="{ active: activeTab === 'tree' }" class="tab-button" @click="activeTab = 'tree'">哈夫曼树</button>
      </div>

      <div class="coder-panel">
        <!-- 输入区 -->
        <div class="input-section">
          <h2>要编码的文本</h2>
          <div class="textarea-wrapper">
            <textarea v-model="textToEncode" placeholder="在此输入文本..."></textarea>
          </div>
          <button @click="handleEncode" class="action-button" :disabled="isLoading">
            {{ isLoading ? '编码中...' : '生成编码' }}
          </button>
        </div>
        <!-- 结果展示区 -->
        <div class="output-section">
          <h2>结果展示</h2>
          <div class="result-content">
            <div v-if="!encodeResult && !isLoading" class="placeholder">
              <div class="placeholder-icon">⌨️</div>
              <p>在左侧输入文本以开始...</p>
            </div>
            <div v-if="isLoading" class="loading-spinner"></div>
            <div v-if="encodeResult" class="result-display">
              <div class="result-item">
                <h3>编码结果</h3>
                <div class="code-box">{{ encodeResult.encodedText }}</div>
              </div>
              <div class="result-item">
                <h3>字符频率</h3>
                <div class="code-box">{{ encodeResult.frequencies }}</div>
              </div>
              <div class="result-item">
                <h3>哈夫曼编码表</h3>
                <div class="code-box">{{ encodeResult.codes }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

const router = useRouter();

// --- 响应式状态 ---
const username = ref('User');
const textToEncode = ref('');
const activeTab = ref('encode');
const isLoading = ref(false);
const encodeResult = ref(null);

// --- 生命周期钩子 ---
onMounted(() => {
  const storedUsername = localStorage.getItem('username');
  if (storedUsername) {
    username.value = storedUsername;
  }
});

// --- 方法 ---
const handleEncode = async () => {
  if (!textToEncode.value.trim()) {
    alert('请输入要编码的文本！');
    return;
  }
  isLoading.value = true;
  encodeResult.value = null;

  try {
    const response = await axios.post('/api/huffman/process', {
      text: textToEncode.value
    });
    encodeResult.value = response.data;
  } catch (error) {
    console.error('编码失败:', error);
    alert('编码失败，请检查后端服务或网络连接。');
  } finally {
    isLoading.value = false;
  }
};

const logout = () => {
  localStorage.removeItem('username');
  router.push('/');
};
</script>

<style>
/* 强制全局重置，防止外部样式干扰 */
body, html {
  margin: 0 !important;
  padding: 0 !important;
  width: 100%;
  height: 100%;
  overflow: hidden; /* 禁止 Body 滚动 */
}
</style>

<style scoped>
/* 1. 主容器布局 - 强制占满视口 */
.main-app-container {
  position: fixed; /* 使用 fixed 定位，脱离文档流，强行铺满 */
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  display: flex;
  background-color: #f4f7f6;
  z-index: 999; /* 确保在最上层 */
}

/* 2. 左侧导航栏 */
.sidebar {
  width: 25vw; /* 使用 vw 单位，严格占据视口的 25% */
  flex: 0 0 25vw;
  background-color: #2c3e50;
  color: #ecf0f1;
  display: flex;
  flex-direction: column;
  padding: 32px;
  box-sizing: border-box;
  box-shadow: 4px 0 10px rgba(0,0,0,0.05);
  height: 100vh;
}

/* 3. 右侧主内容区 */
.main-content {
  width: 75vw; /* 使用 vw 单位，严格占据视口的 75% */
  flex: 0 0 75vw;
  padding: 40px;
  display: flex;
  flex-direction: column;
  overflow-y: auto; /* 仅允许内容区域滚动 */
  height: 100vh;
  box-sizing: border-box;
  background-color: #f4f7f6; /* 确保背景色填充 */
}

/* 头部样式 */
.content-header h1 {
  font-size: 28px;
  margin: 0 0 8px 0;
  color: #2c3e50;
  font-weight: 700;
}
.content-header p {
  font-size: 14px;
  color: #7f8c8d;
  margin: 0 0 24px 0;
}

/* 标签页样式 */
.tool-tabs {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}
.tab-button {
  padding: 8px 24px;
  border: 1px solid #dfe6e9;
  border-radius: 6px;
  background-color: white;
  cursor: pointer;
  font-size: 14px;
  color: #636e72;
  transition: all 0.2s;
  font-weight: 500;
}
.tab-button.active {
  background-color: #3498db;
  color: white;
  border-color: #3498db;
  box-shadow: 0 2px 4px rgba(52, 152, 219, 0.3);
}

/* 编码器面板布局 */
.coder-panel {
  flex-grow: 1;
  display: flex;
  gap: 24px;
  align-items: stretch;
  min-height: 0;
  padding-bottom: 20px; /* 底部留一点空间 */
}

.input-section, .output-section {
  flex: 1;
  background-color: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  border: 1px solid #f1f2f6;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

/* 输入区样式 */
.input-section h2, .output-section h2 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f2f6;
}

.textarea-wrapper {
  flex-grow: 1;
  margin-bottom: 16px;
  position: relative;
}

.input-section textarea {
  width: 100%;
  height: 100%;
  border: 1px solid #dfe6e9;
  border-radius: 8px;
  padding: 16px;
  font-size: 15px;
  resize: none;
  outline: none;
  transition: border-color 0.2s;
  background-color: #fafafa;
  box-sizing: border-box;
  font-family: inherit;
  line-height: 1.5;
}
.input-section textarea:focus {
  border-color: #3498db;
  background-color: white;
}

.action-button {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 8px;
  background-color: #27ae60;
  color: white;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s, transform 0.1s;
  box-shadow: 0 2px 4px rgba(39, 174, 96, 0.2);
}
.action-button:hover:not(:disabled) {
  background-color: #2ecc71;
  transform: translateY(-1px);
}
.action-button:disabled {
  background-color: #95a5a6;
  cursor: not-allowed;
}

/* 结果区样式 */
.result-content {
  flex-grow: 1;
  position: relative;
  overflow-y: auto;
  padding-right: 4px;
}

.output-section .placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #b2bec3;
}
.placeholder-icon { font-size: 40px; margin-bottom: 12px; opacity: 0.5; }

.result-display {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.result-item h3 {
  font-size: 13px;
  color: #636e72;
  margin: 0 0 6px 0;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.code-box {
  background-color: #f8f9fa;
  padding: 12px;
  border-radius: 6px;
  border: 1px solid #e9ecef;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  color: #2d3436;
  word-break: break-all;
  white-space: pre-wrap;
  line-height: 1.5;
}

/* 侧边栏组件样式 */
.user-profile {
  text-align: center;
  margin-bottom: 40px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3498db, #2980b9);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: bold;
  margin: 0 auto 16px;
  box-shadow: 0 4px 10px rgba(0,0,0,0.2);
}
.username { font-size: 18px; font-weight: 600; }
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
  font-size: 15px;
}
.nav-item:hover {
  background-color: rgba(255,255,255,0.05);
  color: white;
  transform: translateX(4px);
}
.nav-item.active {
  background-color: #3498db;
  color: white;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(52, 152, 219, 0.3);
}
.logout-button {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 8px;
  background-color: rgba(231, 76, 60, 0.1);
  color: #e74c3c;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid rgba(231, 76, 60, 0.2);
}
.logout-button:hover { background-color: #e74c3c; color: white; }
.loading-spinner {
  margin: 40px auto;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #3498db;
  border-radius: 50%;
  width: 30px;
  height: 30px;
  animation: spin 1s linear infinite;
}
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>
