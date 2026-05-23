<script setup>
import { RouterView } from 'vue-router'
import GlobalDialog from '@/components/common/GlobalDialog.vue'
import ToastContainer from '@/components/common/ToastContainer.vue'
</script>

<template>
  <RouterView />
  <GlobalDialog />
  <ToastContainer />
</template>

<style>
/* Los estilos globales están en src/assets/main.css con Tailwind */
</style>
