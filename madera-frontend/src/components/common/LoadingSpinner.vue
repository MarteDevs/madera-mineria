<template>
  <div class="flex flex-col items-center justify-center py-12 space-y-4">
    <!-- Premium Spinner -->
    <div class="relative w-12 h-12">
      <div class="absolute inset-0 rounded-full border-4 border-gray-100"></div>
      <div class="absolute inset-0 rounded-full border-4 border-primary-600 border-t-transparent animate-spin"></div>
    </div>
    <p class="text-sm font-medium text-gray-500 animate-pulse-subtle">
      {{ mensaje }}
    </p>
  </div>
</template>

<script setup>
defineProps({
  mensaje: {
    type: String,
    default: 'Cargando datos...'
  }
})
</script>
