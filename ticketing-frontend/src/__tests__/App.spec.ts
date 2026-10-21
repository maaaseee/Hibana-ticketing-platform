import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia } from 'pinia'
import App from '../App.vue'
import HomeView from '../views/HomeView.vue'

describe('App', () => {
  it('mounts and renders properly with router', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', name: 'home', component: HomeView },
        { path: '/events', name: 'events-catalog', component: { template: '<div>events</div>' } },
        { path: '/help', name: 'help', component: { template: '<div>help</div>' } },
        { path: '/support', name: 'support', component: { template: '<div>support</div>' } }
      ]
    })
    const pinia = createPinia()

    router.push('/')
    await router.isReady()

    const wrapper = mount(App, {
      global: {
        plugins: [router, pinia]
      }
    })
    expect(wrapper.exists()).toBe(true)
  })
})
