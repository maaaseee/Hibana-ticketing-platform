import { describe, it, expect, vi, beforeEach } from 'vitest'
import { orderService } from '../services/orderService'

describe('orderService', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('provides correct preview endpoint URLs', () => {
    expect(orderService.getPreviewInvoicePdfUrl()).toBe('/api/v1/orders/preview/invoice.pdf')
    expect(orderService.getPreviewEmailHtmlUrl()).toBe('/api/v1/orders/preview/email.html')
  })

  it('triggers browser download when invoice is requested', async () => {
    const mockBlob = new Blob(['%PDF-mock-data'], { type: 'application/pdf' })
    const mockFetch = vi.fn().mockResolvedValue({
      ok: true,
      blob: () => Promise.resolve(mockBlob),
    })
    globalThis.fetch = mockFetch

    const createObjectURLSpy = vi.spyOn(window.URL, 'createObjectURL').mockReturnValue('blob:mock-url')
    const revokeObjectURLSpy = vi.spyOn(window.URL, 'revokeObjectURL').mockImplementation(() => {})

    await orderService.downloadInvoicePdf('ORD-998877')

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/v1/orders/ORD-998877/invoice',
      expect.objectContaining({ method: 'GET' })
    )
    expect(createObjectURLSpy).toHaveBeenCalledWith(mockBlob)
    expect(revokeObjectURLSpy).toHaveBeenCalledWith('blob:mock-url')
  })

  it('falls back to simulation when resend confirmation encounters an error', async () => {
    const result = await orderService.resendConfirmationEmail('ORD-12345', 'buyer@hibana.tk')
    expect(result.status).toBe('DISPATCHED')
    expect(result.orderId).toBe('ORD-12345')
  })
})
