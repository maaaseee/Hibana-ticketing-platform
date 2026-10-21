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
    parking: {
      available: true,
      title: 'Estacionamiento Oficial Tiro Federal & Ciudad Universitaria',
      description: 'Capacidad para 3.500 vehículos con control de acceso automatizado y vigilancia privada permanente durante todo el evento.',
      price: 20,
      accessRoutes: [
        'Ingreso vehicular por Av. Cantilo o Av. del Libertador',
        'Acceso peatonal directo al estadio por Puente Labruna',
        'Carriles exclusivos para ingreso ágil con código QR de estacionamiento'
      ],
      bookingPassUrl: '/parking/estadio-monumental'
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
        price: 140,
        currency: 'USD',
        capacity: 12000,
        type: 'VIP',
        description: 'Sector frontal frente al escenario, vista directa preferencial y accesos rápidos exclusivos.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-monumental-platea-baja',
        name: 'Platea San Martín / Belgrano Baja',
        price: 110,
        currency: 'USD',
        capacity: 18000,
        type: 'NUMBERED_SEATS',
        description: 'Butacas numeradas laterales con excelente ángulo de visión y elevación acústica.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-monumental-platea-alta',
        name: 'Platea San Martín / Belgrano Alta',
        price: 75,
        currency: 'USD',
        capacity: 22000,
        type: 'NUMBERED_SEATS',
        description: 'Ubicaciones numeradas en nivel superior con panorámica completa del estadio.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-monumental-campo-general',
        name: 'Campo General',
        price: 50,
        currency: 'USD',
        capacity: 16000,
        type: 'GENERAL_ADMISSION',
        description: 'Acceso de pie a campo central detrás del sector VIP con amplio espacio.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-monumental-sivori-alta',
        name: 'Tribuna Sívori / Centenario Alta',
        price: 40,
        currency: 'USD',
        capacity: 16567,
        type: 'GENERAL_ADMISSION',
        description: 'Gradas generales en cabeceras superiores con visual completa del escenario.',
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
    parking: {
      available: true,
      title: 'Estacionamiento Subterráneo Techado Arena Buenos Aires',
      description: '800 cocheras techadas con seguridad 24 horas y celdas de recarga para vehículos híbridos y eléctricos.',
      price: 15,
      accessRoutes: [
        'Ingreso vehicular exclusivo por Humboldt 450 y Juan B. Justo',
        'Conexión directa por ascensores a los niveles de Platea y Palcos',
        'Salida directa hacia Av. Corrientes y Av. Juan B. Justo'
      ],
      bookingPassUrl: '/parking/arena-buenos-aires'
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
        price: 95,
        currency: 'USD',
        capacity: 3000,
        type: 'VIP',
        description: 'Proximidad inmediata al escenario con visuales y sonido de sala inmersivos.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-arena-platea-baja',
        name: 'Platea Baja Preferencial',
        price: 85,
        currency: 'USD',
        capacity: 4500,
        type: 'NUMBERED_SEATS',
        description: 'Butacas numeradas en anillo bajo con acústica de precisión envolvente.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-arena-campo-general',
        name: 'Campo General',
        price: 65,
        currency: 'USD',
        capacity: 3500,
        type: 'GENERAL_ADMISSION',
        description: 'Ubicación de pie central con acceso fluido a barras gastronómicas.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-arena-platea-alta-central',
        name: 'Platea Alta Central',
        price: 55,
        currency: 'USD',
        capacity: 2500,
        type: 'NUMBERED_SEATS',
        description: 'Butacas numeradas en anillo superior con perspectiva central del show.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-arena-platea-alta-lateral',
        name: 'Platea Alta Lateral',
        price: 35,
        currency: 'USD',
        capacity: 1500,
        type: 'NUMBERED_SEATS',
        description: 'Ubicación numerada lateral accesible con visual completa del recinto.',
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
    parking: {
      available: true,
      title: 'Convenio Garajes Comerciales Corrientes y Sarmiento',
      description: 'Convenio preferencial con garajes comerciales a menos de 100 metros del teatro (Av. Corrientes 880 y Sarmiento 940). 20% de descuento validando tu entrada digital Amaryllis.',
      price: 10,
      accessRoutes: [
        'Ingreso por Av. Corrientes 880 (Garage Central Corrientes)',
        'Ingreso alternativo por Sarmiento 940 (Estacionamiento Rex)',
        'Acceso peatonal a 2 minutos caminando de la puerta del teatro sobre calle peatonal nocturna'
      ],
      bookingPassUrl: '/parking/teatro-rex'
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
        id: 'sec-rex-platea-platino',
        name: 'Platea Platino (Filas 1–12)',
        price: 80,
        currency: 'USD',
        capacity: 700,
        type: 'VIP',
        description: 'Ubicación premium en primeras filas de platea baja frente al proscenio.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-rex-platea-oro',
        name: 'Platea Oro (Filas 13–25)',
        price: 65,
        currency: 'USD',
        capacity: 850,
        type: 'NUMBERED_SEATS',
        description: 'Platea baja central con excelente acústica de sala teatral.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-rex-super-pullman',
        name: 'Super Pullman',
        price: 50,
        currency: 'USD',
        capacity: 650,
        type: 'NUMBERED_SEATS',
        description: 'Primer piso voladizo con barandas despejadas y acústica óptima.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-rex-pullman',
        name: 'Pullman',
        price: 38,
        currency: 'USD',
        capacity: 600,
        type: 'NUMBERED_SEATS',
        description: 'Segundo nivel del teatro con excelente campo visual panorámico.',
        status: 'AVAILABLE'
      },
      {
        id: 'sec-rex-tertulia',
        name: 'Tertulia Superior',
        price: 25,
        currency: 'USD',
        capacity: 462,
        type: 'NUMBERED_SEATS',
        description: 'Nivel superior del auditorio histórico con precio más accesible.',
        status: 'AVAILABLE'
      }
    ]
  }
]

export const MOCK_VENUES = mockVenues

export function getVenueById(id: string): VenueInfo | undefined {
  return mockVenues.find(v => v.id === id)
}

export function getVenueByName(name: string): VenueInfo | undefined {
  return mockVenues.find(v => v.name === name || v.shortName === name)
}
