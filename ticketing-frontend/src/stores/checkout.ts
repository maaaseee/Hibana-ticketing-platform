import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useCheckoutStore = defineStore('checkout', () => {
  const buyerName = ref<string>('Carlos Mendoza')
  const buyerEmail = ref<string>('carlos.mendoza@hibana.tk')
  const buyerDni = ref<string>('38.291.849')
  const buyerPhone = ref<string>('+54 9 11 4829-1920')
  const paymentMethod = ref<'pocket_wallet' | 'credit_card' | 'bank_transfer'>('pocket_wallet')

  // Virtual Wallet (Pocket double-entry system: com.aieta.pocket)
  const pocketBalance = ref<number>(450.0)

  const isProcessing = ref<boolean>(false)
  const isCompleted = ref<boolean>(false)
  const lastOrderId = ref<string>('')

  function completeOrder(totalAmount: number): boolean {
    if (paymentMethod.value === 'pocket_wallet' && pocketBalance.value < totalAmount) {
      alert('Saldo insuficiente en tu Billetera Virtual Pocket.')
      return false
    }

    isProcessing.value = true
    setTimeout(() => {
      if (paymentMethod.value === 'pocket_wallet') {
        pocketBalance.value -= totalAmount
      }
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
    pocketBalance,
    isProcessing,
    isCompleted,
    lastOrderId,
    completeOrder,
    resetCheckout,
  }
})
