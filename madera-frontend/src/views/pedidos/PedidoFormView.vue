<template>
  <div class="max-w-2xl mx-auto space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-stone-900">Solicitar Suministro de Madera</h1>
        <p class="text-stone-500 text-sm mt-1">Registrar un nuevo pedido de madera estructural para soporte en mina.</p>
      </div>
      <RouterLink to="/pedidos" class="btn-secondary text-xs">
        ⬅ Volver a Pedidos
      </RouterLink>
    </div>

    <!-- Form Card -->
    <div class="card bg-white p-8">
      <form @submit.prevent="handleSubmit" class="space-y-6">
        <!-- Paso 1: Seleccionar Mina de Destino -->
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-6">
          <div>
            <label for="mina" class="block text-xs font-semibold text-stone-700 uppercase tracking-wider mb-1.5">
              Sede / Mina Solicitante
            </label>
            <select id="mina" v-model="form.mina" @change="alCambiarMina" required class="input-field select-custom">
              <option value="" disabled>Seleccione una sede...</option>
              <option value="Yanacocha">Mina Yanacocha</option>
              <option value="Antamina">Mina Antamina</option>
              <option value="Las Bambas">Mina Las Bambas</option>
              <option value="Cerro Verde">Mina Cerro Verde</option>
            </select>
          </div>

          <div>
            <label for="area" class="block text-xs font-semibold text-stone-700 uppercase tracking-wider mb-1.5">
              Área de Destino / Trabajo
            </label>
            <input
              id="area"
              v-model="form.areaSolicitante"
              type="text"
              required
              class="input-field"
              placeholder="Ej. Galería Sur - Nivel 320"
            />
          </div>
        </div>

        <!-- Paso 2: Seleccionar Madera del Catálogo disponible en esa Mina -->
        <div>
          <label for="madera" class="block text-xs font-semibold text-stone-700 uppercase tracking-wider mb-1.5">
            Madera Estructural
          </label>
          <div v-if="cargandoInventario" class="py-2 flex items-center gap-2 text-stone-500 text-xs">
            <LoadingSpinner size="sm" />
            <span>Cargando catálogo de madera para esta mina...</span>
          </div>
          <select
            v-else
            id="madera"
            v-model="seleccionMadera"
            @change="alCambiarMadera"
            required
            :disabled="!form.mina"
            class="input-field select-custom"
          >
            <option :value="null" disabled>
              {{ form.mina ? 'Seleccione tipo de madera estructural...' : 'Debe seleccionar una Sede/Mina primero' }}
            </option>
            <option v-for="madera in maderasDisponibles" :key="madera.id" :value="madera">
              {{ madera.tipo }} (Para: {{ formatUso(madera.uso) }}) — Stock: {{ madera.stockDisponible != null ? madera.stockDisponible : 0 }} {{ madera.unidad }} — S/. {{ madera.precioPorUnidad != null ? madera.precioPorUnidad.toFixed(2) : '0.00' }}
            </option>
          </select>
          <p v-if="!cargandoInventario && form.mina && maderasDisponibles.length === 0" class="text-xs text-rose-600 mt-1.5">
            ⚠️ No hay madera disponible registrada en el inventario para la sede <b>{{ form.mina }}</b>.
          </p>
        </div>

        <!-- Paso 3: Definir Cantidad a solicitar -->
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-6" v-if="seleccionMadera">
          <div>
            <label for="cantidad" class="block text-xs font-semibold text-stone-700 uppercase tracking-wider mb-1.5">
              Cantidad Requerida
            </label>
            <div class="relative">
              <input
                id="cantidad"
                v-model.number="form.cantidadSolicitada"
                type="number"
                min="1"
                :max="seleccionMadera.stockDisponible"
                required
                class="input-field pr-16"
                placeholder="Ej. 15"
              />
              <span class="absolute inset-y-0 right-0 pr-3.5 flex items-center text-xs text-stone-400 font-bold capitalize pointer-events-none">
                {{ form.unidad }}
              </span>
            </div>
            <span class="text-[10px] text-stone-400 block mt-1">
              Máximo disponible: <b>{{ seleccionMadera.stockDisponible }} {{ form.unidad }}</b>
            </span>
          </div>

          <div>
            <label class="block text-xs font-semibold text-stone-700 uppercase tracking-wider mb-1.5">
              Costo Estimado
            </label>
            <div class="input-field bg-stone-50 border-stone-200 select-none font-semibold text-stone-800 py-3">
              S/. {{ costoEstimado.toFixed(2) }}
            </div>
          </div>
        </div>

        <!-- Alert messages -->
        <AlertMessage v-if="errorMsg" tipo="error">
          {{ errorMsg }}
        </AlertMessage>

        <AlertMessage v-if="successMsg" tipo="success">
          {{ successMsg }}
        </AlertMessage>

        <!-- Actions -->
        <div class="flex items-center justify-end gap-3 pt-4 border-t border-stone-100">
          <button type="button" @click="resetForm" class="btn-secondary" :disabled="loading">
            Limpiar Formulario
          </button>
          <button
            type="submit"
            :disabled="loading || !seleccionMadera || maderasDisponibles.length === 0"
            class="btn-primary flex items-center gap-2 px-6"
          >
            <LoadingSpinner v-if="loading" size="sm" class="!text-white" />
            <span>{{ loading ? 'Registrando Pedido...' : 'Enviar Solicitud de Pedido' }}</span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { usePedidosStore } from '@/stores/pedidos'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import AlertMessage from '@/components/common/AlertMessage.vue'
import api from '@/services/api'

const router = useRouter()
const authStore = useAuthStore()
const pedidosStore = usePedidosStore()

const form = reactive({
  maderaId: null,
  tipoMadera: '',
  cantidadSolicitada: null,
  unidad: 'm3',
  mina: '',
  areaSolicitante: '',
  solicitadoPor: authStore.usuario?.email || ''
})

const seleccionMadera = ref(null)
const maderasDisponibles = ref([])
const cargandoInventario = ref(false)
const loading = ref(false)
const errorMsg = ref(null)
const successMsg = ref(null)

const costoEstimado = computed(() => {
  if (!seleccionMadera.value || !form.cantidadSolicitada) return 0
  return seleccionMadera.value.precioPorUnidad * form.cantidadSolicitada
})

async function alCambiarMina() {
  if (!form.mina) return
  cargandoInventario.value = true
  seleccionMadera.value = null
  maderasDisponibles.value = []
  form.maderaId = null
  form.tipoMadera = ''
  form.cantidadSolicitada = null

  try {
    const { data } = await api.get(`/api/inventario/mina/${form.mina}`)
    // Filtrar solo las maderas con stock disponible
    maderasDisponibles.value = (data || []).filter(m => m.stockDisponible > 0)
  } catch (err) {
    console.error('Error cargando maderas de mina:', err)
  } finally {
    cargandoInventario.value = false
  }
}

function alCambiarMadera() {
  if (!seleccionMadera.value) return
  form.maderaId = seleccionMadera.value.id
  form.tipoMadera = seleccionMadera.value.tipo
  form.unidad = seleccionMadera.value.unidad
  form.cantidadSolicitada = null
}

function formatUso(uso) {
  const mapa = {
    soporte_galeria: 'Soporte de Galería',
    entibado: 'Entibado',
    cuadros: 'Cuadros estructurales'
  }
  return mapa[uso] || uso
}

function resetForm() {
  form.mina = ''
  form.areaSolicitante = ''
  form.maderaId = null
  form.tipoMadera = ''
  form.cantidadSolicitada = null
  seleccionMadera.value = null
  maderasDisponibles.value = []
  errorMsg.value = null
  successMsg.value = null
}

async function handleSubmit() {
  if (!form.mina || !form.areaSolicitante || !form.maderaId || !form.cantidadSolicitada) {
    errorMsg.value = 'Por favor complete todos los campos obligatorios.'
    return
  }

  if (form.cantidadSolicitada > seleccionMadera.value.stockDisponible) {
    errorMsg.value = `No puede solicitar más de la cantidad disponible en stock (${seleccionMadera.value.stockDisponible}).`
    return
  }

  loading.value = true
  errorMsg.value = null
  successMsg.value = null

  try {
    const payload = {
      maderaId: form.maderaId,
      tipoMadera: form.tipoMadera,
      cantidadSolicitada: form.cantidadSolicitada,
      unidad: form.unidad,
      mina: form.mina,
      areaSolicitante: form.areaSolicitante,
      solicitadoPor: form.solicitadoPor
    }

    await pedidosStore.crearPedido(payload)
    successMsg.value = '¡Solicitud de pedido enviada correctamente! En espera de revisión por el Jefe de Almacén.'
    setTimeout(() => {
      router.push('/pedidos')
    }, 1500)
  } catch (err) {
    console.error('Error al solicitar pedido:', err)
    errorMsg.value = err.response?.data?.mensaje || 'Error al enviar la solicitud del pedido.'
  } finally {
    loading.value = false
  }
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
