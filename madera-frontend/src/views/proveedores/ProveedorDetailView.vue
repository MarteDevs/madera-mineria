<template>
  <div class="space-y-6">
    <!-- Breadcrumbs / Back button -->
    <div class="flex items-center gap-2 text-sm text-stone-500">
      <RouterLink to="/proveedores" class="hover:text-stone-900 transition-colors">Proveedores</RouterLink>
      <span>&gt;</span>
      <span class="text-stone-900 font-medium">{{ proveedorStore.proveedorActual?.razonSocial || 'Detalle' }}</span>
    </div>

    <!-- Alert de errores del store -->
    <AlertMessage v-if="proveedorStore.error" tipo="error">
      {{ proveedorStore.error }}
    </AlertMessage>

    <!-- Loading spinner general -->
    <div v-if="loadingProveedor" class="min-h-[350px] flex items-center justify-center card bg-white">
      <LoadingSpinner size="lg" class="text-amber-600" />
    </div>

    <div v-else-if="proveedorStore.proveedorActual" class="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
      <!-- Ficha del Proveedor (Columna izquierda) -->
      <div class="lg:col-span-1 space-y-6">
        <div class="card p-6 bg-white border border-stone-150 shadow-sm relative overflow-hidden">
          <div class="absolute top-0 right-0 left-0 h-1.5 bg-amber-600"></div>

          <div class="flex items-center justify-between mt-2">
            <span class="text-xs font-mono bg-stone-100 text-stone-600 px-2 py-0.5 rounded">RUC {{ proveedorStore.proveedorActual.ruc }}</span>
            <span
              class="px-2 py-0.5 rounded-full text-[10px] font-bold"
              :class="proveedorStore.proveedorActual.activo ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-rose-800'"
            >
              {{ proveedorStore.proveedorActual.activo ? 'ACTIVO' : 'INACTIVO' }}
            </span>
          </div>

          <h2 class="text-xl font-bold text-stone-900 mt-4 leading-snug">{{ proveedorStore.proveedorActual.razonSocial }}</h2>
          <p v-if="proveedorStore.proveedorActual.nombreComercial" class="text-stone-400 text-sm italic">{{ proveedorStore.proveedorActual.nombreComercial }}</p>

          <hr class="border-stone-100 my-4" />

          <div class="space-y-3.5 text-xs">
            <div>
              <span class="block text-stone-400 font-bold uppercase tracking-wider mb-0.5">Ciudad</span>
              <span class="text-stone-800 font-medium capitalize">{{ proveedorStore.proveedorActual.ciudad || 'No especificada' }}</span>
            </div>
            <div>
              <span class="block text-stone-400 font-bold uppercase tracking-wider mb-0.5">Dirección</span>
              <span class="text-stone-800 font-medium">{{ proveedorStore.proveedorActual.direccion || 'No especificada' }}</span>
            </div>
            <div>
              <span class="block text-stone-400 font-bold uppercase tracking-wider mb-0.5">Representante</span>
              <span class="text-stone-800 font-medium">{{ proveedorStore.proveedorActual.contactoNombre || 'No especificado' }}</span>
            </div>
            <div>
              <span class="block text-stone-400 font-bold uppercase tracking-wider mb-0.5">Teléfono</span>
              <span class="text-stone-800 font-medium">{{ proveedorStore.proveedorActual.contactoTelefono || 'No especificado' }}</span>
            </div>
            <div>
              <span class="block text-stone-400 font-bold uppercase tracking-wider mb-0.5">Correo Electrónico</span>
              <span class="text-stone-800 font-medium font-mono text-stone-650">{{ proveedorStore.proveedorActual.contactoEmail || 'No especificado' }}</span>
            </div>
            <div>
              <span class="block text-stone-400 font-bold uppercase tracking-wider mb-0.5">Fecha Registro</span>
              <span class="text-stone-500">{{ formatFecha(proveedorStore.proveedorActual.fechaRegistro) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Contratos y Entregas (Columna derecha) -->
      <div class="lg:col-span-2 space-y-6">
        <!-- Tabs bar -->
        <div class="flex border-b border-stone-200 bg-stone-50/50 p-1.5 rounded-xl gap-2">
          <button
            @click="tabActiva = 'contratos'"
            class="flex-1 py-2.5 px-4 font-bold text-sm rounded-lg transition-all"
            :class="tabActiva === 'contratos' ? 'bg-white text-stone-900 shadow-sm border border-stone-150' : 'text-stone-500 hover:text-stone-900'"
          >
            📋 Contratos ({{ proveedorStore.contratos.length }})
          </button>
          <button
            @click="tabActiva = 'entregas'"
            class="flex-1 py-2.5 px-4 font-bold text-sm rounded-lg transition-all"
            :class="tabActiva === 'entregas' ? 'bg-white text-stone-900 shadow-sm border border-stone-150' : 'text-stone-500 hover:text-stone-900'"
          >
            📥 Entregas Registradas ({{ proveedorStore.entregas.length }})
          </button>
        </div>

        <!-- Seccion Contratos -->
        <div v-if="tabActiva === 'contratos'" class="space-y-4">
          <div class="flex items-center justify-between">
            <h3 class="text-lg font-bold text-stone-950">Historial de Contratos</h3>
            <button
              v-if="authStore.esAdmin"
              @click="abrirNuevoContratoModal"
              class="btn-primary py-1.5 px-3 text-xs"
            >
              + Nuevo Contrato
            </button>
          </div>

          <div class="card p-0 overflow-hidden bg-white border border-stone-150">
            <div class="overflow-x-auto">
              <table class="w-full text-left text-xs border-collapse">
                <thead>
                  <tr class="bg-stone-50 border-b border-stone-200">
                    <th class="table-header py-2">ID</th>
                    <th class="table-header py-2">Madera</th>
                    <th class="table-header py-2">Precio Pactado</th>
                    <th class="table-header py-2">Rango Cantidades</th>
                    <th class="table-header py-2">Vigencia</th>
                    <th class="table-header py-2">Estado</th>
                    <th v-if="authStore.esAdmin" class="table-header py-2 text-right">Acción</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-stone-100">
                  <tr v-if="proveedorStore.contratos.length === 0">
                    <td colspan="7" class="p-8 text-center text-stone-400 italic">
                      No hay contratos registrados para este proveedor.
                    </td>
                  </tr>
                  <tr v-for="c in proveedorStore.contratos" :key="c.id" class="hover:bg-stone-50/50">
                    <td class="table-cell font-semibold text-stone-500">#{{ c.id }}</td>
                    <td class="table-cell">
                      <span class="font-bold text-stone-850 capitalize">{{ c.tipoMadera }}</span>
                      <span class="text-[10px] text-stone-400 block mt-0.5">Unidad: {{ c.unidad }}</span>
                    </td>
                    <td class="table-cell font-bold text-stone-800">S/. {{ c.precioPactado.toFixed(2) }}</td>
                    <td class="table-cell text-stone-600">
                      Min: {{ c.cantidadMinima || '-' }} / Max: {{ c.cantidadMaxima || '-' }}
                    </td>
                    <td class="table-cell text-stone-500">
                      {{ c.fechaInicio }} al {{ c.fechaFin }}
                    </td>
                    <td class="table-cell">
                      <span
                        class="px-2 py-0.5 rounded-full text-[9px] font-bold"
                        :class="formatEstadoContratoClase(c.estado)"
                      >
                        {{ c.estado }}
                      </span>
                    </td>
                    <td v-if="authStore.esAdmin" class="table-cell text-right">
                      <select
                        :value="c.estado"
                        @change="cambiarEstadoContrato(c.id, $event.target.value)"
                        class="border rounded px-1.5 py-0.5 text-[11px] bg-white border-stone-300 text-stone-700 focus:outline-none focus:ring-1 focus:ring-amber-500"
                      >
                        <option value="VIGENTE">VIGENTE</option>
                        <option value="VENCIDO">VENCIDO</option>
                        <option value="SUSPENDIDO">SUSPENDIDO</option>
                      </select>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- Seccion Entregas -->
        <div v-if="tabActiva === 'entregas'" class="space-y-4">
          <div class="flex items-center justify-between">
            <h3 class="text-lg font-bold text-stone-950">Historial de Entregas de Madera</h3>
            <button
              v-if="authStore.esAdmin || authStore.esAlmacen"
              @click="abrirNuevaEntregaModal"
              class="btn-primary py-1.5 px-3 text-xs bg-amber-700 hover:bg-amber-800"
            >
              📥 Registrar Entrega
            </button>
          </div>

          <div class="card p-0 overflow-hidden bg-white border border-stone-150">
            <div class="overflow-x-auto">
              <table class="w-full text-left text-xs border-collapse">
                <thead>
                  <tr class="bg-stone-50 border-b border-stone-200">
                    <th class="table-header py-2">ID</th>
                    <th class="table-header py-2">Fecha</th>
                    <th class="table-header py-2">Madera</th>
                    <th class="table-header py-2">Cantidad</th>
                    <th class="table-header py-2">Monto Total</th>
                    <th class="table-header py-2">Guía Remisión</th>
                    <th class="table-header py-2">Stock Sync</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-stone-100">
                  <tr v-if="proveedorStore.entregas.length === 0">
                    <td colspan="7" class="p-8 text-center text-stone-400 italic">
                      No hay entregas registradas.
                    </td>
                  </tr>
                  <tr v-for="e in proveedorStore.entregas" :key="e.id" class="hover:bg-stone-50/50">
                    <td class="table-cell font-semibold text-stone-500">#{{ e.id }}</td>
                    <td class="table-cell text-stone-500">{{ formatFecha(e.fechaEntrega) }}</td>
                    <td class="table-cell capitalize">
                      <span class="font-bold text-stone-850">{{ e.tipoMadera }}</span>
                      <span class="text-[10px] text-stone-400 block mt-0.5">Precio: S/. {{ e.precioUnitario.toFixed(2) }}</span>
                    </td>
                    <td class="table-cell font-bold text-stone-800">
                      {{ e.cantidadEntregada }} <span class="text-[10px] font-normal text-stone-400 lowercase">{{ e.unidad }}</span>
                    </td>
                    <td class="table-cell font-extrabold text-stone-900">S/. {{ e.montoTotal.toFixed(2) }}</td>
                    <td class="table-cell text-stone-600 font-mono text-[10px]">{{ e.guiaRemision }}</td>
                    <td class="table-cell">
                      <span
                        class="inline-flex items-center gap-1 font-bold text-[9px] px-1.5 py-0.5 rounded"
                        :class="e.stockActualizado ? 'bg-emerald-50 text-emerald-700 border border-emerald-250' : 'bg-rose-50 text-rose-700 border border-rose-250'"
                      >
                        {{ e.stockActualizado ? '✓ SYNC' : '✗ FALLÓ' }}
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal Nuevo Contrato (Inline) -->
    <div
      v-if="modalContrato"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-950/60 backdrop-blur-md"
    >
      <div class="bg-white border border-stone-200 w-full max-w-md rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[95vh] text-left">
        <div class="h-1.5 w-full bg-amber-600"></div>
        <div class="px-6 pt-5 pb-3 border-b border-stone-100 flex items-center justify-between">
          <h3 class="text-base font-bold text-stone-900">Crear Contrato de Suministro</h3>
          <button @click="cerrarContratoModal" class="text-stone-400 hover:text-stone-700 text-xl font-bold">&times;</button>
        </div>

        <form @submit.prevent="guardarContrato">
          <div class="px-6 py-4 space-y-4 max-h-[60vh] overflow-y-auto text-xs">
            <div class="grid grid-cols-2 gap-4">
              <!-- Tipo Madera -->
              <div>
                <label class="block font-bold text-stone-600 uppercase mb-1">Tipo de Madera *</label>
                <select v-model="formContrato.tipoMadera" required class="input-field py-1.5">
                  <option value="">Seleccione...</option>
                  <option value="eucalipto">Eucalipto</option>
                  <option value="pino">Pino</option>
                  <option value="roble">Roble</option>
                  <option value="lupuna">Lupuna</option>
                </select>
              </div>

              <!-- Unidad -->
              <div>
                <label class="block font-bold text-stone-600 uppercase mb-1">Unidad *</label>
                <select v-model="formContrato.unidad" required class="input-field py-1.5">
                  <option value="m3">m3 (Metro Cúbico)</option>
                  <option value="metro_lineal">Metro Lineal</option>
                  <option value="unidad">Unidades</option>
                </select>
              </div>

              <!-- Precio Pactado -->
              <div>
                <label class="block font-bold text-stone-600 uppercase mb-1">Precio Pactado (S/.) *</label>
                <input
                  v-model="formContrato.precioPactado"
                  type="number"
                  step="0.01"
                  min="0"
                  required
                  class="input-field py-1.5"
                />
              </div>

              <!-- Estado -->
              <div>
                <label class="block font-bold text-stone-600 uppercase mb-1">Estado inicial</label>
                <select v-model="formContrato.estado" class="input-field py-1.5">
                  <option value="VIGENTE">VIGENTE</option>
                  <option value="SUSPENDIDO">SUSPENDIDO</option>
                </select>
              </div>

              <!-- Cantidad Minima -->
              <div>
                <label class="block font-bold text-stone-600 uppercase mb-1">Cant. Mínima</label>
                <input
                  v-model="formContrato.cantidadMinima"
                  type="number"
                  min="0"
                  class="input-field py-1.5"
                />
              </div>

              <!-- Cantidad Maxima -->
              <div>
                <label class="block font-bold text-stone-600 uppercase mb-1">Cant. Máxima</label>
                <input
                  v-model="formContrato.cantidadMaxima"
                  type="number"
                  min="0"
                  class="input-field py-1.5"
                />
              </div>

              <!-- Fecha Inicio -->
              <div>
                <label class="block font-bold text-stone-600 uppercase mb-1">Fecha Inicio *</label>
                <input
                  v-model="formContrato.fechaInicio"
                  type="date"
                  required
                  class="input-field py-1.5"
                />
              </div>

              <!-- Fecha Fin -->
              <div>
                <label class="block font-bold text-stone-600 uppercase mb-1">Fecha Fin *</label>
                <input
                  v-model="formContrato.fechaFin"
                  type="date"
                  required
                  class="input-field py-1.5"
                />
              </div>
            </div>

            <!-- Observaciones -->
            <div>
              <label class="block font-bold text-stone-600 uppercase mb-1">Observaciones</label>
              <textarea
                v-model="formContrato.observaciones"
                rows="2"
                class="input-field py-1.5"
                placeholder="Detalles adicionales del contrato..."
              ></textarea>
            </div>
          </div>

          <div class="px-6 py-4 bg-stone-50 border-t border-stone-100 flex justify-end gap-3">
            <button type="button" @click="cerrarContratoModal" class="btn-secondary text-xs py-1.5 px-4">Cancelar</button>
            <button
              type="submit"
              :disabled="proveedorStore.loading"
              class="btn-primary text-xs py-1.5 px-4"
            >
              Registrar Contrato
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Modal Nueva Entrega (Inline) -->
    <div
      v-if="modalEntrega"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-950/60 backdrop-blur-md"
    >
      <div class="bg-white border border-stone-200 w-full max-w-md rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[95vh] text-left">
        <div class="h-1.5 w-full bg-amber-700"></div>
        <div class="px-6 pt-5 pb-3 border-b border-stone-100 flex items-center justify-between">
          <h3 class="text-base font-bold text-stone-900">Registrar Entrega de Madera</h3>
          <button @click="cerrarEntregaModal" class="text-stone-400 hover:text-stone-700 text-xl font-bold">&times;</button>
        </div>

        <form @submit.prevent="guardarEntrega">
          <div class="px-6 py-4 space-y-4 max-h-[60vh] overflow-y-auto text-xs">
            <!-- Selector Madera en Inventario -->
            <div>
              <label class="block font-bold text-stone-600 uppercase mb-1">Madera en Inventario (Sede/Mina) *</label>
              <select
                v-model="selecMaderaIdx"
                required
                @change="onMaderaSelect"
                class="input-field py-1.5 select-custom"
              >
                <option value="">Seleccione madera y sede...</option>
                <option
                  v-for="(mad, idx) in maderasActivas"
                  :key="mad.id"
                  :value="idx"
                >
                  {{ mad.tipo }} ({{ mad.unidad }}) - Sede: {{ mad.mina }} - Stock: {{ mad.stockDisponible }}
                </option>
              </select>
              <p class="text-[10px] text-stone-400 mt-1 italic">
                * Relaciona esta entrega con el stock físico de la mina en ms-inventario.
              </p>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <!-- Cantidad Entregada -->
              <div>
                <label class="block font-bold text-stone-600 uppercase mb-1">Cantidad Entregada *</label>
                <input
                  v-model="formEntrega.cantidadEntregada"
                  type="number"
                  min="1"
                  required
                  class="input-field py-1.5"
                />
              </div>

              <!-- Guía Remisión -->
              <div>
                <label class="block font-bold text-stone-600 uppercase mb-1">Guía Remisión *</label>
                <input
                  v-model="formEntrega.guiaRemision"
                  type="text"
                  required
                  placeholder="GR-000-00000"
                  class="input-field py-1.5 font-mono"
                />
              </div>
            </div>

            <!-- Responsable de Recepción -->
            <div>
              <label class="block font-bold text-stone-600 uppercase mb-1">Responsable de Recepción (Almacén) *</label>
              <input
                v-model="formEntrega.responsableRecepcion"
                type="text"
                required
                placeholder="Nombre del operario que recibe"
                class="input-field py-1.5"
              />
            </div>
          </div>

          <div class="px-6 py-4 bg-stone-50 border-t border-stone-100 flex justify-end gap-3">
            <button type="button" @click="cerrarEntregaModal" class="btn-secondary text-xs py-1.5 px-4">Cancelar</button>
            <button
              type="submit"
              :disabled="proveedorStore.loading"
              class="btn-primary text-xs py-1.5 px-4 bg-amber-700 hover:bg-amber-800"
            >
              Registrar Entrega
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useProveedoresStore } from '@/stores/proveedores'
import { useInventarioStore } from '@/stores/inventario'
import { useToastStore } from '@/stores/toast'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import AlertMessage from '@/components/common/AlertMessage.vue'

const authStore = useAuthStore()
const proveedorStore = useProveedoresStore()
const inventarioStore = useInventarioStore()
const toastStore = useToastStore()
const route = useRoute()

const loadingProveedor = ref(true)
const tabActiva = ref('contratos')

// Contrato Modal
const modalContrato = ref(false)
const formContrato = reactive({
  tipoMadera: '',
  unidad: 'm3',
  precioPactado: 0,
  cantidadMinima: 10,
  cantidadMaxima: 1000,
  fechaInicio: '',
  fechaFin: '',
  estado: 'VIGENTE',
  observaciones: ''
})

// Entrega Modal
const modalEntrega = ref(false)
const selecMaderaIdx = ref('')
const formEntrega = reactive({
  maderaId: null,
  tipoMadera: '',
  cantidadEntregada: 0,
  unidad: '',
  guiaRemision: '',
  responsableRecepcion: ''
})

// Maderas del inventario
const maderasActivas = computed(() => inventarioStore.maderas)

onMounted(async () => {
  const id = route.params.id
  try {
    await proveedorStore.fetchProveedor(id)
    await proveedorStore.fetchContratos(id)
    await proveedorStore.fetchEntregas(id)
    await inventarioStore.fetchMaderas() // Cargar el inventario para poder relacionar las entregas
  } catch (e) {
    // Manejado por store
  } finally {
    loadingProveedor.value = false
  }
})

// Mapeos y formatos
function formatFecha(fechaString) {
  if (!fechaString) return '-'
  const d = new Date(fechaString)
  return d.toLocaleDateString('es-PE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function formatEstadoContratoClase(estado) {
  switch (estado) {
    case 'VIGENTE':
      return 'bg-emerald-100 text-emerald-800'
    case 'VENCIDO':
      return 'bg-amber-100 text-amber-800'
    case 'SUSPENDIDO':
      return 'bg-rose-100 text-rose-800 font-extrabold border border-rose-250'
    default:
      return 'bg-stone-100 text-stone-800'
  }
}

// Acciones Contrato
function abrirNuevoContratoModal() {
  formContrato.tipoMadera = ''
  formContrato.unidad = 'm3'
  formContrato.precioPactado = 0
  formContrato.cantidadMinima = 10
  formContrato.cantidadMaxima = 1000
  formContrato.fechaInicio = new Date().toISOString().split('T')[0]
  formContrato.fechaFin = new Date(Date.now() + 365 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  formContrato.estado = 'VIGENTE'
  formContrato.observaciones = ''
  modalContrato.value = true
}

function cerrarContratoModal() {
  modalContrato.value = false
}

async function guardarContrato() {
  try {
    await proveedorStore.crearContrato(route.params.id, { ...formContrato })
    toastStore.show({
      mensaje: 'Contrato registrado exitosamente.',
      tipo: 'success'
    })
    cerrarContratoModal()
    await proveedorStore.fetchContratos(route.params.id)
  } catch (e) {
    toastStore.show({
      mensaje: e.response?.data?.mensaje || 'Error al crear contrato.',
      tipo: 'error'
    })
  }
}

async function cambiarEstadoContrato(contratoId, nuevoEstado) {
  try {
    await proveedorStore.actualizarEstadoContrato(contratoId, nuevoEstado)
    toastStore.show({
      mensaje: `Contrato #${contratoId} actualizado a ${nuevoEstado}.`,
      tipo: 'success'
    })
    await proveedorStore.fetchContratos(route.params.id)
  } catch (e) {
    toastStore.show({
      mensaje: e.response?.data?.mensaje || 'Error al cambiar estado.',
      tipo: 'error'
    })
  }
}

// Acciones Entrega
function abrirNuevaEntregaModal() {
  selecMaderaIdx.value = ''
  formEntrega.maderaId = null
  formEntrega.tipoMadera = ''
  formEntrega.cantidadEntregada = 1
  formEntrega.unidad = ''
  formEntrega.guiaRemision = ''
  formEntrega.responsableRecepcion = authStore.usuario?.nombre || ''
  modalEntrega.value = true
}

function cerrarEntregaModal() {
  modalEntrega.value = false
}

function onMaderaSelect() {
  const idx = selecMaderaIdx.value
  if (idx !== '') {
    const mad = maderasActivas.value[idx]
    formEntrega.maderaId = mad.id
    formEntrega.tipoMadera = mad.tipo
    formEntrega.unidad = mad.unidad
  } else {
    formEntrega.maderaId = null
    formEntrega.tipoMadera = ''
    formEntrega.unidad = ''
  }
}

async function guardarEntrega() {
  if (!formEntrega.maderaId) {
    toastStore.show({
      mensaje: 'Por favor, selecciona una madera del inventario.',
      tipo: 'warning'
    })
    return
  }

  try {
    await proveedorStore.registrarEntrega(route.params.id, { ...formEntrega })
    toastStore.show({
      mensaje: 'Entrega registrada e inventario actualizado exitosamente.',
      tipo: 'success'
    })
    cerrarEntregaModal()
    await proveedorStore.fetchEntregas(route.params.id)
  } catch (e) {
    toastStore.show({
      mensaje: e.response?.data?.mensaje || 'Error al registrar entrega.',
      tipo: 'error'
    })
  }
}
</script>
