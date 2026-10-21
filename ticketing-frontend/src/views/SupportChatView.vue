<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useSupportStore } from '../stores/support'

const supportStore = useSupportStore()
const userInput = ref('')
const messagesContainer = ref<HTMLElement | null>(null)

const suggestedChips = [
  '¿Cómo funciona la sala de espera virtual?',
  '¿Por qué mi código QR cambia constantemente?',
  'Reenviar entradas a mi correo',
  'Hablar con soporte humano'
]

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
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
    <div class="flex items-center justify-between pb-4 border-b border-border-default shrink-0">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-xl bg-gradient-to-tr from-primary-500 to-secondary-400 flex items-center justify-center text-surface-base font-bold shadow-md shadow-primary-500/20">
          <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
        </div>
        <div>
          <div class="flex items-center gap-2">
            <h1 class="text-base sm:text-lg font-bold font-display text-text-primary">
              Amaryllis Intelligent Support
            </h1>
            <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-mono font-semibold bg-secondary-500/10 text-secondary-400 border border-secondary-500/30">
              <span class="w-1.5 h-1.5 rounded-full bg-secondary-400 animate-pulse"></span>
              Live RAG
            </span>
          </div>
          <p class="text-xs text-text-secondary">
            Spring WebFlux WebSocket &bull; Redis Pub/Sub room: <code>chat.room.default</code>
          </p>
        </div>
      </div>

      <button
        @click="clearChat"
        class="text-xs font-mono text-text-muted hover:text-text-primary transition-colors cursor-pointer"
        title="Clear conversation"
      >
        Reset Chat
      </button>
    </div>

    <!-- Chat Messages Scroll Area -->
    <div
      ref="messagesContainer"
      class="flex-1 overflow-y-auto py-6 space-y-4 pr-1"
    >
      <div
        v-for="msg in supportStore.userMessages"
        :key="msg.id"
        :class="[
          'flex gap-3 max-w-[85%] sm:max-w-[75%]',
          msg.sender === 'user' ? 'ml-auto flex-row-reverse' : 'mr-auto'
        ]"
      >
        <!-- Avatar -->
        <div
          :class="[
            'w-8 h-8 rounded-lg flex items-center justify-center shrink-0 text-xs font-bold font-mono',
            msg.sender === 'user'
              ? 'bg-secondary-500/20 text-secondary-300 border border-secondary-500/30'
              : 'bg-primary-500/20 text-primary-300 border border-primary-500/30'
          ]"
        >
          {{ msg.sender === 'user' ? 'U' : (msg.sender === 'operator' ? 'OP' : 'AI') }}
        </div>

        <!-- Bubble -->
        <div class="space-y-1">
          <div
            :class="[
              'p-4 rounded-2xl text-xs sm:text-sm leading-relaxed shadow-sm',
              msg.sender === 'user'
                ? 'bg-primary-500 text-surface-base rounded-tr-none font-medium'
                : 'bg-surface-card border border-border-default text-text-primary rounded-tl-none'
            ]"
          >
            {{ msg.text }}
          </div>
          <div
            :class="[
              'text-[10px] font-mono text-text-muted px-1',
              msg.sender === 'user' ? 'text-right' : 'text-left'
            ]"
          >
            {{ msg.timestamp }} &bull; {{ msg.senderName }}
          </div>
        </div>
      </div>

      <!-- Typing indicator -->
      <div v-if="supportStore.isAiTyping" class="flex gap-3 items-center mr-auto">
        <div class="w-8 h-8 rounded-lg bg-primary-500/20 text-primary-300 border border-primary-500/30 flex items-center justify-center text-xs font-bold">
          AI
        </div>
        <div class="p-3 bg-surface-card border border-border-default rounded-2xl rounded-tl-none flex items-center gap-1.5">
          <span class="w-2 h-2 rounded-full bg-primary-400 animate-bounce"></span>
          <span class="w-2 h-2 rounded-full bg-primary-400 animate-bounce [animation-delay:0.2s]"></span>
          <span class="w-2 h-2 rounded-full bg-primary-400 animate-bounce [animation-delay:0.4s]"></span>
        </div>
      </div>
    </div>

    <!-- Suggested Question Chips -->
    <div class="py-2 flex items-center gap-2 overflow-x-auto shrink-0 no-scrollbar">
      <span class="text-[11px] font-mono text-text-muted shrink-0">Suggestions:</span>
      <button
        v-for="chip in suggestedChips"
        :key="chip"
        @click="handleChipClick(chip)"
        class="text-xs px-3 py-1.5 rounded-full bg-surface-card hover:bg-surface-elevated border border-border-default text-text-secondary hover:text-text-primary transition-colors whitespace-nowrap cursor-pointer shrink-0"
      >
        {{ chip }}
      </button>
    </div>

    <!-- Message Input Bar -->
    <div class="pt-3 border-t border-border-default shrink-0">
      <form @submit.prevent="handleSendMessage" class="flex items-center gap-2">
        <input
          v-model="userInput"
          type="text"
          placeholder="Ask a question about tickets, seat locks, or policies..."
          :disabled="supportStore.isAiTyping"
          class="flex-1 px-4 py-3 rounded-xl bg-surface-card border border-border-default text-text-primary placeholder:text-text-muted text-sm focus:outline-none focus:border-primary-400 transition-colors disabled:opacity-50"
        />
        <button
          type="submit"
          :disabled="!userInput.trim() || supportStore.isAiTyping"
          class="px-5 py-3 rounded-xl font-bold bg-primary-400 text-surface-base hover:bg-primary-300 disabled:opacity-40 disabled:cursor-not-allowed shadow-md shadow-primary-500/20 cursor-pointer transition-all flex items-center gap-2 shrink-0"
        >
          <span>Send</span>
          <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3" />
          </svg>
        </button>
      </form>
    </div>

  </div>
</template>
