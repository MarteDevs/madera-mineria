<template>
  <div class="max-w-2xl mx-auto space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-stone-900">Registrar Nueva Madera</h1>
        <p class="text-stone-500 text-sm mt-1">Registrar un nuevo tipo de madera estructural para uso en mina.</p>
      </div>
      <RouterLink to="/inventario" class="btn-secondary text-xs">
        ⬅ Volver al Inventario
      </RouterLink>
    </div>

    <!-- Form Card -->
    <div class="card bg-white p-8">
      <form @submit.prevent="handleSubmit" class="space-y-6">
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-6">
          <div>
            <label for="tipo" class="block text-xs font-semibold text-stone-700 uppercase tracking-wider mb-1.5">
              Tipo de Madera
            </label>
            <input
              id="tipo"
              v-model="form.tipo"
              type="text"
              required
              class="input-field text-sm"
              placeholder="Ej. Eucalipto Serrano, Pino Oregón"
            />
          </div>

          <div>
            <label for="uso" class="block text-xs font-semibold text-stone-700 uppercase tracking-wider mb-1.5">
              Uso / Aplicación Estructural
            </label>
            <select id="uso" v-model="form.uso" required class="input-field select-custom">
              <option value="soporte_galeria">Soporte de Galería</option>
              <option value="entibado">Entibado (Revestimiento)</option>
              <option value="cuadros">Cuadros de Madera</option>
            </select>
          </div>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-3 gap-6">
          <div>
            <label for="unidad" class="block text-xs font-semibold text-stone-700 uppercase tracking-wider mb-1.5">
              Unidad de Medida
            </label>
            <select id="unidad" v-model="form.unidad" required class="input-field select-custom">
              <option value="m3">Metros Cúbicos (m³)</option>
              <option value="metro_lineal">Metros Lineales</option>
              <option value="unidad">Unidades</option>
            </select>
          </div>

          <div>
            <label for="precio" class="block text-xs font-semibold text-stone-700 uppercase tracking-wider mb-1.5">
              Precio Unitario (S/.)
            </label>
            <input
              id="precio"
              v-model.number="form.precio"
              type="number"
              step="0.01"
              min="0.01"
              required
              class="input-field text-sm"
              placeholder="Ej. 120.00"
            />
          </div>

          <div>
            <label for="stock" class="block text-xs font-semibold text-stone-700 uppercase tracking-wider mb-1.5">
              Stock Inicial
            </label>
            <input
              id="stock"
              v-model.number="form.stock"
              type="number"
              min="0"
              required
              class="input-field text-sm"
              placeholder="Ej. 100"
            />
          </div>
        </div>

        <div>
          <label for="mina" class="block text-xs font-semibold text-stone-700 uppercase tracking-wider mb-1.5">
            Sede / Mina de Almacenamiento
          </label>
          <select id="mina" v-model="form.mina" required class="input-field select-custom">
            <option value="Yanacocha">Mina Yanacocha</option>
            <option value="Antamina">Mina Antamina</option>
            <option value="Las Bambas">Mina Las Bambas</option>
            <option value="Cerro Verde">Mina Cerro Verde</option>
          </select>
        </div>

        <!-- Alert messages -->
        <AlertMessage v-if="errorMsg" tipo="error">
          {{ errorMsg }}
        </AlertMessage>

        <AlertMessage v-slot:default v-if="successMsg" tipo="success">
          {{ successMsg }}
        </AlertMessage>

        <!-- Actions buttons -->
        <div class="flex items-center justify-end gap-3 pt-4 border-t border-stone-100">
          <button type="reset" class="btn-secondary" :disabled="loading">
            Restaurar
          </button>
          <button
            type="submit"
            :disabled="loading"
            class="btn-primary flex items-center gap-2 px-6"
          >
            <LoadingSpinner v-if="loading" size="sm" class="!text-white" />
            <span>{{ loading ? 'Guardando Registro...' : 'Registrar Madera' }}</span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useInventarioStore } from '@/stores/inventario'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import AlertMessage from '@/components/common/AlertMessage.vue'

const router = useRouter()
const inventarioStore = useInventarioStore()

const form = reactive({
  tipo: '',
  uso: 'soporte_galeria',
  unidad: 'm3',
  precio: null,
  stock: null,
  mina: 'Yanacocha'
})

const loading = ref(false)
const errorMsg = ref(null)
const successMsg = ref(null)

async function handleSubmit() {
  loading.value = true
  errorMsg.value = null
  successMsg.value = null

  if (form.precio == null || form.precio <= 0 || form.stock == null || form.stock < 0) {
    errorMsg.value = 'El precio debe ser mayor a 0 y el stock no puede estar vacío ni ser negativo.'
    loading.value = false
    return
  }

  try {
    const payload = {
      tipo: form.tipo.trim(),
      uso: form.uso,
      unidad: form.unidad,
      precioPorUnidad: parseFloat(form.precio),
      stockDisponible: parseInt(form.stock),
      mina: form.mina,
      estado: form.stock > 0 ? 'DISPONIBLE' : 'AGOTADO'
    }

    await inventarioStore.crearMadera(payload)
    successMsg.value = '¡Madera registrada exitosamente en el inventario!'
    setTimeout(() => {
      router.push('/inventario')
    }, 1500)
  } catch (err) {
    console.error('Error registrando madera:', err)
    errorMsg.value = err.response?.data?.mensaje || 'Ocurrió un error al registrar la madera.'
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
