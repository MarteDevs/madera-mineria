<template>
  <div class="space-y-6">
    <!-- Header -->
    <div>
      <h1 class="text-2xl font-bold text-stone-900">Mapeo y Rutas de Entregas</h1>
      <p class="text-stone-500 text-sm mt-1">Gestión logística de cargamento y distribución de maderas estructurales hacia minas.</p>
    </div>

    <!-- Filtros -->
    <div class="card p-4">
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div>
          <label class="block text-xs font-semibold text-stone-500 uppercase tracking-wider mb-1.5 font-medium">Buscar por mina</label>
          <input
            v-model="filtros.mina"
            placeholder="Ej. Antamina..."
            class="input-field"
          />
        </div>
        <div>
          <label class="block text-xs font-semibold text-stone-500 uppercase tracking-wider mb-1.5 font-medium">Estado del Envío</label>
          <select v-model="filtros.estado" class="input-field select-custom">
            <option value="">Todos los estados</option>
            <option value="PENDIENTE">Pendiente (Por asignar)</option>
            <option value="PREPARANDO">Preparando Carga</option>
            <option value="EN_RUTA">En ruta (Tránsito)</option>
            <option value="ENTREGADO">Entregado (Confirmado)</option>
            <option value="FALLIDO">Fallido / Incidente</option>
          </select>
        </div>
        <div>
          <label class="block text-xs font-semibold text-stone-500 uppercase tracking-wider mb-1.5 font-medium">Sede de Origen</label>
          <select v-model="filtros.origen" class="input-field select-custom">
            <option value="">Todos los orígenes</option>
            <option value="Almacén Central Lima">Almacén Central Lima</option>
            <option value="Almacén Central Junín">Almacén Central Junín</option>
            <option value="Almacén Arequipa">Almacén Arequipa</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Errores del Store -->
    <AlertMessage v-if="entregasStore.error" tipo="error">
      {{ entregasStore.error }}
    </AlertMessage>

    <!-- Table Content -->
    <div v-if="entregasStore.loading" class="min-h-[250px] flex items-center justify-center card bg-white">
      <LoadingSpinner size="lg" class="text-amber-600" />
    </div>

    <div v-else class="card p-0 overflow-hidden shadow-sm border border-stone-100 bg-white">
      <div class="overflow-x-auto">
        <table class="w-full text-left text-sm border-collapse">
          <thead>
            <tr class="bg-stone-50/80 border-b border-stone-200">
              <th class="table-header">ID</th>
              <th class="table-header">Pedido ID</th>
              <th class="table-header">Madera</th>
              <th class="table-header">Cantidad</th>
              <th class="table-header">Mina Destino</th>
              <th class="table-header">Transportista</th>
              <th class="table-header">Vehículo</th>
              <th class="table-header">Estado</th>
              <th class="table-header text-right">Acciones</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-stone-100">
            <tr v-for="entrega in entregasFiltradas" :key="entrega.id" class="hover:bg-stone-50/50 transition-colors">
              <td class="table-cell font-semibold text-stone-900">#{{ entrega.id }}</td>
              <td class="table-cell">
                <RouterLink :to="`/pedidos/${entrega.pedidoId}`" class="text-xs text-amber-650 hover:underline font-semibold">
                  Pedido #{{ entrega.pedidoId }}
                </RouterLink>
              </td>
              <td class="table-cell capitalize font-medium text-stone-800">
                {{ entrega.tipoMadera }}
              </td>
              <td class="table-cell font-bold text-stone-700">
                {{ entrega.cantidad }}
              </td>
              <td class="table-cell text-stone-550 font-medium">{{ entrega.minaDestino }}</td>
              <td class="table-cell text-stone-600 font-medium">
                {{ entrega.transportista || '⚠️ Por asignar' }}
              </td>
              <td class="table-cell text-stone-500 font-mono text-xs">
                {{ entrega.vehiculo || '—' }}
              </td>
              <td class="table-cell">
                <BadgeEstado :estado="entrega.estado" />
              </td>
              <td class="table-cell text-right">
                <RouterLink
                  :to="`/entregas/${entrega.id}`"
                  class="text-xs text-stone-600 hover:text-stone-900 border border-stone-200 hover:border-stone-400 bg-white px-2.5 py-1.5 rounded-lg font-semibold inline-block transition-all"
                >
                  🚚 Gestionar Ruta
                </RouterLink>
              </td>
            </tr>
            <tr v-if="entregasFiltradas.length === 0">
              <td colspan="9" class="text-center py-12 text-stone-400 italic">
                No se registraron rutas de entregas.
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useEntregasStore } from '@/stores/entregas'
import BadgeEstado from '@/components/common/BadgeEstado.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import AlertMessage from '@/components/common/AlertMessage.vue'

const entregasStore = useEntregasStore()

const filtros = reactive({
  mina: '',
  estado: '',
  origen: ''
})

onMounted(async () => {
  await entregasStore.fetchEntregas()
})

const entregasFiltradas = computed(() => {
  return entregasStore.entregas.filter(e => {
    const coincideMina = !filtros.mina || e.minaDestino.toLowerCase().includes(filtros.mina.toLowerCase())
    const coincideEstado = !filtros.estado || e.estado === filtros.estado
    const coincideOrigen = !filtros.origen || e.almacenOrigen === filtros.origen
    return coincideMina && coincideEstado && coincideOrigen
  })
})
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
