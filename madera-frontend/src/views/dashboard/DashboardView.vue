<script setup>
import { ref, onMounted } from 'vue';
import api from '@/services/api';
import { 
  CubeIcon, 
  ClipboardDocumentListIcon, 
  TruckIcon,
  BellIcon 
} from '@heroicons/vue/24/outline';

const stats = ref([
  { name: 'Stock Total', value: '0', icon: CubeIcon, color: 'bg-blue-500' },
  { name: 'Pedidos Pendientes', value: '0', icon: ClipboardDocumentListIcon, color: 'bg-yellow-500' },
  { name: 'Entregas en Ruta', value: '0', icon: TruckIcon, color: 'bg-green-500' },
  { name: 'Notificaciones', value: '0', icon: BellIcon, color: 'bg-purple-500' },
]);

const loading = ref(true);

onMounted(async () => {
  try {
    // Aquí podrías hacer llamadas paralelas a tus microservicios a través del gateway
    // const [inventario, pedidos, entregas] = await Promise.all([
    //   api.get('/api/inventario/stock/total'),
    //   api.get('/api/pedidos/pendientes/count'),
    //   api.get('/api/entregas/ruta/count')
    // ]);
    
    // Por ahora simulamos datos o dejamos en 0 hasta que los MS estén arriba
    loading.value = false;
  } catch (error) {
    console.error('Error cargando estadísticas', error);
    loading.value = false;
  }
});
</script>

<template>
  <div>
    <div class="mb-8">
      <h2 class="text-2xl font-bold text-gray-800">Panel de Control</h2>
      <p class="text-gray-500">Resumen general de las operaciones de Madera & Minería</p>
    </div>

    <!-- Stats Grid -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
      <div v-for="stat in stats" :key="stat.name" class="card flex items-center">
        <div :class="[stat.color, 'p-4 rounded-xl text-white mr-4']">
          <component :is="stat.icon" class="w-8 h-8" />
        </div>
        <div>
          <p class="text-sm text-gray-500 font-medium">{{ stat.name }}</p>
          <p class="text-2xl font-bold text-gray-800">{{ stat.value }}</p>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
      <!-- Recent Activity (Placeholder) -->
      <div class="card">
        <h3 class="text-lg font-bold mb-4 border-bottom pb-2">Últimos Pedidos</h3>
        <div class="space-y-4">
          <p class="text-gray-400 text-center py-8 italic">No hay actividad reciente para mostrar</p>
        </div>
      </div>

      <!-- Quick Actions -->
      <div class="card">
        <h3 class="text-lg font-bold mb-4 border-bottom pb-2">Acciones Rápidas</h3>
        <div class="grid grid-cols-2 gap-4">
          <button class="p-4 border-2 border-dashed border-gray-200 rounded-xl hover:border-primary-500 hover:bg-primary-50 transition-all text-left">
            <p class="font-bold text-gray-700">Nuevo Pedido</p>
            <p class="text-xs text-gray-500">Registrar solicitud de madera</p>
          </button>
          <button class="p-4 border-2 border-dashed border-gray-200 rounded-xl hover:border-primary-500 hover:bg-primary-50 transition-all text-left">
            <p class="font-bold text-gray-700">Ver Inventario</p>
            <p class="text-xs text-gray-500">Consultar stock disponible</p>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
