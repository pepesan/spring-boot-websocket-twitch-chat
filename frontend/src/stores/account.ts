import { defineStore } from 'pinia'

export type AccountStatus = 'loading' | 'anonymous' | 'authenticated'

interface CsrfResponse {
  headerName: string
  token: string
}

interface SessionResponse {
  email: string
}

export const useAccountStore = defineStore('account', {
  state: () => ({
    status: 'loading' as AccountStatus,
    email: '',
    busy: false,
    errorKey: '',
    registrationPending: false,
  }),
  actions: {
    async loadSession(): Promise<boolean> {
      try {
        const response = await fetch('/account/session', {
          credentials: 'same-origin',
          headers: { Accept: 'application/json' },
        })
        if (response.status === 401) {
          this.status = 'anonymous'
          this.email = ''
          return false
        }
        if (!response.ok) throw new Error('session-unavailable')

        const session = await response.json() as SessionResponse
        if (typeof session.email !== 'string' || !session.email) throw new Error('invalid-session')
        this.status = 'authenticated'
        this.email = session.email
        this.errorKey = ''
        return true
      } catch {
        this.status = 'anonymous'
        this.email = ''
        this.errorKey = 'backendError'
        return false
      }
    },

    async register(email: string, password: string): Promise<boolean> {
      this.busy = true
      this.errorKey = ''
      this.registrationPending = false
      try {
        const csrf = await this.fetchCsrf()
        const response = await fetch('/account/register', {
          method: 'POST',
          credentials: 'same-origin',
          headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
          body: JSON.stringify({ email, password }),
        })
        if (!response.ok) {
          this.errorKey = 'registerError'
          return false
        }
        this.registrationPending = true
        return true
      } catch {
        this.errorKey = 'backendError'
        return false
      } finally {
        this.busy = false
      }
    },

    async login(email: string, password: string): Promise<boolean> {
      this.busy = true
      this.errorKey = ''
      try {
        const csrf = await this.fetchCsrf()
        const response = await fetch('/account/login', {
          method: 'POST',
          credentials: 'same-origin',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded', [csrf.headerName]: csrf.token },
          body: new URLSearchParams({ username: email.trim().toLowerCase(), password }),
        })
        if (response.status === 401 || response.status === 403) {
          this.errorKey = 'loginError'
          return false
        }

        if (!await this.loadSession()) {
          this.errorKey = 'loginError'
          return false
        }
        await this.fetchCsrf()
        return true
      } catch {
        this.errorKey = 'backendError'
        return false
      } finally {
        this.busy = false
      }
    },

    async logout(): Promise<void> {
      this.busy = true
      this.errorKey = ''
      try {
        const csrf = await this.fetchCsrf()
        const response = await fetch('/account/logout', {
          method: 'POST',
          credentials: 'same-origin',
          headers: { [csrf.headerName]: csrf.token },
        })
        if (!response.ok) throw new Error('logout-failed')
        this.status = 'anonymous'
        this.email = ''
        this.registrationPending = false
      } catch {
        this.errorKey = 'backendError'
      } finally {
        this.busy = false
      }
    },

    async fetchCsrf(): Promise<CsrfResponse> {
      const response = await fetch('/account/csrf', {
        credentials: 'same-origin',
        headers: { Accept: 'application/json' },
      })
      if (!response.ok) throw new Error('csrf-unavailable')
      const token = await response.json() as Partial<CsrfResponse>
      if (typeof token.headerName !== 'string' || typeof token.token !== 'string') {
        throw new Error('invalid-csrf-response')
      }
      return { headerName: token.headerName, token: token.token }
    },
  },
})
