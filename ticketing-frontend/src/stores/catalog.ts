import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { EventItem } from '../types'

export const useCatalogStore = defineStore('catalog', () => {
  const events = ref<EventItem[]>([
    {
      id: 'symphony-of-lights',
      title: 'Symphony of Lights 2026',
      subtitle: 'Una noche donde la orquestación clásica se fusiona con visuales inmersivos y tecnología de iluminación volumétrica.',
      date: '15 de Octubre de 2026',
      time: '19:00 HRS',
      venue: 'Movistar Arena',
      location: 'Santiago, Chile',
      category: 'Música & Conciertos',
      genre: 'Music & Concerts',
      dateFilter: 'this_weekend',
      price: 45,
      imageUrl: 'https://images.unsplash.com/photo-1465847899084-d164df4dedc6?auto=format&fit=crop&w=1200&q=80',
      badgeText: 'Confirmado',
      badgeType: 'secondary',
      status: 'AVAILABLE',
      description: 'The Symphony of Lights regresa al Movistar Arena con un despliegue audiovisual sin precedentes, sonido 360 y orquesta en vivo.',
      aiSimilarityScore: 0.94,
      lineup: 'The Lumineers, Of Monsters and Men',
      doorsOpen: '19:00 HRS',
      availableDates: ['Viernes, 15 de Octubre - 20:00', 'Sábado, 16 de Octubre - 20:00'],
      ticketTypes: [
        { name: 'General Admission ($45.00)', price: 45 },
        { name: 'VIP Pit ($85.00)', price: 85 },
        { name: 'Balcony Reserved ($60.00)', price: 60 }
      ]
    },
    {
      id: 'chronos-fest-2026',
      title: 'Neon Nights: Electronic Festival',
      subtitle: 'La convergencia de techno melódico y visuales inmersivos más grande de Iberoamérica. Más de 12 horas de música continua.',
      date: '24 de Octubre de 2026',
      time: '20:00 HRS',
      venue: 'Arena Central',
      location: 'Santiago, Chile',
      category: 'Música & Conciertos',
      genre: 'Music & Concerts',
      dateFilter: 'this_weekend',
      price: 65,
      imageUrl: 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=1200&q=80',
      badgeText: 'Últimas Entradas',
      badgeType: 'primary',
      status: 'AVAILABLE',
      description: 'Una noche con despliegue lumínico bioluminiscente de 360 grados, sistemas de audio L-Acoustics K1 y referentes del techno melódico.',
      aiSimilarityScore: 0.98,
      lineup: 'Tale of Us, Boris Brejcha, Stephan Bodzin, Mathame',
      doorsOpen: '18:30 HRS',
      availableDates: ['Sábado, 24 de Octubre - 20:00', 'Domingo, 25 de Octubre - 20:00'],
      ticketTypes: [
        { name: 'General Admission ($65.00)', price: 65 },
        { name: 'VIP Front Stage ($110.00)', price: 110 }
      ]
    },
    {
      id: 'global-tech-summit',
      title: 'Global Tech Summit 2026',
      subtitle: 'El futuro del desarrollo de software, sistemas distribuidos e inteligencia artificial con líderes de ingeniería mundial.',
      date: '12 de Noviembre de 2026',
      time: '09:00 HRS',
      venue: 'Tech Hub Arena, Grand Hall',
      location: 'Santiago, Chile',
      category: 'Conferencias Tech',
      genre: 'Tech Conferences',
      dateFilter: 'next_30_days',
      price: 120,
      imageUrl: 'https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&w=1400&q=80',
      badgeText: 'Destacado',
      badgeType: 'primary',
      status: 'AVAILABLE',
      description: 'Encuentro para arquitectos de sistemas, ingenieros principales y directores de tecnología con deep dives en arquitecturas cloud-native.',
      aiSimilarityScore: 0.96,
      lineup: 'Cloud Architects & Distributed Computing Fellows',
      doorsOpen: '08:00 HRS',
      availableDates: ['Jueves, 12 de Noviembre - 09:00', 'Viernes, 13 de Noviembre - 09:00'],
      ticketTypes: [
        { name: 'Standard Pass ($120.00)', price: 120 },
        { name: 'VIP All-Access + Workshop ($240.00)', price: 240 }
      ]
    },
    {
      id: 'design-systems-workshop',
      title: 'Design Systems Workshop',
      subtitle: 'Construcción de interfaces consistentes, escalables y tokenizadas con los más altos estándares de accesibilidad.',
      date: '18 de Noviembre de 2026',
      time: '10:00 HRS',
      venue: 'Tech Hub, Sala A',
      location: 'Buenos Aires, Argentina',
      category: 'Conferencias Tech',
      genre: 'Tech Conferences',
      dateFilter: 'next_30_days',
      price: 50,
      imageUrl: 'https://images.unsplash.com/photo-1531403009284-440f080d1e12?auto=format&fit=crop&w=1200&q=80',
      badgeText: 'Cupos Reducidos',
      badgeType: 'warning',
      status: 'AVAILABLE',
      description: 'Aprende arquitectura de tokens semánticos, diseño modular y sincronización de componentes UI entre Figma y código.',
      aiSimilarityScore: 0.88,
      lineup: 'Lead Design Engineers & UI Architects',
      doorsOpen: '09:30 HRS',
      availableDates: ['Miércoles, 18 de Noviembre - 10:00'],
      ticketTypes: [
        { name: 'Workshop Pass ($50.00)', price: 50 }
      ]
    },
    {
      id: 'championship-finals',
      title: 'Championship Finals 2026',
      subtitle: 'La gran final de la temporada competitiva internacional en vivo ante más de 40.000 espectadores.',
      date: '05 de Diciembre de 2026',
      time: '15:30 HRS',
      venue: 'Estadio Nacional',
      location: 'Santiago, Chile',
      category: 'Deportes',
      genre: 'Sports',
      dateFilter: 'future',
      price: 55,
      imageUrl: 'https://images.unsplash.com/photo-1508098682722-e99c43a406b2?auto=format&fit=crop&w=1200&q=80',
      badgeText: 'Sala de Espera',
      badgeType: 'secondary',
      status: 'QUEUE_ACTIVE',
      description: 'La fiesta del deporte y entretenimiento con pantallas perimetrales de 360 grados y transmisión satelital simultánea.',
      aiSimilarityScore: 0.91,
      lineup: 'Finalistas Oficiales & Show de Apertura',
      doorsOpen: '13:00 HRS',
      availableDates: ['Sábado, 05 de Diciembre - 15:30'],
      ticketTypes: [
        { name: 'Tribuna General ($55.00)', price: 55 },
        { name: 'Platea Baja ($95.00)', price: 95 },
        { name: 'Palco VIP ($160.00)', price: 160 }
      ]
    },
    {
      id: 'symphony-in-code',
      title: 'Symphony in Code: Música Clásica',
      subtitle: 'Obras maestras de compositores clásicos fusionadas con interpretaciones acústicas contemporáneas.',
      date: '18 de Enero de 2027',
      time: '19:00 HRS',
      venue: 'Grand Opera House',
      location: 'Buenos Aires, Argentina',
      category: 'Música & Conciertos',
      genre: 'Music & Concerts',
      dateFilter: 'future',
      price: 40,
      imageUrl: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=1200&q=80',
      badgeText: 'Confirmado',
      badgeType: 'secondary',
      status: 'AVAILABLE',
      description: 'Una velada íntima de música sinfónica en uno de los recintos acústicos más prestigiosos del continente.',
      aiSimilarityScore: 0.89,
      lineup: 'Orquesta Sinfónica Nacional & Solistas Invitados',
      doorsOpen: '18:15 HRS',
      availableDates: ['Lunes, 18 de Enero - 19:00'],
      ticketTypes: [
        { name: 'Platea Alta ($40.00)', price: 40 },
        { name: 'Platea Baja ($70.00)', price: 70 }
      ]
    },
    {
      id: 'indie-rock-sessions',
      title: 'Indie Rock Sessions 2026',
      subtitle: 'Guitarras potentes, sintetizadores vintage y las bandas emergentes más aclamadas de la escena alternativa internacional.',
      date: '28 de Noviembre de 2026',
      time: '21:00 HRS',
      venue: 'Teatro Caupolicán',
      location: 'Santiago, Chile',
      category: 'Música & Conciertos',
      genre: 'Music & Concerts',
      dateFilter: 'next_30_days',
      price: 35,
      imageUrl: 'https://images.unsplash.com/photo-1501386761578-eac5c94b800a?auto=format&fit=crop&w=1200&q=80',
      badgeText: 'Nuevo',
      badgeType: 'primary',
      status: 'AVAILABLE',
      description: 'Tres escenarios simultáneos y más de 8 horas de música independiente en una acústica inigualable.',
      aiSimilarityScore: 0.92,
      lineup: 'Arctic Echoes, The Midnight Echo, Luna Club',
      doorsOpen: '19:00 HRS',
      availableDates: ['Sábado, 28 de Noviembre - 21:00'],
      ticketTypes: [
        { name: 'Cancha General ($35.00)', price: 35 },
        { name: 'Platea Lateral ($55.00)', price: 55 }
      ]
    },
    {
      id: 'urban-trap-festival',
      title: 'Urban Flow: Trap & Hip-Hop Fest',
      subtitle: 'El epicentro del género urbano con los máximos exponentes del trap, freestyle y beats en vivo.',
      date: '12 de Diciembre de 2026',
      time: '17:00 HRS',
      venue: 'Parque Bicentenario',
      location: 'Santiago, Chile',
      category: 'Música & Festivales',
      genre: 'Music & Concerts',
      dateFilter: 'future',
      price: 50,
      imageUrl: 'https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?auto=format&fit=crop&w=1200&q=80',
      badgeText: 'Alta Demanda',
      badgeType: 'warning',
      status: 'FEW_SEATS',
      description: 'Más de 20 artistas en dos escenarios gigantescos con experiencia gastronómica y skatepark.',
      aiSimilarityScore: 0.95,
      lineup: 'Duki, Bizarrap, Nicki Nicole, Trueno',
      doorsOpen: '15:00 HRS',
      availableDates: ['Sábado, 12 de Diciembre - 17:00'],
      ticketTypes: [
        { name: 'Campo General ($50.00)', price: 50 },
        { name: 'Campo VIP Fan ($90.00)', price: 90 }
      ]
    },
    {
      id: 'cloud-native-devcon',
      title: 'Cloud Native & Kubernetes Summit 2027',
      subtitle: 'Conferencia técnica sobre microservicios, service mesh, edge computing y plataformas distribuidas de alta escala.',
      date: '25 de Enero de 2027',
      time: '09:00 HRS',
      venue: 'Centro de Convenciones Titanium',
      location: 'Santiago, Chile',
      category: 'Conferencias Tech',
      genre: 'Tech Conferences',
      dateFilter: 'future',
      price: 90,
      imageUrl: 'https://images.unsplash.com/photo-1515187029135-18ee286d815b?auto=format&fit=crop&w=1200&q=80',
      badgeText: 'Nuevo',
      badgeType: 'secondary',
      status: 'AVAILABLE',
      description: 'Workshops prácticos, laboratorios hands-on con Cloud Architects y networking con líderes de la industria.',
      aiSimilarityScore: 0.91,
      lineup: 'CNCF Ambassadors & Cloud Native Architects',
      doorsOpen: '08:00 HRS',
      availableDates: ['Lunes, 25 de Enero - 09:00', 'Martes, 26 de Enero - 09:00'],
      ticketTypes: [
        { name: 'Pase General Conferencia ($90.00)', price: 90 },
        { name: 'Pase VIP + Certificación ($180.00)', price: 180 }
      ]
    },
    {
      id: 'electronica-open-air',
      title: 'Open Air Sun & Beats Festival',
      subtitle: 'Atardecer al aire libre con música electrónica melódica, ambient y visuales generativos con IA.',
      date: '08 de Febrero de 2027',
      time: '16:00 HRS',
      venue: 'Espacio Riesco',
      location: 'Santiago, Chile',
      category: 'Música & Festivales',
      genre: 'Music & Concerts',
      dateFilter: 'future',
      price: 42,
      imageUrl: 'https://images.unsplash.com/photo-1492684223066-81342ee5ff30?auto=format&fit=crop&w=1200&q=80',
      badgeText: 'Preventa 1',
      badgeType: 'primary',
      status: 'AVAILABLE',
      description: 'Disfruta de una experiencia sensorial única con gastronomía de autor y atardecer musical.',
      aiSimilarityScore: 0.93,
      lineup: 'Ben Böhmer, Lane 8, Nora En Pure',
      doorsOpen: '15:30 HRS',
      availableDates: ['Domingo, 08 de Febrero - 16:00'],
      ticketTypes: [
        { name: 'Early Bird General ($42.00)', price: 42 },
        { name: 'VIP Lounge Open Air ($85.00)', price: 85 }
      ]
    }
  ])

  const searchQuery = ref<string>('')

  // Featured Slides for Carousel
  const featuredEvents = computed(() => {
    return [
      events.value.find(e => e.id === 'chronos-fest-2026'),
      events.value.find(e => e.id === 'global-tech-summit'),
      events.value.find(e => e.id === 'symphony-of-lights'),
    ].filter(Boolean) as EventItem[]
  })

  // 1. Shows agregados recientemente (Ordenados por fecha de publicación/novedad)
  const recentlyAddedEvents = computed(() => {
    const recentIds = [
      'electronica-open-air',
      'cloud-native-devcon',
      'urban-trap-festival',
      'indie-rock-sessions',
      'global-tech-summit',
      'design-systems-workshop',
      'symphony-in-code',
      'championship-finals',
      'chronos-fest-2026',
      'symphony-of-lights'
    ]
    return recentIds
      .map(id => events.value.find(e => e.id === id))
      .filter(Boolean) as EventItem[]
  })

  // 2. Shows próximos a ocurrir (Orden cronológico más cercano)
  const upcomingEvents = computed(() => {
    const upcomingIds = [
      'symphony-of-lights',      // 15 Oct 2026
      'chronos-fest-2026',        // 24 Oct 2026
      'global-tech-summit',       // 12 Nov 2026
      'design-systems-workshop',  // 18 Nov 2026
      'indie-rock-sessions',      // 28 Nov 2026
      'championship-finals',      // 05 Dic 2026
      'urban-trap-festival',      // 12 Dic 2026
      'symphony-in-code',         // 18 Ene 2027
      'cloud-native-devcon',      // 25 Ene 2027
      'electronica-open-air'       // 08 Feb 2027
    ]
    return upcomingIds
      .map(id => events.value.find(e => e.id === id))
      .filter(Boolean) as EventItem[]
  })

  function getEventById(id: string): EventItem | undefined {
    return events.value.find(e => e.id === id)
  }

  return {
    events,
    searchQuery,
    featuredEvents,
    recentlyAddedEvents,
    upcomingEvents,
    getEventById,
  }
})
