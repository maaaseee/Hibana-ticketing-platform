import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Ticket, Seat } from '../types'

export const useTicketsStore = defineStore('tickets', () => {
  const tickets = ref<Ticket[]>([
    {
      id: 'tkt_01J9X8A1B2C3D4E5',
      orderId: 'ORD-99120',
      eventId: 'chronos-fest-2026',
      eventTitle: 'CHRONOS FESTIVAL 2026',
      eventDate: '28 Nov 2026 • 21:00 ART',
      venue: 'Estadio Obras Sanitarias',
      sector: 'Platea A',
      row: 'B',
      seatNumber: 14,
      customerName: 'Lucas Ferraro',
      customerEmail: 'lucas.ferraro@gmail.com',
      status: 'ISSUED',
      qrPayload: {
        tid: 'tkt_01J9X8A1B2C3D4E5',
        eid: 'evt_chronos_2026',
        vid: 'ven_obras_sanitarias',
        sec: 'sec_platea_a',
        sig: 'MEQCIAx9821zLmOP84920qX918a',
      },
      totpRemainingSeconds: 15,
      barcode: '9840219482019482',
    },
    {
      id: 'tkt_01J9X8A1B2C3D4E6',
      orderId: 'ORD-99120',
      eventId: 'chronos-fest-2026',
      eventTitle: 'CHRONOS FESTIVAL 2026',
      eventDate: '28 Nov 2026 • 21:00 ART',
      venue: 'Estadio Obras Sanitarias',
      sector: 'Platea A',
      row: 'B',
      seatNumber: 15,
      customerName: 'Lucas Ferraro',
      customerEmail: 'lucas.ferraro@gmail.com',
      status: 'ISSUED',
      qrPayload: {
        tid: 'tkt_01J9X8A1B2C3D4E6',
        eid: 'evt_chronos_2026',
        vid: 'ven_obras_sanitarias',
        sec: 'sec_platea_a',
        sig: 'MEQCIAy1829zLmOP84920qX918b',
      },
      totpRemainingSeconds: 15,
      barcode: '9840219482019483',
    },
    {
      id: 'tkt_01J8Y7B2C3D4E5F6',
      orderId: 'ORD-84012',
      eventId: 'cybersymphony-tour',
      eventTitle: 'CYBERSYMPHONY TOUR 2025',
      eventDate: '15 Oct 2025 • 20:30 ART',
      venue: 'Teatro Colón',
      sector: 'VIP Lounge',
      row: 'VIP',
      seatNumber: 4,
      customerName: 'Lucas Ferraro',
      customerEmail: 'lucas.ferraro@gmail.com',
      status: 'REDEEMED',
      qrPayload: {
        tid: 'tkt_01J8Y7B2C3D4E5F6',
        eid: 'evt_cybersymphony_2025',
        vid: 'ven_colon',
        sec: 'sec_vip',
        sig: 'MEQCIAz7461xLmOP84920qX918c',
      },
      totpRemainingSeconds: 0,
      barcode: '7461029482019481',
    },
  ])

  function transferTicket(ticketId: string, newEmail: string): boolean {
    const ticket = tickets.value.find(t => t.id === ticketId)
    if (!ticket) return false
    ticket.customerEmail = newEmail
    ticket.status = 'TRANSFERRED'
    return true
  }

  function addIssuedTickets(eventTitle: string, eventDate: string, venue: string, selectedSeats: Seat[], orderId: string, customerName: string, customerEmail: string) {
    selectedSeats.forEach((seat, idx) => {
      const id = `tkt_${Date.now().toString(36)}_${idx}`
      tickets.value.unshift({
        id,
        orderId,
        eventId: 'chronos-fest-2026',
        eventTitle,
        eventDate,
        venue,
        sector: seat.sector,
        row: seat.row,
        seatNumber: seat.number,
        customerName,
        customerEmail,
        status: 'ISSUED',
        qrPayload: {
          tid: id,
          eid: 'evt_dynamic',
          vid: 'ven_main',
          sec: seat.sector,
          sig: `MEQCIA_${Math.random().toString(36).substring(2, 10)}`,
        },
        totpRemainingSeconds: 15,
        barcode: `${Math.floor(1000000000000000 + Math.random() * 9000000000000000)}`,
      })
    })
  }

  return {
    tickets,
    transferTicket,
    addIssuedTickets,
  }
})
