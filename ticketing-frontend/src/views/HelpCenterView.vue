<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

interface FaqItem {
  id: string
  question: string
  answer: string
  tags: string[]
}

const route = useRoute()

// Reactive state
const searchQuery = ref('')
const openItem = ref<string | null>('faq-tickets-delivery')

const faqs: FaqItem[] = [
  {
    id: 'faq-tickets-delivery',
    question: '¿Cómo y cuándo recibo mis entradas después de comprar?',
    answer:
      'Una vez confirmado tu pago, tus entradas quedan disponibles de inmediato en la sección <strong class="text-on-surface font-semibold">Mis Entradas</strong> de tu cuenta. No enviamos archivos PDF por correo para evitar fraudes y duplicados; en su lugar, verás tu código QR digital seguro directamente en la pantalla de tu móvil, listo para ser escaneado al entrar al evento.',
    tags: ['recibir', 'donde estan', 'pdf', 'correo', 'comprar', 'mis entradas', 'qr', 'entrega']
  },
  {
    id: 'faq-dynamic-qr',
    question: '¿Por qué mi código QR cambia continuamente en la pantalla?',
    answer:
      'Es una medida de seguridad para protegerte contra estafas, fotos robadas y reventa no autorizada. Tu código QR se actualiza automáticamente cada pocos segundos para asegurar que solo la persona con la sesión activa pueda ingresar. Por este motivo, las capturas de pantalla o fotos impresas no son válidas en las puertas de acceso.',
    tags: ['qr', 'pantalla', 'seguridad', 'captura', 'foto', 'cambia', 'reventa']
  },
  {
    id: 'faq-transfers',
    question: '¿Cómo puedo transferir una entrada a un amigo o familiar?',
    answer:
      'Ingresa a <strong class="text-on-surface font-semibold">Mis Entradas</strong>, abre los detalles del boleto que quieres traspasar y presiona <strong class="text-on-surface font-semibold">Transferir Entrada</strong>. Solo debes escribir el correo electrónico de la persona que lo recibirá (debe tener una cuenta creada en Amaryllis). Por seguridad, las transferencias pueden realizarse hasta 2 horas antes de la apertura de puertas del evento.',
    tags: ['transferir', 'regalar', 'amigo', 'familiar', 'cambiar nombre', 'titular', 'ceder']
  },
  {
    id: 'faq-seat-timer',
    question: '¿Qué significa el tiempo límite de 10 minutos al elegir asientos o pagar?',
    answer:
      'Cuando eliges tus asientos, el sistema los aparta de forma exclusiva para ti durante 10 minutos. Esto evita que otra persona te gane los mismos lugares mientras ingresas tus datos con calma. Si no completas la compra dentro de ese tiempo, los asientos se liberan automáticamente para que otros asistentes puedan seleccionarlos.',
    tags: ['tiempo', 'minutos', 'reloj', 'temporizador', 'asientos', 'butacas', 'reserva', 'limite']
  },
  {
    id: 'faq-virtual-queue',
    question: '¿Cómo funciona la fila virtual cuando hay mucha demanda en un evento?',
    answer:
      'En eventos de alta concurrencia, activamos una fila de espera virtual para asegurar que todos tengan la misma oportunidad de compra de manera justa y ordenada, sin que el sitio web se caiga. Al ingresar, se te asigna un turno automáticamente. Te recomendamos no refrescar ni cerrar la página mientras esperas para no perder tu posición.',
    tags: ['fila virtual', 'espera', 'turno', 'demanda', 'masiva', 'concierto', 'agotado']
  },
  {
    id: 'faq-offline-access',
    question: '¿Puedo ingresar al evento si me quedo sin internet o sin señal en el móvil?',
    answer:
      '¡Sí! Nuestra plataforma almacena de forma segura tu entrada en tu dispositivo. Te sugerimos abrir la sección <strong class="text-on-surface font-semibold">Mis Entradas</strong> unas horas antes de salir de casa mientras tengas conexión a internet o datos; así tu boleto quedará listo para validarse en el acceso aunque no haya señal en el recinto.',
    tags: ['offline', 'sin internet', 'sin señal', 'datos', 'celular', 'estadio', 'acceso']
  },
  {
    id: 'faq-cancellations-refunds',
    question: '¿Qué ocurre si un evento se cancela o cambia de fecha?',
    answer:
      'Te notificaremos inmediatamente por correo electrónico. Si el espectáculo se reprograma para una nueva fecha, tu entrada seguirá siendo válida automáticamente. Si no puedes asistir a la nueva fecha o el evento se cancela de forma definitiva, dispondrás de 15 días continuos para solicitar la devolución total de tu dinero a tu medio de pago original o a tu saldo Amaryllis Pocket.',
    tags: ['cancelacion', 'reprogramado', 'devolucion', 'reembolso', 'dinero', 'fecha']
  },
  {
    id: 'faq-payment-methods',
    question: '¿Cuáles son los métodos de pago aceptados?',
    answer:
      'Aceptamos tarjetas de débito y crédito nacionales e internacionales (Visa, Mastercard, American Express), transferencias bancarias y saldo disponible en tu billetera digital Amaryllis Pocket. Todos los pagos se procesan con certificación de máxima seguridad financiera.',
    tags: ['pago', 'tarjetas', 'credito', 'debito', 'transferencia', 'pocket', 'cuotas']
  },
  {
    id: 'faq-tax-receipts',
    question: '¿Dónde puedo descargar mi comprobante o factura de compra con RUT?',
    answer:
      'En la sección <strong class="text-on-surface font-semibold">Mis Entradas</strong>, haz clic en la compra correspondiente. Allí podrás descargar tu boleta electrónica o ingresar tus datos tributarios (RUT o razón social) para emitir tu factura antes del cierre del mes.',
    tags: ['factura', 'boleta', 'rut', 'comprobante', 'recibo', 'impuestos', 'tributario']
  },
  {
    id: 'faq-ticket-limits',
    question: '¿Cuántas entradas puedo comprar por persona?',
    answer:
      'Para evitar la reventa y permitir que más personas disfruten del espectáculo, la mayoría de los eventos permiten adquirir entre 4 y 6 entradas por persona o tarjeta bancaria. El límite exacto siempre se especifica claramente antes de iniciar el proceso de compra.',
    tags: ['limite', 'cuantas', 'maximo', 'entradas', 'persona', 'comprar']
  },
  {
    id: 'faq-children-admission',
    question: '¿Los niños o menores de edad pagan entrada?',
    answer:
      'Depende de las políticas de cada recinto y espectáculo. Generalmente los menores a partir de 2 o 3 años deben tener su propia butaca y entrada. En eventos con restricción de edad (por ejemplo, para mayores de 18 años), se solicitará documento de identidad en los accesos. Puedes consultar las edades permitidas en la ficha informativa de cada show.',
    tags: ['niños', 'menores', 'edad', 'hijos', 'familia', 'documento', 'cedula']
  },
  {
    id: 'faq-missing-charges',
    question: '¿Qué hago si veo el cobro en mi banco pero no aparecen mis entradas?',
    answer:
      'En ocasiones muy puntuales, los bancos pueden tardar unos minutos en confirmar la transacción. Si tras 10 minutos tus entradas no aparecen en tu cuenta, no vuelvas a comprar para evitar cargos duplicados. Presiona el botón <strong class="text-on-surface font-semibold">Iniciar chat de soporte</strong> y nuestro equipo verificará tu transacción al instante con tu número de comprobante.',
    tags: ['cobro', 'banco', 'descuento', 'no aparecen', 'error', 'problema', 'soporte']
  }
]

const filteredFaqs = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  if (!query) return faqs

  return faqs.filter((faq) => {
    const inQuestion = faq.question.toLowerCase().includes(query)
    const inAnswer = faq.answer.toLowerCase().includes(query)
    const inTags = faq.tags.some((t) => t.toLowerCase().includes(query))
    return inQuestion || inAnswer || inTags
  })
})

function toggleFaq(id: string) {
  openItem.value = openItem.value === id ? null : id
}

function clearSearch() {
  searchQuery.value = ''
}

function scrollToFaq(id: string) {
  setTimeout(() => {
    const el = document.getElementById(id)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  }, 100)
}

onMounted(() => {
  const hash = route.hash.replace('#', '')
  if (hash) {
    const target = faqs.find((f) => f.id === hash)
    if (target) {
      openItem.value = target.id
      scrollToFaq(target.id)
    }
  }
})
</script>

<template>
  <div class="w-full bg-surface min-h-[calc(100vh-64px)] text-high-emphasis relative overflow-hidden pb-20">
    <!-- Ambient Topography & Glow Background -->
    <div class="pointer-events-none absolute inset-x-0 top-0 h-160 overflow-hidden z-0 select-none" aria-hidden="true">
      <div class="w-full h-full bg-topography topography-hero-mask opacity-40"></div>
      <div class="absolute inset-0 bg-[radial-gradient(ellipse_75%_50%_at_50%_10%,rgba(0,207,243,0.1),transparent_70%)] pointer-events-none"></div>
      <div class="absolute inset-0 bg-linear-to-b from-transparent via-surface/60 to-surface pointer-events-none"></div>
    </div>

    <div class="relative z-10 w-full max-w-[80vw] mx-auto px-4 sm:px-6 lg:px-8 pt-10 sm:pt-14 flex flex-col gap-10">
      
      <!-- ========================================== -->
      <!-- 1. Hero Title & Search Bar                 -->
      <!-- ========================================== -->
      <section class="text-center flex flex-col items-center gap-6">
        <div class="flex flex-col items-center gap-2">
          <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary/10 border border-primary/20 text-primary text-xs font-mono font-semibold uppercase tracking-wider">
            <span class="material-symbols-outlined text-[15px]">help_center</span>
            Centro de Ayuda & Soporte
          </span>
          <h1 class="font-display text-3xl sm:text-4xl lg:text-5xl font-bold text-white tracking-tight drop-shadow-sm">
            ¿En qué podemos ayudarte hoy?
          </h1>
          <p class="font-body text-sm sm:text-base text-muted-light max-w-xl">
            Respuestas inmediatas sobre tus entradas digitales, transferencias seguras, accesos y medios de pago.
          </p>
        </div>

        <!-- Search Bar -->
        <div class="w-full max-w-2xl relative shadow-2xl">
          <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-muted-light">
            <span class="material-symbols-outlined text-[22px]">search</span>
          </div>
          <input
            v-model="searchQuery"
            class="w-full pl-12 pr-28 py-4 bg-surface-card/90 border border-border-subtle rounded-2xl text-high-emphasis placeholder:text-muted-light font-body text-sm sm:text-base focus:outline-none focus:border-primary/60 focus:ring-1 focus:ring-primary/40 focus:bg-surface-elevated/90 transition-all shadow-inner"
            placeholder="Buscar por término, ej: 'entradas', 'reembolso', 'QR', 'transferir'..."
            type="text"
            aria-label="Buscar en Centro de Ayuda"
          />
          <div class="absolute inset-y-0 right-2 flex items-center gap-1.5">
            <button
              v-if="searchQuery"
              @click="clearSearch"
              class="text-muted-light hover:text-white p-1.5 rounded-lg hover:bg-surface-interactive transition-colors cursor-pointer"
              title="Borrar búsqueda"
              aria-label="Borrar búsqueda"
            >
              <span class="material-symbols-outlined text-[18px]">close</span>
            </button>
            <button
              type="button"
              class="bg-primary hover:bg-btn-buy-hover text-on-primary-fixed font-body text-xs sm:text-sm font-bold px-4 py-2.5 rounded-xl transition-all flex items-center gap-1 shadow-sm select-none cursor-pointer"
            >
              <span>Buscar</span>
              <span class="material-symbols-outlined text-[16px]">arrow_forward</span>
            </button>
          </div>
        </div>
      </section>

      <!-- ========================================== -->
      <!-- 2. Accordion FAQ Section                   -->
      <!-- ========================================== -->
      <section class="bg-surface-card/70 backdrop-blur-xs rounded-2xl p-5 sm:p-8 border border-border-subtle flex flex-col gap-6 shadow-xl">
        <!-- Accordion Header -->
        <div class="flex flex-col sm:flex-row sm:items-center justify-between border-b border-border-subtle/70 pb-4 gap-3">
          <div class="flex items-center gap-2.5">
            <div class="w-8 h-8 rounded-lg bg-secondary/10 border border-secondary/20 text-secondary flex items-center justify-center">
              <span class="material-symbols-outlined text-[20px]">quiz</span>
            </div>
            <h2 class="font-display text-xl sm:text-2xl text-white font-bold tracking-tight">
              Preguntas Frecuentes
            </h2>
            <span class="ml-1.5 px-2.5 py-0.5 rounded-full bg-surface-elevated text-primary font-mono text-xs border border-border-subtle">
              {{ filteredFaqs.length }}
            </span>
          </div>

          <div class="flex items-center gap-4 text-xs font-mono">
            <button
              v-if="searchQuery"
              @click="clearSearch"
              class="text-primary hover:text-accent-sky underline transition-colors cursor-pointer"
            >
              Ver todas las preguntas
            </button>
            <span class="text-muted-light flex items-center gap-1">
              <span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
              Actualizado hoy
            </span>
          </div>
        </div>

        <!-- FAQ Accordion Items List -->
        <div class="flex flex-col gap-3">
          <div
            v-for="faq in filteredFaqs"
            :key="faq.id"
            :id="faq.id"
            :class="[
              'rounded-xl p-4 sm:p-5 flex flex-col gap-2 border transition-all duration-300 ease-in-out',
              openItem === faq.id
                ? 'bg-surface-elevated/80 border-primary/50 shadow-lg ring-1 ring-primary/20'
                : 'bg-surface-container-deep/80 border-border-subtle hover:border-border-structural hover:bg-surface-card'
            ]"
          >
            <!-- Accordion Toggle Button -->
            <button
              @click="toggleFaq(faq.id)"
              class="w-full flex items-center justify-between cursor-pointer text-left select-none focus:outline-none group gap-3 py-1"
              :aria-expanded="openItem === faq.id"
            >
              <span
                :class="[
                  'font-display font-semibold transition-colors duration-200 text-sm sm:text-base leading-snug',
                  openItem === faq.id ? 'text-primary' : 'text-high-emphasis group-hover:text-primary'
                ]"
              >
                {{ faq.question }}
              </span>
              <span
                :class="[
                  'material-symbols-outlined text-[22px] transition-transform duration-300 ease-in-out shrink-0',
                  openItem === faq.id ? 'text-primary rotate-180' : 'text-muted-light group-hover:text-white'
                ]"
              >
                expand_more
              </span>
            </button>

            <!-- Collapsible Content Animated with CSS Grid -->
            <div
              class="grid transition-all duration-300 ease-in-out"
              :class="openItem === faq.id ? 'grid-rows-[1fr] opacity-100' : 'grid-rows-[0fr] opacity-0'"
            >
              <div class="overflow-hidden">
                <div class="font-body text-medium-emphasis text-sm sm:text-base leading-relaxed pt-2 pb-1 border-t border-border-subtle/50 mt-2">
                  <p v-html="faq.answer" class="text-medium-emphasis"></p>
                </div>
              </div>
            </div>
          </div>

          <!-- Empty Search State -->
          <div
            v-if="filteredFaqs.length === 0"
            class="py-12 px-4 text-center flex flex-col items-center gap-3 bg-surface-container-deep/50 rounded-xl border border-dashed border-border-subtle"
          >
            <div class="w-12 h-12 rounded-full bg-surface-elevated flex items-center justify-center text-muted-light">
              <span class="material-symbols-outlined text-[28px]">search_off</span>
            </div>
            <div class="flex flex-col gap-1 max-w-md">
              <h3 class="font-display text-base font-semibold text-white">
                No encontramos respuestas para "{{ searchQuery }}"
              </h3>
              <p class="font-body text-xs sm:text-sm text-muted-light">
                Intenta buscar con palabras más simples como "entradas", "devolución", "pago" o consulta a nuestro equipo mediante el chat de soporte.
              </p>
            </div>
            <button
              @click="clearSearch"
              class="mt-2 bg-surface-elevated hover:bg-surface-interactive text-primary px-4 py-2 rounded-lg font-mono text-xs border border-border-subtle transition-colors cursor-pointer"
            >
              Borrar búsqueda
            </button>
          </div>
        </div>
      </section>

      <!-- ========================================== -->
      <!-- 3. Escalation / Support CTA Banner         -->
      <!-- ========================================== -->
      <section class="bg-surface-card border border-border-subtle rounded-2xl p-6 sm:p-8 flex flex-col md:flex-row items-center justify-between gap-6 shadow-xl">
        <div class="flex items-start gap-4">
          <div class="w-12 h-12 rounded-xl bg-primary/10 border border-primary/20 text-primary flex items-center justify-center shrink-0 shadow-inner">
            <span class="material-symbols-outlined text-[28px]">support_agent</span>
          </div>
          <div class="flex flex-col gap-1 text-left">
            <h3 class="font-display text-lg sm:text-xl text-white font-bold tracking-tight">
              ¿No encontraste lo que buscabas o necesitas más ayuda?
            </h3>
            <p class="font-body text-sm sm:text-base text-muted-light max-w-xl leading-relaxed">
              Conéctate con nuestro asistente virtual inteligente para resolver incidentes con tus compras o transferir tu caso a un especialista en minutos.
            </p>
          </div>
        </div>

        <div class="flex flex-col sm:flex-row items-center gap-3 w-full md:w-auto shrink-0">
          <RouterLink
            :to="{ name: 'support-chat' }"
            class="w-full sm:w-auto bg-primary hover:bg-btn-buy-hover text-on-primary-fixed font-body text-sm font-bold px-6 py-3 rounded-xl flex items-center justify-center gap-2 transition-all shadow-md cursor-pointer"
          >
            <span class="material-symbols-outlined text-[20px]">chat</span>
            <span>Iniciar chat de soporte</span>
          </RouterLink>
          <RouterLink
            :to="{ name: 'tickets' }"
            class="w-full sm:w-auto bg-surface-elevated hover:bg-surface-interactive text-white px-5 py-3 rounded-xl font-body text-sm border border-border-subtle transition-all flex items-center justify-center gap-2 cursor-pointer"
          >
            <span class="material-symbols-outlined text-[18px]">receipt_long</span>
            <span>Ver mis tickets (3)</span>
          </RouterLink>
        </div>
      </section>

    </div>
  </div>
</template>
