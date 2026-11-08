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
  <main class="overlay-shell">
    <section class="overlay-messages" aria-live="polite" :aria-label="t('app.liveTitle')">
      <div v-if="!chat.messages.length" class="overlay-empty">{{ t('app.overlayEmpty') }}</div>
      <article v-for="(message, index) in chat.messages" :key="`${message.userId}-${index}`" class="overlay-message"><strong>{{ message.username }}</strong><span v-if="message.firstMessage" class="first-badge">{{ t('app.first') }}</span><span>{{ message.message }}</span></article>
    </section>
  </main>
</template>
