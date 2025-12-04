// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router';
import Login from '../views/Login.vue';
import MainApp from '../views/MainApp.vue'; // 这是包含侧边栏和主要功能区的新组件

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: Login },
  { path: '/app', component: MainApp, meta: { requiresAuth: true } }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// 添加导航守卫，检查用户是否登录
router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !localStorage.getItem('username')) {
    next('/login');
  } else {
    next();
  }
});

export default router;
