<script setup>
import { RouterView } from 'vue-router'
import GlobalDialog from '@/components/common/GlobalDialog.vue'
</script>

<template>
  <RouterView />
  <GlobalDialog />
</template>

<style>
/* Los estilos globales están en src/assets/main.css con Tailwind */
</style>
