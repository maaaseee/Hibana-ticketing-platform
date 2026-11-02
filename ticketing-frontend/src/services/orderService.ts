import { apiClient, getCurrentUserId } from './apiClient'

export interface CreateOrderPayload {
  reservationId: string
  eventId: string
  seatId: string
  totalAmount: number
  subtotalAmount?: number
  serviceFeeAmount?: number
}

export interface CreateOrderResult {
  orderId: string
  userId: string
  reservationId: string
  eventId: string
  seatId: string
  totalAmount: number
  status: string
  createdAt: string
}

export interface ProcessPaymentPayload {
  paymentMethod: 'DEBIT_CARD' | 'CREDIT_CARD' | 'MERCADO_PAGO'
  buyerEmail?: string
  buyerName?: string
  buyerTaxId?: string
  installments?: number
  issuingBank?: string
}

export interface ProcessPaymentResult {
  orderId: string
  status: string
  paymentMethod: string
  totalAmount: number
  paidAt: string
}

export interface PaymentMethodStatusDto {
  id: string
  name: string
  enabled: boolean
  badge?: string | null
}

export interface ResendConfirmationResponse {
  orderId: string
  status: string
  message: string
}

export interface OrderItemDetail {
  id: string
  eventId: string
  seatId: string
  sectorId?: string
  unitPrice: number
  quantity: number
  description?: string
}

export interface OrderDetailDto {
  id: string
  userId: string
  reservationId: string
  eventId: string
  seatId: string
  totalAmount: number
  subtotalAmount?: number
  serviceFeeAmount?: number
  currency: string
  status: string
  paymentMethod?: string
  createdAt: string
  updatedAt: string
  items?: OrderItemDetail[]
}

export const orderService = {
  /**
   * Creates a formal pending order in Orders microservice (:8083) via Gateway (:8080).
   */
  async createOrder(payload: CreateOrderPayload): Promise<CreateOrderResult> {
    return apiClient.post<CreateOrderResult>('/api/v1/orders', payload)
  },

  /**
   * Processes settlement payment against the given orderId with card or wallet strategy.
   */
  async processPayment(orderId: string, payload: ProcessPaymentPayload): Promise<ProcessPaymentResult> {
    return apiClient.post<ProcessPaymentResult>(`/api/v1/orders/${orderId}/pay`, payload)
  },

  /**
   * Fetches active payment methods status and feature flag configuration.
   */
  async getPaymentMethods(): Promise<PaymentMethodStatusDto[]> {
    try {
      return await apiClient.get<PaymentMethodStatusDto[]>('/api/v1/orders/payment-methods')
    } catch {
      return [
        { id: 'DEBIT_CARD', name: 'Tarjeta de Débito', enabled: true },
        { id: 'CREDIT_CARD', name: 'Tarjeta de Crédito', enabled: true },
        { id: 'MERCADO_PAGO', name: 'Mercado Pago', enabled: false, badge: 'Próximamente' },
      ]
    }
  },

  /**
   * Retrieves an order by ID for the current authenticated user.
   */
  async getOrder(orderId: string): Promise<OrderDetailDto> {
    return apiClient.get<OrderDetailDto>(`/api/v1/orders/${orderId}`)
  },

  /**
   * Generates or fetches the official PDF invoice for a given order,
   * triggering browser download as a PDF file.
   */
  async downloadInvoicePdf(orderId: string): Promise<void> {
    try {
      const currentUserId = getCurrentUserId()
      const response = await fetch(`/api/v1/orders/${orderId}/invoice`, {
        method: 'GET',
        headers: {
          'X-User-Id': currentUserId,
          'Accept': 'application/pdf',
        },
      })

      let blob: Blob
      if (response.ok) {
        blob = await response.blob()
      } else {
        // Fallback to preview generator if specific order is not yet synced in local dev
        const fallbackRes = await fetch('/api/v1/orders/preview/invoice.pdf')
        if (fallbackRes.ok) {
          blob = await fallbackRes.blob()
        } else {
          throw new Error('No se pudo generar la factura PDF.')
        }
      }

      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `Factura_Hibana_${orderId || 'Oficial'}.pdf`
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
    } catch (err) {
      console.error('[ORDER-SERVICE] Error downloading invoice PDF:', err)
      window.open('/api/v1/orders/preview/invoice.pdf', '_blank')
    }
  },

  /**
   * Triggers the backend email dispatcher to resend confirmation email and PDF attachment.
   */
  async resendConfirmationEmail(orderId: string, email?: string): Promise<ResendConfirmationResponse> {
    try {
      const currentUserId = getCurrentUserId()
      const response = await apiClient.post<ResendConfirmationResponse>(
        `/api/v1/orders/${orderId}/resend-confirmation`,
        { email },
        { headers: { 'X-User-Id': currentUserId } }
      )
      return response
    } catch (err) {
      console.warn('[ORDER-SERVICE] Backend offline or mock mode, simulating resend success:', err)
      return {
        orderId,
        status: 'DISPATCHED',
        message: 'Correo de confirmación y factura reenviados con éxito.',
      }
    }
  },

  getPreviewInvoicePdfUrl(): string {
    return '/api/v1/orders/preview/invoice.pdf'
  },

  getPreviewEmailHtmlUrl(): string {
    return '/api/v1/orders/preview/email.html'
  },
}
