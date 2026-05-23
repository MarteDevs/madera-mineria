import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import { useToastStore } from '@/stores/toast'

export const useNotificacionesStore = defineStore('notificaciones', () => {
  const notificaciones = ref([])
  const pendientes = ref(0)
  const loading = ref(false)
  const error = ref(null)
  const initialized = ref(false)
  const eventSource = ref(null)

  const toastStore = useToastStore()

  async function fetchNotificaciones() {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.get('/api/notificaciones')
      notificaciones.value = data
      initialized.value = true
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error cargando notificaciones'
    } finally {
      loading.value = false
    }
  }

  async function fetchPendientesCount() {
    try {
      const { data } = await api.get('/api/notificaciones/pendientes/count')
      pendientes.value = typeof data === 'number' ? data : (data?.count || 0)
      initialized.value = true
    } catch (e) {
      console.error('Error al cargar cantidad de notificaciones pendientes', e)
    }
  }

  async function marcarComoLeida(id) {
    loading.value = true
    error.value = null
    try {
      await api.put(`/api/notificaciones/${id}/leer`)
      
      // Actualizar localmente
      const idx = notificaciones.value.findIndex(n => n.id === id)
      if (idx !== -1) {
        notificaciones.value[idx].estado = 'LEIDA'
      }
      
      // Decrementar contador de pendientes localmente
      pendientes.value = Math.max(0, pendientes.value - 1)
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error al marcar notificación como leída'
      throw e
    } finally {
      loading.value = false
    }
  }

  function conectarSSE() {
    if (eventSource.value) return // Ya conectado
    
    const token = localStorage.getItem('token')
    if (!token) return
    
    const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080'
    const sseUrl = `${baseUrl}/api/notificaciones/stream?token=${encodeURIComponent(token)}`
    
    console.log('Iniciando conexión SSE en notificaciones store...')
    eventSource.value = new EventSource(sseUrl)
    
    eventSource.value.onopen = () => {
      console.log('Conexión Server-Sent Events (SSE) establecida.')
    }
    
    eventSource.value.addEventListener('notificacion', (event) => {
      try {
        const notificacion = JSON.parse(event.data)
        console.log('Nueva notificación SSE recibida:', notificacion)
        
        // Evitar duplicados
        const yaExiste = notificaciones.value.some(n => n.id === notificacion.id)
        if (!yaExiste) {
          notificaciones.value.unshift(notificacion)
          pendientes.value += 1
          
          // Mostrar Toast flotante con desaparición automática
          toastStore.show({
            mensaje: notificacion.mensaje,
            tipo: 'info'
          })
        }
      } catch (err) {
        console.error('Error parseando datos de notificación SSE:', err)
      }
    })
    
    eventSource.value.onerror = (err) => {
      console.warn('Error detectado en canal SSE, reestableciendo conexión...', err)
      desconectarSSE()
      // Reintentar conexión en 5 segundos
      setTimeout(() => {
        // Solo reconectar si todavía hay un token (usuario no ha cerrado sesión)
        if (localStorage.getItem('token')) {
          conectarSSE()
        }
      }, 5000)
    }
  }

  function desconectarSSE() {
    if (eventSource.value) {
      eventSource.value.close()
      eventSource.value = null
      console.log('Conexión Server-Sent Events (SSE) cerrada.')
    }
  }

  return {
    notificaciones,
    pendientes,
    loading,
    error,
    fetchNotificaciones,
    fetchPendientesCount,
    marcarComoLeida,
    conectarSSE,
    desconectarSSE
  }
})
