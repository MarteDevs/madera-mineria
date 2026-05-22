import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'

export const useEntregasStore = defineStore('entregas', () => {
  const entregas = ref([])
  const entregaActual = ref(null)
  const loading = ref(false)
  const error = ref(null)

  async function fetchEntregas() {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.get('/api/entregas')
      entregas.value = data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error cargando entregas'
    } finally {
      loading.value = false
    }
  }

  async function fetchEntrega(id) {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.get(`/api/entregas/${id}`)
      entregaActual.value = data
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error cargando detalle de entrega'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchPorPedido(pedidoId) {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.get(`/api/entregas/pedido/${pedidoId}`)
      entregaActual.value = data
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error cargando entrega del pedido'
    } finally {
      loading.value = false
    }
  }

  async function asignarTransportista(id, { transportista, vehiculo }) {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.put(
        `/api/entregas/${id}/asignar-transportista?transportista=${transportista}&vehiculo=${vehiculo}`
      )
      actualizarEnLista(data)
      if (entregaActual.value && entregaActual.value.id === id) entregaActual.value = data
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error al asignar transportista'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function marcarEnRuta(id) {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.put(`/api/entregas/${id}/en-ruta`)
      actualizarEnLista(data)
      if (entregaActual.value && entregaActual.value.id === id) entregaActual.value = data
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error al poner entrega en ruta'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function confirmarRecepcion(id, { recibidoPor, observaciones }) {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.put(
        `/api/entregas/${id}/confirmar-recepcion?recibidoPor=${recibidoPor}&observaciones=${observaciones || ''}`
      )
      actualizarEnLista(data)
      if (entregaActual.value && entregaActual.value.id === id) entregaActual.value = data
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error al confirmar recepción de entrega'
      throw e
    } finally {
      loading.value = false
    }
  }

  function actualizarEnLista(entregaActualizada) {
    const idx = entregas.value.findIndex(e => e.id === entregaActualizada.id)
    if (idx !== -1) entregas.value[idx] = entregaActualizada
  }

  return {
    entregas,
    entregaActual,
    loading,
    error,
    fetchEntregas,
    fetchEntrega,
    fetchPorPedido,
    asignarTransportista,
    marcarEnRuta,
    confirmarRecepcion
  }
})
