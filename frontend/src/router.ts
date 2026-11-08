import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from './views/DashboardView.vue'
import ChatView from './views/ChatView.vue'
import OverlayView from './views/OverlayView.vue'
import { useAccountStore } from './stores/account'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'dashboard', component: DashboardView },
    { path: '/chat', name: 'chat', component: ChatView, meta: { requiresAccount: true } },
    { path: '/embed/chat', name: 'overlay', component: OverlayView, meta: { overlay: true } },
  ],
})

router.beforeEach(async to => {
  if (to.meta.requiresAccount && !await useAccountStore().loadSession()) return { name: 'dashboard' }
  return true
})

export default router
