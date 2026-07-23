import { defineStore } from 'pinia'

export type UserRole = 'member' | 'admin'

interface User {
  id: number | null
  username: string
  realName: string
  avatar: string
  permissions: string[]
}

interface AuthState {
  token: string
  user: User
  roleType: UserRole | null
}

const TOKEN_KEY_MEMBER = 'member-token'
const TOKEN_KEY_ADMIN = 'admin-token'
const USER_KEY_MEMBER = 'member-user'
const USER_KEY_ADMIN = 'admin-user'

export const getTokenKey = (roleType: UserRole | null) => {
  return roleType === 'admin' ? TOKEN_KEY_ADMIN : TOKEN_KEY_MEMBER
}

export const getUserKey = (roleType: UserRole | null) => {
  return roleType === 'admin' ? USER_KEY_ADMIN : USER_KEY_MEMBER
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: '',
    user: {
      id: null,
      username: '',
      realName: '',
      avatar: '',
      permissions: [],
    },
    roleType: null,
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    userInfo: (state) => state.user,
  },

  actions: {
    async memberLogin(phone: string, password?: string, code?: string) {
      const { post } = useApi()
      const payload: Record<string, string> = { phone }
      if (password) payload.password = password
      if (code) payload.code = code
      const res: any = await post('/api/member/login', payload)
      const data = res.data || res
      const token = data.token || data.accessToken || ''
      const user = {
        id: data.userId || data.id || null,
        username: data.username || data.phone || '',
        realName: data.realName || data.nickname || '',
        avatar: data.avatar || '',
        permissions: data.permissions || [],
      }
      this.roleType = 'member'
      this.setToken(token)
      this.user = user
      localStorage.setItem(getUserKey('member'), JSON.stringify(user))
    },

    async adminLogin(username: string, password: string) {
      const { post } = useApi()
      const res: any = await post('/api/auth/login', { username, password })
      const data = res.data || res
      const token = data.token || data.accessToken || ''
      const user = {
        id: data.userId || data.id || null,
        username: data.username || username,
        realName: data.realName || '',
        avatar: data.avatar || '',
        permissions: data.permissions || [],
      }
      this.roleType = 'admin'
      this.setToken(token)
      this.user = user
      localStorage.setItem(getUserKey('admin'), JSON.stringify(user))
    },

    setToken(token: string) {
      this.token = token
      const key = getTokenKey(this.roleType)
      if (token) {
        localStorage.setItem(key, token)
      } else {
        localStorage.removeItem(key)
      }
    },

    async logout() {
      try {
        const { post } = useApi()
        await post('/api/auth/logout')
      } catch {
        // ignore, still clear local state
      }
      this.token = ''
      this.user = { id: null, username: '', realName: '', avatar: '', permissions: [] }
      localStorage.removeItem(getTokenKey(this.roleType))
      localStorage.removeItem(getUserKey(this.roleType))
    },

    async fetchMemberUser() {
      const { get } = useApi()
      const res: any = await get('/api/member/info')
      const data = res.data || res
      const user = {
        id: data.id || data.userId || null,
        username: data.phone || data.username || '',
        realName: data.realName || data.nickname || '',
        avatar: data.avatar || '',
        permissions: data.permissions || [],
      }
      this.user = user
      localStorage.setItem(getUserKey('member'), JSON.stringify(user))
    },

    async fetchAdminUser() {
      const { get } = useApi()
      const res: any = await get('/api/auth/info')
      const data = res.data || res
      const user = {
        id: data.userId || data.id || null,
        username: data.username || '',
        realName: data.realName || '',
        avatar: data.avatar || '',
        permissions: data.permissions || [],
      }
      this.user = user
      localStorage.setItem(getUserKey('admin'), JSON.stringify(user))
    },

    initFromStorage(roleType: UserRole = 'member') {
      this.roleType = roleType
      const token = localStorage.getItem(getTokenKey(roleType))
      const userStr = localStorage.getItem(getUserKey(roleType))
      if (token) {
        this.token = token
      }
      if (userStr) {
        try {
          this.user = JSON.parse(userStr)
        } catch {
          // ignore parse error
        }
      }
    },
  },
})
