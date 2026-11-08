<script setup lang="ts">
import { computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { useTheme } from 'vuetify'
import { usePreferencesStore } from './stores/preferences'

const prefs = usePreferencesStore()
const { t, locale } = useI18n()
const route = useRoute()
const theme = useTheme()
const isOverlay = computed(() => route.meta.overlay === true)
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
          <RouterLink to="/chat">{{ t('app.chat') }}</RouterLink>
          <RouterLink to="/embed/chat">{{ t('app.overlay') }}</RouterLink>
        </nav>
        <v-spacer />
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
