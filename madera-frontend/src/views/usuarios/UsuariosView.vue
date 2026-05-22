<template>
  <div class="space-y-6 animate-fade-in">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-stone-900">Control de Usuarios</h1>
        <p class="text-stone-500 text-sm mt-1">Gestión administrativa de cuentas de usuario, roles de seguridad y accesos autorizados.</p>
      </div>
      <div>
        <RouterLink
          to="/register"
          class="btn-primary text-xs font-bold flex items-center gap-1.5 bg-stone-900 hover:bg-stone-850 shadow-sm transition-all"
        >
          ➕ Registrar Nuevo Personal
        </RouterLink>
      </div>
    </div>

    <!-- Filtros -->
    <div class="card p-4 bg-white border border-stone-150 shadow-sm">
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div>
          <label class="block text-xs font-semibold text-stone-500 uppercase tracking-wider mb-1.5">Buscar por nombre o correo</label>
          <input
            v-model="filtros.busqueda"
            placeholder="Ej. Juan Pérez..."
            class="input-field text-xs"
          />
        </div>
        <div>
          <label class="block text-xs font-semibold text-stone-500 uppercase tracking-wider mb-1.5">Filtrar por Rol</label>
          <select v-model="filtros.rol" class="input-field text-xs select-custom">
            <option value="">Todos los roles</option>
            <option value="ROLE_ADMIN">Administrador</option>
            <option value="ROLE_ALMACEN">Jefe de Almacén</option>
            <option value="ROLE_COMPRAS">Superintendente de Compras</option>
            <option value="ROLE_TRANSPORTE">Transportista / Operador</option>
          </select>
        </div>
        <div>
          <label class="block text-xs font-semibold text-stone-500 uppercase tracking-wider mb-1.5">Estado Cuenta</label>
          <select v-model="filtros.estado" class="input-field text-xs select-custom">
            <option value="">Todos los estados</option>
            <option value="activo">Activos</option>
            <option value="inactivo">Inactivos</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Mensajes de Alerta -->
    <AlertMessage v-if="error" tipo="error">
      {{ error }}
    </AlertMessage>

    <AlertMessage v-if="successMsg" tipo="exito">
      {{ successMsg }}
    </AlertMessage>

    <!-- Loading spinner -->
    <div v-if="loading" class="min-h-[250px] flex items-center justify-center card bg-white">
      <LoadingSpinner size="lg" class="text-amber-600" />
    </div>

    <!-- Table of Users -->
    <div v-else class="card p-0 overflow-hidden shadow-sm border border-stone-150 bg-white">
      <div class="overflow-x-auto">
        <table class="w-full text-left text-sm border-collapse">
          <thead>
            <tr class="bg-stone-50/80 border-b border-stone-200">
              <th class="table-header">ID</th>
              <th class="table-header">Personal</th>
              <th class="table-header">Correo Electrónico</th>
              <th class="table-header">Rol</th>
              <th class="table-header">Mina / Sede</th>
              <th class="table-header">Estado</th>
              <th class="table-header">Fecha Creación</th>
              <th class="table-header text-right">Acciones</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-stone-100">
            <tr v-for="user in usuariosFiltrados" :key="user.id" class="hover:bg-stone-50/50 transition-colors">
              <td class="table-cell font-mono text-xs text-stone-500">#{{ user.id }}</td>
              <td class="table-cell">
                <div class="font-bold text-stone-850">
                  {{ user.nombre }} {{ user.apellido || '' }}
                </div>
              </td>
              <td class="table-cell text-stone-600 font-medium text-xs">{{ user.email }}</td>
              <td class="table-cell">
                <span :class="obtenerClaseRol(user.rol)" class="text-[10px] font-bold px-2 py-0.5 rounded-full uppercase">
                  {{ formatRol(user.rol) }}
                </span>
              </td>
              <td class="table-cell">
                <span v-if="user.mina" class="text-stone-700 font-semibold text-xs bg-stone-100 px-2 py-0.5 rounded">
                  ⛰️ {{ user.mina }}
                </span>
                <span v-else class="text-stone-400 font-medium italic text-xs">Ninguna</span>
              </td>
              <td class="table-cell">
                <span
                  v-if="user.activo"
                  class="bg-emerald-100 text-emerald-800 text-[10px] font-bold px-2 py-0.5 rounded-full"
                >
                  Activo
                </span>
                <span
                  v-else
                  class="bg-red-100 text-red-800 text-[10px] font-bold px-2 py-0.5 rounded-full"
                >
                  Inactivo
                </span>
              </td>
              <td class="table-cell text-xs text-stone-500">{{ formatFecha(user.fechaCreacion) }}</td>
              <td class="table-cell text-right">
                <button
                  v-if="user.activo"
                  @click="confirmarDesactivacion(user)"
                  :disabled="togglingId === user.id"
                  class="text-xs text-red-650 hover:text-red-800 border border-red-200 hover:border-red-400 bg-white hover:bg-red-50/20 px-2.5 py-1.5 rounded-lg font-bold transition-all disabled:opacity-50"
                >
                  <span v-if="togglingId === user.id">Desactivando...</span>
                  <span v-else>⚠️ Desactivar</span>
                </button>
                <span v-else class="text-xs text-stone-400 italic font-semibold">Sin acciones</span>
              </td>
            </tr>
            <tr v-if="usuariosFiltrados.length === 0">
              <td colspan="8" class="text-center py-12 text-stone-400 italic">
                No se encontraron usuarios registrados.
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import api from '@/services/api'
import { useDialogStore } from '@/stores/dialog'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import AlertMessage from '@/components/common/AlertMessage.vue'

const usuarios = ref([])
const loading = ref(true)
const error = ref(null)
const successMsg = ref(null)
const togglingId = ref(null)
const dialogStore = useDialogStore()

const filtros = reactive({
  busqueda: '',
  rol: '',
  estado: ''
})

async function cargarUsuarios() {
  loading.value = true
  error.value = null
  try {
    const { data } = await api.get('/api/auth/usuarios')
    usuarios.value = data || []
  } catch (err) {
    console.error('Error fetching users:', err)
    error.value = err.response?.data?.mensaje || 'Error al recuperar la lista de usuarios.'
  } finally {
    loading.value = false
  }
}

onMounted(cargarUsuarios)

const usuariosFiltrados = computed(() => {
  return usuarios.value.filter(u => {
    const nombreCompleto = `${u.nombre || ''} ${u.apellido || ''}`.toLowerCase()
    const coincideBusqueda = !filtros.busqueda ||
      nombreCompleto.includes(filtros.busqueda.toLowerCase()) ||
      (u.email || '').toLowerCase().includes(filtros.busqueda.toLowerCase())
    
    const coincideRol = !filtros.rol || u.rol === filtros.rol
    
    const coincideEstado = !filtros.estado ||
      (filtros.estado === 'activo' && u.activo) ||
      (filtros.estado === 'inactivo' && !u.activo)

    return coincideBusqueda && coincideRol && coincideEstado
  })
})

async function confirmarDesactivacion(usuario) {
  const confirmado = await dialogStore.confirm({
    titulo: 'Desactivar Usuario',
    mensaje: `¿Está completamente seguro de desactivar la cuenta del usuario "${usuario.nombre} ${usuario.apellido || ''}"? Esta acción no se puede deshacer en esta versión.`,
    confirmLabel: 'Sí, Desactivar',
    cancelLabel: 'Cancelar',
    tipoEsPeligroso: true
  })
  if (!confirmado) return
  
  togglingId.value = usuario.id
  error.value = null
  successMsg.value = null
  try {
    await api.put(`/api/auth/usuarios/${usuario.id}/desactivar`)
    
    // Actualizar localmente
    const idx = usuarios.value.findIndex(u => u.id === usuario.id)
    if (idx !== -1) {
      usuarios.value[idx].activo = false
    }
    
    successMsg.value = `El usuario "${usuario.nombre}" ha sido desactivado correctamente.`
    setTimeout(() => { successMsg.value = null }, 4000)
  } catch (err) {
    console.error('Error desactivating user:', err)
    error.value = err.response?.data?.mensaje || 'Error al desactivar el usuario.'
  } finally {
    togglingId.value = null
  }
}

function formatRol(rol) {
  if (!rol) return ''
  switch (rol) {
    case 'ROLE_ADMIN': return 'Administrador'
    case 'ROLE_ALMACEN': return 'Almacén'
    case 'ROLE_COMPRAS': return 'Compras'
    case 'ROLE_TRANSPORTE': return 'Transporte'
    default: return rol.replace('ROLE_', '')
  }
}

function obtenerClaseRol(rol) {
  switch (rol) {
    case 'ROLE_ADMIN': return 'bg-purple-100 text-purple-800'
    case 'ROLE_ALMACEN': return 'bg-amber-100 text-amber-800'
    case 'ROLE_COMPRAS': return 'bg-emerald-100 text-emerald-800'
    case 'ROLE_TRANSPORTE': return 'bg-blue-100 text-blue-800'
    default: return 'bg-stone-100 text-stone-850'
  }
}

function formatFecha(fecha) {
  if (!fecha) return '—'
  return new Date(fecha).toLocaleDateString('es-PE', {
    day: '2-digit',
    month: 'short',
    year: 'numeric'
  })
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
