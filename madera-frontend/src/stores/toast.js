import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useToastStore = defineStore('toast', () => {
  const toasts = ref([])

  function show({ mensaje, tipo = 'info', duracion = 4500 }) {
    const id = Date.now() + Math.random().toString(36).substring(2, 9)
    toasts.value.push({ id, mensaje, tipo })
    
    setTimeout(() => {
      remove(id)
    }, duracion)
  }

  function remove(id) {
    const idx = toasts.value.findIndex(t => t.id === id)
    if (idx !== -1) {
      toasts.value.splice(idx, 1)
    }
  }

  return {
    toasts,
    show,
    remove
  }
})
