import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import { createVuetify } from 'vuetify'
import { VAlert, VApp, VAppBar, VAppBarTitle, VBtn, VMain, VSpacer } from 'vuetify/components'
import 'vuetify/styles'
import '@mdi/font/css/materialdesignicons.css'
import './style.css'
import App from './App.vue'
import router from './router'
import { messages } from './i18n'
import { usePreferencesStore } from './stores/preferences'

const pinia = createPinia()
const i18n = createI18n({ legacy: false, locale: 'en', fallbackLocale: 'en', messages })
const vuetify = createVuetify({
  components: { VAlert, VApp, VAppBar, VAppBarTitle, VBtn, VMain, VSpacer },
  theme: { defaultTheme: 'dark', themes: {
    dark: { dark: true, colors: { background: '#101116', surface: '#191b23', primary: '#b9f45b', secondary: '#a9a4ff', error: '#ff777e' } },
    light: { dark: false, colors: { background: '#f4f5f8', surface: '#ffffff', primary: '#557d16', secondary: '#635bba', error: '#b4232c' } },
  } },
})
const app = createApp(App)
app.use(pinia)
app.use(i18n)
app.use(vuetify)
app.use(router)
const preferences = usePreferencesStore(pinia)
i18n.global.locale.value = preferences.language
vuetify.theme.change(preferences.theme)
app.mount('#app')
