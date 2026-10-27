<script setup lang="ts">
defineProps<{
  suggestedChips: string[]
  isAiTyping: boolean
}>()

const userInput = defineModel<string>({ required: true })

const emit = defineEmits<{
  (e: 'send'): void
  (e: 'selectChip', chip: string): void
}>()
</script>

<template>
  <div class="flex flex-col shrink-0">
    <!-- Suggested Question Chips -->
    <div class="py-2 flex items-center gap-2 overflow-x-auto shrink-0 no-scrollbar">
      <span class="text-[11px] font-mono text-muted-light shrink-0">Suggestions:</span>
      <button
        v-for="chip in suggestedChips"
        :key="chip"
        @click="emit('selectChip', chip)"
        class="text-xs px-3 py-1.5 rounded-full bg-surface-card hover:bg-surface-elevated border border-border-subtle text-medium-emphasis hover:text-high-emphasis transition-colors whitespace-nowrap cursor-pointer shrink-0"
      >
        {{ chip }}
      </button>
    </div>

    <!-- Message Input Bar -->
    <div class="pt-3 border-t border-border-subtle shrink-0">
      <form @submit.prevent="emit('send')" class="flex items-center gap-2">
        <input
          v-model="userInput"
          type="text"
          placeholder="Ask a question about tickets, seat locks, or policies..."
          :disabled="isAiTyping"
          class="flex-1 px-4 py-3 rounded-xl bg-surface-card border border-border-subtle text-high-emphasis placeholder:text-muted text-sm focus:outline-none focus:border-primary transition-colors disabled:opacity-50"
        />
        <button
          type="submit"
          :disabled="!userInput.trim() || isAiTyping"
          class="px-5 py-3 rounded-xl font-bold bg-btn-buy text-white hover:bg-btn-buy-hover disabled:opacity-40 disabled:cursor-not-allowed shadow-md shadow-primary/20 cursor-pointer transition-all flex items-center gap-2 shrink-0"
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
