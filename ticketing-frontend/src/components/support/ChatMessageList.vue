<script setup lang="ts">
import { ref } from 'vue'

export interface ChatMessage {
  id: string
  sender: 'user' | 'ai' | 'operator' | 'system'
  senderName: string
  text: string
  timestamp: string
}

defineProps<{
  messages: ChatMessage[]
  isAiTyping: boolean
}>()

const containerRef = ref<HTMLElement | null>(null)

defineExpose({
  containerRef
})
</script>

<template>
  <div
    ref="containerRef"
    class="flex-1 overflow-y-auto py-6 space-y-4 pr-1"
  >
    <div
      v-for="msg in messages"
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
            ? 'bg-secondary/15 text-secondary border border-secondary/30'
            : 'bg-primary/15 text-primary border border-primary/30'
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
              ? 'bg-primary-container text-white rounded-tr-none font-medium'
              : 'bg-surface-card border border-border-subtle text-high-emphasis rounded-tl-none'
          ]"
        >
          {{ msg.text }}
        </div>
        <div
          :class="[
            'text-[10px] font-mono text-muted-light px-1',
            msg.sender === 'user' ? 'text-right' : 'text-left'
          ]"
        >
          {{ msg.timestamp }} &bull; {{ msg.senderName }}
        </div>
      </div>
    </div>

    <!-- Typing indicator -->
    <div v-if="isAiTyping" class="flex gap-3 items-center mr-auto">
      <div class="w-8 h-8 rounded-lg bg-primary/15 text-primary border border-primary/30 flex items-center justify-center text-xs font-bold">
        AI
      </div>
      <div class="p-3 bg-surface-card border border-border-subtle rounded-2xl rounded-tl-none flex items-center gap-1.5">
        <span class="w-2 h-2 rounded-full bg-primary animate-bounce"></span>
        <span class="w-2 h-2 rounded-full bg-primary animate-bounce [animation-delay:0.2s]"></span>
        <span class="w-2 h-2 rounded-full bg-primary animate-bounce [animation-delay:0.4s]"></span>
      </div>
    </div>
  </div>
</template>
