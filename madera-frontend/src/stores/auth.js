import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/services/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || null)
  const usuario = ref(JSON.parse(localStorage.getItem('usuario') || 'null'))

  const isAuthenticated = computed(() => !!token.value)
  const rol = computed(() => usuario.value?.rol || '')
  const nombreCompleto = computed(() =>
    usuario.value ? `${usuario.value.nombre}` : '')

  // Helpers de rol
  const esAdmin = computed(() => rol.value === 'ROLE_ADMIN')
  const esAlmacen = computed(() => rol.value === 'ROLE_ALMACEN')
  const esCompras = computed(() => rol.value === 'ROLE_COMPRAS')
  const esTransporte = computed(() => rol.value === 'ROLE_TRANSPORTE')

  async function login(email, password) {
    const { data } = await api.post('/api/auth/login', { email, password })
    token.value = data.token
    usuario.value = {
      email: data.email,
      nombre: data.nombre,
      rol: data.rol
    }
    localStorage.setItem('token', data.token)
    localStorage.setItem('usuario', JSON.stringify(usuario.value))
    return data
  }

  function logout() {
    token.value = null
    usuario.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('usuario')
  }

  return {
    token, usuario, isAuthenticated,
    rol, nombreCompleto,
    esAdmin, esAlmacen, esCompras, esTransporte,
    login, logout
  }
})
