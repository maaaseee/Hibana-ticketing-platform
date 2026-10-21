<script setup lang="ts">
import { ref, computed } from 'vue'

interface FaqItem {
  id: string
  question: string
  answer: string
  category: 'booking' | 'queue' | 'security' | 'refunds'
}

const searchQuery = ref('')
const selectedCategory = ref<'all' | 'booking' | 'queue' | 'security' | 'refunds'>('all')
const openItem = ref<string | null>('faq-1')

const faqs: FaqItem[] = [
  {
    id: 'faq-1',
    question: 'How does the Virtual Waiting Room operate during massive onsale spikes?',
    answer: 'When high traffic is detected on top-tier concert releases, requests are placed in an orderly Redis-backed FIFO queue (Sorted Sets). A deterministic position number is assigned. Once you reach position #1, an ephemeral cryptographic token (X-Checkout-Token) is issued, allowing 10 uninterrupted minutes to finalize your seat reservation.',
    category: 'queue'
  },
  {
    id: 'faq-2',
    question: 'Why does my dynamic ticket QR code rotate every 15 seconds?',
    answer: 'To protect attendees against counterfeit duplicates and secondary market scalper screenshots, each ticket payload is continuously re-signed using TOTP (Time-Based One-Time Password) seeds with Ed25519 signatures. Static screenshots will fail validation at venue optical scanners.',
    category: 'security'
  },
  {
    id: 'faq-3',
    question: 'What is the Redlock timer on the seat selection screen?',
    answer: 'When you tap a seat, a distributed lock is acquired across our Redis clusters (Redlock algorithm) under the key lock:event:{id}:seat:{id}. This prevents two buyers from checking out the exact same seat simultaneously. The lock is held for 10 minutes.',
    category: 'booking'
  },
  {
    id: 'faq-4',
    question: 'How do refunds work if a show is rescheduled or canceled?',
    answer: 'Cancellations trigger an automated compensating Saga workflow. Funds are credited directly back to your payment method or your Pocket digital balance with an exact double-entry audit trail.',
    category: 'refunds'
  },
  {
    id: 'faq-5',
    question: 'Can I safely transfer my digital pass to a family member or friend?',
    answer: 'Yes! From the My Passes tab, click "Transfer Pass" and provide the recipient\'s email address. Once accepted, cryptographic ownership is revoked from your wallet and securely minted into the recipient\'s profile.',
    category: 'booking'
  },
  {
    id: 'faq-6',
    question: 'What payment options are supported?',
    answer: 'We support all major Visa/MasterCard credit/debit cards via a PCI-DSS certified gateway, as well as the Amaryllis Pocket digital wallet featuring zero transaction surcharges and instant atomic settlements.',
    category: 'refunds'
  }
]

const categories = [
  { id: 'all', label: 'All Topics' },
  { id: 'queue', label: 'Waiting Room & Queue' },
  { id: 'security', label: 'Anti-Scalper Security' },
  { id: 'booking', label: 'Seat Selection & Redlock' },
  { id: 'refunds', label: 'Refunds & Wallet' }
]

const filteredFaqs = computed(() => {
  return faqs.filter((faq) => {
    const matchesCat = selectedCategory.value === 'all' || faq.category === selectedCategory.value
    const matchesQuery = !searchQuery.value ||
      faq.question.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      faq.answer.toLowerCase().includes(searchQuery.value.toLowerCase())
    return matchesCat && matchesQuery
  })
})

function toggleFaq(id: string) {
  openItem.value = openItem.value === id ? null : id
}
</script>

<template>
  <div class="py-10 px-4 sm:px-6 lg:px-8 max-w-5xl mx-auto space-y-12">
    
    <!-- Hero Header -->
    <div class="text-center space-y-4 max-w-2xl mx-auto">
      <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-primary-500/10 text-primary-300 border border-primary-500/20">
        Knowledge Base & FAQ
      </span>
      <h1 class="text-3xl sm:text-4xl font-extrabold font-display text-text-primary tracking-tight">
        How can we assist you today?
      </h1>
      <p class="text-sm text-text-secondary leading-relaxed">
        Everything you need to know about the Amaryllis ticketing platform, queue mechanics, dynamic passes, and distributed seat locking.
      </p>

      <!-- Search Input -->
      <div class="relative pt-2">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Search by keywords (e.g., waiting room, refund, qr code, redlock)..."
          class="w-full pl-11 pr-4 py-3.5 rounded-2xl bg-surface-card border border-border-default text-text-primary placeholder:text-text-muted text-sm shadow-xl focus:outline-none focus:border-primary-400 transition-colors"
        />
        <svg class="w-5 h-5 text-text-muted absolute left-4 top-5.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
      </div>
    </div>

    <!-- Category Filter Pills -->
    <div class="flex flex-wrap items-center justify-center gap-2">
      <button
        v-for="cat in categories"
        :key="cat.id"
        @click="selectedCategory = cat.id as any"
        :class="[
          'px-4 py-2 rounded-xl text-xs font-semibold transition-all cursor-pointer',
          selectedCategory === cat.id
            ? 'bg-primary-400 text-surface-base shadow-md'
            : 'bg-surface-card text-text-secondary hover:text-text-primary border border-border-default'
        ]"
      >
        {{ cat.label }}
      </button>
    </div>

    <!-- Accordion List -->
    <div class="space-y-4 max-w-3xl mx-auto">
      <div
        v-for="faq in filteredFaqs"
        :key="faq.id"
        class="bg-surface-card border border-border-default rounded-2xl overflow-hidden shadow-sm transition-all hover:border-border-default/80"
      >
        <button
          @click="toggleFaq(faq.id)"
          class="w-full p-5 text-left flex items-center justify-between gap-4 cursor-pointer focus:outline-none"
        >
          <span class="font-bold text-sm sm:text-base text-text-primary">
            {{ faq.question }}
          </span>
          <span
            :class="[
              'w-6 h-6 rounded-full bg-surface-base border border-border-default flex items-center justify-center text-primary-400 text-sm transition-transform duration-200 shrink-0',
              openItem === faq.id ? 'rotate-180 bg-primary-500/20 text-primary-300' : ''
            ]"
          >
            &darr;
          </span>
        </button>

        <div
          v-if="openItem === faq.id"
          class="px-5 pb-5 text-sm text-text-secondary leading-relaxed border-t border-border-default/50 pt-3"
        >
          {{ faq.answer }}
        </div>
      </div>

      <div v-if="filteredFaqs.length === 0" class="text-center py-12 text-text-muted text-sm">
        No articles matched your search query. Try another term or contact support directly.
      </div>
    </div>

    <!-- Escalation / Still need help CTA Banner -->
    <div class="max-w-3xl mx-auto bg-gradient-to-r from-surface-card via-surface-card to-primary-950/30 border border-primary-500/30 rounded-3xl p-8 shadow-xl flex flex-col sm:flex-row items-center justify-between gap-6">
      <div class="space-y-1 text-center sm:text-left">
        <h3 class="text-lg font-bold font-display text-text-primary">Still have questions?</h3>
        <p class="text-xs text-text-secondary max-w-md">
          Chat with our intelligent AI concierge or connect with an operations representative in real time.
        </p>
      </div>

      <RouterLink
        :to="{ name: 'support-chat' }"
        class="px-6 py-3 rounded-xl font-bold text-xs bg-primary-400 text-surface-base hover:bg-primary-300 shadow-lg shadow-primary-500/20 transition-all shrink-0 flex items-center gap-2"
      >
        <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
        </svg>
        <span>Open Support Chat</span>
      </RouterLink>
    </div>

  </div>
</template>
