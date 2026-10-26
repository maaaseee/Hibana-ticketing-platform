import type { VenueInfo } from '../types'

export const mockVenues: VenueInfo[] = [
  {
    id: 'estadio-monumental',
    name: 'Estadio Monumental (Buenos Aires)',
    shortName: 'Estadio Monumental',
    city: 'Buenos Aires',
    country: 'Argentina',
    address: 'Av. Figueroa Alcorta 7597, Núñez, CABA',
    capacity: 84567,
    type: 'ESTADIO_MASIVO',
    imageUrl: 'https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?auto=format&fit=crop&w=1200&q=80',
    parking: {
      available: true,
      title: 'Estacionamiento Oficial Tiro Federal & Ciudad Universitaria',
      description: 'Capacidad para 3.500 vehículos con control de acceso automatizado y vigilancia privada permanente durante todo el evento.',
      price: 20000,
      accessRoutes: [
        'Ingreso vehicular por Av. Cantilo o Av. del Libertador',
        'Acceso peatonal directo al estadio por Puente Labruna',
        'Carriles exclusivos para ingreso ágil con código QR de estacionamiento'
      ],
      bookingPassUrl: '/parking/estadio-monumental'
    },
    transit: {
      busLines: ['15', '28', '29', '42', '107', '130'],
      trains: [
        'FFCC Belgrano Norte (Estación Ciudad Universitaria - a 300m)',
        'FFCC Mitre (Estación Núñez o Belgrano C)'
      ],
      subway: [
        'Subte Línea D (Estación Congreso de Tucumán - combinación con colectivos 29 y 130)'
      ],
      googleMapsUrl: 'https://maps.google.com/?q=Estadio+Monumental+River+Plate+Buenos+Aires',
      embedQuery: 'Estadio Monumental, Av. Figueroa Alcorta 7597, Buenos Aires'
    },
    paymentMethods: [
      {
        id: 'pm-santander-monumental',
        bank: 'Santander',
        card: 'Visa / Mastercard Santander',
        installments: 'Hasta 6 cuotas sin interés',
        description: '6 cuotas sin interés en todas las ubicaciones con tarjetas Santander American Express, Visa y Mastercard.',
        badgeText: '6 cuotas sin interés'
      },
      {
        id: 'pm-galicia-monumental',
        bank: 'Galicia',
        card: 'Visa / Mastercard Galicia',
        installments: 'Hasta 3 cuotas sin interés',
        description: '3 cuotas sin interés abonando con tarjetas Galicia Visa o Mastercard.',
        badgeText: '3 cuotas sin interés'
      },
      {
        id: 'pm-modo-monumental',
        bank: 'MODO',
        card: 'Tarjetas vinculadas en app MODO',
        installments: '1 pago con reintegro',
        description: '15% de reintegro los días viernes pagando con código QR desde MODO o app bancaria.',
        badgeText: '15% de reintegro'
      }
    ],
    sectors: [
      {
        id: 'sec-monumental-campo-vip',
        name: 'Campo Delantero VIP',
        price: 140000,
        currency: 'ARS',
        capacity: 12000,
        type: 'VIP',
        description: 'Sector frontal frente al escenario, vista directa preferencial y accesos rápidos exclusivos.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-monumental-campo-general',
        name: 'Campo General',
        price: 50000,
        currency: 'ARS',
        capacity: 16000,
        type: 'GENERAL_ADMISSION',
        description: 'Acceso de pie a campo central detrás del sector VIP con amplio espacio.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-monumental-platea-baja-sanmartin-norte',
        name: 'Platea San Martín Baja (Visión Reducida)',
        price: 75000,
        currency: 'ARS',
        capacity: 2500,
        type: 'NUMBERED_SEATS',
        description: 'Ubicación numerada en lateral oeste bajo sector norte con ángulo sesgado hacia el escenario Sívori.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-monumental-platea-baja-sanmartin',
        name: 'Platea San Martín Baja',
        price: 110000,
        currency: 'ARS',
        capacity: 6500,
        type: 'NUMBERED_SEATS',
        description: 'Butacas numeradas en lateral oeste bajo con excelente ángulo de visión y elevación acústica.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-monumental-platea-alta-sanmartin-norte',
        name: 'Platea San Martín Alta (Visión Reducida)',
        price: 50000,
        currency: 'ARS',
        capacity: 3000,
        type: 'NUMBERED_SEATS',
        description: 'Ubicación alta en lateral oeste tramo norte a precio preferencial por ángulo ciego.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-monumental-platea-alta-sanmartin',
        name: 'Platea San Martín Alta',
        price: 75000,
        currency: 'ARS',
        capacity: 8000,
        type: 'NUMBERED_SEATS',
        description: 'Ubicaciones numeradas en lateral oeste alto con panorámica completa del estadio.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-monumental-platea-baja-belgrano-norte',
        name: 'Platea Belgrano Baja (Visión Reducida)',
        price: 75000,
        currency: 'ARS',
        capacity: 2500,
        type: 'NUMBERED_SEATS',
        description: 'Ubicación numerada en lateral este bajo sector norte con ángulo ciego relativo al escenario.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-monumental-platea-baja-belgrano',
        name: 'Platea Belgrano Baja',
        price: 110000,
        currency: 'ARS',
        capacity: 6500,
        type: 'NUMBERED_SEATS',
        description: 'Butacas numeradas en lateral este bajo con cercanía y visibilidad óptima.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-monumental-platea-alta-belgrano-norte',
        name: 'Platea Belgrano Alta (Visión Reducida)',
        price: 50000,
        currency: 'ARS',
        capacity: 3000,
        type: 'NUMBERED_SEATS',
        description: 'Ubicación alta en lateral este tramo norte a precio promocional por visión reducida.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-monumental-platea-alta-belgrano',
        name: 'Platea Belgrano Alta',
        price: 75000,
        currency: 'ARS',
        capacity: 8000,
        type: 'NUMBERED_SEATS',
        description: 'Ubicaciones numeradas en lateral este alto con vista amplia del espectáculo.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-monumental-centenario-baja',
        name: 'Tribuna Centenario Baja',
        price: 45000,
        currency: 'ARS',
        capacity: 8000,
        type: 'GENERAL_ADMISSION',
        description: 'Ubicación en cabecera sur baja frente directo al escenario monumental.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-monumental-centenario-alta',
        name: 'Tribuna Centenario Alta',
        price: 38000,
        currency: 'ARS',
        capacity: 8567,
        type: 'GENERAL_ADMISSION',
        description: 'Gradas generales en cabecera sur alta con perspectiva panorámica de todo el estadio.',
        status: 'AVAILABLE'
      }
    ]
  },
  {
    id: 'arena-buenos-aires',
    name: 'Arena Buenos Aires (Buenos Aires)',
    shortName: 'Arena Buenos Aires',
    city: 'Buenos Aires',
    country: 'Argentina',
    address: 'Humboldt 450, Villa Crespo, CABA',
    capacity: 15000,
    type: 'ARENA_CUBIERTO',
    imageUrl: '/images/venues/arena_buenos_aires.jpg',
    parking: {
      available: true,
      title: 'Estacionamiento Subterráneo Techado Arena Buenos Aires',
      description: '800 cocheras techadas con seguridad 24 horas y celdas de recarga para vehículos híbridos y eléctricos.',
      price: 15000,
      accessRoutes: [
        'Ingreso vehicular exclusivo por Humboldt 450 y Juan B. Justo',
        'Conexión directa por ascensores a los niveles de Platea y Palcos',
        'Salida directa hacia Av. Corrientes y Av. Juan B. Justo'
      ],
      bookingPassUrl: '/parking/arena-buenos-aires'
    },
    transit: {
      busLines: ['34', '42', '55', '65', '71', '76', '109', '112', '166'],
      trains: [
        'FFCC San Martín (Estación Villa Crespo - a 100m)'
      ],
      subway: [
        'Subte Línea B (Estación Dorrego - a 250m)'
      ],
      googleMapsUrl: 'https://maps.google.com/?q=Movistar+Arena+Humboldt+450+Buenos+Aires',
      embedQuery: 'Humboldt 450, Villa Crespo, Buenos Aires'
    },
    paymentMethods: [
      {
        id: 'pm-santander-arena',
        bank: 'Santander',
        card: 'Santander American Express / Visa',
        installments: 'Hasta 6 cuotas sin interés',
        description: 'Exclusivo 6 cuotas sin interés con todas las tarjetas Santander.',
        badgeText: '6 cuotas sin interés'
      },
      {
        id: 'pm-bbva-arena',
        bank: 'BBVA',
        card: 'BBVA Visa / Mastercard',
        installments: 'Hasta 3 cuotas sin interés',
        description: '3 cuotas sin interés con tarjetas de crédito BBVA.',
        badgeText: '3 cuotas sin interés'
      },
      {
        id: 'pm-mercadopago-arena',
        bank: 'Mercado Pago',
        card: 'Dinero en cuenta y tarjetas de débito',
        installments: '1 pago al contado',
        description: 'Acreditación instantánea de tickets abonando con saldo en cuenta o tarjeta de débito.',
        badgeText: 'Débito y saldo'
      }
    ],
    sectors: [
      {
        id: 'sec-arena-campo-vip',
        name: 'Campo VIP Frontal',
        price: 95000,
        currency: 'ARS',
        capacity: 3000,
        type: 'VIP',
        description: 'Proximidad inmediata al escenario con visuales y sonido de sala inmersivos.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-arena-campo-general',
        name: 'Campo General',
        price: 65000,
        currency: 'ARS',
        capacity: 3500,
        type: 'GENERAL_ADMISSION',
        description: 'Ubicación de pie central con acceso fluido a barras gastronómicas.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-arena-platea-baja-lateral-oeste',
        name: 'Platea Baja Lateral Oeste',
        price: 85000,
        currency: 'ARS',
        capacity: 1500,
        type: 'NUMBERED_SEATS',
        description: 'Butacas numeradas en anillo bajo lateral oeste con acústica envolvente.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-arena-platea-baja-lateral-este',
        name: 'Platea Baja Lateral Este',
        price: 85000,
        currency: 'ARS',
        capacity: 1500,
        type: 'NUMBERED_SEATS',
        description: 'Butacas numeradas en anillo bajo lateral este con vista privilegiada del show.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-arena-platea-baja-cabecera',
        name: 'Platea Baja Cabecera Sur',
        price: 80000,
        currency: 'ARS',
        capacity: 1500,
        type: 'NUMBERED_SEATS',
        description: 'Butacas numeradas en anillo bajo frontal con visual directa hacia el escenario.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-arena-platea-alta-lateral-oeste',
        name: 'Platea Alta Lateral Oeste',
        price: 50000,
        currency: 'ARS',
        capacity: 1300,
        type: 'NUMBERED_SEATS',
        description: 'Ubicación numerada en nivel alto lateral oeste con panorámica completa del recinto.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-arena-platea-alta-lateral-este',
        name: 'Platea Alta Lateral Este',
        price: 50000,
        currency: 'ARS',
        capacity: 1300,
        type: 'NUMBERED_SEATS',
        description: 'Ubicación numerada en nivel alto lateral este con visual de 360 grados.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-arena-platea-alta-cabecera',
        name: 'Platea Alta Cabecera Sur',
        price: 45000,
        currency: 'ARS',
        capacity: 1400,
        type: 'NUMBERED_SEATS',
        description: 'Ubicación numerada en nivel alto frente directo al escenario.',
        status: 'AVAILABLE'
      }
    ]
  },
  {
    id: 'teatro-rex',
    name: 'Teatro Rex (Buenos Aires)',
    shortName: 'Teatro Rex',
    city: 'Buenos Aires',
    country: 'Argentina',
    address: 'Av. Corrientes 857, San Nicolás, CABA',
    capacity: 3262,
    type: 'TEATRO_HISTORICO',
    imageUrl: 'https://images.unsplash.com/photo-1507676184212-d03ab07a01bf?auto=format&fit=crop&w=1200&q=80',
    parking: {
      available: true,
      title: 'Convenio Garajes Comerciales Corrientes y Sarmiento',
      description: 'Convenio preferencial con garajes comerciales a menos de 100 metros del teatro (Av. Corrientes 880 y Sarmiento 940). 20% de descuento validando tu entrada digital Amaryllis.',
      price: 10000,
      accessRoutes: [
        'Ingreso por Av. Corrientes 880 (Garage Central Corrientes)',
        'Ingreso alternativo por Sarmiento 940 (Estacionamiento Rex)',
        'Acceso peatonal a 2 minutos caminando de la puerta del teatro sobre calle peatonal nocturna'
      ],
      bookingPassUrl: '/parking/teatro-rex'
    },
    transit: {
      busLines: ['5', '6', '7', '9', '24', '26', '50', '111', '115', '140', '180'],
      trains: [
        'Conexión directa vía Subte con terminales Retiro, Constitución y Once'
      ],
      subway: [
        'Subte Línea B (Estación Carlos Pellegrini - a 50m)',
        'Subte Línea D (Estación 9 de Julio - a 80m)',
        'Subte Línea C (Estación Diagonal Norte - a 100m)'
      ],
      googleMapsUrl: 'https://maps.google.com/?q=Teatro+Gran+Rex+Av+Corrientes+857+Buenos+Aires',
      embedQuery: 'Av. Corrientes 857, San Nicolás, Buenos Aires'
    },
    paymentMethods: [
      {
        id: 'pm-bancociudad-rex',
        bank: 'Banco Ciudad',
        card: 'Visa / Mastercard Banco Ciudad',
        installments: 'Hasta 3 cuotas sin interés',
        description: '3 cuotas sin interés en funciones teatrales y recitales acústicos.',
        badgeText: '3 cuotas sin interés'
      },
      {
        id: 'pm-santander-rex',
        bank: 'Santander',
        card: 'Visa / Mastercard Santander',
        installments: 'Hasta 3 cuotas sin interés',
        description: '3 cuotas sin interés para clientes Santander.',
        badgeText: '3 cuotas sin interés'
      },
      {
        id: 'pm-cuentadni-rex',
        bank: 'Cuenta DNI',
        card: 'Billetera digital Cuenta DNI / MODO',
        installments: '1 pago con reintegro',
        description: 'Beneficios bancarios y reintegros semanales en espectáculos de artes escénicas.',
        badgeText: 'Beneficios semanales'
      }
    ],
    sectors: [
      {
        id: 'sec-rex-palcos-izq',
        name: 'Palcos Bajos Izquierda',
        price: 90000,
        currency: 'ARS',
        capacity: 100,
        type: 'VIP',
        description: 'Exclusivos palcos privados en el lateral izquierdo con atención preferencial y vista directa.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-rex-platea-platino',
        name: 'Platea Platino (Filas 1–12)',
        price: 80000,
        currency: 'ARS',
        capacity: 700,
        type: 'VIP',
        description: 'Ubicación premium en primeras filas de platea baja frente al proscenio.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-rex-platea-oro',
        name: 'Platea Oro (Filas 13–25)',
        price: 65000,
        currency: 'ARS',
        capacity: 850,
        type: 'NUMBERED_SEATS',
        description: 'Platea baja central con excelente acústica de sala teatral.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-rex-palcos-der',
        name: 'Palcos Bajos Derecha',
        price: 90000,
        currency: 'ARS',
        capacity: 100,
        type: 'VIP',
        description: 'Exclusivos palcos privados en el lateral derecho con vista panorámica del proscenio.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-rex-super-pullman',
        name: 'Super Pullman (Primer Piso)',
        price: 50000,
        currency: 'ARS',
        capacity: 650,
        type: 'NUMBERED_SEATS',
        description: 'Primer piso voladizo con barandas despejadas y acústica óptima.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-rex-pullman',
        name: 'Pullman (Segundo Nivel)',
        price: 38000,
        currency: 'ARS',
        capacity: 600,
        type: 'NUMBERED_SEATS',
        description: 'Segundo nivel del teatro con excelente campo visual panorámico.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-rex-tertulia',
        name: 'Tertulia Superior',
        price: 25000,
        currency: 'ARS',
        capacity: 262,
        type: 'NUMBERED_SEATS',
        description: 'Nivel superior del auditorio histórico con precio más accesible.',
        status: 'AVAILABLE'
      }
    ]
  }
]

export const MOCK_VENUES = mockVenues

export function getVenueById(id?: string): VenueInfo | undefined {
  if (!id) return undefined
  const direct = mockVenues.find(v => v.id === id || id.includes(v.id) || v.id.includes(id))
  if (direct) return direct
  if (id.includes('nacional')) {
    return {
      id: 'ven_estadio_nacional',
      name: 'Estadio Nacional',
      shortName: 'Estadio Nacional',
      city: 'Buenos Aires',
      country: 'Argentina',
      address: 'Av. Libertador y Parque Nacional',
      capacity: 60000,
      type: 'ESTADIO_MASIVO',
      parking: mockVenues[0]!.parking,
      transit: mockVenues[0]!.transit,
      paymentMethods: mockVenues[0]!.paymentMethods,
      sectors: mockVenues[0]!.sectors
    }
  }
  return undefined
}

export function getVenueByName(name?: string): VenueInfo | undefined {
  if (!name) return undefined
  const lower = name.toLowerCase()
  const direct = mockVenues.find(v =>
    v.name.toLowerCase() === lower ||
    v.shortName.toLowerCase() === lower ||
    v.name.toLowerCase().includes(lower) ||
    lower.includes(v.shortName.toLowerCase()) ||
    lower.includes(v.id.toLowerCase())
  )
  if (direct) return direct
  if (lower.includes('nacional')) {
    return getVenueById('ven_estadio_nacional')
  }
  return undefined
}

