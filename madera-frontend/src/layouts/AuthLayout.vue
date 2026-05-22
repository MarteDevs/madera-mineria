<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-stone-900 via-stone-850 to-amber-950 px-4 py-12 relative overflow-hidden select-none">
    <!-- Animated background patterns -->
    <div class="absolute inset-0 bg-[radial-gradient(ellipse_at_top_right,_var(--tw-gradient-stops))] from-amber-600/10 via-transparent to-transparent pointer-events-none"></div>
    <div class="absolute -top-40 -left-40 w-80 h-80 bg-amber-700/5 rounded-full blur-3xl pointer-events-none"></div>
    <div class="absolute -bottom-40 -right-40 w-96 h-96 bg-amber-900/10 rounded-full blur-3xl pointer-events-none"></div>

    <div class="max-w-md w-full z-10 animate-fade-in">
      <router-view />
    </div>
  </div>
</template>

<script setup>
// Layout for authentication views (login/register)
</script>
