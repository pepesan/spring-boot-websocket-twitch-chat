<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { useAccountStore } from '../stores/account'
import { useChatStore } from '../stores/chat'

const { t } = useI18n()
const route = useRoute()
const account = useAccountStore()
const chat = useChatStore()
const mode = ref<'register' | 'login'>('register')
const registerEmail = ref('')
const registerPassword = ref('')
const loginEmail = ref('')
const loginPassword = ref('')
const verificationNotice = computed(() => {
  if (route.query.account === 'verified') return 'auth.verifySuccess'
  if (route.query.account === 'verification-failed') return 'auth.verifyFailure'
  return ''
})

async function submitRegistration() {
  if (await account.register(registerEmail.value, registerPassword.value)) {
    registerPassword.value = ''
    mode.value = 'login'
  }
}

async function submitLogin() {
  if (await account.login(loginEmail.value, loginPassword.value)) loginPassword.value = ''
}
</script>

<template>
  <main class="page-shell">
    <section v-if="account.status !== 'authenticated'" class="auth-panel">
      <div class="auth-steps">
        <span class="active">{{ t('auth.stepAccount') }}</span>
        <span>{{ t('auth.stepTwitch') }}</span>
      </div>
      <h1>{{ mode === 'register' ? t('auth.registerTitle') : t('auth.loginTitle') }}</h1>
      <p class="lead">{{ mode === 'register' ? t('auth.registerCopy') : t('auth.loginCopy') }}</p>

      <v-alert v-if="account.errorKey" type="error" variant="tonal" class="auth-alert">
        {{ t(`auth.${account.errorKey}`) }}
      </v-alert>
      <v-alert v-if="verificationNotice" :type="route.query.account === 'verified' ? 'success' : 'error'" variant="tonal" class="auth-alert">
        {{ t(verificationNotice) }}
      </v-alert>
      <v-alert v-if="account.registrationPending" type="info" variant="tonal" class="auth-alert">
        {{ t('auth.verifyPending') }}
      </v-alert>

      <form v-if="mode === 'register'" class="auth-form" @submit.prevent="submitRegistration">
        <label class="auth-field">
          <span>{{ t('auth.email') }}</span>
          <input v-model="registerEmail" type="email" name="register-email" autocomplete="email" maxlength="254" required>
        </label>
        <label class="auth-field">
          <span>{{ t('auth.password') }}</span>
          <input v-model="registerPassword" type="password" name="register-password" autocomplete="new-password" minlength="12" maxlength="1024" required>
        </label>
        <p class="subtle">{{ t('auth.registrationNote') }}</p>
        <v-btn color="primary" type="submit" :loading="account.busy" :disabled="account.busy">
          {{ t('auth.registerButton') }}
        </v-btn>
        <button class="auth-switch" type="button" @click="mode = 'login'; account.errorKey = ''">
          {{ t('auth.existingAccount') }}
        </button>
      </form>

      <form v-else class="auth-form" @submit.prevent="submitLogin">
        <label class="auth-field">
          <span>{{ t('auth.email') }}</span>
          <input v-model="loginEmail" type="email" name="login-email" autocomplete="username" maxlength="254" required>
        </label>
        <label class="auth-field">
          <span>{{ t('auth.password') }}</span>
          <input v-model="loginPassword" type="password" name="login-password" autocomplete="current-password" maxlength="1024" required>
        </label>
        <v-btn color="primary" type="submit" :loading="account.busy" :disabled="account.busy">
          {{ t('auth.loginButton') }}
        </v-btn>
        <button class="auth-switch" type="button" @click="mode = 'register'; account.errorKey = ''">
          {{ t('auth.newAccount') }}
        </button>
      </form>
    </section>

    <template v-else>
      <div class="auth-steps">
        <span class="complete">{{ t('auth.stepAccount') }} · {{ account.email }}</span>
        <span class="active">{{ t('auth.stepTwitch') }}</span>
      </div>
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
        <v-alert v-if="account.errorKey" type="error" variant="tonal">{{ t(`auth.${account.errorKey}`) }}</v-alert>
        <p class="subtle">{{ t('app.connectionNote') }}</p>
      </section>
      <section class="status-card" aria-live="polite">
        <div>
          <div class="eyebrow">{{ t('app.status') }}</div>
          <div class="status-row"><span class="status-dot" :class="chat.status" />{{ t(`app.${chat.status}`) }}</div>
        </div>
        <RouterLink class="text-link" to="/chat">{{ t('app.chat') }} →</RouterLink>
      </section>
      <section class="bottom-card">
        <div>
          <div class="eyebrow">OBS</div>
          <h2>{{ t('app.overlay') }}</h2>
          <p class="subtle">{{ t('app.overlayCopy') }}</p>
        </div>
        <RouterLink class="text-link" to="/embed/chat">{{ t('app.overlay') }} →</RouterLink>
      </section>
    </template>
  </main>
</template>
