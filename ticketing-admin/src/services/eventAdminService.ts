import type { CreateEventRequest, EventCreatedResponse, EventResponse } from '@/types/event'
import { apiClient } from './api'

export const eventAdminService = {
  async getEvents(params?: { category?: string; q?: string; venue?: string; status?: string }): Promise<EventResponse[]> {
    const query = new URLSearchParams()
    query.append('status', params?.status || 'ALL')
    if (params?.category) query.append('category', params.category)
    if (params?.q) query.append('q', params.q)
    if (params?.venue) query.append('venue', params.venue)

    const queryString = query.toString() ? `?${query.toString()}` : ''
    return apiClient<EventResponse[]>(`/api/v1/events${queryString}`)
  },

  async getEventById(id: string): Promise<EventResponse> {
    return apiClient<EventResponse>(`/api/v1/events/${id}`)
  },

  async createEvent(request: CreateEventRequest): Promise<EventCreatedResponse> {
    return apiClient<EventCreatedResponse>('/api/v1/events', {
      method: 'POST',
      body: JSON.stringify(request),
    })
  },

  async publishEvent(id: string): Promise<void> {
    await apiClient<void>(`/api/v1/events/${id}/publish`, {
      method: 'POST',
    })
  },

  async cancelEvent(id: string, reason?: string): Promise<void> {
    const query = reason ? `?reason=${encodeURIComponent(reason)}` : ''
    await apiClient<void>(`/api/v1/events/${id}${query}`, {
      method: 'DELETE',
    })
  },
}
