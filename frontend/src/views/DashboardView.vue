<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { useChatStore } from '../stores/chat'
const { t } = useI18n()
const chat = useChatStore()
const route = useRoute()
</script>

<template>
  <main class="page-shell">
    <section class="hero-panel">
      <div class="eyebrow">TWITCH · LIVE CHAT</div>
      <h1>{{ t('app.dashboardTitle') }}</h1>
      <p class="lead">{{ t('app.dashboardCopy') }}</p>
      <div class="hero-actions">
        <v-btn color="primary" size="large" href="/callback/twitch/start">{{ t('app.connect') }}</v-btn>
        <v-btn variant="outlined" size="large" to="/chat">{{ t('app.startChat') }}</v-btn>
      </div>
      <p class="subtle">{{ t('app.connectHint') }}</p>
      <v-alert v-if="route.query.twitch === 'connected'" type="success" variant="tonal">{{ t('app.connected') }}</v-alert>
      <p class="subtle">{{ t('app.connectionNote') }}</p>
    </section>
    <section class="status-card" aria-live="polite">
      <div><div class="eyebrow">{{ t('app.status') }}</div><div class="status-row"><span class="status-dot" :class="chat.status" />{{ t(`app.${chat.status}`) }}</div></div>
      <RouterLink class="text-link" to="/chat">{{ t('app.chat') }} →</RouterLink>
    </section>
    <section class="bottom-card">
      <div><div class="eyebrow">OBS</div><h2>{{ t('app.overlay') }}</h2><p class="subtle">{{ t('app.overlayCopy') }}</p></div>
      <RouterLink class="text-link" to="/embed/chat">{{ t('app.overlay') }} →</RouterLink>
    </section>
  </main>
</template>
