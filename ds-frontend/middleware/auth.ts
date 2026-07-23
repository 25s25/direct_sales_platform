export default defineNuxtRouteMiddleware((to) => {
  // Allow access to login page without authentication
  if (to.path === '/login') {
    return
  }

  const isAdminRoute = to.path.startsWith('/admin')
  const tokenKey = isAdminRoute ? 'admin-token' : 'member-token'
  const token = localStorage.getItem(tokenKey)
  console.log('[Auth MW] Checking token for', to.path, ':', token ? `${token.substring(0, 20)}...` : '(no token)')
  if (!token) {
    console.log('[Auth MW] No token, redirecting to /login')
    return navigateTo('/login')
  }
})
