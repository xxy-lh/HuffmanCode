import { createApp } from 'vue'
import App from './App.vue'
import router from './router' // 1. 导入路由

// 导入 App.vue 中定义的全局样式
import './assets/main.css'; // 如果您有全局样式文件，请确保路径正确

const app = createApp(App)

app.use(router) // 2. 在应用挂载前，使用路由

app.mount('#app')
