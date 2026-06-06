<template>
  <div class="max-w-2xl mx-auto space-y-6">
    <!-- Breadcrumbs / Back button -->
    <div class="flex items-center gap-2 text-sm text-stone-500">
      <RouterLink to="/proveedores" class="hover:text-stone-900 transition-colors">Proveedores</RouterLink>
      <span>&gt;</span>
      <span class="text-stone-900 font-medium">Registrar Proveedor</span>
    </div>

    <!-- Title -->
    <div>
      <h1 class="text-2xl font-bold text-stone-900">Registrar Nuevo Proveedor</h1>
      <p class="text-stone-500 text-sm mt-1">Completa los datos de la empresa y contacto del proveedor.</p>
    </div>

    <!-- Alert de errores -->
    <AlertMessage v-if="errorMsg" tipo="error" class="mb-4">
      {{ errorMsg }}
    </AlertMessage>

    <!-- Form card -->
    <div class="card p-6 bg-white shadow-sm border border-stone-150">
      <form @submit.prevent="handleSubmit" class="space-y-4">
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <!-- RUC -->
          <div>
            <label class="block text-xs font-bold text-stone-600 uppercase mb-1.5">RUC *</label>
            <input
              v-model="form.ruc"
              type="text"
              maxlength="11"
              required
              placeholder="11 dígitos"
              class="input-field font-mono"
            />
          </div>

          <!-- Razón Social -->
          <div>
            <label class="block text-xs font-bold text-stone-600 uppercase mb-1.5">Razón Social *</label>
            <input
              v-model="form.razonSocial"
              type="text"
              required
              placeholder="Nombre legal"
              class="input-field"
            />
          </div>

          <!-- Nombre Comercial -->
          <div>
            <label class="block text-xs font-bold text-stone-600 uppercase mb-1.5">Nombre Comercial</label>
            <input
              v-model="form.nombreComercial"
              type="text"
              placeholder="Nombre de fantasía"
              class="input-field"
            />
          </div>

          <!-- Ciudad -->
          <div>
            <label class="block text-xs font-bold text-stone-600 uppercase mb-1.5">Ciudad</label>
            <input
              v-model="form.ciudad"
              type="text"
              placeholder="Ejem: Arequipa, Lima"
              class="input-field"
            />
          </div>

          <!-- Dirección -->
          <div class="sm:col-span-2">
            <label class="block text-xs font-bold text-stone-600 uppercase mb-1.5">Dirección</label>
            <input
              v-model="form.direccion"
              type="text"
              placeholder="Dirección fiscal"
              class="input-field"
            />
          </div>
        </div>

        <hr class="border-stone-150 my-6" />

        <h3 class="text-sm font-bold text-stone-900 mb-3">Datos del Contacto / Representante</h3>

        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <!-- Nombre de Contacto -->
          <div class="sm:col-span-3">
            <label class="block text-xs font-bold text-stone-600 uppercase mb-1.5">Nombre Completo</label>
            <input
              v-model="form.contactoNombre"
              type="text"
              placeholder="Persona de contacto"
              class="input-field"
            />
          </div>

          <!-- Correo de Contacto -->
          <div class="sm:col-span-2">
            <label class="block text-xs font-bold text-stone-600 uppercase mb-1.5">Correo Electrónico</label>
            <input
              v-model="form.contactoEmail"
              type="email"
              placeholder="correo@proveedor.com"
              class="input-field"
            />
          </div>

          <!-- Teléfono de Contacto -->
          <div>
            <label class="block text-xs font-bold text-stone-600 uppercase mb-1.5">Teléfono</label>
            <input
              v-model="form.contactoTelefono"
              type="text"
              placeholder="Celular / Teléfono"
              class="input-field"
            />
          </div>
        </div>

        <!-- Acciones -->
        <div class="flex items-center justify-end gap-3 pt-6">
          <RouterLink to="/proveedores" class="btn-secondary">
            Cancelar
          </RouterLink>
          <button
            type="submit"
            :disabled="proveedoresStore.loading"
            class="btn-primary flex items-center justify-center gap-2 min-w-[140px]"
          >
            <span v-if="proveedoresStore.loading">Guardando...</span>
            <span v-else>Guardar Proveedor</span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useProveedoresStore } from '@/stores/proveedores'
import { useToastStore } from '@/stores/toast'
import AlertMessage from '@/components/common/AlertMessage.vue'

const proveedoresStore = useProveedoresStore()
const toastStore = useToastStore()
const router = useRouter()

const errorMsg = ref(null)

const form = reactive({
  ruc: '',
  razonSocial: '',
  nombreComercial: '',
  contactoNombre: '',
  contactoEmail: '',
  contactoTelefono: '',
  direccion: '',
  ciudad: ''
})

async function handleSubmit() {
  errorMsg.value = null
  if (form.ruc.length !== 11 || !/^\d+$/.test(form.ruc)) {
    errorMsg.value = 'El RUC debe tener exactamente 11 números.'
    return
  }

  try {
    await proveedoresStore.crearProveedor({ ...form })
    toastStore.show({
      mensaje: 'Proveedor registrado exitosamente.',
      tipo: 'success'
    })
    router.push('/proveedores')
  } catch (e) {
    errorMsg.value = e.response?.data?.mensaje || 'No se pudo crear el proveedor. Verifica los datos.'
  }
}
</script>
