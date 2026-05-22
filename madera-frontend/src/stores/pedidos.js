import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'

export const usePedidosStore = defineStore('pedidos', () => {
  const pedidos = ref([])
  const pedidoActual = ref(null)
  const loading = ref(false)
  const error = ref(null)

  async function fetchPedidos() {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.get('/api/pedidos')
      pedidos.value = data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error cargando pedidos'
    } finally {
      loading.value = false
    }
  }

  async function fetchPedido(id) {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.get(`/api/pedidos/${id}`)
      pedidoActual.value = data
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error cargando detalle del pedido'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchPorMina(mina) {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.get(`/api/pedidos/mina/${mina}`)
      pedidos.value = data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error filtrando pedidos por mina'
    } finally {
      loading.value = false
    }
  }

  async function crearPedido(pedidoData) {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.post('/api/pedidos', pedidoData)
      pedidos.value.unshift(data)
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error al crear pedido'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function aprobarPedido(id, aprobadoPor) {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.put(
        `/api/pedidos/${id}/aprobar?aprobadoPor=${aprobadoPor}`
      )
      actualizarEnLista(data)
      if (pedidoActual.value && pedidoActual.value.id === id) {
        pedidoActual.value = data
      }
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error al aprobar pedido'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function rechazarPedido(id, motivo) {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.put(
        `/api/pedidos/${id}/rechazar?motivo=${motivo}`
      )
      actualizarEnLista(data)
      if (pedidoActual.value && pedidoActual.value.id === id) {
        pedidoActual.value = data
      }
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error al rechazar pedido'
      throw e
    } finally {
      loading.value = false
    }
  }

  function actualizarEnLista(pedidoActualizado) {
    const idx = pedidos.value.findIndex(p => p.id === pedidoActualizado.id)
    if (idx !== -1) pedidos.value[idx] = pedidoActualizado
  }

  return {
    pedidos,
    pedidoActual,
    loading,
    error,
    fetchPedidos,
    fetchPedido,
    fetchPorMina,
    crearPedido,
    aprobarPedido,
    rechazarPedido
  }
})
