import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'

export const useReportesStore = defineStore('reportes', () => {
  const dashboard = ref(null)
  const stock = ref(null)
  const pedidos = ref(null)
  const entregas = ref(null)
  const historial = ref([])
  const loading = ref(false)
  const error = ref(null)

  async function fetchDashboard() {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.get('/api/reportes/dashboard')
      dashboard.value = data
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error cargando dashboard de reportes'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchStock(mina = '') {
    loading.value = true
    error.value = null
    try {
      const params = mina ? `?mina=${mina}` : ''
      const { data } = await api.get(`/api/reportes/stock${params}`)
      stock.value = data
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error generando reporte de stock'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchPedidos(mina = '', desde = '', hasta = '') {
    loading.value = true
    error.value = null
    try {
      const queryParams = new URLSearchParams()
      if (mina) queryParams.append('mina', mina)
      if (desde) queryParams.append('desde', desde)
      if (hasta) queryParams.append('hasta', hasta)
      
      const { data } = await api.get(`/api/reportes/pedidos?${queryParams.toString()}`)
      pedidos.value = data
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error generando reporte de pedidos'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchEntregas(mina = '') {
    loading.value = true
    error.value = null
    try {
      const params = mina ? `?mina=${mina}` : ''
      const { data } = await api.get(`/api/reportes/entregas${params}`)
      entregas.value = data
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error generando reporte de entregas'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchHistorial() {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.get('/api/reportes/historial')
      historial.value = data
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error cargando historial de reportes'
    } finally {
      loading.value = false
    }
  }

  return {
    dashboard,
    stock,
    pedidos,
    entregas,
    historial,
    loading,
    error,
    fetchDashboard,
    fetchStock,
    fetchPedidos,
    fetchEntregas,
    fetchHistorial
  }
})
