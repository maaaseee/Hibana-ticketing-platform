import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ChatMessage } from '../types'

export const useSupportStore = defineStore('support', () => {
  // Customer Chat State
  const userMessages = ref<ChatMessage[]>([
    {
      id: 'm1',
      sender: 'ai',
      senderName: 'Hibana Concierge AI',
      text: '¡Hola Carlos! He identificado tu cuenta asociada a carlos.mendoza@hibana.tk y veo tu compra reciente para Neon Nights 2025 • General VIP (x2). ¿En qué puedo orientarte hoy?',
      timestamp: '15:40',
      quickActions: [
        { label: 'Reenviar entradas a mi correo', action: 'resend_email', icon: 'mail' },
        { label: 'Problema con mi código QR', action: 'qr_issue', icon: 'qr_code' },
        { label: 'Transferir entradas', action: 'transfer', icon: 'swap_horiz' },
        { label: 'Hablar con soporte humano', action: 'escalate_human', icon: 'support_agent' },
      ],
    },
  ])

  const isAiTyping = ref<boolean>(false)
  const isEscalatedToHuman = ref<boolean>(false)

  function sendUserMessage(text: string) {
    userMessages.value.push({
      id: `m_${Date.now()}`,
      sender: 'user',
      senderName: 'Carlos Mendoza',
      text,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    })

    // Simulate AI response with knowledge retrieval (RAG)
    isAiTyping.value = true
    setTimeout(() => {
      isAiTyping.value = false
      let reply = 'He verificado tu solicitud en el cluster de inventario. Todo se encuentra en orden.'
      const lower = text.toLowerCase()

      if (lower.includes('qr') || lower.includes('código') || lower.includes('no veo')) {
        reply = 'Comprobado en el servidor de emisión. La orden #HBN-9821 está autorizada. He reenviado el enlace con tus 2 códigos QR cifrados a tu correo y puedes verlos actualizados en tu Dashboard.'
      } else if (lower.includes('correo') || lower.includes('email') || lower.includes('mail')) {
        reply = 'He disparado una re-emisión instantánea de tus credenciales a carlos.mendoza@hibana.tk. Verifica tu bandeja de entrada o spam.'
      } else if (lower.includes('humano') || lower.includes('agente') || lower.includes('operador')) {
        isEscalatedToHuman.value = true
        reply = 'Te he transferido a la cola prioritaria de operadores en vivo mediante canal Redis Pub/Sub. Un operador de guardia tomará el control en segundos.'
      }

      userMessages.value.push({
        id: `m_${Date.now() + 1}`,
        sender: isEscalatedToHuman.value ? 'operator' : 'ai',
        senderName: isEscalatedToHuman.value ? 'Operador de Guardia' : 'Hibana Concierge AI',
        text: reply,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      })
    }, 1000)
  }

  return {
    userMessages,
    isAiTyping,
    isEscalatedToHuman,
    sendUserMessage,
  }
})
