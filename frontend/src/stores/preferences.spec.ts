import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { usePreferencesStore } from './preferences'

beforeEach(() => {
  localStorage.clear()
  setActivePinia(createPinia())
})

describe('preferences', () => {
  it('starts with dark English preferences and persists updates', () => {
    const preferences = usePreferencesStore()
    expect(preferences.language).toBe('en')
    expect(preferences.theme).toBe('dark')
    preferences.setLanguage('es')
    preferences.setTheme('light')
    expect(localStorage.getItem('rebel-chat-preferences')).toBe(JSON.stringify({ language: 'es', theme: 'light' }))
  })

  it('restores valid preferences from local storage', () => {
    localStorage.setItem('rebel-chat-preferences', JSON.stringify({ language: 'es', theme: 'light' }))
    const preferences = usePreferencesStore()
    expect(preferences.language).toBe('es')
    expect(preferences.theme).toBe('light')
  })

  it('falls back safely for malformed or unsupported stored values', () => {
    localStorage.setItem('rebel-chat-preferences', '{broken')
    const malformed = usePreferencesStore()
    expect(malformed.language).toBe('en')
    expect(malformed.theme).toBe('dark')
    setActivePinia(createPinia())
    localStorage.setItem('rebel-chat-preferences', JSON.stringify({ language: 'fr', theme: 'system' }))
    const unsupported = usePreferencesStore()
    expect(unsupported.language).toBe('en')
    expect(unsupported.theme).toBe('dark')
  })
})
