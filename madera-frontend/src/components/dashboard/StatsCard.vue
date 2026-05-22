<template>
  <div class="bg-white rounded-xl shadow-sm border border-stone-100 p-6 flex items-center justify-between hover:shadow-md transition-all duration-300 transform hover:-translate-y-0.5 select-none relative overflow-hidden group">
    <!-- Background subtle accent color -->
    <div :class="[bgGlowClass, 'absolute -right-6 -bottom-6 w-24 h-24 rounded-full opacity-10 group-hover:scale-110 transition-transform duration-500 pointer-events-none']"></div>

    <div class="flex items-center space-x-4">
      <div :class="[bgClass, textClass, 'w-12 h-12 rounded-xl flex items-center justify-center text-2xl font-bold shadow-md shrink-0 transition-transform duration-300 group-hover:rotate-6']">
        <slot name="icon">{{ icon }}</slot>
      </div>
      <div>
        <p class="text-xs font-semibold text-stone-400 uppercase tracking-wider">{{ title }}</p>
        <h3 class="text-2xl font-bold text-stone-800 mt-1 leading-none">{{ value }}</h3>
      </div>
    </div>

    <!-- Arrow or minor action indicator (optional) -->
    <div class="text-stone-300 group-hover:text-stone-400 transition-colors text-lg font-bold">
      ➔
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, required: true },
  value: { type: [String, Number], default: '0' },
  icon: { type: String, default: '🪵' },
  color: { type: String, default: 'amber' }
})

const bgClass = computed(() => {
  const map = {
    amber: 'bg-amber-100',
    blue: 'bg-blue-100',
    green: 'bg-emerald-100',
    red: 'bg-rose-100',
    purple: 'bg-purple-100',
    indigo: 'bg-indigo-100'
  }
  return map[props.color] || 'bg-stone-100'
})

const textClass = computed(() => {
  const map = {
    amber: 'text-amber-700',
    blue: 'text-blue-700',
    green: 'text-emerald-700',
    red: 'text-rose-700',
    purple: 'text-purple-700',
    indigo: 'text-indigo-700'
  }
  return map[props.color] || 'text-stone-700'
})

const bgGlowClass = computed(() => {
  const map = {
    amber: 'bg-amber-600',
    blue: 'bg-blue-600',
    green: 'bg-emerald-600',
    red: 'bg-rose-600',
    purple: 'bg-purple-600',
    indigo: 'bg-indigo-600'
  }
  return map[props.color] || 'bg-stone-600'
})
</script>
