import type { EventItem, SectorPricing } from '../types'
import { mockVenues } from './mockVenues'

const monumentalVenue = mockVenues.find(v => v.id === 'estadio-monumental')!
const arenaVenue = mockVenues.find(v => v.id === 'arena-buenos-aires')!
const rexVenue = mockVenues.find(v => v.id === 'teatro-rex')!

function buildTicketTypes(sectors: SectorPricing[]): { name: string; price: number }[] {
  return sectors.map(sec => ({
    name: `${sec.name} ($${sec.price.toLocaleString('es-AR')})`,
    price: sec.price
  }))
}

export const mockEvents: EventItem[] = [
  {
    id: 'symphony-of-lights',
    title: 'Symphony of Lights 2026',
    subtitle: 'Una noche donde la orquestación clásica se fusiona con visuales inmersivos y tecnología de iluminación volumétrica.',
    date: '15 de Octubre de 2026',
    dateIso: '2026-10-15T20:00:00',
    time: '20:00 HRS',
    doorsOpen: '18:00 HRS',
    venue: arenaVenue.name,
    venueId: arenaVenue.id,
    location: 'Buenos Aires, Argentina',
    category: 'Música & Conciertos',
    genre: 'Music & Concerts',
    dateFilter: 'this_weekend',
    price: 45000,
    minPrice: 45000,
    imageUrl: 'https://images.unsplash.com/photo-1465847899084-d164df4dedc6?auto=format&fit=crop&w=1200&q=80',
    badgeText: 'Confirmado',
    badgeType: 'secondary',
    status: 'AVAILABLE',
    description: 'The Symphony of Lights llega al Arena Buenos Aires con un despliegue audiovisual sin precedentes, sonido 360 y una orquesta en vivo con más de 60 músicos en escena.',
    aiSimilarityScore: 0.94,
    lineup: 'The Lumineers, Of Monsters and Men, Orquesta Filarmónica',
    venueMapUrl: '/images/venues/arena-buenos-aires-map.svg',
    parking: arenaVenue.parking,
    transit: arenaVenue.transit,
    paymentPromotions: arenaVenue.paymentMethods,
    sectors: arenaVenue.sectors,
    functions: [
      {
        id: 'fn-symphony-1',
        dateIso: '2026-10-15T20:00:00',
        formattedDate: 'Jueves 15 de Octubre de 2026',
        doorsOpen: '18:00 hs',
        showStart: '20:00 hs',
        status: 'AVAILABLE'
      },
      {
        id: 'fn-symphony-2',
        dateIso: '2026-10-16T20:00:00',
        formattedDate: 'Viernes 16 de Octubre de 2026',
        doorsOpen: '18:00 hs',
        showStart: '20:00 hs',
        status: 'AVAILABLE'
      }
    ],
    availableDates: [
      'Jueves 15 de Octubre de 2026 - 20:00 hs',
      'Viernes 16 de Octubre de 2026 - 20:00 hs'
    ],
    ticketTypes: buildTicketTypes(arenaVenue.sectors),
    maxTicketsPerUser: 4,
    salesPolicy: { maxTicketsPerUser: 4, waitingRoomEnabled: true }
  },
  {
    id: 'chronos-fest-2026',
    title: 'Neon Nights: Electronic Festival',
    subtitle: 'La convergencia de techno melódico y visuales inmersivos más grande de Iberoamérica. Más de 12 horas de música continua.',
    date: '24 de Octubre de 2026',
    dateIso: '2026-10-24T21:00:00',
    time: '21:00 HRS',
    doorsOpen: '18:30 HRS',
    venue: arenaVenue.name,
    venueId: arenaVenue.id,
    location: 'Buenos Aires, Argentina',
    category: 'Música & Conciertos',
    genre: 'Music & Concerts',
    dateFilter: 'this_weekend',
    price: 45000,
    minPrice: 45000,
    imageUrl: 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=1200&q=80',
    badgeText: 'Últimas Entradas',
    badgeType: 'primary',
    status: 'FEW_SEATS',
    description: 'Una noche con despliegue lumínico bioluminiscente de 360 grados, sistemas de audio L-Acoustics K1 y los mayores referentes del techno melódico global.',
    aiSimilarityScore: 0.98,
    lineup: 'Tale of Us, Boris Brejcha, Stephan Bodzin, Mathame',
    venueMapUrl: '/images/venues/arena-buenos-aires-map.svg',
    parking: arenaVenue.parking,
    transit: arenaVenue.transit,
    paymentPromotions: arenaVenue.paymentMethods,
    sectors: arenaVenue.sectors,
    functions: [
      {
        id: 'fn-chronos-1',
        dateIso: '2026-10-24T21:00:00',
        formattedDate: 'Sábado 24 de Octubre de 2026',
        doorsOpen: '18:30 hs',
        showStart: '21:00 hs',
        status: 'FEW_SEATS'
      },
      {
        id: 'fn-chronos-2',
        dateIso: '2026-10-25T21:00:00',
        formattedDate: 'Domingo 25 de Octubre de 2026',
        doorsOpen: '18:30 hs',
        showStart: '21:00 hs',
        status: 'AVAILABLE'
      }
    ],
    availableDates: [
      'Sábado 24 de Octubre de 2026 - 21:00 hs',
      'Domingo 25 de Octubre de 2026 - 21:00 hs'
    ],
    ticketTypes: buildTicketTypes(arenaVenue.sectors),
    maxTicketsPerUser: 4,
    salesPolicy: { maxTicketsPerUser: 4, waitingRoomEnabled: true }
  },
  {
    id: 'coldplay-music-spheres',
    title: 'Coldplay: Music of the Spheres Tour 2026',
    subtitle: 'El espectáculo en estadio más imponente del planeta con pulseras LED interactivas y escenografía sustentable.',
    date: '06 de Noviembre de 2026',
    dateIso: '2026-11-06T21:00:00',
    time: '21:00 HRS',
    doorsOpen: '17:00 HRS',
    venue: monumentalVenue.name,
    venueId: monumentalVenue.id,
    location: 'Buenos Aires, Argentina',
    category: 'Música & Conciertos',
    genre: 'Music & Concerts',
    dateFilter: 'next_30_days',
    price: 38000,
    minPrice: 38000,
    imageUrl: '/images/events/coldplay-tour.jpg',
    badgeText: 'Gira Mundial',
    badgeType: 'primary',
    status: 'AVAILABLE',
    description: 'Coldplay regresa a Núñez con su aclamado Music of the Spheres World Tour, combinando éxitos legendarios con una puesta en escena futurista, pirotecnia eco-friendly y sincronía total de pulseras luminosas.',
    aiSimilarityScore: 0.99,
    lineup: 'Coldplay, H.E.R., Zoe Gotusso',
    venueMapUrl: '/images/venues/monumental-map.svg',
    parking: monumentalVenue.parking,
    transit: monumentalVenue.transit,
    paymentPromotions: monumentalVenue.paymentMethods,
    sectors: monumentalVenue.sectors,
    functions: [
      {
        id: 'fn-coldplay-1',
        dateIso: '2026-11-06T21:00:00',
        formattedDate: 'Viernes 06 de Noviembre de 2026',
        doorsOpen: '17:00 hs',
        showStart: '21:00 hs',
        status: 'AVAILABLE'
      },
      {
        id: 'fn-coldplay-2',
        dateIso: '2026-11-07T21:00:00',
        formattedDate: 'Sábado 07 de Noviembre de 2026',
        doorsOpen: '17:00 hs',
        showStart: '21:00 hs',
        status: 'AVAILABLE'
      },
      {
        id: 'fn-coldplay-3',
        dateIso: '2026-11-08T21:00:00',
        formattedDate: 'Domingo 08 de Noviembre de 2026',
        doorsOpen: '17:00 hs',
        showStart: '21:00 hs',
        status: 'AVAILABLE'
      }
    ],
    availableDates: [
      'Viernes 06 de Noviembre de 2026 - 21:00 hs',
      'Sábado 07 de Noviembre de 2026 - 21:00 hs',
      'Domingo 08 de Noviembre de 2026 - 21:00 hs'
    ],
    ticketTypes: buildTicketTypes(monumentalVenue.sectors),
    maxTicketsPerUser: 6,
    salesPolicy: { maxTicketsPerUser: 6, waitingRoomEnabled: true }
  },
  {
    id: 'global-tech-summit',
    title: 'Global Tech Summit Buenos Aires 2026',
    subtitle: 'Arquitecturas distribuidas, IA generativa y sistemas de computación distribuida de alta disponibilidad.',
    date: '12 de Noviembre de 2026',
    dateIso: '2026-11-12T09:00:00',
    time: '09:00 HRS',
    doorsOpen: '08:00 HRS',
    venue: rexVenue.name,
    venueId: rexVenue.id,
    location: 'Buenos Aires, Argentina',
    category: 'Conferencias & Tech',
    genre: 'Tech Conferences',
    dateFilter: 'next_30_days',
    price: 25000,
    minPrice: 25000,
    imageUrl: 'https://images.unsplash.com/photo-1505373877841-8d25f7d46678?auto=format&fit=crop&w=1200&q=80',
    badgeText: 'Conferencia',
    badgeType: 'secondary',
    status: 'AVAILABLE',
    description: 'Dos jornadas completas de arquitectura de software, inteligencia artificial agentiva y microservicios de ultra baja latencia con oradores referentes de la industria tecnológica mundial.',
    aiSimilarityScore: 0.89,
    lineup: 'Keynotes por líderes globales de ingeniería y computación en la nube',
    venueMapUrl: '/images/venues/teatro-rex-map.svg',
    parking: rexVenue.parking,
    transit: rexVenue.transit,
    paymentPromotions: rexVenue.paymentMethods,
    sectors: rexVenue.sectors,
    functions: [
      {
        id: 'fn-techsummit-1',
        dateIso: '2026-11-12T09:00:00',
        formattedDate: 'Jueves 12 de Noviembre de 2026',
        doorsOpen: '08:00 hs',
        showStart: '09:00 hs',
        status: 'AVAILABLE'
      },
      {
        id: 'fn-techsummit-2',
        dateIso: '2026-11-13T09:00:00',
        formattedDate: 'Viernes 13 de Noviembre de 2026',
        doorsOpen: '08:00 hs',
        showStart: '09:00 hs',
        status: 'AVAILABLE'
      }
    ],
    availableDates: [
      'Jueves 12 de Noviembre de 2026 - 09:00 hs',
      'Viernes 13 de Noviembre de 2026 - 09:00 hs'
    ],
    ticketTypes: buildTicketTypes(rexVenue.sectors)
  },
  {
    id: 'indie-rock-sessions',
    title: 'Indie Rock Sessions 2026',
    subtitle: 'Guitarras eléctricas, sintetizadores analógicos y las bandas emergentes de la escena porteña.',
    date: '28 de Noviembre de 2026',
    dateIso: '2026-11-28T20:30:00',
    time: '20:30 HRS',
    doorsOpen: '19:00 HRS',
    venue: rexVenue.name,
    venueId: rexVenue.id,
    location: 'Buenos Aires, Argentina',
    category: 'Música & Conciertos',
    genre: 'Music & Concerts',
    dateFilter: 'next_30_days',
    price: 25000,
    minPrice: 25000,
    imageUrl: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=1200&q=80',
    badgeText: 'Formato Acústico',
    badgeType: 'secondary',
    status: 'AVAILABLE',
    description: 'Presentaciones acústicas e íntimas de las agrupaciones más representativas del nuevo rock iberoamericano en el emblemático Teatro Rex sobre la mítica calle Corrientes.',
    aiSimilarityScore: 0.91,
    lineup: 'Él Mató a un Policía Motorizado, Conociendo Rusia, Bandalos Chinos',
    venueMapUrl: '/images/venues/teatro-rex-map.svg',
    parking: rexVenue.parking,
    transit: rexVenue.transit,
    paymentPromotions: rexVenue.paymentMethods,
    sectors: rexVenue.sectors,
    functions: [
      {
        id: 'fn-indie-1',
        dateIso: '2026-11-28T20:30:00',
        formattedDate: 'Sábado 28 de Noviembre de 2026',
        doorsOpen: '19:00 hs',
        showStart: '20:30 hs',
        status: 'AVAILABLE'
      }
    ],
    availableDates: [
      'Sábado 28 de Noviembre de 2026 - 20:30 hs'
    ],
    ticketTypes: buildTicketTypes(rexVenue.sectors)
  },
  {
    id: 'river-stadium-rockfest',
    title: 'Monumental Rock Festival 2026',
    subtitle: 'Más de 80.000 personas celebrando las leyendas del rock en el estadio más grande de Sudamérica.',
    date: '05 de Diciembre de 2026',
    dateIso: '2026-12-05T16:00:00',
    time: '16:00 HRS',
    doorsOpen: '14:00 HRS',
    venue: monumentalVenue.name,
    venueId: monumentalVenue.id,
    location: 'Buenos Aires, Argentina',
    category: 'Música & Conciertos',
    genre: 'Music & Concerts',
    dateFilter: 'future',
    price: 38000,
    minPrice: 38000,
    imageUrl: 'https://images.unsplash.com/photo-1459749411175-04bf5292ceea?auto=format&fit=crop&w=1200&q=80',
    badgeText: 'Sala de Espera',
    badgeType: 'warning',
    status: 'QUEUE_ACTIVE',
    description: 'La máxima fiesta del rock nacional e internacional se reúne en el Estadio Monumental. Por alta demanda, el evento cuenta con sala de espera virtual activa con orden de llegada estricto.',
    aiSimilarityScore: 0.96,
    lineup: 'Divididos, La Renga, Ciro y los Persas, Airbag',
    venueMapUrl: '/images/venues/monumental-map.svg',
    parking: monumentalVenue.parking,
    transit: monumentalVenue.transit,
    paymentPromotions: monumentalVenue.paymentMethods,
    sectors: monumentalVenue.sectors,
    functions: [
      {
        id: 'fn-rockfest-1',
        dateIso: '2026-12-05T16:00:00',
        formattedDate: 'Sábado 05 de Diciembre de 2026',
        doorsOpen: '14:00 hs',
        showStart: '16:00 hs',
        status: 'QUEUE_ACTIVE'
      }
    ],
    availableDates: [
      'Sábado 05 de Diciembre de 2026 - 16:00 hs'
    ],
    ticketTypes: buildTicketTypes(monumentalVenue.sectors),
    maxTicketsPerUser: 6,
    salesPolicy: { maxTicketsPerUser: 6, waitingRoomEnabled: true }
  },
  {
    id: 'urban-trap-festival',
    title: 'Urban Flow: Trap & Hip-Hop Fest',
    subtitle: 'Duki, Bizarrap, Nicki Nicole y Trueno en una noche histórica de música urbana en Núñez.',
    date: '12 de Diciembre de 2026',
    dateIso: '2026-12-12T18:00:00',
    time: '18:00 HRS',
    doorsOpen: '15:00 HRS',
    venue: monumentalVenue.name,
    venueId: monumentalVenue.id,
    location: 'Buenos Aires, Argentina',
    category: 'Música & Festivales',
    genre: 'Music & Concerts',
    dateFilter: 'future',
    price: 38000,
    minPrice: 38000,
    imageUrl: 'https://images.unsplash.com/photo-1501386761578-eac5c94b800a?auto=format&fit=crop&w=1200&q=80',
    badgeText: 'Imperdible',
    badgeType: 'primary',
    status: 'AVAILABLE',
    description: 'El festival de música urbana definitivo reúne a las mayores estrellas mundiales del género nacidas en Argentina sobre el escenario masivo del Monumental.',
    aiSimilarityScore: 0.97,
    lineup: 'Duki, Bizarrap, Nicki Nicole, Trueno, Milo J',
    venueMapUrl: '/images/venues/monumental-map.svg',
    parking: monumentalVenue.parking,
    transit: monumentalVenue.transit,
    paymentPromotions: monumentalVenue.paymentMethods,
    sectors: monumentalVenue.sectors,
    functions: [
      {
        id: 'fn-trapfest-1',
        dateIso: '2026-12-12T18:00:00',
        formattedDate: 'Sábado 12 de Diciembre de 2026',
        doorsOpen: '15:00 hs',
        showStart: '18:00 hs',
        status: 'AVAILABLE'
      }
    ],
    availableDates: [
      'Sábado 12 de Diciembre de 2026 - 18:00 hs'
    ],
    ticketTypes: buildTicketTypes(monumentalVenue.sectors),
    maxTicketsPerUser: 6,
    salesPolicy: { maxTicketsPerUser: 6, waitingRoomEnabled: true }
  },
  {
    id: 'opera-clasica-verano',
    title: 'Noche Lírica: Grandes Obras Maestras',
    subtitle: 'Solistas internacionales interpretando arias inmortales de Puccini y Verdi en la acústica histórica de calle Corrientes.',
    date: '18 de Enero de 2027',
    dateIso: '2027-01-18T20:00:00',
    time: '20:00 HRS',
    doorsOpen: '18:30 HRS',
    venue: rexVenue.name,
    venueId: rexVenue.id,
    location: 'Buenos Aires, Argentina',
    category: 'Teatro & Cultura',
    genre: 'Theater',
    dateFilter: 'future',
    price: 25000,
    minPrice: 25000,
    imageUrl: 'https://images.unsplash.com/photo-1507676184212-d03ab07a01bf?auto=format&fit=crop&w=1200&q=80',
    badgeText: 'Gala Exclusiva',
    badgeType: 'secondary',
    status: 'AVAILABLE',
    description: 'Una noche de gala lírica con los tenores y sopranos más distinguidos del Teatro Colón interpretando fragmentos inmortales de Turandot, La Traviata y Carmen.',
    aiSimilarityScore: 0.85,
    lineup: 'Solistas líricos del Teatro Colón & Orquesta Estable',
    venueMapUrl: '/images/venues/teatro-rex-map.svg',
    parking: rexVenue.parking,
    transit: rexVenue.transit,
    paymentPromotions: rexVenue.paymentMethods,
    sectors: rexVenue.sectors,
    functions: [
      {
        id: 'fn-opera-1',
        dateIso: '2027-01-18T20:00:00',
        formattedDate: 'Lunes 18 de Enero de 2027',
        doorsOpen: '18:30 hs',
        showStart: '20:00 hs',
        status: 'AVAILABLE'
      }
    ],
    availableDates: [
      'Lunes 18 de Enero de 2027 - 20:00 hs'
    ],
    ticketTypes: buildTicketTypes(rexVenue.sectors)
  },
  {
    id: 'cloud-native-devcon',
    title: 'Cloud Native & Kubernetes Summit 2027',
    subtitle: 'Conferencia técnica con deep-dives en plataformas distribuidas, service mesh e infraestructura resiliente.',
    date: '25 de Enero de 2027',
    dateIso: '2027-01-25T09:00:00',
    time: '09:00 HRS',
    doorsOpen: '08:00 HRS',
    venue: arenaVenue.name,
    venueId: arenaVenue.id,
    location: 'Buenos Aires, Argentina',
    category: 'Conferencias & Tech',
    genre: 'Tech Conferences',
    dateFilter: 'future',
    price: 45000,
    minPrice: 45000,
    imageUrl: 'https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=1200&q=80',
    badgeText: 'Cupos Limitados',
    badgeType: 'secondary',
    status: 'AVAILABLE',
    description: 'Encuentro intensivo para ingenieros de confiabilidad (SRE), arquitectos de software y líderes de infraestructura con talleres prácticos y networking de alto nivel.',
    aiSimilarityScore: 0.88,
    lineup: 'Ingenieros Cloud Native Foundation, Kubernetes Contributors',
    venueMapUrl: '/images/venues/arena-buenos-aires-map.svg',
    parking: arenaVenue.parking,
    transit: arenaVenue.transit,
    paymentPromotions: arenaVenue.paymentMethods,
    sectors: arenaVenue.sectors,
    functions: [
      {
        id: 'fn-cloud-1',
        dateIso: '2027-01-25T09:00:00',
        formattedDate: 'Lunes 25 de Enero de 2027',
        doorsOpen: '08:00 hs',
        showStart: '09:00 hs',
        status: 'AVAILABLE'
      },
      {
        id: 'fn-cloud-2',
        dateIso: '2027-01-26T09:00:00',
        formattedDate: 'Martes 26 de Enero de 2027',
        doorsOpen: '08:00 hs',
        showStart: '09:00 hs',
        status: 'AVAILABLE'
      }
    ],
    availableDates: [
      'Lunes 25 de Enero de 2027 - 09:00 hs',
      'Martes 26 de Enero de 2027 - 09:00 hs'
    ],
    ticketTypes: buildTicketTypes(arenaVenue.sectors),
    maxTicketsPerUser: 4,
    salesPolicy: { maxTicketsPerUser: 4, waitingRoomEnabled: true }
  },
  {
    id: 'electronica-sunset',
    title: 'Open Air Sun & Melodic Beats',
    subtitle: 'Experiencia audiovisual inmersiva y deep progressive house con Ben Böhmer y Lane 8.',
    date: '08 de Febrero de 2027',
    dateIso: '2027-02-08T17:00:00',
    time: '17:00 HRS',
    doorsOpen: '15:30 HRS',
    venue: arenaVenue.name,
    venueId: arenaVenue.id,
    location: 'Buenos Aires, Argentina',
    category: 'Música & Conciertos',
    genre: 'Music & Concerts',
    dateFilter: 'future',
    price: 45000,
    minPrice: 45000,
    imageUrl: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=1200&q=80',
    badgeText: 'Edición Especial',
    badgeType: 'primary',
    status: 'AVAILABLE',
    description: 'La puesta de sol musical más esperada del verano porteño, fusionando paisajes sonoros orgánicos con tecnología acústica de precisión en el Arena Buenos Aires.',
    aiSimilarityScore: 0.95,
    lineup: 'Ben Böhmer, Lane 8, Nora En Pure',
    venueMapUrl: '/images/venues/arena-buenos-aires-map.svg',
    parking: arenaVenue.parking,
    transit: arenaVenue.transit,
    paymentPromotions: arenaVenue.paymentMethods,
    sectors: arenaVenue.sectors,
    functions: [
      {
        id: 'fn-sunset-1',
        dateIso: '2027-02-08T17:00:00',
        formattedDate: 'Lunes 08 de Febrero de 2027',
        doorsOpen: '15:30 hs',
        showStart: '17:00 hs',
        status: 'AVAILABLE'
      }
    ],
    availableDates: [
      'Lunes 08 de Febrero de 2027 - 17:00 hs'
    ],
    ticketTypes: buildTicketTypes(arenaVenue.sectors)
  }
]

export const MOCK_EVENTS = mockEvents

