<template>
  <div class="w-full h-64 flex items-center justify-center relative">
    <div v-if="isEmpty" class="text-stone-400 italic text-sm">
      No hay suficientes datos para graficar
    </div>
    <div v-else class="w-full h-full max-h-[220px]">
      <Doughnut :data="chartData" :options="chartOptions" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Doughnut } from 'vue-chartjs'
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  ArcElement,
  CategoryScale
} from 'chart.js'

ChartJS.register(Title, Tooltip, Legend, ArcElement, CategoryScale)

const props = defineProps({
  datos: {
    type: Object,
    default: () => ({})
  }
})

const isEmpty = computed(() => {
  if (!props.datos) return true
  return Object.values(props.datos).reduce((sum, val) => sum + val, 0) === 0
})

const chartData = computed(() => {
  const labelsMap = {
    PENDIENTE: 'Pendiente',
    APROBADO: 'Aprobado',
    EN_PREPARACION: 'En preparación',
    DESPACHADO: 'Despachado',
    ENTREGADO: 'Entregado',
    RECHAZADO: 'Rechazado'
  }

  const colorsMap = {
    PENDIENTE: '#f59e0b', // Yellow
    APROBADO: '#3b82f6', // Blue
    EN_PREPARACION: '#a855f7', // Purple
    DESPACHADO: '#6366f1', // Indigo
    ENTREGADO: '#10b981', // Green
    RECHAZADO: '#ef4444' // Red
  }

  const labels = []
  const data = []
  const backgroundColor = []

  for (const [key, val] of Object.entries(props.datos)) {
    if (val > 0) {
      labels.push(labelsMap[key] || key)
      data.push(val)
      backgroundColor.push(colorsMap[key] || '#9ca3af')
    }
  }

  return {
    labels,
    datasets: [
      {
        data,
        backgroundColor,
        borderWidth: 2,
        borderColor: '#ffffff',
        hoverOffset: 4
      }
    ]
  }
})

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'bottom',
      labels: {
        usePointStyle: true,
        pointStyle: 'circle',
        padding: 15,
        font: {
          family: "'Inter', sans-serif",
          size: 11,
          weight: '500'
        },
        color: '#4b5563'
      }
    },
    tooltip: {
      backgroundColor: 'rgba(15, 23, 42, 0.9)',
      padding: 10,
      titleFont: {
        family: "'Inter', sans-serif",
        size: 12,
        weight: '600'
      },
      bodyFont: {
        family: "'Inter', sans-serif",
        size: 12
      },
      usePointStyle: true
    }
  },
  cutout: '65%'
}
</script>
