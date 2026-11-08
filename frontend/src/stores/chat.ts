import { defineStore } from 'pinia'

export interface ChatMessage { username: string; userId: string; firstMessage: boolean; message: string }
type ConnectionStatus = 'disconnected' | 'connecting' | 'connected' | 'error'

export const useChatStore = defineStore('chat', {
  state: () => ({ status: 'disconnected' as ConnectionStatus, messages: [] as ChatMessage[], socket: null as WebSocket | null }),
  actions: {
    connect() {
      if (this.socket && this.socket.readyState <= WebSocket.OPEN) return
      this.status = 'connecting'
      const socket = new WebSocket(`${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/websocket`)
      this.socket = socket
      socket.addEventListener('open', () => { if (this.socket === socket) this.status = 'connected' })
      socket.addEventListener('message', (event: MessageEvent<string>) => {
        try {
          const value = JSON.parse(event.data) as Partial<ChatMessage>
          if (typeof value.username === 'string' && typeof value.userId === 'string' && typeof value.firstMessage === 'boolean' && typeof value.message === 'string') {
            this.messages.push({ username: value.username, userId: value.userId, firstMessage: value.firstMessage, message: value.message })
          }
        } catch { /* Ignore malformed frames; render only the documented message shape. */ }
      })
      socket.addEventListener('close', () => { if (this.socket === socket) { this.status = 'disconnected'; this.socket = null } })
      socket.addEventListener('error', () => { if (this.socket === socket) this.status = 'error' })
    },
    disconnect() { this.socket?.close(); this.socket = null; this.status = 'disconnected' },
  },
})
