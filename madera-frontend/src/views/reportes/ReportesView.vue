<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-stone-900">Módulo de Reportería & Business Intelligence</h1>
        <p class="text-stone-500 text-sm mt-1">Monitoreo consolidado de métricas logísticas y contratos de suministro en tiempo real.</p>
      </div>
    </div>

    <!-- Alert de errores del store -->
    <AlertMessage v-if="reportesStore.error" tipo="error" class="mb-4">
      {{ reportesStore.error }}
    </AlertMessage>

    <!-- Tabs Menu -->
    <div class="flex border-b border-stone-200 bg-stone-50/50 p-1.5 rounded-xl gap-2">
      <button
        @click="tabActiva = 'dashboard'"
        class="flex-1 py-2.5 px-4 font-bold text-sm rounded-lg transition-all"
        :class="tabActiva === 'dashboard' ? 'bg-white text-stone-900 shadow-sm border border-stone-150' : 'text-stone-500 hover:text-stone-900'"
      >
        📊 Dashboard Consolidado
      </button>
      <button
        @click="tabActiva = 'generar'"
        class="flex-1 py-2.5 px-4 font-bold text-sm rounded-lg transition-all"
        :class="tabActiva === 'generar' ? 'bg-white text-stone-900 shadow-sm border border-stone-150' : 'text-stone-500 hover:text-stone-900'"
      >
        📈 Generar Reportes Dinámicos
      </button>
      <button
        @click="cargarHistorialTab"
        class="flex-1 py-2.5 px-4 font-bold text-sm rounded-lg transition-all"
        :class="tabActiva === 'historial' ? 'bg-white text-stone-900 shadow-sm border border-stone-150' : 'text-stone-500 hover:text-stone-900'"
      >
        ⏳ Historial de Consultas
      </button>
    </div>

    <!-- ── TAB: DASHBOARD CONSOLIDADO ── -->
    <div v-if="tabActiva === 'dashboard'" class="space-y-6">
      <div v-if="loadingDashboard" class="min-h-[250px] flex items-center justify-center card bg-white">
        <LoadingSpinner size="lg" class="text-amber-600" />
      </div>

      <div v-else-if="reportesStore.dashboard" class="space-y-6">
        <!-- Banner de servicios no disponibles (Circuit Breaker status) -->
        <div
          v-if="reportesStore.dashboard.serviciosNoDisponibles && reportesStore.dashboard.serviciosNoDisponibles.length > 0"
          class="bg-amber-50 border border-amber-200 p-4 rounded-xl flex items-start gap-3"
        >
          <span class="text-xl">⚠️</span>
          <div>
            <h4 class="font-bold text-amber-800 text-sm">Estado parcial de servicios (Resilience4j activo)</h4>
            <p class="text-amber-700 text-xs mt-1">
              Los siguientes microservicios no están disponibles y devolvieron datos de contingencia:
              <span class="font-mono font-bold bg-amber-150 px-1 py-0.5 rounded text-amber-900 text-[10px] ml-1" v-for="srv in reportesStore.dashboard.serviciosNoDisponibles" :key="srv">{{ srv }}</span>
            </p>
          </div>
        </div>

        <!-- KPIs Grid -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <div class="card p-5 bg-white border border-stone-150 relative overflow-hidden">
            <div class="absolute top-0 right-0 left-0 h-1 bg-amber-600"></div>
            <span class="text-stone-400 font-bold uppercase tracking-wider text-[10px]">Stock Total de Madera</span>
            <div class="text-2xl font-extrabold text-stone-900 mt-2">{{ reportesStore.dashboard.stockTotalUnidades || 0 }} und</div>
            <span class="text-[10px] text-stone-500 mt-1 block">En {{ reportesStore.dashboard.totalTiposMadera || 0 }} variedades físicas</span>
          </div>

          <div class="card p-5 bg-white border border-stone-150 relative overflow-hidden">
            <div class="absolute top-0 right-0 left-0 h-1 bg-blue-600"></div>
            <span class="text-stone-400 font-bold uppercase tracking-wider text-[10px]">Pedidos Registrados</span>
            <div class="text-2xl font-extrabold text-stone-900 mt-2">{{ reportesStore.dashboard.totalPedidos || 0 }}</div>
            <div class="flex justify-between items-center text-[10px] text-stone-500 mt-1.5">
              <span>Pendientes: <b>{{ reportesStore.dashboard.pedidosPendientes || 0 }}</b></span>
              <span>Entregados: <b>{{ reportesStore.dashboard.pedidosEntregados || 0 }}</b></span>
            </div>
          </div>

          <div class="card p-5 bg-white border border-stone-150 relative overflow-hidden">
            <div class="absolute top-0 right-0 left-0 h-1 bg-emerald-600"></div>
            <span class="text-stone-400 font-bold uppercase tracking-wider text-[10px]">Despacho / Entregas</span>
            <div class="text-2xl font-extrabold text-stone-900 mt-2">{{ reportesStore.dashboard.totalEntregas || 0 }}</div>
            <div class="flex justify-between items-center text-[10px] text-stone-500 mt-1.5">
              <span>En ruta: <b>{{ reportesStore.dashboard.entregasEnTransito || 0 }}</b></span>
              <span>Tiempo Prom: <b>{{ (reportesStore.dashboard.tiempoPromedioEntregaHoras || 0).toFixed(1) }}h</b></span>
            </div>
          </div>

          <div class="card p-5 bg-white border border-stone-150 relative overflow-hidden">
            <div class="absolute top-0 right-0 left-0 h-1 bg-purple-600"></div>
            <span class="text-stone-400 font-bold uppercase tracking-wider text-[10px]">Proveedores & Contratos</span>
            <div class="text-2xl font-extrabold text-stone-900 mt-2">{{ reportesStore.dashboard.totalProveedores || 0 }}</div>
            <div class="flex justify-between items-center text-[10px] text-stone-500 mt-1.5">
              <span>Activos: <b>{{ reportesStore.dashboard.proveedoresActivos || 0 }}</b></span>
              <span class="text-rose-600 font-bold">Por vencer: {{ reportesStore.dashboard.contratosProximosAVencer || 0 }}</span>
            </div>
          </div>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <!-- Alertas de Stock Bajo -->
          <div class="card bg-white p-5 border border-stone-150 flex flex-col justify-between">
            <div>
              <h4 class="text-sm font-bold text-stone-900 border-b border-stone-100 pb-2 mb-3">🚨 Alertas de Stock Crítico (&lt; Mínimo)</h4>
              <div v-if="!reportesStore.dashboard.tiposConStockBajo || reportesStore.dashboard.tiposConStockBajo.length === 0" class="text-center py-6 text-stone-400 italic text-xs">
                No hay maderas con stock por debajo del mínimo.
              </div>
              <ul v-else class="space-y-1.5">
                <li
                  v-for="item in reportesStore.dashboard.tiposConStockBajo"
                  :key="item"
                  class="flex items-center justify-between text-xs p-2 bg-stone-50 rounded-lg hover:bg-stone-100 transition-colors"
                >
                  <span class="font-semibold text-stone-800 capitalize">{{ item.split(' (')[0] }}</span>
                  <span class="text-[10px] text-stone-500 font-bold uppercase bg-stone-200/50 px-2 py-0.5 rounded">{{ item.split(' (')[1]?.replace(')', '') }}</span>
                </li>
              </ul>
            </div>
          </div>

          <!-- Distribución de madera por Sede -->
          <div class="card bg-white p-5 border border-stone-150">
            <h4 class="text-sm font-bold text-stone-900 border-b border-stone-100 pb-2 mb-3">📦 Stock Físico por Mina / Sede</h4>
            <div v-if="!reportesStore.dashboard.stockPorMina || Object.keys(reportesStore.dashboard.stockPorMina).length === 0" class="text-center py-6 text-stone-400 italic text-xs">
              No hay distribución de stock registrada.
            </div>
            <div v-else class="space-y-3">
              <div v-for="(cant, mina) in reportesStore.dashboard.stockPorMina" :key="mina" class="space-y-1">
                <div class="flex justify-between text-xs font-semibold text-stone-700">
                  <span>{{ mina }}</span>
                  <span>{{ cant }} und</span>
                </div>
                <div class="w-full bg-stone-100 h-2 rounded-full overflow-hidden">
                  <div
                    class="bg-amber-600 h-full rounded-full transition-all duration-500"
                    :style="`width: ${Math.min(100, (cant / (reportesStore.dashboard.stockTotalUnidades || 1)) * 100)}%`"
                  ></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ── TAB: GENERAR REPORTES ── -->
    <div v-if="tabActiva === 'generar'" class="space-y-6">
      <!-- Formulario para configurar el reporte -->
      <div class="card p-5 bg-white border border-stone-150">
        <h3 class="text-sm font-bold text-stone-900 mb-4 border-b border-stone-100 pb-2">Configuración del Reporte</h3>
        <div class="grid grid-cols-1 sm:grid-cols-4 gap-4 items-end">
          <div>
            <label class="block text-xs font-bold text-stone-600 uppercase mb-1.5">Tipo Reporte *</label>
            <select v-model="tipoReporteSelect" class="input-field select-custom">
              <option value="STOCK">Stock de Madera</option>
              <option value="PEDIDOS">Pedidos Recibidos</option>
              <option value="ENTREGAS">Entregas & Conformidad</option>
            </select>
          </div>

          <div>
            <label class="block text-xs font-bold text-stone-600 uppercase mb-1.5">Mina / Sede (Filtro)</label>
            <select v-model="filtroMina" class="input-field select-custom">
              <option value="">Todas las Sedes</option>
              <option value="Yanacocha">Yanacocha</option>
              <option value="Antamina">Antamina</option>
              <option value="Las Bambas">Las Bambas</option>
              <option value="Cerro Verde">Cerro Verde</option>
            </select>
          </div>

          <div v-if="tipoReporteSelect === 'PEDIDOS'">
            <label class="block text-xs font-bold text-stone-600 uppercase mb-1.5">Fecha Desde</label>
            <input v-model="filtroDesde" type="date" class="input-field" />
          </div>

          <div v-if="tipoReporteSelect === 'PEDIDOS'">
            <label class="block text-xs font-bold text-stone-600 uppercase mb-1.5">Fecha Hasta</label>
            <input v-model="filtroHasta" type="date" class="input-field" />
          </div>

          <div :class="tipoReporteSelect === 'PEDIDOS' ? 'sm:col-span-4 flex justify-end mt-2' : 'sm:col-span-2'">
            <button
              @click="generarReporte"
              :disabled="reportesStore.loading"
              class="btn-primary w-full sm:w-auto min-w-[140px] justify-center py-2"
            >
              {{ reportesStore.loading ? 'Generando...' : '⚙️ Generar' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Spinner al cargar reporte dinámico -->
      <div v-if="reportesStore.loading" class="min-h-[200px] flex items-center justify-center card bg-white">
        <LoadingSpinner size="lg" class="text-amber-600" />
      </div>

      <!-- Resultados del Reporte Dinámico -->
      <div v-else-if="datosReporte" class="space-y-6">
        <!-- 1. RESULTADO STOCK -->
        <div v-if="tipoReporteSelect === 'STOCK' && reportesStore.stock" class="space-y-4">
          <div class="flex justify-between items-center border-b pb-2">
            <h3 class="text-base font-bold text-stone-900">Métricas de Stock (Mina: {{ reportesStore.stock.mina }})</h3>
            <span class="text-[10px] text-stone-400">Generado el: {{ formatFecha(reportesStore.stock.fechaGeneracion) }}</span>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div class="p-3 bg-stone-50 rounded-xl border border-stone-200">
              <span class="text-[10px] font-bold text-stone-500 uppercase block">Total Unidades Físicas</span>
              <span class="text-xl font-bold text-stone-850 mt-1 block">{{ reportesStore.stock.totalUnidades }} unidades</span>
            </div>
            <div class="p-3 bg-rose-50/20 rounded-xl border border-rose-100">
              <span class="text-[10px] font-bold text-rose-700 uppercase block">Variedades con Stock Bajo</span>
              <span class="text-xl font-bold text-rose-800 mt-1 block">{{ reportesStore.stock.itemsConStockBajo }} variedades</span>
            </div>
          </div>

          <div class="card p-0 overflow-hidden border bg-white shadow-sm text-xs">
            <table class="w-full text-left">
              <thead>
                <tr class="bg-stone-50 border-b">
                  <th class="table-header py-2">Tipo Madera</th>
                  <th class="table-header py-2">Uso Estructural</th>
                  <th class="table-header py-2">Stock Disponible</th>
                  <th class="table-header py-2">Stock Mínimo</th>
                  <th class="table-header py-2">Estado</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in reportesStore.stock.items" :key="item.tipo" class="hover:bg-stone-50/50">
                  <td class="table-cell capitalize font-bold text-stone-850">{{ item.tipo }}</td>
                  <td class="table-cell capitalize text-stone-600">{{ item.uso?.replace('_', ' ') }}</td>
                  <td class="table-cell font-bold">{{ item.stock }} <span class="text-[10px] text-stone-400 font-normal">{{ item.unidad }}</span></td>
                  <td class="table-cell text-stone-500">{{ item.stockMinimo }} <span class="text-[10px] text-stone-400 font-normal">{{ item.unidad }}</span></td>
                  <td class="table-cell">
                    <span
                      class="px-1.5 py-0.5 rounded text-[9px] font-bold"
                      :class="item.stockBajo ? 'bg-rose-100 text-rose-800' : 'bg-emerald-100 text-emerald-800'"
                    >
                      {{ item.stockBajo ? 'CRÍTICO' : 'OPTIMO' }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 2. RESULTADO PEDIDOS -->
        <div v-if="tipoReporteSelect === 'PEDIDOS' && reportesStore.pedidos" class="space-y-4">
          <div class="flex justify-between items-center border-b pb-2">
            <h3 class="text-base font-bold text-stone-900">Métricas de Pedidos (Mina: {{ reportesStore.pedidos.mina }})</h3>
            <span class="text-[10px] text-stone-400">Generado el: {{ formatFecha(reportesStore.pedidos.fechaGeneracion) }}</span>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div class="p-3 bg-stone-50 rounded-xl border border-stone-200">
              <span class="text-[10px] font-bold text-stone-500 uppercase block">Total Pedidos</span>
              <span class="text-xl font-bold text-stone-850 mt-1 block">{{ reportesStore.pedidos.totalPedidos }} solicitudes</span>
            </div>
            <div class="p-3 bg-stone-50 rounded-xl border border-stone-200">
              <span class="text-[10px] font-bold text-stone-500 uppercase block">Total Madera Solicitada</span>
              <span class="text-xl font-bold text-stone-850 mt-1 block">{{ reportesStore.pedidos.totalUnidadesSolicitadas }} unidades</span>
            </div>
            <div class="p-3 bg-stone-50 rounded-xl border border-stone-200">
              <span class="text-[10px] font-bold text-stone-500 uppercase block">Estados de Pedido</span>
              <span class="text-xs text-stone-700 mt-1.5 block">
                Entregado: <b>{{ reportesStore.pedidos.porEstado?.ENTREGADO || 0 }}</b> | Pendiente: <b>{{ reportesStore.pedidos.porEstado?.PENDIENTE || 0 }}</b>
              </span>
            </div>
          </div>

          <div class="card p-0 overflow-hidden border bg-white shadow-sm text-xs">
            <table class="w-full text-left">
              <thead>
                <tr class="bg-stone-50 border-b">
                  <th class="table-header py-2">ID</th>
                  <th class="table-header py-2">Tipo Madera</th>
                  <th class="table-header py-2">Cantidad Solicitada</th>
                  <th class="table-header py-2">Sede</th>
                  <th class="table-header py-2">Estado</th>
                  <th class="table-header py-2">Solicitado Por</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="p in reportesStore.pedidos.detalle" :key="p.id" class="hover:bg-stone-50/50">
                  <td class="table-cell font-mono font-bold">#{{ p.id }}</td>
                  <td class="table-cell capitalize font-medium">{{ p.tipoMadera }}</td>
                  <td class="table-cell font-bold">{{ p.cantidadSolicitada }} <span class="text-[10px] text-stone-400 font-normal">{{ p.unidad }}</span></td>
                  <td class="table-cell text-stone-600">{{ p.mina }}</td>
                  <td class="table-cell">
                    <span
                      class="px-1.5 py-0.5 rounded text-[9px] font-bold"
                      :class="formatEstadoPedidoClase(p.estado)"
                    >
                      {{ p.estado }}
                    </span>
                  </td>
                  <td class="table-cell text-stone-500 font-mono">{{ p.solicitadoPor }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 3. RESULTADO ENTREGAS -->
        <div v-if="tipoReporteSelect === 'ENTREGAS' && reportesStore.entregas" class="space-y-4">
          <div class="flex justify-between items-center border-b pb-2">
            <h3 class="text-base font-bold text-stone-900">Métricas de Entregas & Viajes</h3>
            <span class="text-[10px] text-stone-400">Generado el: {{ formatFecha(reportesStore.entregas.fechaGeneracion) }}</span>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-4 gap-4">
            <div class="p-3 bg-stone-50 rounded-xl border border-stone-200">
              <span class="text-[10px] font-bold text-stone-500 uppercase block">Total Viajes</span>
              <span class="text-xl font-bold text-stone-850 mt-1 block">{{ reportesStore.entregas.totalEntregas }} viajes</span>
            </div>
            <div class="p-3 bg-stone-50 rounded-xl border border-stone-200">
              <span class="text-[10px] font-bold text-stone-500 uppercase block">Viajes Completados</span>
              <span class="text-xl font-bold text-emerald-800 mt-1 block">{{ reportesStore.entregas.entregasCompletadas }} entregas</span>
            </div>
            <div class="p-3 bg-stone-50 rounded-xl border border-stone-200">
              <span class="text-[10px] font-bold text-stone-500 uppercase block">Tiempo Promedio</span>
              <span class="text-xl font-bold text-stone-850 mt-1 block">{{ (reportesStore.entregas.tiempoPromedioHoras || 0).toFixed(1) }} horas</span>
            </div>
            <div class="p-3 bg-stone-50 rounded-xl border border-stone-200">
              <span class="text-[10px] font-bold text-stone-500 uppercase block">% Conformidad Chofer</span>
              <span class="text-xl font-bold text-purple-800 mt-1 block">{{ (reportesStore.entregas.porcentajeConformidad || 0).toFixed(1) }}%</span>
            </div>
          </div>

          <div class="card p-0 overflow-hidden border bg-white shadow-sm text-xs">
            <table class="w-full text-left">
              <thead>
                <tr class="bg-stone-50 border-b">
                  <th class="table-header py-2">ID</th>
                  <th class="table-header py-2">Madera</th>
                  <th class="table-header py-2">Cantidad</th>
                  <th class="table-header py-2">Transportista</th>
                  <th class="table-header py-2">Vehículo</th>
                  <th class="table-header py-2">Estado Viaje</th>
                  <th class="table-header py-2">Conformidad</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="e in reportesStore.entregas.detalle" :key="e.id" class="hover:bg-stone-50/50">
                  <td class="table-cell font-mono font-bold">#{{ e.id }}</td>
                  <td class="table-cell capitalize font-medium">{{ e.tipoMadera }}</td>
                  <td class="table-cell font-bold">{{ e.cantidad }}</td>
                  <td class="table-cell text-stone-700">{{ e.transportista || 'Sin asignar' }}</td>
                  <td class="table-cell text-stone-500 font-mono">{{ e.vehiculo || 'Sin asignar' }}</td>
                  <td class="table-cell">
                    <span
                      class="px-1.5 py-0.5 rounded text-[9px] font-bold"
                      :class="formatEstadoEntregaClase(e.estado)"
                    >
                      {{ e.estado }}
                    </span>
                  </td>
                  <td class="table-cell">
                    <span v-if="e.conformidad === true" class="text-emerald-600 font-bold">✓ CONFORME</span>
                    <span v-else-if="e.conformidad === false" class="text-rose-600 font-bold">✗ RECLAMO</span>
                    <span v-else class="text-stone-400 italic">Pendiente</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- ── TAB: HISTORIAL DE CONSULTAS ── -->
    <div v-if="tabActiva === 'historial'" class="space-y-4">
      <div class="flex items-center justify-between">
        <h3 class="text-lg font-bold text-stone-950">Registro Histórico de Generación</h3>
        <button @click="cargarHistorialTab" class="btn-secondary py-1 px-3 text-xs">🔄 Actualizar</button>
      </div>

      <div v-if="loadingHistorial" class="min-h-[200px] flex items-center justify-center card bg-white">
        <LoadingSpinner size="lg" class="text-amber-600" />
      </div>

      <div v-else class="card p-0 overflow-hidden border bg-white shadow-sm text-xs">
        <table class="w-full text-left">
          <thead>
            <tr class="bg-stone-50 border-b">
              <th class="table-header py-2">ID</th>
              <th class="table-header py-2">Fecha Generación</th>
              <th class="table-header py-2">Tipo Reporte</th>
              <th class="table-header py-2">Tiempo Latencia</th>
              <th class="table-header py-2">Consultados</th>
              <th class="table-header py-2">Estado Carga</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-stone-100">
            <tr v-if="reportesStore.historial.length === 0">
              <td colspan="6" class="p-8 text-center text-stone-400 italic">
                No hay historial de reportes.
              </td>
            </tr>
            <tr v-for="h in reportesStore.historial" :key="h.id" class="hover:bg-stone-50/50">
              <td class="table-cell text-stone-500 font-mono">#{{ h.id }}</td>
              <td class="table-cell text-stone-600">{{ formatFecha(h.fechaGeneracion) }}</td>
              <td class="table-cell font-bold text-stone-850">{{ h.tipo }}</td>
              <td class="table-cell font-bold text-stone-800">{{ h.tiempoGeneracionMs }} ms</td>
              <td class="table-cell">
                <span class="text-[10px] text-stone-500">{{ h.serviciosConsultados }}</span>
              </td>
              <td class="table-cell">
                <span
                  class="px-1.5 py-0.5 rounded text-[9px] font-bold"
                  :class="h.exitoso ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'"
                >
                  {{ h.exitoso ? '✓ COMPLETO' : '⚠ PARCIAL' }}
                </span>
                <span v-if="h.serviciosFallidos" class="text-[9px] text-rose-500 font-mono block mt-0.5">Falló: {{ h.serviciosFallidos }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useReportesStore } from '@/stores/reportes'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import AlertMessage from '@/components/common/AlertMessage.vue'

const reportesStore = useReportesStore()

const tabActiva = ref('dashboard')
const loadingDashboard = ref(true)
const loadingHistorial = ref(false)
const datosReporte = ref(false)

// Config de filtros dinámicos
const tipoReporteSelect = ref('STOCK')
const filtroMina = ref('')
const filtroDesde = ref('')
const filtroHasta = ref('')

onMounted(async () => {
  try {
    await reportesStore.fetchDashboard()
  } catch (e) {
    // Manejado por store
  } finally {
    loadingDashboard.value = false
  }
})

async function cargarHistorialTab() {
  tabActiva.value = 'historial'
  loadingHistorial.value = true
  try {
    await reportesStore.fetchHistorial()
  } finally {
    loadingHistorial.value = false
  }
}

async function generarReporte() {
  datosReporte.value = false
  try {
    if (tipoReporteSelect.value === 'STOCK') {
      await reportesStore.fetchStock(filtroMina.value)
    } else if (tipoReporteSelect.value === 'PEDIDOS') {
      // Si el usuario ingresa YYYY-MM-DD, el endpoint soporta fechas simples o completas
      await reportesStore.fetchPedidos(filtroMina.value, filtroDesde.value, filtroHasta.value)
    } else if (tipoReporteSelect.value === 'ENTREGAS') {
      await reportesStore.fetchEntregas(filtroMina.value)
    }
    datosReporte.value = true
  } catch (e) {
    // error manejado por store
  }
}

// Helpers Formateo
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

function formatEstadoPedidoClase(estado) {
  switch (estado) {
    case 'PENDIENTE': return 'bg-amber-100 text-amber-800'
    case 'APROBADO': return 'bg-blue-100 text-blue-800'
    case 'ENTREGADO': return 'bg-emerald-100 text-emerald-800'
    case 'RECHAZADO': return 'bg-rose-100 text-rose-800'
    default: return 'bg-stone-100 text-stone-850'
  }
}

function formatEstadoEntregaClase(estado) {
  switch (estado) {
    case 'PREPARANDO': return 'bg-amber-100 text-amber-800'
    case 'EN_RUTA': return 'bg-blue-100 text-blue-800'
    case 'ENTREGADO': return 'bg-emerald-100 text-emerald-800'
    case 'FALLIDO': return 'bg-rose-100 text-rose-800 font-extrabold'
    default: return 'bg-stone-100 text-stone-850'
  }
}
</script>
