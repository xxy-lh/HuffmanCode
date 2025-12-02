<template>
  <div class="login-container">
    <div class="login-box">
      <!-- 标题 -->
      <h2>{{ isRegister ? '创建新账户' : '欢迎回来' }}</h2>
      <p>{{ isRegister ? '加入我们，开始使用哈夫曼编码工具。' : '登录以继续。' }}</p>

      <!-- 错误提示 -->
      <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>

      <!-- 表单 -->
      <form @submit.prevent="isRegister ? handleRegister() : handleLogin()">
        <div class="input-group">
          <label for="username">用户名</label>
          <input type="text" id="username" v-model="username" required placeholder="输入您的用户名">
        </div>
        <div class="input-group">
          <label for="password">密码</label>
          <input type="password" id="password" v-model="password" required placeholder="输入您的密码">
        </div>
        <button type="submit" class="submit-btn">{{ isRegister ? '注册' : '登录' }}</button>
      </form>

      <!-- 切换链接 -->
      <div class="toggle-form">
        <a @click.prevent="toggleForm">{{ isRegister ? '已有账户？点击登录' : '没有账户？点击注册' }}</a>
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

function toggleForm() {
  isRegister.value = !isRegister.value;
  errorMessage.value = ''; // 清除错误信息
  username.value = '';
  password.value = '';
}

async function handleLogin() {
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

    // 登录成功，存储用户信息并跳转
    localStorage.setItem('user', JSON.stringify({ username: data.username }));
    await router.push('/app');

  } catch (error) {
    errorMessage.value = error.message;
    console.error('登录错误:', error);
  }
}

async function handleRegister() {
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

    // 注册成功，提示用户登录
    alert('注册成功！请现在登录。');
    toggleForm(); // 切换回登录表单

  } catch (error) {
    errorMessage.value = error.message;
    console.error('注册错误:', error);
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-image: url('https://images.unsplash.com/photo-1550745165-9bc0b252726a?q=80&w=1920');
  background-size: cover;
  background-position: center;
}

.login-box {
  width: 100%;
  max-width: 400px;
  padding: 2.5rem;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  text-align: center;
}

h2 {
  margin-top: 0;
  margin-bottom: 0.5rem;
  font-size: 1.8rem;
  color: #333;
}

p {
  margin-bottom: 1.5rem;
  color: #666;
}

.input-group {
  margin-bottom: 1.5rem;
  text-align: left;
}

.input-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #555;
}

.input-group input {
  width: 100%;
  padding: 0.8rem 1rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
}

.submit-btn {
  width: 100%;
  padding: 1rem;
  border: none;
  border-radius: 8px;
  background: #007bff;
  color: white;
  font-size: 1.1rem;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.3s;
}

.submit-btn:hover {
  background: #0056b3;
}

.toggle-form {
  margin-top: 1.5rem;
}

.toggle-form a {
  color: #007bff;
  cursor: pointer;
  text-decoration: none;
}

.toggle-form a:hover {
  text-decoration: underline;
}

.error-message {
  background-color: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
  padding: 0.75rem 1.25rem;
  border-radius: 8px;
  margin-bottom: 1rem;
}
</style>
