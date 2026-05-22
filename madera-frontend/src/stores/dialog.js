import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useDialogStore = defineStore('dialog', () => {
  const abierto = ref(false)
  const titulo = ref('')
  const mensaje = ref('')
  const tipo = ref('confirm') // 'confirm', 'alert', 'error', 'success', 'warning', 'info'
  const confirmLabel = ref('Aceptar')
  const cancelLabel = ref('Cancelar')
  const conInput = ref(false)
  const inputLabel = ref('')
  const inputPlaceholder = ref('')
  const inputValue = ref('')
  const tipoEsPeligroso = ref(false)

  let resolvePromise = null

  function confirm({
    titulo: t = 'Confirmar',
    mensaje: m = '',
    confirmLabel: cl = 'Aceptar',
    cancelLabel: xl = 'Cancelar',
    conInput: ci = false,
    inputLabel: il = 'Comentario:',
    inputPlaceholder: ip = 'Escribe aquí...',
    tipoEsPeligroso: tp = false
  }) {
    titulo.value = t
    mensaje.value = m
    confirmLabel.value = cl
    cancelLabel.value = xl
    conInput.value = ci
    inputLabel.value = il
    inputPlaceholder.value = ip
    inputValue.value = ''
    tipoEsPeligroso.value = tp
    tipo.value = 'confirm'
    abierto.value = true

    return new Promise((resolve) => {
      resolvePromise = resolve
    })
  }

  function alert({
    titulo: t = 'Información',
    mensaje: m = '',
    confirmLabel: cl = 'Entendido',
    tipo: tp = 'info' // 'info', 'error', 'success', 'warning'
  }) {
    titulo.value = t
    mensaje.value = m
    confirmLabel.value = cl
    cancelLabel.value = ''
    conInput.value = false
    tipoEsPeligroso.value = tp === 'error' || tp === 'warning'
    tipo.value = tp
    abierto.value = true

    return new Promise((resolve) => {
      resolvePromise = resolve
    })
  }

  function aceptar(valor = null) {
    abierto.value = false
    if (resolvePromise) {
      if (conInput.value) {
        resolvePromise(valor || inputValue.value)
      } else {
        resolvePromise(true)
      }
      resolvePromise = null
    }
  }

  function cancelar() {
    abierto.value = false
    if (resolvePromise) {
      resolvePromise(false)
      resolvePromise = null
    }
  }

  return {
    abierto,
    titulo,
    mensaje,
    tipo,
    confirmLabel,
    cancelLabel,
    conInput,
    inputLabel,
    inputPlaceholder,
    inputValue,
    tipoEsPeligroso,
    confirm,
    alert,
    aceptar,
    cancelar
  }
})
