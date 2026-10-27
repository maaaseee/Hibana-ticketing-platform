import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import CreditCardForm from '../components/checkout/CreditCardForm.vue'

describe('CreditCardForm.vue - Phase 2.2 Interactive Formatting & Issuer Detection', () => {
  function createWrapper(props = {}) {
    return mount(CreditCardForm, {
      props: {
        cardNumber: '',
        cardHolder: '',
        cardExpiry: '',
        cardCvc: '',
        saveCard: false,
        ...props,
      },
    })
  }

  it('displays default credit_card icon when no BIN is entered', () => {
    const wrapper = createWrapper({ cardNumber: '' })
    expect(wrapper.find('[data-testid="card-issuer-badge"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('credit_card')
  })

  it('detects Visa when card number starts with 4', () => {
    const wrapper = createWrapper({ cardNumber: '4532 0150 1234 5678' })
    const badge = wrapper.find('[data-testid="card-issuer-badge"]')
    expect(badge.exists()).toBe(true)
    expect(badge.text()).toBe('VISA')
  })

  it('detects Mastercard when card number starts with 51-55 or 22-27', () => {
    const wrapper = createWrapper({ cardNumber: '5353 1234 5678 9012' })
    const badge = wrapper.find('[data-testid="card-issuer-badge"]')
    expect(badge.exists()).toBe(true)
    expect(badge.text()).toBe('Mastercard')
  })

  it('detects Amex when card number starts with 34 or 37', () => {
    const wrapper = createWrapper({ cardNumber: '3782 822463 10005' })
    const badge = wrapper.find('[data-testid="card-issuer-badge"]')
    expect(badge.exists()).toBe(true)
    expect(badge.text()).toBe('AMEX')
  })

  it('detects Cabal when card number starts with 6042, 5896, or 60', () => {
    const wrapper = createWrapper({ cardNumber: '6042 1234 5678 9012' })
    const badge = wrapper.find('[data-testid="card-issuer-badge"]')
    expect(badge.exists()).toBe(true)
    expect(badge.text()).toBe('CABAL')
  })

  it('formats standard card numbers in 4-digit groups on input', async () => {
    const wrapper = createWrapper()
    const input = wrapper.find('input[placeholder="0000 0000 0000 0000"]')
    
    // Simulate user typing digits without spaces
    ;(input.element as HTMLInputElement).value = '4111222233334444'
    await input.trigger('input')

    expect(wrapper.emitted('update:cardNumber')?.[0]).toEqual(['4111 2222 3333 4444'])
    expect(wrapper.emitted('formatCardNumber')).toBeTruthy()
  })

  it('formats Amex card numbers with 4-6-5 structure on input', async () => {
    const wrapper = createWrapper()
    const input = wrapper.find('input[placeholder="0000 0000 0000 0000"]')
    
    ;(input.element as HTMLInputElement).value = '378282246310005'
    await input.trigger('input')

    expect(wrapper.emitted('update:cardNumber')?.[0]).toEqual(['3782 822463 10005'])
  })

  it('auto-inserts slash into expiration date MM/YY', async () => {
    const wrapper = createWrapper()
    const input = wrapper.find('input[placeholder="MM/AA"]')

    ;(input.element as HTMLInputElement).value = '1228'
    await input.trigger('input')

    expect(wrapper.emitted('update:cardExpiry')?.[0]).toEqual(['12/28'])
    expect(wrapper.emitted('formatExpiry')).toBeTruthy()
  })

  it('strictly restricts CVV to numeric characters and enforces 3 digits for standard cards', async () => {
    const wrapper = createWrapper({ cardNumber: '4532 0000 0000 0000' })
    const input = wrapper.find('input[placeholder="123"]')

    ;(input.element as HTMLInputElement).value = '98a7x1'
    await input.trigger('input')

    // Only first 3 digits kept
    expect(wrapper.emitted('update:cardCvc')?.[0]).toEqual(['987'])
  })

  it('allows 4 numeric digits for Amex CVV', async () => {
    const wrapper = createWrapper({ cardNumber: '3782 000000 00000' })
    const input = wrapper.find('input[placeholder="1234"]')
    expect(input.exists()).toBe(true)

    ;(input.element as HTMLInputElement).value = '88889'
    await input.trigger('input')

    expect(wrapper.emitted('update:cardCvc')?.[0]).toEqual(['8888'])
  })
})
