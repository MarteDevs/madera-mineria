<template>
  <div class="card p-8 bg-white/95 backdrop-blur-md rounded-2xl shadow-2xl border border-stone-200/50">
    <!-- Logo & Header -->
    <div class="text-center mb-8">
      <div class="inline-flex w-16 h-16 rounded-2xl bg-amber-600 items-center justify-center text-3xl shadow-lg shadow-amber-700/20 mb-4 animate-bounce-subtle">
        🪵
      </div>
      <h2 class="text-2xl font-bold text-stone-900 tracking-tight">Madera & Minería</h2>
      <p class="text-stone-500 text-sm mt-1.5">Mantenimiento y Control de Pedidos de Madera</p>
    </div>

    <!-- Login Form -->
    <form @submit.prevent="handleLogin" class="space-y-5">
      <div>
        <label for="email" class="block text-xs font-semibold text-stone-700 uppercase tracking-wider mb-1.5">
          Correo Electrónico
        </label>
        <div class="relative">
          <span class="absolute inset-y-0 left-0 pl-3.5 flex items-center text-stone-400">
            ✉️
          </span>
          <input
            id="email"
            v-model="form.email"
            type="email"
            required
            class="input-field pl-10"
            placeholder="correo@ejemplo.com"
          />
        </div>
      </div>

      <div>
        <label for="password" class="block text-xs font-semibold text-stone-700 uppercase tracking-wider mb-1.5">
          Contraseña
        </label>
        <div class="relative">
          <span class="absolute inset-y-0 left-0 pl-3.5 flex items-center text-stone-400">
            🔒
          </span>
          <input
            id="password"
            v-model="form.password"
            type="password"
            required
            class="input-field pl-10"
            placeholder="••••••••"
          />
        </div>
      </div>

      <!-- Alert messages -->
      <div v-if="errorMsg" class="bg-rose-50 border border-rose-100 text-rose-700 text-xs px-4 py-3 rounded-xl">
        {{ errorMsg }}
      </div>

      <!-- Submit button -->
      <button
        type="submit"
        :disabled="loading"
        class="w-full btn-primary py-3 rounded-xl font-bold text-sm tracking-wide shadow-md shadow-primary-700/10 flex items-center justify-center gap-2"
      >
        <LoadingSpinner v-if="loading" size="sm" class="!text-white" />
        <span v-if="loading">Verificando Credenciales...</span>
        <span v-else>Iniciar Sesión</span>
      </button>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const authStore = useAuthStore()
const router = useRouter()

const form = reactive({
  email: '',
  password: ''
})

const loading = ref(false)
const errorMsg = ref(null)

async function handleLogin() {
  loading.value = true
  errorMsg.value = null
  try {
    await authStore.login(form.email, form.password)
    router.push('/dashboard')
  } catch (err) {
    console.error('Error logging in:', err)
    errorMsg.value = err.response?.data?.mensaje || 'Error de conexión. Verifique su email y contraseña.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
@keyframes bounceSubtle {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}
.animate-bounce-subtle {
  animation: bounceSubtle 3s ease-in-out infinite;
}
</style>
