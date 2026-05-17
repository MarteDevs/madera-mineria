<script setup>
import { ref, reactive } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useRouter } from 'vue-router';

const auth = useAuthStore();
const router = useRouter();

const form = reactive({
  username: '',
  password: ''
});

const handleLogin = async () => {
  const success = await auth.login(form);
  if (success) {
    router.push('/');
  }
};
</script>

<template>
  <div class="card">
    <div class="text-center mb-8">
      <h2 class="text-3xl font-bold text-madera-oscuro">Iniciar Sesión</h2>
      <p class="text-gray-500 mt-2">Accede al panel de Madera & Minería</p>
    </div>

    <form @submit.prevent="handleLogin" class="space-y-6">
      <div>
        <label class="block text-sm font-medium text-gray-700">Usuario</label>
        <input 
          v-model="form.username"
          type="text" 
          required
          class="mt-1 block w-full px-4 py-3 rounded-lg border border-gray-300 focus:ring-primary-500 focus:border-primary-500 outline-none transition-all"
          placeholder="Tu usuario"
        />
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700">Contraseña</label>
        <input 
          v-model="form.password"
          type="password" 
          required
          class="mt-1 block w-full px-4 py-3 rounded-lg border border-gray-300 focus:ring-primary-500 focus:border-primary-500 outline-none transition-all"
          placeholder="••••••••"
        />
      </div>

      <div v-if="auth.error" class="bg-red-50 text-red-600 p-3 rounded-lg text-sm border border-red-100">
        {{ auth.error }}
      </div>

      <button 
        type="submit" 
        :disabled="auth.loading"
        class="w-full btn-primary py-3 text-lg font-semibold flex items-center justify-center"
      >
        <span v-if="auth.loading">Cargando...</span>
        <span v-else>Entrar al Sistema</span>
      </button>
    </form>
  </div>
</template>
