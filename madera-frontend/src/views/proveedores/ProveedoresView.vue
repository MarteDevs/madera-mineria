<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-stone-900">Gestión de Proveedores</h1>
        <p class="text-stone-500 text-sm mt-1">Directorio de proveedores de madera y control de contratos.</p>
      </div>
      <RouterLink
        v-if="authStore.esAdmin"
        to="/proveedores/nuevo"
        class="btn-primary flex items-center gap-2"
      >
        <span>+</span> Registrar Proveedor
      </RouterLink>
    </div>

    <!-- Filtros -->
    <div class="card p-4">
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div class="sm:col-span-2">
          <label class="block text-xs font-semibold text-stone-500 uppercase tracking-wider mb-1.5">Buscar por Razón Social o RUC</label>
          <input
            v-model="filtroBuscar"
            placeholder="Ejem: Maderera del Sur, 20123456789..."
            class="input-field"
          />
        </div>
        <div>
          <label class="block text-xs font-semibold text-stone-500 uppercase tracking-wider mb-1.5">Estado</label>
          <select v-model="filtroEstado" class="input-field select-custom">
            <option value="">Todos</option>
            <option value="true">Activos</option>
            <option value="false">Inactivos</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Alert de errores -->
    <AlertMessage v-if="proveedoresStore.error" tipo="error" class="mb-4">
      {{ proveedoresStore.error }}
    </AlertMessage>

    <!-- Table content -->
    <div v-if="proveedoresStore.loading" class="min-h-[250px] flex items-center justify-center card bg-white">
      <LoadingSpinner size="lg" class="text-amber-600" />
    </div>

    <div v-else class="card p-0 overflow-hidden shadow-sm border border-stone-100 bg-white">
      <div class="overflow-x-auto">
        <table class="w-full text-left text-sm border-collapse">
          <thead>
            <tr class="bg-stone-50/80 border-b border-stone-200">
              <th class="table-header">ID</th>
              <th class="table-header">Razón Social</th>
              <th class="table-header">RUC</th>
              <th class="table-header">Contacto</th>
              <th class="table-header">Ciudad</th>
              <th class="table-header">Estado</th>
              <th class="table-header text-right">Acciones</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-stone-100">
            <tr v-if="proveedoresFiltrados.length === 0">
              <td colspan="7" class="p-8 text-center text-stone-400 italic">
                No se encontraron proveedores.
              </td>
            </tr>
            <tr v-for="prov in proveedoresFiltrados" :key="prov.id" class="hover:bg-stone-50/50 transition-colors">
              <td class="table-cell font-semibold text-stone-900">#{{ prov.id }}</td>
              <td class="table-cell">
                <span class="font-bold text-stone-850 block">{{ prov.razonSocial }}</span>
                <span v-if="prov.nombreComercial" class="text-xs text-stone-400 block mt-0.5">{{ prov.nombreComercial }}</span>
              </td>
              <td class="table-cell font-mono text-stone-600 text-xs">{{ prov.ruc }}</td>
              <td class="table-cell">
                <span class="text-stone-700 block">{{ prov.contactoNombre || 'Sin contacto' }}</span>
                <span v-if="prov.contactoEmail" class="text-xs text-stone-400 block mt-0.5">{{ prov.contactoEmail }}</span>
              </td>
              <td class="table-cell text-stone-600 capitalize">{{ prov.ciudad || 'No especificada' }}</td>
              <td class="table-cell">
                <span
                  class="px-2 py-0.5 rounded-full text-[10px] font-bold"
                  :class="prov.activo ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-rose-800'"
                >
                  {{ prov.activo ? 'ACTIVO' : 'INACTIVO' }}
                </span>
              </td>
              <td class="table-cell text-right">
                <RouterLink
                  :to="`/proveedores/${prov.id}`"
                  class="btn-secondary py-1 px-3 text-xs inline-flex items-center gap-1.5"
                >
                  🔎 Ver Detalle
                </RouterLink>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useProveedoresStore } from '@/stores/proveedores'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import AlertMessage from '@/components/common/AlertMessage.vue'

const authStore = useAuthStore()
const proveedoresStore = useProveedoresStore()

const filtroBuscar = ref('')
const filtroEstado = ref('true') // Mostrar activos por defecto

onMounted(async () => {
  await proveedoresStore.fetchProveedores()
})

const proveedoresFiltrados = computed(() => {
  return proveedoresStore.proveedores.filter(p => {
    const term = filtroBuscar.value.toLowerCase()
    const coincideBuscar = !term || 
      p.razonSocial.toLowerCase().includes(term) || 
      (p.nombreComercial && p.nombreComercial.toLowerCase().includes(term)) ||
      p.ruc.includes(term)
    
    const coincideEstado = filtroEstado.value === '' || 
      p.activo.toString() === filtroEstado.value

    return coincideBuscar && coincideEstado
  })
})
</script>
