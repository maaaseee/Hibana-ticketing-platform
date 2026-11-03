import { defineStore } from 'pinia'
import { ref } from 'vue'

export type ToastType = 'success' | 'error' | 'info' | 'warning'

export interface ToastItem {
  id: string
  title: string
  description?: string
  type: ToastType
  duration?: number
  action?: {
    label: string
    onClick: () => void
  }
}

export const useToastStore = defineStore('toast', () => {
  const toasts = ref<ToastItem[]>([])

  function addToast(toast: Omit<ToastItem, 'id'> & { id?: string }) {
    const id = toast.id || `toast-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`
    const newToast: ToastItem = {
      ...toast,
      id,
      duration: toast.duration ?? 4000
    }
    toasts.value.push(newToast)

    if (newToast.duration && newToast.duration > 0) {
      setTimeout(() => {
        removeToast(id)
      }, newToast.duration)
    }

    return id
  }

  function removeToast(id: string) {
    toasts.value = toasts.value.filter(t => t.id !== id)
  }

  function clearAll() {
    toasts.value = []
  }

  function success(title: string, description?: string, duration?: number) {
    return addToast({ title, description, type: 'success', duration })
  }

  function error(title: string, description?: string, duration?: number) {
    return addToast({ title, description, type: 'error', duration: duration ?? 5500 })
  }

  function info(title: string, description?: string, duration?: number) {
    return addToast({ title, description, type: 'info', duration })
  }

  function warning(title: string, description?: string, duration?: number) {
    return addToast({ title, description, type: 'warning', duration: duration ?? 5000 })
  }

  return {
    toasts,
    addToast,
    removeToast,
    clearAll,
    success,
    error,
    info,
    warning
  }
})
