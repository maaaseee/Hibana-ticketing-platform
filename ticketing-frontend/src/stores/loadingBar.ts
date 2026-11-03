import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useLoadingBarStore = defineStore('loadingBar', () => {
  const isLoading = ref(false)
  const progress = ref(0)
  const isFailed = ref(false)
  let timer: ReturnType<typeof setInterval> | null = null

  function start() {
    if (timer) clearInterval(timer)
    isFailed.value = false
    isLoading.value = true
    progress.value = 15

    timer = setInterval(() => {
      if (progress.value < 85) {
        progress.value += Math.random() * 12
      }
    }, 150)
  }

  function finish() {
    if (timer) clearInterval(timer)
    progress.value = 100
    setTimeout(() => {
      isLoading.value = false
      setTimeout(() => {
        progress.value = 0
      }, 200)
    }, 250)
  }

  function fail() {
    if (timer) clearInterval(timer)
    isFailed.value = true
    progress.value = 100
    setTimeout(() => {
      isLoading.value = false
      setTimeout(() => {
        progress.value = 0
        isFailed.value = false
      }, 200)
    }, 400)
  }

  return {
    isLoading,
    progress,
    isFailed,
    start,
    finish,
    fail
  }
})
