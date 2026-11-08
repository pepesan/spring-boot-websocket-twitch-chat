import { defineStore } from 'pinia'

export type Language = 'en' | 'es'
export type Theme = 'light' | 'dark'
const STORAGE_KEY = 'rebel-chat-preferences'

function loadPreferences(): { language: Language; theme: Theme } {
  try {
    const value = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '{}') as Record<string, unknown>
    return {
      language: value.language === 'es' ? 'es' : 'en',
      theme: value.theme === 'light' ? 'light' : 'dark',
    }
  } catch {
    return { language: 'en', theme: 'dark' }
  }
}

export const usePreferencesStore = defineStore('preferences', {
  state: () => loadPreferences(),
  actions: {
    setLanguage(language: Language) { this.language = language; this.persist() },
    setTheme(theme: Theme) { this.theme = theme; this.persist() },
    persist() { localStorage.setItem(STORAGE_KEY, JSON.stringify({ language: this.language, theme: this.theme })) },
  },
})
