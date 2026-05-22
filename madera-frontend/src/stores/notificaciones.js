import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'

export const useNotificacionesStore = defineStore('notificaciones', () => {
  const notificaciones = ref([])
  const pendientes = ref(0)
  const loading = ref(false)
  const error = ref(null)

  async function fetchNotificaciones() {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.get('/api/notificaciones')
      notificaciones.value = data
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
      
      // Recargar contador
      await fetchPendientesCount()
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error al marcar notificación como leída'
      throw e
    } finally {
      loading.value = false
    }
  }

  return {
    notificaciones,
    pendientes,
    loading,
    error,
    fetchNotificaciones,
    fetchPendientesCount,
    marcarComoLeida
  }
})
