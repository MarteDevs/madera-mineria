<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-stone-900">Vehículos y Mantenimiento</h1>
        <p class="text-stone-500 text-sm mt-1">Control de kilometraje, historial de mantenimiento preventivo y alertas de circulación.</p>
      </div>
      <button
        v-if="authStore.esAdmin"
        @click="abrirModalVehiculo"
        class="btn-primary flex items-center justify-center gap-2 py-2.5 shadow-md shadow-amber-900/10"
      >
        <span class="text-lg">+</span> Registrar Vehículo
      </button>
    </div>

    <!-- KPIs / Metrics Cards -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-6">
      <!-- Total Vehiculos Card -->
      <div class="card bg-gradient-to-br from-stone-900 to-stone-950 text-white p-5 border border-stone-850">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-xs text-stone-400 font-semibold uppercase tracking-wider">Total Vehículos</p>
            <h3 class="text-3xl font-extrabold mt-2 text-white">{{ store.vehiculos.length }}</h3>
          </div>
          <div class="w-12 h-12 rounded-xl bg-stone-800/80 flex items-center justify-center text-2xl">
            🚛
          </div>
        </div>
        <p class="text-[11px] text-stone-400 mt-4 font-medium">
          {{ countOperativos }} Operativos · {{ countEnTaller }} En taller
        </p>
      </div>

      <!-- Requieren Mantenimiento Card -->
      <div class="card bg-white p-5">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-xs text-stone-500 font-semibold uppercase tracking-wider">Requieren Mantenimiento</p>
            <h3 class="text-3xl font-extrabold mt-2 text-stone-900">{{ countRequierenMant }}</h3>
          </div>
          <div
            class="w-12 h-12 rounded-xl flex items-center justify-center text-2xl"
            :class="countRequierenMant > 0 ? 'bg-amber-100 text-amber-700 animate-pulse-subtle' : 'bg-stone-100 text-stone-500'"
          >
            🔧
          </div>
        </div>
        <p class="text-[11px] text-stone-400 mt-4 font-medium">
          Vehículos con kilometraje excedido
        </p>
      </div>

      <!-- Alertas Activas Card -->
      <div class="card bg-white p-5">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-xs text-stone-500 font-semibold uppercase tracking-wider">Alertas de Documentación</p>
            <h3 class="text-3xl font-extrabold mt-2 text-rose-600">{{ store.alertas.length }}</h3>
          </div>
          <div
            class="w-12 h-12 rounded-xl flex items-center justify-center text-2xl"
            :class="store.alertas.length > 0 ? 'bg-rose-100 text-rose-700' : 'bg-stone-100 text-stone-500'"
          >
            ⚠️
          </div>
        </div>
        <p class="text-[11px] text-stone-400 mt-4 font-medium">
          Alertas de SOAT o Revisión Técnica
        </p>
      </div>
    </div>

    <!-- Pestañas de Navegación -->
    <div class="flex border-b border-stone-200">
      <button
        @click="activeTab = 'vehiculos'"
        class="px-5 py-3 font-semibold text-sm border-b-2 transition-all duration-200 focus:outline-none"
        :class="activeTab === 'vehiculos' ? 'border-amber-600 text-amber-600 bg-amber-50/20' : 'border-transparent text-stone-500 hover:text-stone-700'"
      >
        Lista de Vehículos
      </button>
      <button
        @click="activeTab = 'alertas'"
        class="px-5 py-3 font-semibold text-sm border-b-2 transition-all duration-200 focus:outline-none flex items-center gap-2"
        :class="activeTab === 'alertas' ? 'border-amber-600 text-amber-600 bg-amber-50/20' : 'border-transparent text-stone-500 hover:text-stone-700'"
      >
        Alertas de Sistema
        <span
          v-if="store.alertas.length > 0"
          class="bg-rose-600 text-white font-bold text-[10px] px-1.5 py-0.5 rounded-full"
        >
          {{ store.alertas.length }}
        </span>
      </button>
    </div>

    <!-- Errores -->
    <AlertMessage v-if="store.error" tipo="error" dismissible @update:modelValue="store.error = null">
      {{ store.error }}
    </AlertMessage>

    <AlertMessage v-if="mensajeExito" tipo="success" dismissible @update:modelValue="mensajeExito = null">
      {{ mensajeExito }}
    </AlertMessage>

    <!-- LOADING STATE -->
    <div v-if="store.loading && store.vehiculos.length === 0" class="card py-12 flex items-center justify-center">
      <LoadingSpinner mensaje="Cargando información del módulo..." />
    </div>

    <!-- TABS CONTENIDO -->
    <div v-else>
      <!-- TAB 1: VEHICULOS -->
      <div v-if="activeTab === 'vehiculos'" class="space-y-4">
        <!-- Filtros -->
        <div class="card p-4 flex flex-col sm:flex-row sm:items-center gap-4 justify-between">
          <div class="flex-1">
            <input
              v-model="filtroPlaca"
              type="text"
              placeholder="Buscar vehículo por placa o conductor..."
              class="input-field w-full max-w-md"
            />
          </div>
          <div class="flex items-center gap-2">
            <label class="text-xs font-semibold text-stone-500 uppercase tracking-wider">Estado:</label>
            <select v-model="filtroEstado" class="input-field py-1.5 text-xs max-w-[150px]">
              <option value="">Todos</option>
              <option value="OPERATIVO">Operativo</option>
              <option value="EN_MANTENIMIENTO">En Mantenimiento</option>
              <option value="INACTIVO">Inactivo</option>
            </select>
          </div>
        </div>

        <!-- Tabla -->
        <div class="card p-0 overflow-hidden shadow-sm border border-stone-100 bg-white">
          <div class="overflow-x-auto">
            <table class="w-full text-left text-sm border-collapse">
              <thead>
                <tr class="bg-stone-50/80 border-b border-stone-200">
                  <th class="table-header">Placa</th>
                  <th class="table-header">Vehículo</th>
                  <th class="table-header">Conductor</th>
                  <th class="table-header">Kilometraje</th>
                  <th class="table-header">SOAT / R.T.</th>
                  <th class="table-header">Estado</th>
                  <th class="table-header text-right">Acciones</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-stone-100">
                <tr v-if="vehiculosFiltrados.length === 0">
                  <td colspan="7" class="p-8 text-center text-stone-400 italic">
                    No se encontraron vehículos registrados.
                  </td>
                </tr>
                <tr v-for="v in vehiculosFiltrados" :key="v.id" class="hover:bg-stone-50/50 transition-colors">
                  <!-- Placa -->
                  <td class="table-cell">
                    <div class="flex items-center gap-2">
                      <span class="px-3 py-1 font-mono font-bold text-xs bg-stone-100 border border-stone-300 rounded text-stone-800 shadow-sm uppercase tracking-wide">
                        {{ v.placa }}
                      </span>
                      <span v-if="v.requiereMantenimiento" title="Kilometraje límite superado" class="text-amber-500 animate-pulse text-base cursor-default">
                        ⚠️
                      </span>
                    </div>
                  </td>
                  <!-- Vehiculo Details -->
                  <td class="table-cell">
                    <span class="font-bold text-stone-850 block">{{ v.marca }} - {{ v.modelo }}</span>
                    <span class="text-xs text-stone-400 block mt-0.5 capitalize">{{ v.tipo }} · {{ v.capacidadToneladasM3 }} m³</span>
                  </td>
                  <!-- Conductor -->
                  <td class="table-cell">
                    <span class="text-stone-700 font-medium block">{{ v.conductorNombre || 'Sin conductor' }}</span>
                    <span v-if="v.conductorTelefono" class="text-xs text-stone-400 block mt-0.5">📞 {{ v.conductorTelefono }}</span>
                  </td>
                  <!-- Kilometraje -->
                  <td class="table-cell">
                    <div class="space-y-1">
                      <span class="text-stone-800 font-bold block text-xs">{{ formatKm(v.kmActual) }} km actuales</span>
                      <span class="text-[10px] text-stone-400 block">Prox. serv: {{ formatKm(v.kmProximoMantenimiento) }} km</span>
                      <div class="w-28 bg-stone-100 rounded-full h-1.5">
                        <div
                          class="h-1.5 rounded-full transition-all duration-300"
                          :class="barraKmClase(v)"
                          :style="{ width: barraKmPorcentaje(v) + '%' }"
                        ></div>
                      </div>
                    </div>
                  </td>
                  <!-- SOAT / RT -->
                  <td class="table-cell">
                    <div class="space-y-1 text-xs">
                      <div class="flex items-center gap-1.5">
                        <span class="text-[10px] font-bold text-stone-400 uppercase">SOAT:</span>
                        <span :class="fechaVencimientoClase(v.vencimientoSoat)">
                          {{ formatFecha(v.vencimientoSoat) }}
                        </span>
                      </div>
                      <div class="flex items-center gap-1.5">
                        <span class="text-[10px] font-bold text-stone-400 uppercase">R.T.:</span>
                        <span :class="fechaVencimientoClase(v.vencimientoRevisionTecnica)">
                          {{ formatFecha(v.vencimientoRevisionTecnica) }}
                        </span>
                      </div>
                    </div>
                  </td>
                  <!-- Estado -->
                  <td class="table-cell">
                    <span :class="badgeEstadoClase(v.estado)" class="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold uppercase">
                      {{ estadoLabel(v.estado) }}
                    </span>
                  </td>
                  <!-- Acciones -->
                  <td class="table-cell text-right">
                    <div class="flex items-center justify-end gap-2">
                      <button
                        v-if="authStore.esAdmin"
                        @click="abrirModalMantenimiento(v)"
                        class="btn-secondary py-1 px-2.5 text-xs inline-flex items-center gap-1 hover:bg-amber-50 hover:text-amber-700 hover:border-amber-200"
                        title="Registrar servicio"
                      >
                        ⚙️ Mantenimiento
                      </button>
                      <button
                        @click="abrirHistorial(v)"
                        class="btn-secondary py-1 px-2.5 text-xs inline-flex items-center gap-1"
                        title="Ver historial de servicios"
                      >
                        📋 Historial
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- TAB 2: ALERTAS -->
      <div v-if="activeTab === 'alertas'" class="space-y-4 animate-fade-in">
        <div class="card p-0 overflow-hidden shadow-sm border border-stone-100 bg-white">
          <div class="overflow-x-auto">
            <table class="w-full text-left text-sm border-collapse">
              <thead>
                <tr class="bg-stone-50/80 border-b border-stone-200">
                  <th class="table-header">Prioridad</th>
                  <th class="table-header">Vehículo</th>
                  <th class="table-header">Tipo Alerta</th>
                  <th class="table-header">Mensaje</th>
                  <th class="table-header">Fecha Alerta</th>
                  <th class="table-header text-right">Acción</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-stone-100">
                <tr v-if="store.alertas.length === 0">
                  <td colspan="6" class="p-8 text-center text-stone-400 italic">
                    No hay alertas activas de mantenimiento o documentos en este momento.
                  </td>
                </tr>
                <tr v-for="alerta in store.alertas" :key="alerta.id" class="hover:bg-stone-50/50 transition-colors">
                  <!-- Prioridad -->
                  <td class="table-cell">
                    <span
                      class="px-2 py-0.5 rounded text-[10px] font-bold"
                      :class="alerta.prioridad === 'ALTA' ? 'bg-rose-100 text-rose-800 border border-rose-200' : 'bg-amber-100 text-amber-800 border border-amber-200'"
                    >
                      {{ alerta.prioridad }}
                    </span>
                  </td>
                  <!-- Vehiculo -->
                  <td class="table-cell font-mono font-bold text-stone-800">
                    {{ alerta.placaVehiculo }}
                  </td>
                  <!-- Tipo -->
                  <td class="table-cell">
                    <span class="text-xs font-semibold text-stone-600">
                      {{ formatTipoAlerta(alerta.tipoAlerta) }}
                    </span>
                  </td>
                  <!-- Mensaje -->
                  <td class="table-cell text-xs text-stone-600 max-w-xs truncate" :title="alerta.mensaje">
                    {{ alerta.mensaje }}
                  </td>
                  <!-- Fecha -->
                  <td class="table-cell text-xs text-stone-500">
                    {{ formatDateTime(alerta.fechaGeneracion) }}
                  </td>
                  <!-- Accion -->
                  <td class="table-cell text-right">
                    <button
                      v-if="authStore.esAdmin"
                      @click="abrirMantenimientoDesdeAlerta(alerta)"
                      class="btn-primary py-0.5 px-2 text-[11px]"
                    >
                      Resolver
                    </button>
                    <span v-else class="text-stone-400 italic text-xs">Requiere Admin</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- MODAL 1: REGISTRAR NUEVO VEHICULO -->
    <Teleport to="body">
      <div v-if="modalVehiculoVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <!-- Overlay -->
        <div class="absolute inset-0 bg-stone-900/60 backdrop-blur-sm" @click="cerrarModalVehiculo"></div>
        <!-- Contenido Modal -->
        <div class="relative bg-white rounded-2xl shadow-2xl w-full max-w-lg overflow-hidden animate-fade-in flex flex-col max-h-[90vh]">
          <div class="p-6 border-b border-stone-150 flex items-center justify-between bg-stone-50">
            <h3 class="text-lg font-bold text-stone-950 flex items-center gap-2">
              <span>🚛</span> Registrar Vehículo de Transporte
            </h3>
            <button @click="cerrarModalVehiculo" class="text-stone-400 hover:text-stone-600 text-lg">✕</button>
          </div>

          <form @submit.prevent="guardarVehiculo" class="p-6 space-y-4 overflow-y-auto flex-1">
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <!-- Placa -->
              <div>
                <label class="block text-[11px] font-bold text-stone-500 uppercase tracking-wider mb-1">Placa *</label>
                <input
                  v-model="formVehiculo.placa"
                  required
                  placeholder="Ej: ABC-123"
                  class="input-field"
                />
              </div>
              <!-- Tipo -->
              <div>
                <label class="block text-[11px] font-bold text-stone-500 uppercase tracking-wider mb-1">Tipo de Vehículo</label>
                <select v-model="formVehiculo.tipo" class="input-field">
                  <option value="camion">Camión</option>
                  <option value="camioneta">Camioneta</option>
                  <option value="furgon">Furgón</option>
                  <option value="volquete">Volquete</option>
                </select>
              </div>
              <!-- Marca -->
              <div>
                <label class="block text-[11px] font-bold text-stone-500 uppercase tracking-wider mb-1">Marca *</label>
                <input
                  v-model="formVehiculo.marca"
                  required
                  placeholder="Ej: Scania, Volvo"
                  class="input-field"
                />
              </div>
              <!-- Modelo -->
              <div>
                <label class="block text-[11px] font-bold text-stone-500 uppercase tracking-wider mb-1">Modelo</label>
                <input
                  v-model="formVehiculo.modelo"
                  placeholder="Ej: FH16, R500"
                  class="input-field"
                />
              </div>
              <!-- Año -->
              <div>
                <label class="block text-[11px] font-bold text-stone-500 uppercase tracking-wider mb-1">Año de fabricación</label>
                <input
                  v-model.number="formVehiculo.anio"
                  type="number"
                  placeholder="Ej: 2021"
                  class="input-field"
                />
              </div>
              <!-- Capacidad -->
              <div>
                <label class="block text-[11px] font-bold text-stone-500 uppercase tracking-wider mb-1">Capacidad (Toneladas o m³)</label>
                <input
                  v-model.number="formVehiculo.capacidadToneladasM3"
                  type="number"
                  step="0.1"
                  placeholder="Ej: 20.5"
                  class="input-field"
                />
              </div>
            </div>

            <hr class="border-stone-100 my-2" />
            <h4 class="text-xs font-bold text-amber-800 uppercase tracking-wider">Conductor Asignado</h4>
            
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <!-- Conductor Nombre -->
              <div class="sm:col-span-2">
                <label class="block text-[11px] font-bold text-stone-500 uppercase tracking-wider mb-1">Nombre Completo</label>
                <input
                  v-model="formVehiculo.conductorNombre"
                  placeholder="Ej: Juan Pérez"
                  class="input-field"
                />
              </div>
              <!-- Licencia -->
              <div>
                <label class="block text-[11px] font-bold text-stone-500 uppercase tracking-wider mb-1">Licencia de Conducir</label>
                <input
                  v-model="formVehiculo.conductorLicencia"
                  placeholder="Ej: Q21356789"
                  class="input-field"
                />
              </div>
              <!-- Telefono -->
              <div>
                <label class="block text-[11px] font-bold text-stone-500 uppercase tracking-wider mb-1">Celular de Contacto</label>
                <input
                  v-model="formVehiculo.conductorTelefono"
                  placeholder="Ej: 987654321"
                  class="input-field"
                />
              </div>
            </div>

            <hr class="border-stone-100 my-2" />
            <h4 class="text-xs font-bold text-amber-800 uppercase tracking-wider">Odómetro y Vencimientos</h4>

            <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <!-- KM Actual -->
              <div>
                <label class="block text-[11px] font-bold text-stone-500 uppercase tracking-wider mb-1">Odómetro (Km)</label>
                <input
                  v-model.number="formVehiculo.kmActual"
                  type="number"
                  required
                  class="input-field"
                />
              </div>
              <!-- Vencimiento SOAT -->
              <div>
                <label class="block text-[11px] font-bold text-stone-500 uppercase tracking-wider mb-1">SOAT Expiración</label>
                <input
                  v-model="formVehiculo.vencimientoSoat"
                  type="date"
                  class="input-field"
                />
              </div>
              <!-- Vencimiento Revision Tecnica -->
              <div>
                <label class="block text-[11px] font-bold text-stone-500 uppercase tracking-wider mb-1">R.T. Expiración</label>
                <input
                  v-model="formVehiculo.vencimientoRevisionTecnica"
                  type="date"
                  class="input-field"
                />
              </div>
            </div>

            <div class="p-3 bg-stone-50 border border-stone-200 rounded-lg text-stone-500 text-[11px]">
              * Campos obligatorios. La generación del próximo servicio se establecerá automáticamente sumando 5,000 km al odómetro inicial.
            </div>

            <!-- Acciones -->
            <div class="flex gap-3 pt-4 border-t border-stone-150">
              <button type="button" @click="cerrarModalVehiculo" class="btn-secondary flex-1">Cancelar</button>
              <button type="submit" :disabled="formSubmitting" class="btn-primary flex-1">
                {{ formSubmitting ? 'Registrando...' : 'Confirmar Registro' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>

    <!-- MODAL 2: REGISTRAR MANTENIMIENTO -->
    <Teleport to="body">
      <div v-if="modalMantenimientoVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <!-- Overlay -->
        <div class="absolute inset-0 bg-stone-900/60 backdrop-blur-sm" @click="cerrarModalMantenimiento"></div>
        <!-- Contenido Modal -->
        <div class="relative bg-white rounded-2xl shadow-2xl w-full max-w-lg overflow-hidden animate-fade-in flex flex-col max-h-[90vh]">
          <div class="p-6 border-b border-stone-150 flex items-center justify-between bg-stone-50">
            <h3 class="text-lg font-bold text-stone-950 flex items-center gap-2">
              <span>🔧</span> Registrar Servicio de Taller
            </h3>
            <button @click="cerrarModalMantenimiento" class="text-stone-400 hover:text-stone-600 text-lg">✕</button>
          </div>

          <div class="px-6 py-3 bg-amber-50 border-b border-amber-100 flex items-center justify-between">
            <span class="text-xs font-bold text-amber-800">Vehículo: Placa {{ vehiculoSeleccionado?.placa }}</span>
            <span class="text-xs text-stone-500">Km actual: {{ formatKm(vehiculoSeleccionado?.kmActual) }}</span>
          </div>

          <form @submit.prevent="guardarMantenimiento" class="p-6 space-y-4 overflow-y-auto flex-1">
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <!-- Tipo -->
              <div>
                <label class="block text-[11px] font-bold text-stone-500 uppercase tracking-wider mb-1">Tipo de Mantenimiento *</label>
                <select v-model="formMantenimiento.tipo" required class="input-field">
                  <option value="PREVENTIVO">Preventivo (Programado)</option>
                  <option value="CORRECTIVO">Correctivo (Reparación de Falla)</option>
                  <option value="REVISION">Revisión Técnica / Inspección</option>
                </select>
              </div>
              <!-- Costo -->
              <div>
                <label class="block text-[11px] font-bold text-stone-500 uppercase tracking-wider mb-1">Costo Total (Soles) *</label>
                <input
                  v-model.number="formMantenimiento.costo"
                  type="number"
                  step="0.01"
                  required
                  placeholder="Ej: 850.00"
                  class="input-field"
                />
              </div>
              <!-- Taller -->
              <div>
                <label class="block text-[11px] font-bold text-stone-500 uppercase tracking-wider mb-1">Taller de Servicio</label>
                <input
                  v-model="formMantenimiento.taller"
                  placeholder="Ej: Mecánica Automotriz SAC"
                  class="input-field"
                />
              </div>
              <!-- Tecnico Responsable -->
              <div>
                <label class="block text-[11px] font-bold text-stone-500 uppercase tracking-wider mb-1">Técnico Mecánico</label>
                <input
                  v-model="formMantenimiento.tecnicoResponsable"
                  placeholder="Ej: Ing. Luis Flores"
                  class="input-field"
                />
              </div>
              <!-- Fecha Ingreso -->
              <div>
                <label class="block text-[11px] font-bold text-stone-500 uppercase tracking-wider mb-1">Fecha Ingreso *</label>
                <input
                  v-model="formMantenimiento.fechaIngreso"
                  type="date"
                  required
                  class="input-field"
                />
              </div>
              <!-- Fecha Salida -->
              <div>
                <label class="block text-[11px] font-bold text-stone-500 uppercase tracking-wider mb-1">Fecha Salida (En taller si vacío)</label>
                <input
                  v-model="formMantenimiento.fechaSalida"
                  type="date"
                  class="input-field"
                />
              </div>
            </div>

            <!-- Descripción -->
            <div>
              <label class="block text-[11px] font-bold text-stone-500 uppercase tracking-wider mb-1">Descripción del Trabajo Realizado *</label>
              <textarea
                v-model="formMantenimiento.descripcion"
                required
                rows="3"
                placeholder="Escriba los detalles de los trabajos (cambio de aceite, repuestos, frenos...)"
                class="input-field resize-none"
              ></textarea>
            </div>

            <!-- Observaciones -->
            <div>
              <label class="block text-[11px] font-bold text-stone-500 uppercase tracking-wider mb-1">Observaciones o Recomendaciones</label>
              <textarea
                v-model="formMantenimiento.observaciones"
                rows="2"
                placeholder="Notas adicionales para la siguiente revisión..."
                class="input-field resize-none"
              ></textarea>
            </div>

            <div class="p-3 bg-stone-50 border border-stone-200 rounded-lg text-stone-500 text-[10px]">
              Al registrar la salida del taller, las alertas activas de este vehículo se resolverán automáticamente y se restablecerán los límites de kilometraje (+5,000 km).
            </div>

            <!-- Acciones -->
            <div class="flex gap-3 pt-4 border-t border-stone-150">
              <button type="button" @click="cerrarModalMantenimiento" class="btn-secondary flex-1">Cancelar</button>
              <button type="submit" :disabled="formSubmitting" class="btn-primary flex-1">
                {{ formSubmitting ? 'Registrando...' : 'Registrar Mantenimiento' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>

    <!-- MODAL 3: HISTORIAL DE MANTENIMIENTO -->
    <Teleport to="body">
      <div v-if="modalHistorialVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <!-- Overlay -->
        <div class="absolute inset-0 bg-stone-900/60 backdrop-blur-sm" @click="cerrarHistorial"></div>
        <!-- Contenido Modal -->
        <div class="relative bg-white rounded-2xl shadow-2xl w-full max-w-2xl overflow-hidden animate-fade-in flex flex-col max-h-[90vh]">
          <div class="p-6 border-b border-stone-150 flex items-center justify-between bg-stone-50">
            <h3 class="text-lg font-bold text-stone-950 flex items-center gap-2">
              <span>📋</span> Historial de Servicios Logísticos
            </h3>
            <button @click="cerrarHistorial" class="text-stone-400 hover:text-stone-600 text-lg">✕</button>
          </div>

          <div class="px-6 py-4 bg-stone-100 border-b border-stone-200 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
            <div>
              <span class="text-sm font-bold text-stone-800">Vehículo: Placa {{ vehiculoSeleccionado?.placa }}</span>
              <span class="text-xs text-stone-500 block sm:inline sm:ml-4">{{ vehiculoSeleccionado?.marca }} {{ vehiculoSeleccionado?.modelo }}</span>
            </div>
            <div class="text-xs text-stone-500 sm:text-right">
              <div>Kilometraje actual: <span class="font-bold text-stone-700">{{ formatKm(vehiculoSeleccionado?.kmActual) }} km</span></div>
              <div>Costo histórico acumulado: <span class="font-bold text-emerald-600">S/. {{ totalCostosAcumulados }}</span></div>
            </div>
          </div>

          <!-- Historial Content -->
          <div class="p-6 overflow-y-auto flex-1 space-y-6">
            <div v-if="loadingHistorial" class="flex flex-col items-center justify-center py-12">
              <LoadingSpinner mensaje="Buscando historial..." />
            </div>

            <div v-else-if="historial.length === 0" class="text-center py-12 text-stone-400 italic">
              Este vehículo no registra mantenimientos previos en el sistema.
            </div>

            <!-- Timeline -->
            <div v-else class="relative border-l border-stone-200 pl-6 ml-3 space-y-8">
              <div v-for="h in historial" :key="h.id" class="relative">
                <!-- Dot indicator -->
                <span class="absolute -left-[31px] top-1.5 w-4 h-4 rounded-full border-2 border-white flex items-center justify-center text-[10px]"
                      :class="timelineDotClase(h.tipo)">
                </span>

                <div class="card p-4 border border-stone-150 bg-stone-50/40">
                  <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                    <span class="text-xs font-bold text-stone-400 uppercase tracking-wider">
                      Fecha: {{ formatFecha(h.fechaIngreso) }} {{ h.fechaSalida ? ' a ' + formatFecha(h.fechaSalida) : ' (En taller)' }}
                    </span>
                    <span class="px-2 py-0.5 rounded text-[10px] font-bold text-white tracking-wider self-start sm:self-center"
                          :class="timelineBadgeClase(h.tipo)">
                      {{ h.tipo }}
                    </span>
                  </div>
                  
                  <h4 class="font-bold text-stone-800 mt-2 text-sm">{{ h.descripcion }}</h4>
                  
                  <div class="grid grid-cols-1 sm:grid-cols-3 gap-2 mt-4 pt-3 border-t border-stone-200/50 text-[11px] text-stone-600">
                    <div>
                      <span class="font-bold block text-stone-400 uppercase text-[9px]">Taller</span>
                      {{ h.taller || 'No especificado' }}
                    </div>
                    <div>
                      <span class="font-bold block text-stone-400 uppercase text-[9px]">Técnico responsable</span>
                      {{ h.tecnicoResponsable || 'No especificado' }}
                    </div>
                    <div>
                      <span class="font-bold block text-stone-400 uppercase text-[9px]">Costo de Servicio</span>
                      <span class="font-bold text-stone-900">S/. {{ h.costo ? h.costo.toFixed(2) : '0.00' }}</span>
                    </div>
                  </div>

                  <div v-if="h.observaciones" class="mt-3 p-2 bg-stone-100/60 rounded text-[11px] text-stone-500">
                    <strong class="text-stone-600">Observaciones:</strong> {{ h.observaciones }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="p-4 border-t border-stone-150 bg-stone-50 flex">
            <button @click="cerrarHistorial" class="btn-secondary w-full text-center">Cerrar Historial</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useMantenimientoStore } from '@/stores/mantenimiento'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import AlertMessage from '@/components/common/AlertMessage.vue'

const authStore = useAuthStore()
const store = useMantenimientoStore()

// State
const activeTab = ref('vehiculos')
const filtroPlaca = ref('')
const filtroEstado = ref('')
const mensajeExito = ref(null)

// Modal states
const modalVehiculoVisible = ref(false)
const modalMantenimientoVisible = ref(false)
const modalHistorialVisible = ref(false)
const formSubmitting = ref(false)
const loadingHistorial = ref(false)

// Selections
const vehiculoSeleccionado = ref(null)
const historial = ref([])

// Form states
const formVehiculo = ref({
  placa: '',
  marca: '',
  modelo: '',
  anio: new Date().getFullYear(),
  tipo: 'camion',
  capacidadToneladasM3: 15.0,
  conductorNombre: '',
  conductorLicencia: '',
  conductorTelefono: '',
  kmActual: 0.0,
  vencimientoSoat: '',
  vencimientoRevisionTecnica: ''
})

const formMantenimiento = ref({
  tipo: 'PREVENTIVO',
  descripcion: '',
  taller: '',
  costo: 0.0,
  tecnicoResponsable: '',
  observaciones: '',
  fechaIngreso: new Date().toISOString().split('T')[0],
  fechaSalida: new Date().toISOString().split('T')[0]
})

// Fetch data
onMounted(async () => {
  await cargarTodo()
})

async function cargarTodo() {
  await store.fetchVehiculos()
  await store.fetchAlertas()
}

// Computeds
const countOperativos = computed(() => {
  return store.vehiculos.filter(v => v.estado === 'OPERATIVO').length
})

const countEnTaller = computed(() => {
  return store.vehiculos.filter(v => v.estado === 'EN_MANTENIMIENTO').length
})

const countRequierenMant = computed(() => {
  return store.vehiculos.filter(v => v.requiereMantenimiento).length
})

const vehiculosFiltrados = computed(() => {
  return store.vehiculos.filter(v => {
    const text = filtroPlaca.value.toLowerCase()
    const coincideBuscar = !text ||
      v.placa.toLowerCase().includes(text) ||
      (v.conductorNombre && v.conductorNombre.toLowerCase().includes(text)) ||
      (v.marca && v.marca.toLowerCase().includes(text))

    const coincideEstado = !filtroEstado.value || v.estado === filtroEstado.value

    return coincideBuscar && coincideEstado
  })
})

const totalCostosAcumulados = computed(() => {
  const suma = historial.value.reduce((acc, h) => acc + (h.costo || 0), 0)
  return suma.toFixed(2)
})

// Formatting Helpers
function formatKm(km) {
  if (km == null) return '0'
  return new Intl.NumberFormat('es-PE').format(km)
}

function formatFecha(dateStr) {
  if (!dateStr) return 'No registrada'
  const options = { year: 'numeric', month: 'short', day: 'numeric', timeZone: 'UTC' }
  return new Date(dateStr).toLocaleDateString('es-PE', options)
}

function formatDateTime(dateTimeStr) {
  if (!dateTimeStr) return ''
  const options = { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }
  return new Date(dateTimeStr).toLocaleDateString('es-PE', options)
}

function formatTipoAlerta(tipo) {
  const mapa = {
    KM_EXCEDIDO: 'Kilometraje Excedido',
    SOAT_POR_VENCER: 'Vencimiento SOAT',
    REVISION_POR_VENCER: 'Vencimiento Revisión Técnica'
  }
  return mapa[tipo] || tipo
}

function estadoLabel(est) {
  const mapa = {
    OPERATIVO: 'Operativo',
    EN_MANTENIMIENTO: 'En Taller',
    INACTIVO: 'Inactivo'
  }
  return mapa[est] || est
}

// CSS Helpers
function badgeEstadoClase(est) {
  const mapa = {
    OPERATIVO: 'bg-emerald-100 text-emerald-800',
    EN_MANTENIMIENTO: 'bg-amber-100 text-amber-800 border border-amber-200',
    INACTIVO: 'bg-stone-200 text-stone-600'
  }
  return mapa[est] || 'bg-stone-100 text-stone-500'
}

function barraKmPorcentaje(v) {
  if (!v.kmProximoMantenimiento) return 0
  const totalKmIntervalo = v.kmProximoMantenimiento - v.kmUltimoMantenimiento
  if (totalKmIntervalo <= 0) return 100
  const kmTranscurridos = v.kmActual - v.kmUltimoMantenimiento
  const pct = (kmTranscurridos / totalKmIntervalo) * 100
  return Math.min(100, Math.max(0, pct))
}

function barraKmClase(v) {
  const pct = barraKmPorcentaje(v)
  if (pct >= 95 || v.requiereMantenimiento) return 'bg-rose-500 shadow-sm shadow-rose-600/30'
  if (pct >= 75) return 'bg-amber-500'
  return 'bg-emerald-500'
}

function fechaVencimientoClase(dateStr) {
  if (!dateStr) return 'text-stone-400 italic'
  const hoy = new Date()
  const fecha = new Date(dateStr)
  const diffDays = Math.ceil((fecha - hoy) / (1000 * 60 * 60 * 24))

  if (diffDays < 0) return 'text-rose-600 font-bold bg-rose-50 px-1 rounded'
  if (diffDays <= 7) return 'text-rose-500 font-semibold'
  if (diffDays <= 30) return 'text-amber-600 font-medium'
  return 'text-stone-600'
}

function timelineDotClase(tipo) {
  const mapa = {
    PREVENTIVO: 'bg-emerald-500 border-white ring-4 ring-emerald-50 text-emerald-800',
    CORRECTIVO: 'bg-rose-500 border-white ring-4 ring-rose-50 text-rose-800',
    REVISION: 'bg-sky-500 border-white ring-4 ring-sky-50 text-sky-800'
  }
  return mapa[tipo] || 'bg-stone-500 ring-stone-50'
}

function timelineBadgeClase(tipo) {
  const mapa = {
    PREVENTIVO: 'bg-emerald-600',
    CORRECTIVO: 'bg-rose-600',
    REVISION: 'bg-sky-600'
  }
  return mapa[tipo] || 'bg-stone-600'
}

// Modal Handlers — Nuevo Vehiculo
function abrirModalVehiculo() {
  formVehiculo.value = {
    placa: '',
    marca: '',
    modelo: '',
    anio: new Date().getFullYear(),
    tipo: 'camion',
    capacidadToneladasM3: 15.0,
    conductorNombre: '',
    conductorLicencia: '',
    conductorTelefono: '',
    kmActual: 0.0,
    vencimientoSoat: '',
    vencimientoRevisionTecnica: ''
  }
  modalVehiculoVisible.value = true
}

function cerrarModalVehiculo() {
  modalVehiculoVisible.value = false
}

async function guardarVehiculo() {
  formSubmitting.value = true
  try {
    await store.registrarVehiculo(formVehiculo.value)
    mensajeExito.value = `Vehículo con placa ${formVehiculo.value.placa} registrado con éxito.`
    cerrarModalVehiculo()
    await cargarTodo()
  } catch (e) {
    console.error(e)
  } finally {
    formSubmitting.value = false
  }
}

// Modal Handlers — Registrar Mantenimiento
function abrirModalMantenimiento(vehiculo) {
  vehiculoSeleccionado.value = vehiculo
  formMantenimiento.value = {
    tipo: 'PREVENTIVO',
    descripcion: '',
    taller: '',
    costo: 0.0,
    tecnicoResponsable: '',
    observaciones: '',
    fechaIngreso: new Date().toISOString().split('T')[0],
    fechaSalida: new Date().toISOString().split('T')[0]
  }
  modalMantenimientoVisible.value = true
}

function abrirMantenimientoDesdeAlerta(alerta) {
  const vehiculo = store.vehiculos.find(v => v.placa === alerta.placaVehiculo)
  if (vehiculo) {
    abrirModalMantenimiento(vehiculo)
  } else {
    // Si no está cargada en memoria, buscarla de API
    store.fetchVehiculoPorPlaca(alerta.placaVehiculo).then(data => {
      abrirModalMantenimiento(data)
    })
  }
}

function cerrarModalMantenimiento() {
  modalMantenimientoVisible.value = false
  vehiculoSeleccionado.value = null
}

async function guardarMantenimiento() {
  formSubmitting.value = true
  try {
    await store.registrarMantenimiento(vehiculoSeleccionado.value.id, formMantenimiento.value)
    mensajeExito.value = `Servicio de taller registrado exitosamente para ${vehiculoSeleccionado.value.placa}.`
    cerrarModalMantenimiento()
    await cargarTodo()
  } catch (e) {
    console.error(e)
  } finally {
    formSubmitting.value = false
  }
}

// Modal Handlers — Historial
async function abrirHistorial(vehiculo) {
  vehiculoSeleccionado.value = vehiculo
  modalHistorialVisible.value = true
  loadingHistorial.value = true
  try {
    historial.value = await store.fetchHistorial(vehiculo.id)
  } catch (e) {
    console.error(e)
    cerrarHistorial()
  } finally {
    loadingHistorial.value = false
  }
}

function cerrarHistorial() {
  modalHistorialVisible.value = false
  vehiculoSeleccionado.value = null
  historial.value = []
}
</script>

<style scoped>
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.animate-fade-in {
  animation: fadeIn 0.25s ease-out;
}

@keyframes pulse-subtle {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.05); opacity: 0.85; }
}

.animate-pulse-subtle {
  animation: pulse-subtle 2s ease-in-out infinite;
}
</style>
