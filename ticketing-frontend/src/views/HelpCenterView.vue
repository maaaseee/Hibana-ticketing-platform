<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import HelpSearchBar from '../components/help/HelpSearchBar.vue'
import HelpFaqAccordion, { type FaqItem } from '../components/help/HelpFaqAccordion.vue'
import HelpEscalationBanner from '../components/help/HelpEscalationBanner.vue'

export type { FaqItem }

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
      'Ingresa a <strong class="text-on-surface font-semibold">Mis Entradas</strong>, abre los detalles del boleto que quieres traspasar y presiona <strong class="text-on-surface font-semibold">Transferir Entrada</strong>. Solo debes escribir el correo electrónico de la persona que lo recibirá (debe tener una cuenta creada en Hibana.tk). Por seguridad, las transferencias pueden realizarse hasta 2 horas antes de la apertura de puertas del evento.',
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
      'Te notificaremos inmediatamente por correo electrónico. Si el espectáculo se reprograma para una nueva fecha, tu entrada seguirá siendo válida automáticamente. Si no puedes asistir a la nueva fecha o el evento se cancela de forma definitiva, dispondrás de 15 días continuos para solicitar la devolución total de tu dinero a tu medio de pago original o a tu saldo Hibana Pocket.',
    tags: ['cancelacion', 'reprogramado', 'devolucion', 'reembolso', 'dinero', 'fecha']
  },
  {
    id: 'faq-payment-methods',
    question: '¿Cuáles son los métodos de pago aceptados?',
    answer:
      'Aceptamos tarjetas de débito y crédito nacionales e internacionales (Visa, Mastercard, American Express), transferencias bancarias y saldo disponible en tu billetera digital Hibana Pocket. Todos los pagos se procesan con certificación de máxima seguridad financiera.',
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
      <!-- 1. Hero Title & Search Bar -->
      <HelpSearchBar
        v-model="searchQuery"
        @clear="clearSearch"
      />

      <!-- 2. Accordion FAQ Section -->
      <HelpFaqAccordion
        :faqs="filteredFaqs"
        :search-query="searchQuery"
        :open-item="openItem"
        @toggle="toggleFaq"
        @clear-search="clearSearch"
      />

      <!-- 3. Escalation / Support CTA Banner -->
      <HelpEscalationBanner />
    </div>
  </div>
</template>
