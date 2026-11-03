import { watchEffect, isRef, type Ref } from 'vue'

export interface PageMetaOptions {
  title?: string
  description?: string
  image?: string
  url?: string
  type?: 'website' | 'article' | 'event'
  keywords?: string[]
}

const DEFAULT_TITLE = 'Hibana.tk • Entradas Oficiales y Pases a Conciertos'
const DEFAULT_DESCRIPTION =
  'Plataforma oficial de venta de entradas y reserva de asientos en tiempo real para los mejores recitales y espectáculos en estadios y arenas de Argentina.'
const DEFAULT_IMAGE =
  'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=1200&q=80'

function updateMetaTag(attributeName: 'name' | 'property', attributeValue: string, content: string) {
  if (typeof document === 'undefined') return
  let element = document.querySelector(`meta[${attributeName}="${attributeValue}"]`)
  if (!element) {
    element = document.createElement('meta')
    element.setAttribute(attributeName, attributeValue)
    document.head.appendChild(element)
  }
  element.setAttribute('content', content)
}

export function setPageMeta(options: PageMetaOptions = {}) {
  if (typeof document === 'undefined') return

  const fullTitle = options.title ? `${options.title} | Hibana.tk` : DEFAULT_TITLE
  document.title = fullTitle

  const description = options.description || DEFAULT_DESCRIPTION
  const image = options.image || DEFAULT_IMAGE
  const currentUrl =
    options.url || (typeof window !== 'undefined' ? window.location.href : 'https://www.hibana.tk')
  const ogType = options.type || 'website'

  // Standard metadata
  updateMetaTag('name', 'description', description)
  if (options.keywords && options.keywords.length > 0) {
    updateMetaTag('name', 'keywords', options.keywords.join(', '))
  }

  // Open Graph
  updateMetaTag('property', 'og:title', fullTitle)
  updateMetaTag('property', 'og:description', description)
  updateMetaTag('property', 'og:image', image)
  updateMetaTag('property', 'og:url', currentUrl)
  updateMetaTag('property', 'og:type', ogType)
  updateMetaTag('property', 'og:site_name', 'Hibana.tk')

  // Twitter Cards
  updateMetaTag('name', 'twitter:card', 'summary_large_image')
  updateMetaTag('name', 'twitter:title', fullTitle)
  updateMetaTag('name', 'twitter:description', description)
  updateMetaTag('name', 'twitter:image', image)
}

export function usePageMeta(metaGetter: PageMetaOptions | Ref<PageMetaOptions> | (() => PageMetaOptions)) {
  watchEffect(() => {
    let resolved: PageMetaOptions
    if (typeof metaGetter === 'function') {
      resolved = metaGetter()
    } else if (isRef(metaGetter)) {
      resolved = metaGetter.value
    } else {
      resolved = metaGetter
    }
    setPageMeta(resolved)
  })
}
