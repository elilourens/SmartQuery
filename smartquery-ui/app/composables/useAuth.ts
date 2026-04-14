const API = 'http://localhost:8080'

export const useAuth = () => {
  const accessToken = useCookie<string | null>('access_token', { default: () => null })
  const refreshToken = useCookie<string | null>('refresh_token', { default: () => null })

  const isLoggedIn = computed(() => !!accessToken.value)

  async function register(email: string, password: string) {
    return $fetch(`${API}/api/auth/register`, {
      method: 'POST',
      body: { email, password }
    })
  }

  async function login(email: string, password: string) {
    const res = await $fetch<{ accessToken: string; refreshToken: string }>(`${API}/api/auth/login`, {
      method: 'POST',
      body: { email, password }
    })
    accessToken.value = res.accessToken
    refreshToken.value = res.refreshToken
    return res
  }

  async function refresh() {
    const res = await $fetch<{ accessToken: string; refreshToken: string }>(`${API}/api/auth/refresh`, {
      method: 'POST',
      body: { refreshToken: refreshToken.value }
    })
    accessToken.value = res.accessToken
    refreshToken.value = res.refreshToken
    return res
  }

  function logout() {
    accessToken.value = null
    refreshToken.value = null
  }

  return { accessToken, refreshToken, isLoggedIn, register, login, refresh, logout }
}
