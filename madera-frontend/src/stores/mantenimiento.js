import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'

export const useMantenimientoStore = defineStore('mantenimiento', () => {
  const vehiculos = ref([])
  const alertas = ref([])
  const loading = ref(false)
  const error = ref(null)

  async function fetchVehiculos() {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.get('/api/mantenimiento/vehiculos')
      vehiculos.value = data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error cargando vehículos'
    } finally {
      loading.value = false
    }
  }

  async function fetchVehiculoPorPlaca(placa) {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.get(`/api/mantenimiento/vehiculos/${placa}`)
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error cargando detalle del vehículo'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function registrarVehiculo(vehiculoData) {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.post('/api/mantenimiento/vehiculos', vehiculoData)
      vehiculos.value.unshift(data)
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error registrando vehículo'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchVehiculosQueRequierenMantenimiento() {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.get('/api/mantenimiento/vehiculos/requieren-mantenimiento')
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error cargando vehículos que requieren mantenimiento'
    } finally {
      loading.value = false
    }
  }

  async function registrarMantenimiento(vehiculoId, mantenimientoData) {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.post(`/api/mantenimiento/vehiculos/${vehiculoId}/mantenimiento`, mantenimientoData)
      
      // Actualizar el vehículo en la lista local
      const index = vehiculos.value.findIndex(v => v.id === vehiculoId)
      if (index !== -1) {
        // Obtenemos los detalles actualizados del vehículo
        const updatedVehiculo = await fetchVehiculoPorPlaca(data.placaVehiculo)
        vehiculos.value[index] = updatedVehiculo
      }
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error al registrar servicio de mantenimiento'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchHistorial(vehiculoId) {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.get(`/api/mantenimiento/vehiculos/${vehiculoId}/historial`)
      return data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error cargando historial de mantenimiento'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchAlertas() {
    loading.value = true
    error.value = null
    try {
      const { data } = await api.get('/api/mantenimiento/alertas')
      alertas.value = data
    } catch (e) {
      error.value = e.response?.data?.mensaje || 'Error cargando alertas de mantenimiento'
    } finally {
      loading.value = false
    }
  }

  return {
    vehiculos,
    alertas,
    loading,
    error,
    fetchVehiculos,
    fetchVehiculoPorPlaca,
    registrarVehiculo,
    fetchVehiculosQueRequierenMantenimiento,
    registrarMantenimiento,
    fetchHistorial,
    fetchAlertas
  }
})
