import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import HelpFaqAccordion, { type FaqItem } from '../components/help/HelpFaqAccordion.vue'

describe('HelpFaqAccordion.vue - XSS Sanitization & Safe v-html Rendering', () => {
  it('sanitizes malicious script tags and inline event handlers in FAQ answers', () => {
    const maliciousFaqs: FaqItem[] = [
      {
        id: 'faq-xss-1',
        question: '¿Es seguro el contenido?',
        answer: 'Respuesta normal <script>window.__pwned = true</script><img src="x" onerror="window.__pwned=true"> con <strong>formato válido</strong>',
        tags: ['seguridad'],
      },
    ]

    const wrapper = mount(HelpFaqAccordion, {
      props: {
        faqs: maliciousFaqs,
        searchQuery: '',
        openItem: 'faq-xss-1',
      },
    })

    const htmlContent = wrapper.find('.overflow-hidden p').html()

    // Malicious script and onerror must be stripped by DOMPurify
    expect(htmlContent).not.toContain('<script>')
    expect(htmlContent).not.toContain('onerror')
    expect(htmlContent).not.toContain('window.__pwned')

    // Safe formatting tags like <strong> should be preserved
    expect(htmlContent).toContain('<strong>formato válido</strong>')
    expect(htmlContent).toContain('Respuesta normal')
  })
})
