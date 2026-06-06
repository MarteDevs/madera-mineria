import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'

export const useProveedoresStore = defineStore('proveedores', () => {
  const proveedores = ref([])
  const proveedorActual = ref(null)
  const contratos = ref([])
  const entregas = ref([])
  const alertasContratos = ref([])
  const loading = ref(false)
  const error = ref(null)

  async function fetchProveedores() {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.get('/api/proveedores')
      proveedores.value = data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error cargando proveedores'
    } finally {
      loading.value = false
    }
  }

  async function fetchProveedor(id) {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.get(`/api/proveedores/${id}`)
      proveedorActual.value = data
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error cargando proveedor'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function crearProveedor(proveedorData) {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.post('/api/proveedores', proveedorData)
      proveedores.value.unshift(data)
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error al registrar proveedor'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchContratos(proveedorId) {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.get(`/api/proveedores/${proveedorId}/contratos`)
      contratos.value = data
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error cargando contratos del proveedor'
    } finally {
      loading.value = false
    }
  }

  async function crearContrato(proveedorId, contratoData) {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.post(`/api/contratos/proveedor/${proveedorId}`, contratoData)
      contratos.value.unshift(data)
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error al registrar contrato'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function actualizarEstadoContrato(contratoId, estado) {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.put(`/api/contratos/${contratoId}/estado?estado=${estado}`)
      const idx = contratos.value.findIndex(c => c.id === contratoId)
      if (idx !== -1) contratos.value[idx] = data
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error al cambiar estado del contrato'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchEntregas(proveedorId) {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.get(`/api/proveedores/${proveedorId}/entregas`)
      entregas.value = data
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error cargando entregas del proveedor'
    } finally {
      loading.value = false
    }
  }

  async function registrarEntrega(proveedorId, entregaData) {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.post(`/api/proveedores/${proveedorId}/entregas`, entregaData)
      entregas.value.unshift(data)
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error al registrar entrega de madera'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchContratosProximosAVencer() {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.get('/api/proveedores/contratos/proximos-a-vencer')
      alertasContratos.value = data
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error cargando alertas de vencimiento'
    } finally {
      loading.value = false
    }
  }

  return {
    proveedores,
    proveedorActual,
    contratos,
    entregas,
    alertasContratos,
    loading,
    error,
    fetchProveedores,
    fetchProveedor,
    crearProveedor,
    fetchContratos,
    crearContrato,
    actualizarEstadoContrato,
    fetchEntregas,
    registrarEntrega,
    fetchContratosProximosAVencer
  }
})
