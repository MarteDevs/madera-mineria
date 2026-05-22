<template>
  <div class="space-y-8">
    <!-- Title / Greetings header -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-stone-900 tracking-tight">Panel de Control</h1>
        <p class="text-stone-500 text-sm mt-1">Resumen general y métricas clave del sistema de madera y entregas mineras.</p>
      </div>
      <div class="flex items-center gap-2">
        <span class="inline-flex w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
        <span class="text-xs text-stone-500 font-semibold uppercase tracking-wider">Conexión Segura Gateway</span>
      </div>
    </div>

    <!-- Loading Screen or Stats Cards Grid -->
    <div v-if="globalLoading" class="min-h-[300px] flex items-center justify-center card bg-white">
      <div class="text-center">
        <LoadingSpinner size="lg" class="mx-auto text-amber-600 mb-3" />
        <p class="text-stone-500 text-sm font-medium">Recuperando información en tiempo real...</p>
      </div>
    </div>

    <div v-else class="space-y-8">
      <!-- Stats Cards -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatsCard
          title="Stock Total de Madera"
          :value="`${stats.stockTotal} und`"
          icon="🪵"
          color="amber"
          @click="router.push('/inventario')"
          class="cursor-pointer"
        />

        <StatsCard
          title="Pedidos Pendientes"
          :value="stats.pedidosPendientes"
          icon="📋"
          color="blue"
          @click="router.push('/pedidos')"
          class="cursor-pointer"
        />

        <StatsCard
          title="Entregas en Ruta"
          :value="stats.entregasEnTransito"
          icon="🚛"
          color="green"
          @click="router.push('/entregas')"
          class="cursor-pointer"
        />

        <StatsCard
          title="Notificaciones"
          :value="stats.notificacionesCount"
          icon="🔔"
          color="purple"
          @click="router.push('/notificaciones')"
          class="cursor-pointer"
        />
      </div>

      <!-- Main Section Grid (Charts & Tables) -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <!-- Chart: Pedidos por Estado -->
        <div class="card lg:col-span-1 flex flex-col justify-between">
          <div>
            <h2 class="text-base font-bold text-stone-900 border-b border-stone-100 pb-3 mb-4">Estado de Pedidos</h2>
            <GraficaPedidos :datos="conteoEstados" />
          </div>
          <div class="mt-4 pt-4 border-t border-stone-100 flex justify-between text-xs text-stone-500">
            <span>Total de solicitudes: <b>{{ totalPedidos }}</b></span>
            <span>Refresco Automático</span>
          </div>
        </div>

        <!-- Table: Pedidos Recientes -->
        <div class="card lg:col-span-2">
          <div class="flex items-center justify-between border-b border-stone-100 pb-3 mb-4">
            <h2 class="text-base font-bold text-stone-900">Actividad Reciente (Pedidos)</h2>
            <RouterLink to="/pedidos" class="text-xs text-amber-600 hover:text-amber-700 font-semibold">Ver todos ➔</RouterLink>
          </div>

          <div v-if="pedidosRecientes.length === 0" class="flex items-center justify-center h-48 text-stone-400 italic text-sm">
            No se han registrado pedidos en el sistema.
          </div>
          <div v-else class="overflow-x-auto">
            <table class="w-full text-left text-sm">
              <thead>
                <tr class="text-stone-400 font-semibold text-xs uppercase border-b border-stone-100 pb-2">
                  <th class="py-2.5">ID</th>
                  <th class="py-2.5">Detalles</th>
                  <th class="py-2.5">Sede</th>
                  <th class="py-2.5">Estado</th>
                  <th class="py-2.5 text-right">Cantidad</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-stone-100 text-stone-700">
                <tr v-for="pedido in pedidosRecientes" :key="pedido.id" class="hover:bg-stone-50/50 transition-colors">
                  <td class="py-3 font-semibold text-stone-900">#{{ pedido.id }}</td>
                  <td class="py-3">
                    <p class="font-medium text-stone-800 capitalize">{{ pedido.tipoMadera }}</p>
                    <span class="text-[10px] text-stone-400">{{ formatFecha(pedido.fechaPedido) }}</span>
                  </td>
                  <td class="py-3 text-stone-500">{{ pedido.mina }}</td>
                  <td class="py-3">
                    <BadgeEstado :estado="pedido.estado" />
                  </td>
                  <td class="py-3 text-right font-bold text-stone-900">
                    {{ pedido.cantidadSolicitada }} <span class="text-xs font-normal text-stone-400 capitalize">{{ pedido.unidad }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Quick Actions Grid -->
      <div class="card">
        <h2 class="text-base font-bold text-stone-900 border-b border-stone-100 pb-3 mb-5">Acciones Rápidas del Operador</h2>
        <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
          <!-- Acción Compras -->
          <button
            v-if="authStore.esCompras || authStore.esAdmin"
            @click="router.push('/pedidos/nuevo')"
            class="p-5 border border-dashed border-stone-200 hover:border-amber-500 hover:bg-amber-50/30 rounded-xl transition-all text-left flex flex-col justify-between h-32 group"
          >
            <div class="text-2xl">➕</div>
            <div>
              <p class="font-bold text-stone-850 group-hover:text-amber-800 text-sm">Registrar Pedido</p>
              <p class="text-xs text-stone-400 mt-1">Crear nueva solicitud de madera</p>
            </div>
          </button>

          <!-- Acción Almacén -->
          <button
            v-if="authStore.esAlmacen || authStore.esAdmin"
            @click="router.push('/inventario/nuevo')"
            class="p-5 border border-dashed border-stone-200 hover:border-amber-500 hover:bg-amber-50/30 rounded-xl transition-all text-left flex flex-col justify-between h-32 group"
          >
            <div class="text-2xl">📦</div>
            <div>
              <p class="font-bold text-stone-850 group-hover:text-amber-800 text-sm">Registrar Madera</p>
              <p class="text-xs text-stone-400 mt-1">Ingresar lotes al almacén</p>
            </div>
          </button>

          <!-- Acción Transporte -->
          <button
            v-if="authStore.esTransporte || authStore.esAdmin || authStore.esAlmacen"
            @click="router.push('/entregas')"
            class="p-5 border border-dashed border-stone-200 hover:border-amber-500 hover:bg-amber-50/30 rounded-xl transition-all text-left flex flex-col justify-between h-32 group"
          >
            <div class="text-2xl">🚛</div>
            <div>
              <p class="font-bold text-stone-850 group-hover:text-amber-800 text-sm">Rutas en Tránsito</p>
              <p class="text-xs text-stone-400 mt-1">Monitorear y despachar vehículos</p>
            </div>
          </button>

          <!-- Acción General: Notificaciones -->
          <button
            v-if="authStore.esAdmin"
            @click="router.push('/usuarios')"
            class="p-5 border border-dashed border-stone-200 hover:border-amber-500 hover:bg-amber-50/30 rounded-xl transition-all text-left flex flex-col justify-between h-32 group"
          >
            <div class="text-2xl">👥</div>
            <div>
              <p class="font-bold text-stone-850 group-hover:text-amber-800 text-sm">Gestionar Usuarios</p>
              <p class="text-xs text-stone-400 mt-1">Administración de credenciales</p>
            </div>
          </button>

          <!-- Fallback or Simple Shortcut -->
          <button
            v-else
            @click="router.push('/pedidos')"
            class="p-5 border border-dashed border-stone-200 hover:border-amber-500 hover:bg-amber-50/30 rounded-xl transition-all text-left flex flex-col justify-between h-32 group"
          >
            <div class="text-2xl">📋</div>
            <div>
              <p class="font-bold text-stone-850 group-hover:text-amber-800 text-sm">Historial de Pedidos</p>
              <p class="text-xs text-stone-400 mt-1">Consultar estados de envíos</p>
            </div>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import StatsCard from '@/components/dashboard/StatsCard.vue'
import GraficaPedidos from '@/components/dashboard/GraficaPedidos.vue'
import BadgeEstado from '@/components/common/BadgeEstado.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import api from '@/services/api'

const router = useRouter()
const authStore = useAuthStore()

const globalLoading = ref(true)

// Stats structure
const stats = ref({
  stockTotal: 0,
  pedidosPendientes: 0,
  entregasEnTransito: 0,
  notificacionesCount: 0
})

const pedidos = ref([])
const pedidosRecientes = ref([])

// Conteo de estados
const conteoEstados = ref({
  PENDIENTE: 0,
  APROBADO: 0,
  EN_PREPARACION: 0,
  DESPACHADO: 0,
  ENTREGADO: 0,
  RECHAZADO: 0
})

const totalPedidos = computed(() => pedidos.value.length)

onMounted(async () => {
  try {
    // Realizar llamadas paralelas controlando fallos individuales
    const [resPedidos, resEntregas, resInventario, resNotif] = await Promise.allSettled([
      api.get('/api/pedidos'),
      api.get('/api/entregas'),
      api.get('/api/inventario'),
      authStore.esAdmin || authStore.esAlmacen
        ? api.get('/api/notificaciones/pendientes/count')
        : Promise.resolve({ data: 0 })
    ])

    // 1. Procesar Pedidos
    if (resPedidos.status === 'fulfilled') {
      const list = resPedidos.value.data || []
      pedidos.value = list
      pedidosRecientes.value = list.slice(0, 5)

      // Calcular pendientes
      stats.value.pedidosPendientes = list.filter(p => p.estado === 'PENDIENTE').length

      // Agrupar por estado
      const conteo = { PENDIENTE: 0, APROBADO: 0, EN_PREPARACION: 0, DESPACHADO: 0, ENTREGADO: 0, RECHAZADO: 0 }
      list.forEach(p => {
        if (conteo[p.estado] !== undefined) {
          conteo[p.estado]++
        }
      })
      conteoEstados.value = conteo
    }

    // 2. Procesar Entregas
    if (resEntregas.status === 'fulfilled') {
      const list = resEntregas.value.data || []
      stats.value.entregasEnTransito = list.filter(e => e.estado === 'EN_RUTA' || e.estado === 'PREPARANDO').length
    }

    // 3. Procesar Inventario
    if (resInventario.status === 'fulfilled') {
      const list = resInventario.value.data || []
      // Sumar el stock total de madera
      stats.value.stockTotal = list.reduce((sum, item) => sum + (item.stock || 0), 0)
    }

    // 4. Procesar Notificaciones
    if (resNotif.status === 'fulfilled') {
      stats.value.notificacionesCount = resNotif.value.data || 0
    }

  } catch (err) {
    console.error('Error general cargando dashboard:', err)
  } finally {
    globalLoading.value = false
  }
})

function formatFecha(fecha) {
  if (!fecha) return '—'
  return new Date(fecha).toLocaleDateString('es-PE', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>
