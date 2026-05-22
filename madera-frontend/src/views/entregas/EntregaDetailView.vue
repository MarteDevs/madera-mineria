<template>
  <div class="max-w-4xl mx-auto space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <RouterLink to="/entregas" class="text-xs text-stone-500 hover:text-stone-750 font-semibold flex items-center gap-1.5 mb-2">
          ⬅ Volver al listado de entregas
        </RouterLink>
        <h1 class="text-2xl font-bold text-stone-900">Hoja de Ruta #{{ entregaId }}</h1>
      </div>
      <div v-if="entrega" class="flex items-center gap-3">
        <BadgeEstado :estado="entrega.estado" class="text-sm px-3 py-1.5" />
      </div>
    </div>

    <!-- Error state -->
    <AlertMessage v-if="error" tipo="error">
      {{ error }}
    </AlertMessage>

    <AlertMessage v-if="successMsg" tipo="exito">
      {{ successMsg }}
    </AlertMessage>

    <!-- Loading -->
    <div v-if="loading" class="min-h-[300px] flex items-center justify-center card bg-white">
      <LoadingSpinner size="lg" class="text-amber-600" />
    </div>

    <!-- Detail Content -->
    <div v-else-if="entrega" class="grid grid-cols-1 md:grid-cols-3 gap-6 animate-fade-in">
      <!-- Info Card & Actions -->
      <div class="md:col-span-2 space-y-6">
        <!-- Delivery info -->
        <div class="card bg-white p-6 space-y-6 border border-stone-150 shadow-sm">
          <div>
            <h2 class="text-base font-bold text-stone-900 border-b border-stone-100 pb-3 mb-4 flex items-center gap-2">
              <span>📦 Información del Envío</span>
            </h2>
            <div class="grid grid-cols-2 gap-4 text-sm text-stone-600">
              <div>
                <p class="text-xs font-semibold text-stone-400 uppercase tracking-wider">Tipo de Madera</p>
                <p class="font-bold text-stone-850 capitalize mt-1">{{ entrega.tipoMadera || '—' }}</p>
              </div>
              <div>
                <p class="text-xs font-semibold text-stone-400 uppercase tracking-wider">Cantidad</p>
                <p class="font-bold text-stone-850 mt-1">{{ entrega.cantidad || 0 }} und</p>
              </div>
              <div>
                <p class="text-xs font-semibold text-stone-400 uppercase tracking-wider">Mina de Destino</p>
                <p class="font-medium text-stone-800 mt-1">{{ entrega.minaDestino || '—' }}</p>
              </div>
              <div>
                <p class="text-xs font-semibold text-stone-400 uppercase tracking-wider">Almacén de Origen</p>
                <p class="font-medium text-stone-800 mt-1">{{ entrega.almacenOrigen || 'Almacén Central' }}</p>
              </div>
              <div v-if="entrega.pedidoId">
                <p class="text-xs font-semibold text-stone-400 uppercase tracking-wider">Pedido Relacionado</p>
                <p class="mt-1">
                  <RouterLink :to="`/pedidos/${entrega.pedidoId}`" class="text-amber-650 hover:underline font-bold text-xs inline-flex items-center gap-1">
                    Ver Pedido #{{ entrega.pedidoId }} ➔
                  </RouterLink>
                </p>
              </div>
              <div v-if="entrega.fechaEnvio">
                <p class="text-xs font-semibold text-stone-400 uppercase tracking-wider">Fecha de Envío</p>
                <p class="text-stone-700 mt-1">{{ formatFecha(entrega.fechaEnvio) }}</p>
              </div>
            </div>
          </div>

          <div class="border-t border-stone-100 pt-6">
            <h2 class="text-base font-bold text-stone-900 mb-4 flex items-center gap-2">
              <span>🚛 Información de Logística</span>
            </h2>
            <div class="grid grid-cols-2 gap-4 text-sm text-stone-600">
              <div>
                <p class="text-xs font-semibold text-stone-400 uppercase tracking-wider">Transportista</p>
                <p class="font-bold text-stone-800 mt-1">{{ entrega.transportista || 'Sin asignar' }}</p>
              </div>
              <div>
                <p class="text-xs font-semibold text-stone-400 uppercase tracking-wider">Vehículo / Placa</p>
                <p class="font-medium text-stone-800 mt-1">{{ entrega.vehiculo || 'Sin asignar' }}</p>
              </div>
              <div v-if="entrega.recibidoPor" class="col-span-2 grid grid-cols-2 gap-4 pt-2">
                <div>
                  <p class="text-xs font-semibold text-stone-400 uppercase tracking-wider">Recibido por en Mina</p>
                  <p class="font-bold text-emerald-800 mt-1">{{ entrega.recibidoPor }}</p>
                </div>
                <div>
                  <p class="text-xs font-semibold text-stone-400 uppercase tracking-wider">Fecha de Recepción</p>
                  <p class="text-stone-700 mt-1">{{ formatFecha(entrega.fechaEntrega) || '—' }}</p>
                </div>
              </div>
              <div v-if="entrega.observaciones" class="col-span-2">
                <p class="text-xs font-semibold text-stone-400 uppercase tracking-wider">Observaciones / Incidencias</p>
                <p class="text-stone-700 bg-stone-50 p-3 rounded-lg border border-stone-100 mt-1 text-xs italic">
                  "{{ entrega.observaciones }}"
                </p>
              </div>
            </div>
          </div>
        </div>

        <!-- ACCIONES DE GESTIÓN LOGÍSTICA SEGÚN ESTADO Y ROL -->
        <!-- 1. Asignar transportista y vehículo (Si está PENDIENTE y el rol es ALMACEN, TRANSPORTE, ADMIN) -->
        <div v-if="puedeGestionar && (entrega.estado === 'PENDIENTE' || !entrega.transportista)" class="card bg-amber-50/30 border border-amber-200/60 p-6 space-y-4">
          <div class="flex items-center gap-2 text-amber-900">
            <span class="text-xl">📋</span>
            <div>
              <h3 class="font-bold text-sm">Asignar Personal y Transporte</h3>
              <p class="text-xs text-stone-500">Asigne el chofer responsable y el camión para proceder al despacho.</p>
            </div>
          </div>
          <form @submit.prevent="handleAsignar" class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label class="block text-xs font-semibold text-stone-700 mb-1">Nombre del Transportista *</label>
              <input
                v-model="formAsignacion.transportista"
                type="text"
                required
                placeholder="Ej. Juan Pérez Celis"
                class="input-field text-xs bg-white"
              />
            </div>
            <div>
              <label class="block text-xs font-semibold text-stone-700 mb-1">Vehículo / Placa *</label>
              <input
                v-model="formAsignacion.vehiculo"
                type="text"
                required
                placeholder="Ej. Volvo FMX - V4D-876"
                class="input-field text-xs bg-white"
              />
            </div>
            <div class="sm:col-span-2 flex justify-end">
              <button type="submit" :disabled="submitting" class="btn-primary text-xs py-2 px-5 font-bold bg-amber-600 hover:bg-amber-700 border-none">
                <span v-if="submitting">Asignando...</span>
                <span v-else>Asignar Logística ➔</span>
              </button>
            </div>
          </form>
        </div>

        <!-- 2. Marcar en Ruta / Despachar (Si está PREPARANDO y el rol es ALMACEN, TRANSPORTE, ADMIN) -->
        <div v-if="puedeGestionar && entrega.estado === 'PREPARANDO'" class="card bg-blue-50/20 border border-blue-200/50 p-6 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div class="space-y-1">
            <div class="flex items-center gap-2 text-blue-900">
              <span class="text-xl">🚚</span>
              <h3 class="font-bold text-sm">Cargamento Listo para Despacho</h3>
            </div>
            <p class="text-xs text-stone-500">Confirme que el cargamento de madera estructural está debidamente asegurado y que el vehículo inicia el viaje a mina.</p>
          </div>
          <button @click="handleDespachar" :disabled="submitting" class="btn-success text-xs py-2.5 px-6 font-bold bg-blue-600 hover:bg-blue-700 border-none flex-shrink-0">
            <span v-if="submitting">Registrando salida...</span>
            <span v-else>Iniciar Tránsito (En Ruta) ➔</span>
          </button>
        </div>

        <!-- 3. Confirmar Recepción (Si está EN_RUTA y el rol es ALMACEN, TRANSPORTE, ADMIN) -->
        <div v-if="puedeGestionar && entrega.estado === 'EN_RUTA'" class="card bg-emerald-50/20 border border-emerald-200/50 p-6 space-y-4">
          <div class="flex items-center gap-2 text-emerald-950">
            <span class="text-xl">✅</span>
            <div>
              <h3 class="font-bold text-sm">Confirmar Entrega en Destino</h3>
              <p class="text-xs text-stone-500">Confirme la recepción conforme del cargamento por parte de la superintendencia de mina.</p>
            </div>
          </div>
          <form @submit.prevent="handleConfirmar" class="space-y-4">
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label class="block text-xs font-semibold text-stone-700 mb-1">Recibido por en Mina *</label>
                <input
                  v-model="formConfirmacion.recibidoPor"
                  type="text"
                  required
                  placeholder="Ej. Ing. Carlos Mendoza (Jefe de Guardia)"
                  class="input-field text-xs bg-white"
                />
              </div>
              <div>
                <label class="block text-xs font-semibold text-stone-700 mb-1">Observaciones / Incidencias (Opcional)</label>
                <input
                  v-model="formConfirmacion.observaciones"
                  type="text"
                  placeholder="Ej. Recibido completo y sin rajaduras."
                  class="input-field text-xs bg-white"
                />
              </div>
            </div>
            <div class="flex justify-end">
              <button type="submit" :disabled="submitting" class="btn-success text-xs py-2 px-5 font-bold">
                <span v-if="submitting">Registrando recepción...</span>
                <span v-else>Confirmar Recepción Conforme ✔</span>
              </button>
            </div>
          </form>
        </div>
      </div>

      <!-- Timeline Panel -->
      <div class="space-y-6">
        <!-- Visual timeline generated dynamically based on state -->
        <div class="card bg-white p-6 border border-stone-150 shadow-sm">
          <h3 class="text-sm font-bold text-stone-900 mb-5 pb-2 border-b border-stone-100 flex items-center gap-1.5">
            <span>🕒 Estado del Viaje</span>
          </h3>
          <div class="relative pl-6 border-l-2 border-stone-150 space-y-6">
            <!-- 1. PENDIENTE -->
            <div class="relative">
              <div
                class="absolute -left-[31px] top-0 w-4 h-4 rounded-full border-2 flex items-center justify-center text-[8px] font-bold"
                :class="entrega.estado === 'PENDIENTE' ? 'bg-amber-500 border-amber-500 text-white animate-pulse' : 'bg-emerald-500 border-emerald-500 text-white'"
              >
                ✓
              </div>
              <div>
                <p class="text-xs font-bold" :class="entrega.estado === 'PENDIENTE' ? 'text-amber-600' : 'text-stone-800'">
                  Entrega Creada
                </p>
                <p class="text-[10px] text-stone-400">Automatizada por aprobación de pedido.</p>
              </div>
            </div>

            <!-- 2. PREPARANDO -->
            <div class="relative">
              <div
                class="absolute -left-[31px] top-0 w-4 h-4 rounded-full border-2 flex items-center justify-center text-[8px]"
                :class="entrega.estado === 'PREPARANDO'
                  ? 'bg-blue-500 border-blue-500 text-white animate-pulse font-bold'
                  : ['EN_RUTA', 'ENTREGADO'].includes(entrega.estado)
                    ? 'bg-emerald-500 border-emerald-500 text-white font-bold'
                    : 'bg-white border-stone-300 text-stone-400'"
              >
                <span v-if="['EN_RUTA', 'ENTREGADO'].includes(entrega.estado)">✓</span>
                <span v-else>2</span>
              </div>
              <div>
                <p class="text-xs font-bold" :class="entrega.estado === 'PREPARANDO' ? 'text-blue-600' : (['EN_RUTA', 'ENTREGADO'].includes(entrega.estado) ? 'text-stone-850' : 'text-stone-400')">
                  Asignación y Carga
                </p>
                <p class="text-[10px] text-stone-400">Asignación de chofer y preparación del cargamento.</p>
                <p v-if="entrega.transportista" class="text-[10px] text-amber-650 font-semibold mt-0.5">
                  Chofer: {{ entrega.transportista }}
                </p>
              </div>
            </div>

            <!-- 3. EN_RUTA -->
            <div class="relative">
              <div
                class="absolute -left-[31px] top-0 w-4 h-4 rounded-full border-2 flex items-center justify-center text-[8px]"
                :class="entrega.estado === 'EN_RUTA'
                  ? 'bg-indigo-500 border-indigo-500 text-white animate-pulse font-bold'
                  : entrega.estado === 'ENTREGADO'
                    ? 'bg-emerald-500 border-emerald-500 text-white font-bold'
                    : 'bg-white border-stone-300 text-stone-400'"
              >
                <span v-if="entrega.estado === 'ENTREGADO'">✓</span>
                <span v-else>3</span>
              </div>
              <div>
                <p class="text-xs font-bold" :class="entrega.estado === 'EN_RUTA' ? 'text-indigo-600' : (entrega.estado === 'ENTREGADO' ? 'text-stone-850' : 'text-stone-400')">
                  En Ruta (Tránsito)
                </p>
                <p class="text-[10px] text-stone-400">Cargamento viajando hacia la mina de destino.</p>
              </div>
            </div>

            <!-- 4. ENTREGADO -->
            <div class="relative">
              <div
                class="absolute -left-[31px] top-0 w-4 h-4 rounded-full border-2 flex items-center justify-center text-[8px]"
                :class="entrega.estado === 'ENTREGADO'
                  ? 'bg-emerald-500 border-emerald-500 text-white font-bold'
                  : 'bg-white border-stone-300 text-stone-400'"
              >
                4
              </div>
              <div>
                <p class="text-xs font-bold" :class="entrega.estado === 'ENTREGADO' ? 'text-emerald-600' : 'text-stone-400'">
                  Entregado
                </p>
                <p class="text-[10px] text-stone-400">Recepción verificada con firma de conformidad.</p>
                <p v-if="entrega.recibidoPor" class="text-[10px] text-emerald-700 font-semibold mt-0.5">
                  Recibió: {{ entrega.recibidoPor }}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useEntregasStore } from '@/stores/entregas'
import { useDialogStore } from '@/stores/dialog'
import BadgeEstado from '@/components/common/BadgeEstado.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import AlertMessage from '@/components/common/AlertMessage.vue'

const route = useRoute()
const authStore = useAuthStore()
const entregasStore = useEntregasStore()
const dialogStore = useDialogStore()

const entregaId = route.params.id
const entrega = ref(null)
const loading = ref(true)
const error = ref(null)
const successMsg = ref(null)
const submitting = ref(false)

const formAsignacion = reactive({
  transportista: '',
  vehiculo: ''
})

const formConfirmacion = reactive({
  recibidoPor: '',
  observaciones: ''
})

// Determina si el usuario actual tiene permisos para gestionar logística/despachos/entregas
const puedeGestionar = computed(() => {
  return authStore.esAdmin || authStore.esAlmacen || authStore.esTransporte
})

async function cargarDetalle() {
  loading.value = true
  error.value = null
  try {
    const data = await entregasStore.fetchEntrega(entregaId)
    entrega.value = data
    if (data) {
      formAsignacion.transportista = data.transportista || ''
      formAsignacion.vehiculo = data.vehiculo || ''
      formConfirmacion.recibidoPor = data.recibidoPor || ''
      formConfirmacion.observaciones = data.observaciones || ''
    }
  } catch (err) {
    console.error('Error cargando entrega:', err)
    error.value = err.response?.data?.mensaje || 'Error al recuperar los detalles de la entrega.'
  } finally {
    loading.value = false
  }
}

onMounted(cargarDetalle)

async function handleAsignar() {
  if (!formAsignacion.transportista.trim() || !formAsignacion.vehiculo.trim()) {
    dialogStore.alert({
      titulo: 'Campos Obligatorios',
      mensaje: 'Todos los campos son obligatorios.',
      tipo: 'warning'
    })
    return
  }
  submitting.value = true
  error.value = null
  successMsg.value = null
  try {
    const data = await entregasStore.asignarTransportista(entregaId, {
      transportista: formAsignacion.transportista.trim(),
      vehiculo: formAsignacion.vehiculo.trim()
    })
    entrega.value = data
    successMsg.value = 'Transportista y vehículo asignados con éxito.'
    setTimeout(() => { successMsg.value = null }, 4000)
  } catch (err) {
    error.value = err.response?.data?.mensaje || 'Error al asignar transportista.'
  } finally {
    submitting.value = false
  }
}

async function handleDespachar() {
  const confirmado = await dialogStore.confirm({
    titulo: 'Iniciar Ruta',
    mensaje: '¿Confirmar salida del vehículo e inicio del trayecto hacia la mina?',
    confirmLabel: 'Sí, Despachar',
    cancelLabel: 'Cancelar'
  })
  if (!confirmado) return

  submitting.value = true
  error.value = null
  successMsg.value = null
  try {
    const data = await entregasStore.marcarEnRuta(entregaId)
    entrega.value = data
    successMsg.value = 'Ruta iniciada. El envío ahora se encuentra en tránsito.'
    setTimeout(() => { successMsg.value = null }, 4000)
  } catch (err) {
    error.value = err.response?.data?.mensaje || 'Error al despachar el envío.'
  } finally {
    submitting.value = false
  }
}

async function handleConfirmar() {
  if (!formConfirmacion.recibidoPor.trim()) {
    dialogStore.alert({
      titulo: 'Campo Requerido',
      mensaje: 'Debe indicar quién recibe el cargamento en la mina.',
      tipo: 'warning'
    })
    return
  }
  submitting.value = true
  error.value = null
  successMsg.value = null
  try {
    const data = await entregasStore.confirmarRecepcion(entregaId, {
      recibidoPor: formConfirmacion.recibidoPor.trim(),
      observaciones: formConfirmacion.observaciones.trim()
    })
    entrega.value = data
    successMsg.value = 'Recepción confirmada satisfactoriamente. Entrega finalizada.'
    setTimeout(() => { successMsg.value = null }, 4000)
  } catch (err) {
    error.value = err.response?.data?.mensaje || 'Error al confirmar recepción.'
  } finally {
    submitting.value = false
  }
}

function formatFecha(fecha) {
  if (!fecha) return ''
  return new Date(fecha).toLocaleDateString('es-PE', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>
