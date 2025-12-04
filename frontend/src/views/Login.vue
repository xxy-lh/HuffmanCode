<template>
  <div class="login-container">
    <div class="login-card">
      <!-- 头部文案 -->
      <div class="card-header">
        <h2>{{ isRegister ? '创建账户' : '欢迎回来' }}</h2>
        <p class="subtitle">{{ isRegister ? '注册以开始使用工具箱' : '登录以继续您的操作' }}</p>
      </div>

      <!-- 错误提示 -->
      <div v-if="errorMessage" class="error-alert">
        <span class="error-icon">⚠️</span> {{ errorMessage }}
      </div>

      <!-- 表单区域 -->
      <form @submit.prevent="isRegister ? handleRegister() : handleLogin()">
        <div class="form-group">
          <label for="username">用户名</label>
          <input
            type="text"
            id="username"
            v-model="username"
            required
            placeholder="输入您的用户名"
            autocomplete="username"
          >
        </div>

        <div class="form-group">
          <label for="password">密码</label>
          <input
            type="password"
            id="password"
            v-model="password"
            required
            placeholder="输入您的密码"
            autocomplete="current-password"
          >
        </div>

        <button type="submit" class="submit-btn" :disabled="isLoading">
          {{ isLoading ? '处理中...' : (isRegister ? '注册' : '登录') }}
        </button>
      </form>

      <!-- 底部切换 -->
      <div class="card-footer">
        <a @click.prevent="toggleForm" class="toggle-link">
          {{ isRegister ? '已有账户？点击登录' : '没有账户？点击注册' }}
        </a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const isRegister = ref(false);
const username = ref('');
const password = ref('');
const errorMessage = ref('');
const isLoading = ref(false); // 新增加载状态

function toggleForm() {
  isRegister.value = !isRegister.value;
  errorMessage.value = '';
  username.value = '';
  password.value = '';
}

async function handleLogin() {
  isLoading.value = true;
  errorMessage.value = '';
  try {
    const response = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: username.value, password: password.value }),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.message || '登录失败');
    }

    localStorage.setItem('username', data.username || username.value);
    await router.push('/app');

  } catch (error) {
    errorMessage.value = error.message;
  } finally {
    isLoading.value = false;
  }
}

async function handleRegister() {
  isLoading.value = true;
  errorMessage.value = '';
  try {
    const response = await fetch('http://localhost:8080/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: username.value, password: password.value }),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.message || '注册失败');
    }

    alert('注册成功！请现在登录。');
    toggleForm();

  } catch (error) {
    errorMessage.value = error.message;
  } finally {
    isLoading.value = false;
  }
}
</script>

<style scoped>
/* 全屏容器：深色背景 */
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  width: 100vw;
  background-color: #1a1a1a; /* 深灰色背景，类似参考图 */
  /* 如果你想用背景图，可以取消下面这行的注释并替换URL */
  /* background-image: url('你的图片地址'); background-size: cover; background-position: center; */
}

/* 登录卡片 */
.login-card {
  width: 100%;
  max-width: 400px;
  background: #f5f5f7; /* 柔和的灰白色背景 */
  border-radius: 24px; /* 大圆角 */
  padding: 40px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3); /* 更有质感的阴影 */
  display: flex;
  flex-direction: column;
  gap: 24px;
  transition: transform 0.3s ease;
}

.login-card:hover {
  transform: translateY(-5px);
}

/* 头部样式 */
.card-header {
  text-align: center;
}

h2 {
  margin: 0 0 8px 0;
  font-size: 28px;
  color: #333;
  font-weight: 600;
}

.subtitle {
  margin: 0;
  color: #666;
  font-size: 14px;
}

/* 表单元素 */
.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

label {
  font-size: 14px;
  font-weight: 600;
  color: #444;
  margin-left: 4px;
}

input {
  width: 100%;
  padding: 14px 16px;
  border: 2px solid transparent; /* 默认无边框 */
  background-color: #fff;
  border-radius: 12px;
  font-size: 16px;
  color: #333;
  outline: none;
  transition: all 0.3s ease;
  box-shadow: 0 2px 6px rgba(0,0,0,0.02);
  box-sizing: border-box;
}

/* 输入框聚焦状态 */
input:focus {
  border-color: #007bff;
  background-color: #fff;
  box-shadow: 0 0 0 4px rgba(0, 123, 255, 0.1);
}

/* 按钮样式 */
.submit-btn {
  width: 100%;
  padding: 14px;
  margin-top: 8px;
  border: none;
  border-radius: 12px;
  background-color: #007bff; /* 亮蓝色 */
  color: white;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s, transform 0.1s;
}

.submit-btn:hover {
  background-color: #0069d9;
}

.submit-btn:active {
  transform: scale(0.98);
}

.submit-btn:disabled {
  background-color: #a0cfff;
  cursor: not-allowed;
}

/* 底部链接 */
.card-footer {
  text-align: center;
  margin-top: 8px;
}

.toggle-link {
  color: #007bff;
  font-size: 14px;
  cursor: pointer;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s;
}

.toggle-link:hover {
  color: #0056b3;
  text-decoration: underline;
}

/* 错误提示框 */
.error-alert {
  background-color: #fff2f0;
  border: 1px solid #ffccc7;
  color: #ff4d4f;
  padding: 10px 16px;
  border-radius: 8px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
