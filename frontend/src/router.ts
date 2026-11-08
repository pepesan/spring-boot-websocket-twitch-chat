import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from './views/DashboardView.vue'
import ChatView from './views/ChatView.vue'
import OverlayView from './views/OverlayView.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'dashboard', component: DashboardView },
    { path: '/chat', name: 'chat', component: ChatView },
    { path: '/embed/chat', name: 'overlay', component: OverlayView, meta: { overlay: true } },
  ],
})
