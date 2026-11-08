<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useTheme } from 'vuetify'
import { useAccountStore } from './stores/account'
import { usePreferencesStore } from './stores/preferences'

const account = useAccountStore()
const prefs = usePreferencesStore()
const { t, locale } = useI18n()
const route = useRoute()
const theme = useTheme()
const isOverlay = computed(() => route.meta.overlay === true)
const router = useRouter()
async function signOut() {
  await account.logout()
  if (account.status !== 'authenticated') await router.replace({ name: 'dashboard' })
}
onMounted(() => { void account.loadSession() })
watch(() => prefs.language, value => { locale.value = value })
watch(() => prefs.theme, value => { theme.change(value) })
</script>

<template>
  <v-app :class="{ 'overlay-app': isOverlay }">
    <a v-if="!isOverlay" class="skip-link" href="#main">{{ t('app.skip') }}</a>
    <template v-if="!isOverlay">
      <v-app-bar flat class="app-bar px-4">
        <v-app-bar-title><RouterLink class="brand" to="/">{{ t('app.name') }}</RouterLink></v-app-bar-title>
        <nav class="top-nav" :aria-label="t('app.name')">
          <RouterLink to="/">{{ t('app.dashboard') }}</RouterLink>
          <RouterLink v-if="account.status === 'authenticated'" to="/chat">{{ t('app.chat') }}</RouterLink>
          <RouterLink to="/embed/chat">{{ t('app.overlay') }}</RouterLink>
        </nav>
        <v-spacer />
        <span v-if="account.status === 'authenticated'" class="account-email">{{ account.email }}</span>
        <v-btn v-if="account.status === 'authenticated'" size="small" variant="text" :disabled="account.busy" @click="signOut">{{ t('auth.logout') }}</v-btn>
        <label class="setting-label" for="language-select">{{ t('app.language') }}</label>
        <select id="language-select" :value="prefs.language" @change="prefs.setLanguage(($event.target as HTMLSelectElement).value as 'en' | 'es')">
          <option value="en">English</option><option value="es">Español</option>
        </select>
        <label class="setting-label" for="theme-select">{{ t('app.theme') }}</label>
        <select id="theme-select" :value="prefs.theme" @change="prefs.setTheme(($event.target as HTMLSelectElement).value as 'light' | 'dark')">
          <option value="dark">{{ t('app.dark') }}</option><option value="light">{{ t('app.light') }}</option>
        </select>
      </v-app-bar>
      <v-main id="main"><RouterView /></v-main>
    </template>
    <RouterView v-else />
  </v-app>
</template>
