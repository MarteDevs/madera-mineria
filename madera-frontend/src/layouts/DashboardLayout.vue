<script setup>
import { ref } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useRouter } from 'vue-router';
import { 
  HomeIcon, 
  CubeIcon, 
  ClipboardDocumentListIcon, 
  TruckIcon,
  ArrowLeftOnRectangleIcon,
  Bars3Icon,
  XMarkIcon
} from '@heroicons/vue/24/outline';

const auth = useAuthStore();
const router = useRouter();
const isSidebarOpen = ref(true);

const menuItems = [
  { name: 'Dashboard', path: '/', icon: HomeIcon },
  { name: 'Inventario', path: '/inventario', icon: CubeIcon },
  { name: 'Pedidos', path: '/pedidos', icon: ClipboardDocumentListIcon },
  { name: 'Entregas', path: '/entregas', icon: TruckIcon },
];

const logout = () => {
  auth.logout();
  router.push('/login');
};
</script>

<template>
  <div class="flex h-screen bg-gray-50">
    <!-- Sidebar -->
    <aside 
      class="bg-madera-oscuro text-white transition-all duration-300 flex flex-col"
      :class="[isSidebarOpen ? 'w-64' : 'w-20']"
    >
      <div class="p-6 flex items-center justify-between">
        <h1 v-if="isSidebarOpen" class="text-xl font-bold tracking-wider">MADERA & MINERÍA</h1>
        <span v-else class="text-xl font-bold">M&M</span>
      </div>

      <nav class="flex-1 px-4 space-y-2 mt-4">
        <router-link 
          v-for="item in menuItems" 
          :key="item.name"
          :to="item.path"
          class="flex items-center p-3 rounded-lg hover:bg-white/10 transition-colors"
          active-class="bg-primary-600 text-white"
        >
          <component :is="item.icon" class="w-6 h-6 shrink-0" />
          <span v-if="isSidebarOpen" class="ml-3 font-medium">{{ item.name }}</span>
        </router-link>
      </nav>

      <div class="p-4 border-t border-white/10">
        <button 
          @click="logout"
          class="flex items-center w-full p-3 rounded-lg hover:bg-red-500/20 text-red-300 transition-colors"
        >
          <ArrowLeftOnRectangleIcon class="w-6 h-6 shrink-0" />
          <span v-if="isSidebarOpen" class="ml-3 font-medium">Cerrar Sesión</span>
        </button>
      </div>
    </aside>

    <!-- Main Content -->
    <main class="flex-1 flex flex-col overflow-hidden">
      <!-- Header -->
      <header class="bg-white shadow-sm h-16 flex items-center justify-between px-8 z-10">
        <button @click="isSidebarOpen = !isSidebarOpen" class="text-gray-500 hover:text-gray-700">
          <Bars3Icon v-if="!isSidebarOpen" class="w-6 h-6" />
          <XMarkIcon v-else class="w-6 h-6" />
        </button>
        
        <div class="flex items-center space-x-4">
          <span class="text-sm font-medium text-gray-700">Bienvenido, {{ auth.user?.username || 'Usuario' }}</span>
          <div class="w-10 h-10 rounded-full bg-primary-100 flex items-center justify-center text-primary-700 font-bold">
            {{ auth.user?.username?.charAt(0).toUpperCase() || 'U' }}
          </div>
        </div>
      </header>

      <!-- Page Content -->
      <div class="flex-1 overflow-y-auto p-8">
        <router-view />
      </div>
    </main>
  </div>
</template>
