import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'

export const useInventarioStore = defineStore('inventario', () => {
  const maderas = ref([])
  const maderaActual = ref(null)
  const movimientos = ref([])
  const loading = ref(false)
  const error = ref(null)

  async function fetchMaderas() {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.get('/api/inventario')
      maderas.value = data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error cargando inventario'
    } finally {
      loading.value = false
    }
  }

  async function fetchMadera(id) {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.get(`/api/inventario/${id}`)
      maderaActual.value = data
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error cargando madera'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchMovimientos(id) {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.get(`/api/inventario/${id}/movimientos`)
      movimientos.value = data
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error cargando movimientos de stock'
    } finally {
      loading.value = false
    }
  }

  async function crearMadera(maderaData) {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.post('/api/inventario', maderaData)
      maderas.value.unshift(data)
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error al agregar madera'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function actualizarMadera(id, maderaData) {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.put(`/api/inventario/${id}`, maderaData)
      const idx = maderas.value.findIndex(m => m.id === id)
      if (idx !== -1) maderas.value[idx] = data
      if (maderaActual.value && maderaActual.value.id === id) maderaActual.value = data
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error al actualizar madera'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function registrarEntrada(id, { cantidad, motivo, responsable }) {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.post(
        `/api/inventario/${id}/entrada?cantidad=${cantidad}&motivo=${motivo}&responsable=${responsable}`
      )
      const idx = maderas.value.findIndex(m => m.id === id)
      if (idx !== -1) maderas.value[idx] = data
      if (maderaActual.value && maderaActual.value.id === id) maderaActual.value = data
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error al registrar entrada de stock'
      throw e
    } finally {
      loading.value = false
    }
  }

  return {
    maderas,
    maderaActual,
    movimientos,
    loading,
    error,
    fetchMaderas,
    fetchMadera,
    fetchMovimientos,
    crearMadera,
    actualizarMadera,
    registrarEntrada
  }
})
