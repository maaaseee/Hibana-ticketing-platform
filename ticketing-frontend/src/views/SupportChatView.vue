<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useSupportStore } from '../stores/support'
import ChatHeader from '../components/support/ChatHeader.vue'
import ChatMessageList from '../components/support/ChatMessageList.vue'
import ChatInputBar from '../components/support/ChatInputBar.vue'

const supportStore = useSupportStore()
const userInput = ref('')
const messageListRef = ref<InstanceType<typeof ChatMessageList> | null>(null)

const suggestedChips = [
  '¿Cómo funciona la sala de espera virtual?',
  '¿Por qué mi código QR cambia constantemente?',
  'Reenviar entradas a mi correo',
  'Hablar con soporte humano'
]

function scrollToBottom() {
  nextTick(() => {
    const el = messageListRef.value?.containerRef
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

function handleSendMessage() {
  const text = userInput.value.trim()
  if (!text || supportStore.isAiTyping) return

  userInput.value = ''
  supportStore.sendUserMessage(text)
  scrollToBottom()
  setTimeout(scrollToBottom, 1100)
}

function handleChipClick(chip: string) {
  userInput.value = chip
  handleSendMessage()
}

function clearChat() {
  supportStore.userMessages = [
    {
      id: `m_${Date.now()}`,
      sender: 'ai',
      senderName: 'Amaryllis Concierge AI',
      text: 'Sesión reiniciada. ¿En qué puedo orientarte hoy?',
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    }
  ]
}

onMounted(() => {
  scrollToBottom()
})
</script>

<template>
  <div class="py-6 px-4 sm:px-6 lg:px-8 max-w-4xl mx-auto h-[calc(100vh-140px)] flex flex-col">
    <!-- Header -->
    <ChatHeader @clear="clearChat" />

    <!-- Chat Messages Scroll Area -->
    <ChatMessageList
      ref="messageListRef"
      :messages="supportStore.userMessages"
      :is-ai-typing="supportStore.isAiTyping"
    />

    <!-- Input Bar & Suggestions -->
    <ChatInputBar
      v-model="userInput"
      :suggested-chips="suggestedChips"
      :is-ai-typing="supportStore.isAiTyping"
      @send="handleSendMessage"
      @select-chip="handleChipClick"
    />
  </div>
</template>
