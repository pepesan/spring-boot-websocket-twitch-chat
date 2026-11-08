import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAccountStore } from './account'

const jsonResponse = (body: unknown, status = 200) => new Response(JSON.stringify(body), {
  status,
  headers: { 'Content-Type': 'application/json' },
})

beforeEach(() => {
  localStorage.clear()
  setActivePinia(createPinia())
})

afterEach(() => vi.unstubAllGlobals())

describe('creator account session', () => {
  it('registers with CSRF protection but keeps the creator anonymous pending email verification', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ headerName: 'X-CSRF-TOKEN', token: 'csrf-value' }))
      .mockResolvedValueOnce(jsonResponse({ message: 'If the request can be completed, an email will be sent.' }, 202))
    vi.stubGlobal('fetch', fetchMock)

    const account = useAccountStore()
    account.status = 'anonymous'
    expect(await account.register('creator@example.test', 'long secure password')).toBe(true)

    const request = fetchMock.mock.calls[1][1] as RequestInit
    expect(request.method).toBe('POST')
    expect(request.credentials).toBe('same-origin')
    expect((request.headers as Record<string, string>)['X-CSRF-TOKEN']).toBe('csrf-value')
    expect(JSON.parse(request.body as string)).toEqual({ email: 'creator@example.test', password: 'long secure password' })
    expect(account.status).toBe('anonymous')
    expect(account.registrationPending).toBe(true)
    expect(localStorage.length).toBe(0)
  })

  it('creates a signed-in session only after the backend confirms the account', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ headerName: 'X-CSRF-TOKEN', token: 'login-csrf' }))
      .mockResolvedValueOnce(new Response(null, { status: 302 }))
      .mockResolvedValueOnce(jsonResponse({ email: 'creator@example.test' }))
      .mockResolvedValueOnce(jsonResponse({ headerName: 'X-CSRF-TOKEN', token: 'renewed-csrf' }))
    vi.stubGlobal('fetch', fetchMock)

    const account = useAccountStore()
    expect(await account.login('  CREATOR@EXAMPLE.TEST  ', 'long secure password')).toBe(true)

    const loginRequest = fetchMock.mock.calls[1][1] as RequestInit
    expect(loginRequest.credentials).toBe('same-origin')
    expect((loginRequest.headers as Record<string, string>)['X-CSRF-TOKEN']).toBe('login-csrf')
    const loginBody = loginRequest.body as URLSearchParams
    expect(loginBody.get('username')).toBe('creator@example.test')
    expect(loginBody.get('password')).toBe('long secure password')
    expect(account.status).toBe('authenticated')
    expect(account.email).toBe('creator@example.test')
    expect(localStorage.length).toBe(0)
  })
})
