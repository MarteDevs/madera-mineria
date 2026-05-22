<template>
  <div class="card p-8 bg-white/95 backdrop-blur-md rounded-2xl shadow-2xl border border-stone-200/50">
    <!-- Header -->
    <div class="text-center mb-6">
      <h2 class="text-2xl font-bold text-stone-900 tracking-tight">Crear Cuenta</h2>
      <p class="text-stone-500 text-sm mt-1.5">Registro de usuarios para Madera & Minería</p>
    </div>

    <!-- Register Form -->
    <form @submit.prevent="handleRegister" class="space-y-4">
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label for="nombre" class="block text-xs font-semibold text-stone-700 uppercase tracking-wider mb-1">
            Nombre
          </label>
          <input
            id="nombre"
            v-model="form.nombre"
            type="text"
            required
            class="input-field"
            placeholder="Juan"
          />
        </div>
        <div>
          <label for="apellido" class="block text-xs font-semibold text-stone-700 uppercase tracking-wider mb-1">
            Apellido
          </label>
          <input
            id="apellido"
            v-model="form.apellido"
            type="text"
            required
            class="input-field"
            placeholder="Pérez"
          />
        </div>
      </div>

      <div>
        <label for="email" class="block text-xs font-semibold text-stone-700 uppercase tracking-wider mb-1">
          Correo Electrónico
        </label>
        <input
          id="email"
          v-model="form.email"
          type="email"
          required
          class="input-field"
          placeholder="juan.perez@compania.com"
        />
      </div>

      <div>
        <label for="password" class="block text-xs font-semibold text-stone-700 uppercase tracking-wider mb-1">
          Contraseña
        </label>
        <input
          id="password"
          v-model="form.password"
          type="password"
          required
          class="input-field"
          placeholder="Mínimo 6 caracteres"
        />
      </div>

      <div class="grid grid-cols-2 gap-4">
        <div>
          <label for="rol" class="block text-xs font-semibold text-stone-700 uppercase tracking-wider mb-1">
            Rol de Operación
          </label>
          <select id="rol" v-model="form.rol" required class="input-field select-custom">
            <option value="ROLE_COMPRAS">Compras (Solicitante)</option>
            <option value="ROLE_ALMACEN">Almacén (Aprobador)</option>
            <option value="ROLE_TRANSPORTE">Transporte (Distribución)</option>
            <option value="ROLE_ADMIN">Administrador</option>
          </select>
        </div>
        <div>
          <label for="mina" class="block text-xs font-semibold text-stone-700 uppercase tracking-wider mb-1">
            Sede / Mina
          </label>
          <select id="mina" v-model="form.mina" required class="input-field select-custom">
            <option value="Yanacocha">Mina Yanacocha</option>
            <option value="Antamina">Mina Antamina</option>
            <option value="Las Bambas">Mina Las Bambas</option>
            <option value="Cerro Verde">Mina Cerro Verde</option>
            <option value="Oficina Central">Oficina Central / Lima</option>
          </select>
        </div>
      </div>

      <!-- Alerts -->
      <div v-if="errorMsg" class="bg-rose-50 border border-rose-100 text-rose-700 text-xs px-4 py-3 rounded-xl">
        {{ errorMsg }}
      </div>
      <div v-if="successMsg" class="bg-emerald-50 border border-emerald-100 text-emerald-700 text-xs px-4 py-3 rounded-xl">
        {{ successMsg }}
      </div>

      <!-- Submit button -->
      <button
        type="submit"
        :disabled="loading"
        class="w-full btn-primary py-3 rounded-xl font-bold text-sm tracking-wide shadow-md shadow-primary-700/10 flex items-center justify-center gap-2 mt-2"
      >
        <LoadingSpinner v-if="loading" size="sm" class="!text-white" />
        <span v-if="loading">Creando Cuenta...</span>
        <span v-else>Registrarse</span>
      </button>

      <!-- Back to login link -->
      <div class="text-center mt-4">
        <RouterLink to="/login" class="text-xs text-amber-600 hover:text-amber-700 font-medium">
          ¿Ya tienes cuenta? Iniciar Sesión
        </RouterLink>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/services/api'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const router = useRouter()

const form = reactive({
  nombre: '',
  apellido: '',
  email: '',
  password: '',
  rol: 'ROLE_COMPRAS',
  mina: 'Yanacocha'
})

const loading = ref(false)
const errorMsg = ref(null)
const successMsg = ref(null)

async function handleRegister() {
  loading.value = true
  errorMsg.value = null
  successMsg.value = null
  try {
    await api.post('/api/auth/register', form)
    successMsg.value = 'Registro exitoso. Redirigiendo al login...'
    setTimeout(() => {
      router.push('/login')
    }, 1500)
  } catch (err) {
    console.error('Error in registration:', err)
    errorMsg.value = err.response?.data?.mensaje || 'Error al intentar registrarse. Intente de nuevo.'
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
