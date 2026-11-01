import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useCheckoutStore = defineStore('checkout', () => {
  const buyerName = ref<string>('Carlos Mendoza')
  const buyerEmail = ref<string>('carlos.mendoza@hibana.tk')
  const buyerDni = ref<string>('38.291.849')
  const buyerPhone = ref<string>('+54 9 11 4829-1920')
  const paymentMethod = ref<'debit_card' | 'credit_card' | 'mercado_pago'>('debit_card')
  const issuingBank = ref<string>('GALICIA')
  const installments = ref<number>(1)

  const isProcessing = ref<boolean>(false)
  const isCompleted = ref<boolean>(false)
  const lastOrderId = ref<string>('')

  function completeOrder(_totalAmount?: number): boolean {
    isProcessing.value = true
    setTimeout(() => {
      lastOrderId.value = `ORD-${Date.now().toString().slice(-5)}`
      isProcessing.value = false
      isCompleted.value = true
    }, 1200)

    return true
  }

  function resetCheckout() {
    isProcessing.value = false
    isCompleted.value = false
  }

  return {
    buyerName,
    buyerEmail,
    buyerDni,
    buyerPhone,
    paymentMethod,
    issuingBank,
    installments,
    isProcessing,
    isCompleted,
    lastOrderId,
    completeOrder,
    resetCheckout,
  }
})
