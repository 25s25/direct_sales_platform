import axios from 'axios'

interface ApiResult<T = any> {
  code: number
  data: T
  message?: string
}

function getTokenKeyByUrl(url: string): string {
  if (url.startsWith('/api/system/') || url.startsWith('/api/auth/')) return 'admin-token'
  if (url.startsWith('/api/product/') || url.startsWith('/api/category/')) return 'admin-token'
  if (url.startsWith('/api/oss/admin/')) return 'admin-token'
  if (url.startsWith('/api/member/level')) return 'admin-token'
  if (url.startsWith('/api/member/page') || url.startsWith('/api/member/admin')) return 'admin-token'
  if (url.startsWith('/api/report/dashboard')) return 'admin-token'
  if (url.startsWith('/api/report/sales') || url.startsWith('/api/report/member') || url.startsWith('/api/report/bonus')) return 'admin-token'
  if (url.startsWith('/api/bonus/plan/')) return 'admin-token'
  if (url.startsWith('/api/bonus/record/page') || url.startsWith('/api/bonus/record/grant')) return 'admin-token'
  if (url.startsWith('/api/order/admin/')) return 'admin-token'
  if (url.startsWith('/api/order/return/page') || url.startsWith('/api/order/return/audit')) return 'admin-token'
  if (url.startsWith('/api/finance/withdraw/page') || url.startsWith('/api/finance/withdraw/') && (url.includes('/audit') || url.includes('/grant'))) return 'admin-token'
  if (url.startsWith('/api/finance/wallet/logs') && !url.startsWith('/api/finance/wallet/logs/my')) return 'admin-token'
  if (url.startsWith('/api/member/')) return 'member-token'
  if (url.startsWith('/api/order/')) return 'member-token'
  if (url.startsWith('/api/pay/')) return 'member-token'
  if (url.startsWith('/api/finance/')) return 'member-token'
  if (url.startsWith('/api/bonus/')) return 'member-token'
  if (url.startsWith('/api/report/')) return 'member-token'
  if (url.startsWith('/api/oss/')) return 'member-token'
  return 'member-token'
}

export function useApi() {
  const config = useRuntimeConfig()
  const router = useRouter()

  const instance = axios.create({
    baseURL: import.meta.env.DEV ? '' : (config.public.apiBase as string),
    timeout: 15000,
    headers: {
      'Content-Type': 'application/json',
    },
  })

  // Request interceptor - add token
  instance.interceptors.request.use(
    (config) => {
      const tokenKey = getTokenKeyByUrl(config.url || '')
      const token = localStorage.getItem(tokenKey)
      if (token) {
        config.headers['token'] = token
      }
      return config
    },
    (error) => {
      return Promise.reject(error)
    }
  )

  // Response interceptor - handle business code & 401
  instance.interceptors.response.use(
    (response) => {
      const data = response.data
      if (data && data.code !== undefined && data.code !== 200) {
        return Promise.reject(new Error(data.message || '请求失败'))
      }
      return data
    },
    (error) => {
      if (error.response?.status === 401) {
        localStorage.removeItem('member-token')
        localStorage.removeItem('member-user')
        localStorage.removeItem('admin-token')
        localStorage.removeItem('admin-user')
        router.push('/login')
      }
      const message = error.response?.data?.message || error.message || '请求失败'
      return Promise.reject(new Error(message))
    }
  )

  const get = <T = any>(url: string, params?: Record<string, any>, config?: Record<string, any>): Promise<ApiResult<T>> => {
    return instance.get(url, { params, ...config })
  }

  const post = <T = any>(url: string, data?: Record<string, any>, config?: Record<string, any>): Promise<ApiResult<T>> => {
    return instance.post(url, data, config)
  }

  const put = <T = any>(url: string, data?: Record<string, any>, config?: Record<string, any>): Promise<ApiResult<T>> => {
    return instance.put(url, data, config)
  }

  const del = <T = any>(url: string, params?: Record<string, any>, config?: Record<string, any>): Promise<ApiResult<T>> => {
    return instance.delete(url, { params, ...config })
  }

  const upload = <T = any>(url: string, formData: FormData, config?: Record<string, any>): Promise<ApiResult<T>> => {
    return instance.post(url, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      ...config,
    })
  }

  return { get, post, put, del, upload }
}
