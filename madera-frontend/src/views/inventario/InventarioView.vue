<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-stone-900">Gestión de Inventario</h1>
        <p class="text-stone-500 text-sm mt-1">Monitoreo de stock de maderas estructurales por mina.</p>
      </div>
      <RouterLink
        v-if="authStore.esAdmin || authStore.esAlmacen"
        to="/inventario/nuevo"
        class="btn-primary flex items-center gap-2"
      >
        <span>+</span> Registrar Nuevo Ítem
      </RouterLink>
    </div>

    <!-- Filtros -->
    <div class="card p-4">
      <div class="grid grid-cols-1 sm:grid-cols-4 gap-4">
        <div>
          <label class="block text-xs font-semibold text-stone-500 uppercase tracking-wider mb-1.5">Buscar por tipo</label>
          <input
            v-model="filtros.buscar"
            placeholder="Ejem: Eucalipto, Pino..."
            class="input-field"
          />
        </div>
        <div>
          <label class="block text-xs font-semibold text-stone-500 uppercase tracking-wider mb-1.5">Mina / Sede</label>
          <select v-model="filtros.mina" class="input-field select-custom">
            <option value="">Todas las sedes</option>
            <option value="Yanacocha">Yanacocha</option>
            <option value="Antamina">Antamina</option>
            <option value="Las Bambas">Las Bambas</option>
            <option value="Cerro Verde">Cerro Verde</option>
            <option value="Oficina Central">Oficina Central</option>
          </select>
        </div>
        <div>
          <label class="block text-xs font-semibold text-stone-500 uppercase tracking-wider mb-1.5">Uso Estructural</label>
          <select v-model="filtros.uso" class="input-field select-custom">
            <option value="">Todos los usos</option>
            <option value="soporte_galeria">Soporte Galería</option>
            <option value="entibado">Entibado</option>
            <option value="cuadros">Cuadros de Madera</option>
          </select>
        </div>
        <div>
          <label class="block text-xs font-semibold text-stone-500 uppercase tracking-wider mb-1.5">Disponibilidad</label>
          <select v-model="filtros.estado" class="input-field select-custom">
            <option value="">Todos los estados</option>
            <option value="DISPONIBLE">Disponible</option>
            <option value="RESERVADO">Reservado</option>
            <option value="AGOTADO">Agotado</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Alert de errores del Store -->
    <AlertMessage v-if="inventarioStore.error" tipo="error" class="mb-4">
      {{ inventarioStore.error }}
    </AlertMessage>

    <!-- Table content -->
    <div v-if="inventarioStore.loading" class="min-h-[250px] flex items-center justify-center card bg-white">
      <LoadingSpinner size="lg" class="text-amber-600" />
    </div>

    <div v-else class="card p-0 overflow-hidden shadow-sm border border-stone-100 bg-white">
      <div class="overflow-x-auto">
        <table class="w-full text-left text-sm border-collapse">
          <thead>
            <tr class="bg-stone-50/80 border-b border-stone-200">
              <th class="table-header">ID</th>
              <th class="table-header">Tipo Madera</th>
              <th class="table-header">Aplicación / Uso</th>
              <th class="table-header">Mina</th>
              <th class="table-header">Stock Físico</th>
              <th class="table-header">Precio Unit.</th>
              <th class="table-header">Estado</th>
              <th v-if="authStore.esAdmin || authStore.esAlmacen" class="table-header text-right">Acciones</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-stone-100">
            <tr v-for="madera in maderasFiltradas" :key="madera.id" class="hover:bg-stone-50/50 transition-colors">
              <td class="table-cell font-semibold text-stone-900">#{{ madera.id }}</td>
              <td class="table-cell capitalize">
                <span class="font-semibold text-stone-850">{{ madera.tipo }}</span>
                <span class="text-xs text-stone-400 block mt-0.5 capitalize">Medida: {{ madera.unidad }}</span>
              </td>
              <td class="table-cell text-stone-600 capitalize">
                {{ formatUso(madera.uso) }}
              </td>
              <td class="table-cell text-stone-500 font-medium">{{ madera.mina }}</td>
              <td class="table-cell font-bold text-stone-850">
                {{ madera.stockDisponible != null ? madera.stockDisponible : 0 }} <span class="text-xs font-normal text-stone-400 capitalize">{{ madera.unidad }}</span>
              </td>
              <td class="table-cell font-semibold text-stone-700">
                S/. {{ madera.precioPorUnidad != null ? madera.precioPorUnidad.toFixed(2) : '0.00' }}
              </td>
              <td class="table-cell">
                <BadgeEstado :estado="madera.estado" />
              </td>
              <td v-if="authStore.esAdmin || authStore.esAlmacen" class="table-cell text-right">
                <div class="flex items-center justify-end gap-3">
                  <button
                    @click="abrirEntradaModal(madera)"
                    class="text-xs text-amber-600 hover:text-amber-800 font-semibold border border-amber-200 hover:border-amber-400 bg-amber-50/50 hover:bg-amber-50 px-2.5 py-1.5 rounded-lg transition-all"
                  >
                    📥 + Stock
                  </button>
                  <button
                    @click="abrirHistorial(madera)"
                    class="text-xs text-stone-500 hover:text-stone-700 font-semibold border border-stone-200 hover:border-stone-400 px-2.5 py-1.5 rounded-lg transition-all"
                  >
                    📋 Historial
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="maderasFiltradas.length === 0">
              <td colspan="8" class="text-center py-12 text-stone-400 italic">
                No se encontraron maderas registradas con los filtros aplicados.
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Modal para Registrar Entrada de Stock -->
    <ConfirmModal
      v-if="entradaModal.mostrar"
      titulo="Ingresar Stock Adicional"
      confirmLabel="Registrar Entrada"
      cancelLabel="Cancelar"
      :loading="entradaModal.loading"
      @confirm="confirmarEntrada"
      @cancel="cerrarEntradaModal"
    >
      <template #body>
        <div class="space-y-4 text-left">
          <p class="text-stone-500 text-xs">
            Vas a registrar un movimiento de entrada en el inventario para <b class="capitalize">{{ entradaModal.madera.tipo }}</b> en la sede <b>{{ entradaModal.madera.mina }}</b>.
          </p>

          <div>
            <label class="block text-xs font-semibold text-stone-700 mb-1">Cantidad a ingresar</label>
            <input
              v-model.number="entradaModal.cantidad"
              type="number"
              min="1"
              required
              class="input-field"
              placeholder="Ej. 50"
            />
          </div>

          <div>
            <label class="block text-xs font-semibold text-stone-700 mb-1">Motivo del Ingreso</label>
            <textarea
              v-model="entradaModal.motivo"
              required
              rows="3"
              class="input-field resize-none"
              placeholder="Ej. Recepción de lote del proveedor local..."
            ></textarea>
          </div>
        </div>
      </template>
    </ConfirmModal>

    <!-- Modal para Historial de Movimientos -->
    <ConfirmModal
      v-if="historialModal.mostrar"
      :titulo="`Historial de Movimientos: ${historialModal.madera.tipo} (#${historialModal.madera.id})`"
      confirmLabel="Entendido"
      :mostrarCancelar="false"
      @confirm="cerrarHistorialModal"
    >
      <template #body>
        <div class="space-y-4 max-h-[300px] overflow-y-auto text-left pr-2">
          <div v-if="historialModal.cargando" class="flex justify-center py-8">
            <LoadingSpinner size="md" class="text-stone-500" />
          </div>
          <div v-else-if="inventarioStore.movimientos.length === 0" class="text-center py-6 text-stone-400 italic text-xs">
            No hay movimientos registrados para esta madera.
          </div>
          <div v-else class="space-y-3">
            <div
              v-for="mov in inventarioStore.movimientos"
              :key="mov.id"
              class="p-3 border rounded-xl flex justify-between items-start gap-4 text-xs"
              :class="mov.tipo === 'ENTRADA' ? 'border-emerald-100 bg-emerald-50/20' : 'border-rose-100 bg-rose-50/20'"
            >
              <div>
                <p class="font-bold capitalize" :class="mov.tipo === 'ENTRADA' ? 'text-emerald-700' : 'text-rose-700'">
                  {{ mov.tipo === 'ENTRADA' ? '📥 ENTRADA' : '📤 SALIDA' }} : {{ mov.cantidad }} unidades
                </p>
                <p class="text-stone-600 mt-1"><b>Motivo:</b> {{ mov.motivo }}</p>
                <p class="text-[10px] text-stone-400 mt-1">Responsable: {{ mov.responsable }}</p>
              </div>
              <span class="text-[10px] text-stone-400 whitespace-nowrap">{{ formatFecha(mov.fechaMovimiento) }}</span>
            </div>
          </div>
        </div>
      </template>
    </ConfirmModal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useInventarioStore } from '@/stores/inventario'
import { useDialogStore } from '@/stores/dialog'
import BadgeEstado from '@/components/common/BadgeEstado.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import AlertMessage from '@/components/common/AlertMessage.vue'
import ConfirmModal from '@/components/common/ConfirmModal.vue'

const authStore = useAuthStore()
const inventarioStore = useInventarioStore()
const dialogStore = useDialogStore()

const filtros = reactive({
  buscar: '',
  mina: '',
  uso: '',
  estado: ''
})

onMounted(async () => {
  await inventarioStore.fetchMaderas()
})

const maderasFiltradas = computed(() => {
  return inventarioStore.maderas.filter(m => {
    const coincideBuscar = !filtros.buscar || m.tipo.toLowerCase().includes(filtros.buscar.toLowerCase())
    const coincideMina = !filtros.mina || m.mina === filtros.mina
    const coincideUso = !filtros.uso || m.uso === filtros.uso
    const coincideEstado = !filtros.estado || m.estado === filtros.estado
    return coincideBuscar && coincideMina && coincideUso && coincideEstado
  })
})

function formatUso(uso) {
  const mapa = {
    soporte_galeria: 'Soporte de Galería',
    entibado: 'Entibado',
    cuadros: 'Cuadros estructurales'
  }
  return mapa[uso] || uso
}

function formatFecha(fecha) {
  if (!fecha) return '—'
  return new Date(fecha).toLocaleDateString('es-PE', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// Lógica de Entrada de Stock
const entradaModal = reactive({
  mostrar: false,
  loading: false,
  madera: null,
  cantidad: 10,
  motivo: ''
})

function abrirEntradaModal(madera) {
  entradaModal.madera = madera
  entradaModal.cantidad = 10
  entradaModal.motivo = ''
  entradaModal.mostrar = true
}

function cerrarEntradaModal() {
  entradaModal.mostrar = false
  entradaModal.madera = null
}

async function confirmarEntrada() {
  if (!entradaModal.cantidad || entradaModal.cantidad <= 0) {
    dialogStore.alert({
      titulo: 'Cantidad Inválida',
      mensaje: 'Ingrese una cantidad válida mayor a 0.',
      tipo: 'warning'
    })
    return
  }
  if (!entradaModal.motivo.trim()) {
    dialogStore.alert({
      titulo: 'Motivo Requerido',
      mensaje: 'Por favor ingrese un motivo.',
      tipo: 'warning'
    })
    return
  }

  entradaModal.loading = true
  try {
    await inventarioStore.registrarEntrada(entradaModal.madera.id, {
      cantidad: entradaModal.cantidad,
      motivo: entradaModal.motivo,
      responsable: authStore.usuario.email
    })
    cerrarEntradaModal()
  } catch (err) {
    dialogStore.alert({
      titulo: 'Error',
      mensaje: err.response?.data?.mensaje || 'Error registrando la entrada.',
      tipo: 'error'
    })
  } finally {
    entradaModal.loading = false
  }
}

// Lógica de Historial
const historialModal = reactive({
  mostrar: false,
  cargando: false,
  madera: null
})

async function abrirHistorial(madera) {
  historialModal.madera = madera
  historialModal.mostrar = true
  historialModal.cargando = true
  try {
    await inventarioStore.fetchMovimientos(madera.id)
  } catch (err) {
    console.error(err)
  } finally {
    historialModal.cargando = false
  }
}

function cerrarHistorialModal() {
  historialModal.mostrar = false
  historialModal.madera = null
}
</script>

<style scoped>
.select-custom {
  appearance: none;
  background-image: url("data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 20 20'%3E%3Cpath stroke='%236B7280' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='m6 8 4 4 4-4'/%3E%3C/svg%3E");
  background-position: right 0.5rem center;
  background-repeat: no-repeat;
  background-size: 1.25em 1.25em;
  padding-right: 2rem;
}
</style>
