<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useChatStore } from '../stores/chat'
const { t } = useI18n()
const chat = useChatStore()
onMounted(() => chat.connect())
onUnmounted(() => chat.disconnect())
</script>

<template>
  <main class="page-shell chat-page">
    <div class="page-heading"><div><div class="eyebrow">TWITCH · WEBSOCKET</div><h1>{{ t('app.liveTitle') }}</h1><p class="lead">{{ t('app.liveCopy') }}</p></div>
      <div class="connection-pill" aria-live="polite"><span class="status-dot" :class="chat.status" />{{ t(`app.${chat.status}`) }}</div>
    </div>
    <section class="chat-card" aria-live="polite" :aria-label="t('app.liveTitle')">
      <div v-if="!chat.messages.length" class="empty-state">{{ t('app.empty') }}</div>
      <article v-for="(message, index) in chat.messages" :key="`${message.userId}-${index}`" class="message-row">
        <div class="message-meta"><strong>{{ message.username }}</strong><span v-if="message.firstMessage" class="first-badge">{{ t('app.first') }}</span><span class="user-id">{{ t('app.userId') }}: {{ message.userId }}</span></div>
        <p>{{ message.message }}</p>
      </article>
    </section>
    <div class="chat-actions"><v-btn v-if="chat.status !== 'connected' && chat.status !== 'connecting'" color="primary" @click="chat.connect()">{{ t('app.reconnect') }}</v-btn><v-btn v-else variant="outlined" @click="chat.disconnect()">{{ t('app.disconnect') }}</v-btn></div>
  </main>
</template>
