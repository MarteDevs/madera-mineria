<template>
  <div class="max-w-2xl mx-auto space-y-6 animate-fade-in">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-stone-900">Registrar Nuevo Personal</h1>
        <p class="text-stone-500 text-sm mt-1">Crea una nueva cuenta de personal con roles operativos específicos.</p>
      </div>
      <div>
        <RouterLink
          to="/usuarios"
          class="btn-secondary text-xs font-bold flex items-center gap-1.5"
        >
          ⬅️ Volver a Usuarios
        </RouterLink>
      </div>
    </div>

    <!-- Formulario -->
    <div class="card bg-white p-6 border border-stone-150 shadow-sm rounded-xl">
      <form @submit.prevent="handleRegister" class="space-y-6">
        
        <!-- Nombres y Apellidos -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label for="nombre" class="block text-xs font-semibold text-stone-700 uppercase tracking-wider mb-1.5">
              Nombre <span class="text-red-500">*</span>
            </label>
            <input
              id="nombre"
              v-model="form.nombre"
              type="text"
              required
              class="input-field text-sm"
              placeholder="Ej. Carlos"
            />
          </div>
          <div>
            <label for="apellido" class="block text-xs font-semibold text-stone-700 uppercase tracking-wider mb-1.5">
              Apellido <span class="text-red-500">*</span>
            </label>
            <input
              id="apellido"
              v-model="form.apellido"
              type="text"
              required
              class="input-field text-sm"
              placeholder="Ej. Mamani"
            />
          </div>
        </div>

        <!-- Email y Contraseña -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label for="email" class="block text-xs font-semibold text-stone-700 uppercase tracking-wider mb-1.5">
              Correo Electrónico <span class="text-red-500">*</span>
            </label>
            <input
              id="email"
              v-model="form.email"
              type="email"
              required
              class="input-field text-sm"
              placeholder="carlos.mamani@empresa.com"
            />
          </div>
          <div>
            <label for="password" class="block text-xs font-semibold text-stone-700 uppercase tracking-wider mb-1.5">
              Contraseña Temporal <span class="text-red-500">*</span>
            </label>
            <input
              id="password"
              v-model="form.password"
              type="password"
              required
              class="input-field text-sm"
              placeholder="Mínimo 6 caracteres"
            />
          </div>
        </div>

        <!-- Rol y Mina/Sede -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label for="rol" class="block text-xs font-semibold text-stone-700 uppercase tracking-wider mb-1.5">
              Rol de Seguridad <span class="text-red-500">*</span>
            </label>
            <select id="rol" v-model="form.rol" required class="input-field text-sm select-custom">
              <option value="ROLE_COMPRAS">Superintendente de Compras</option>
              <option value="ROLE_ALMACEN">Jefe de Almacén</option>
              <option value="ROLE_TRANSPORTE">Transportista / Operador</option>
              <option value="ROLE_ADMIN">Administrador de Sistema</option>
            </select>
          </div>
          <div>
            <label for="mina" class="block text-xs font-semibold text-stone-700 uppercase tracking-wider mb-1.5">
              Sede / Mina Asignada
            </label>
            <select id="mina" v-model="form.mina" class="input-field text-sm select-custom">
              <option value="">Ninguna (Oficina / Admin)</option>
              <option value="Yanacocha">Mina Yanacocha</option>
              <option value="Antamina">Mina Antamina</option>
              <option value="Las Bambas">Mina Las Bambas</option>
              <option value="Cerro Verde">Mina Cerro Verde</option>
              <option value="Oficina Central">Oficina Central / Lima</option>
            </select>
          </div>
        </div>

        <!-- Mensaje de Error Interno en Formulario -->
        <AlertMessage v-if="errorMsg" tipo="error">
          {{ errorMsg }}
        </AlertMessage>

        <!-- Botones de Acción -->
        <div class="flex items-center justify-end gap-3 pt-4 border-t border-stone-100">
          <RouterLink
            to="/usuarios"
            class="px-5 py-2.5 rounded-lg border border-stone-200 hover:border-stone-300 text-stone-600 font-bold text-xs hover:bg-stone-50 transition-all"
          >
            Cancelar
          </RouterLink>
          <button
            type="submit"
            :disabled="loading"
            class="px-6 py-2.5 rounded-lg bg-stone-900 hover:bg-stone-850 text-white font-bold text-xs shadow-sm flex items-center gap-2 disabled:opacity-50 transition-all"
          >
            <LoadingSpinner v-if="loading" size="sm" class="!text-white" />
            <span>{{ loading ? 'Registrando...' : 'Registrar Personal' }}</span>
          </button>
        </div>

      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/services/api'
import { useDialogStore } from '@/stores/dialog'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import AlertMessage from '@/components/common/AlertMessage.vue'

const router = useRouter()
const dialogStore = useDialogStore()

const form = reactive({
  nombre: '',
  apellido: '',
  email: '',
  password: '',
  rol: 'ROLE_COMPRAS',
  mina: ''
})

const loading = ref(false)
const errorMsg = ref(null)

async function handleRegister() {
  loading.value = true
  errorMsg.value = null
  try {
    // Si la mina es vacía, enviar null
    const payload = {
      ...form,
      mina: form.mina || null
    }

    await api.post('/api/auth/register', payload)

    // Mostrar alerta de éxito
    await dialogStore.alert({
      titulo: 'Personal Registrado',
      mensaje: `El usuario "${form.nombre} ${form.apellido}" ha sido registrado con éxito en el sistema.`,
      confirmLabel: 'Entendido',
      tipo: 'success'
    })

    // Redirigir al listado de usuarios
    router.push('/usuarios')
  } catch (err) {
    console.error('Error al registrar personal:', err)
    errorMsg.value = err.response?.data?.mensaje || 'No se pudo registrar al personal. Verifique que el correo no esté en uso.'
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
