<template>
  <div class="flex h-screen bg-gray-50 overflow-hidden">
    <!-- Sidebar component -->
    <Sidebar />

    <!-- Main Content Area -->
    <div class="flex-1 flex flex-col overflow-hidden">
      <!-- Navbar component -->
      <Navbar />

      <!-- Router View container with animation -->
      <main class="flex-1 overflow-y-auto bg-gray-50/50 p-6 md:p-8">
        <div class="max-w-7xl mx-auto animate-fade-in">
          <router-view />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import Sidebar from '@/components/common/Sidebar.vue'
import Navbar  from '@/components/common/Navbar.vue'
</script>
