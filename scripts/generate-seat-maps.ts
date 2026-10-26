import * as fs from 'fs';
import * as path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

/**
 * Common Interfaces for Parametric Venue Layouts
 */

export type SectorShapeType = 'GRID' | 'ARC' | 'POLYGON';

export interface BaseSectorLayout {
  id: string;
  name: string;
  type: 'VIP' | 'NUMBERED_SEATS' | 'GENERAL_ADMISSION';
  shapeType: SectorShapeType;
  color: string;
  strokeColor: string;
  capacity: number;
}

export interface GridSectorLayout extends BaseSectorLayout {
  shapeType: 'GRID';
  x: number;
  y: number;
  width: number;
  height: number;
  rows: number;
  seatsPerRow: number;
  rowLabels: string[];
  aislesAfterColumns?: number[]; // e.g. aisle after col 10 and 20
}

export interface ArcSectorLayout extends BaseSectorLayout {
  shapeType: 'ARC';
  centerX: number;
  centerY: number;
  innerRadius: number;
  outerRadius: number;
  startAngleDeg: number;
  endAngleDeg: number;
  rows: number;
  seatsPerRow: number;
  rowLabels: string[];
  aislesAfterColumns?: number[];
}

export interface PolygonSectorLayout extends BaseSectorLayout {
  shapeType: 'POLYGON';
  points: [number, number][]; // [ [x1, y1], [x2, y2], ... ]
  labelPosition: [number, number];
  standingArea: true;
}

export type SectorGeometry = GridSectorLayout | ArcSectorLayout | PolygonSectorLayout;

export interface StageGeometry {
  x: number;
  y: number;
  width: number;
  height: number;
  label: string;
  shape: 'RECTANGLE' | 'THRUST';
}

export interface VenueSeatMapLayout {
  venueId: string;
  venueName: string;
  venueType: 'ESTADIO_MASIVO' | 'ARENA_CUBIERTO' | 'TEATRO_HISTORICO';
  canvasWidth: number;
  canvasHeight: number;
  viewBox: { minX: number; minY: number; maxX: number; maxY: number };
  stage: StageGeometry;
  sectors: SectorGeometry[];
}

/**
 * 1. ESTADIO MONUMENTAL (River Plate - 84,567 capacity)
 * Coordinates in canvas space: 1200 x 1000
 * Center of stadium: (600, 500)
 * Stage located at top North (near Sívori / Cabecera) facing South
 */
export function generateEstadioMonumental(): VenueSeatMapLayout {
  return {
    venueId: 'estadio-monumental',
    venueName: 'Estadio Monumental (Buenos Aires)',
    venueType: 'ESTADIO_MASIVO',
    canvasWidth: 1200,
    canvasHeight: 1000,
    viewBox: { minX: 0, minY: 0, maxX: 1200, maxY: 1000 },
    stage: {
      x: 420,
      y: 70,
      width: 360,
      height: 70,
      label: 'ESCENARIO PRINCIPAL (SÍVORI SUR)',
      shape: 'RECTANGLE'
    },
    sectors: [
      // Campo VIP (Front of stage)
      {
        id: 'sec-monumental-campo-vip',
        name: 'Campo Delantero VIP',
        type: 'VIP',
        shapeType: 'POLYGON',
        color: 'rgba(234, 179, 8, 0.25)',
        strokeColor: '#eab308',
        capacity: 12000,
        standingArea: true,
        points: [
          [400, 160],
          [800, 160],
          [840, 360],
          [360, 360]
        ],
        labelPosition: [600, 260]
      },
      // Campo General (Middle to Back Field)
      {
        id: 'sec-monumental-campo-general',
        name: 'Campo General',
        type: 'GENERAL_ADMISSION',
        shapeType: 'POLYGON',
        color: 'rgba(56, 189, 248, 0.2)',
        strokeColor: '#38bdf8',
        capacity: 16000,
        standingArea: true,
        points: [
          [350, 375],
          [850, 375],
          [820, 640],
          [380, 640]
        ],
        labelPosition: [600, 500]
      },
      // Platea San Martín Baja (West lateral - left side)
      {
        id: 'sec-monumental-platea-baja-sanmartin',
        name: 'Platea San Martín Baja (Occidente)',
        type: 'NUMBERED_SEATS',
        shapeType: 'ARC',
        color: 'rgba(99, 102, 241, 0.25)',
        strokeColor: '#6366f1',
        capacity: 9000,
        centerX: 600,
        centerY: 460,
        innerRadius: 280,
        outerRadius: 360,
        startAngleDeg: 125,
        endAngleDeg: 235,
        rows: 25,
        seatsPerRow: 60,
        rowLabels: Array.from({ length: 25 }, (_, i) => `SM-B${i + 1}`),
        aislesAfterColumns: [20, 40]
      },
      // Platea Belgrano Baja (East lateral - right side)
      {
        id: 'sec-monumental-platea-baja-belgrano',
        name: 'Platea Belgrano Baja (Oriente)',
        type: 'NUMBERED_SEATS',
        shapeType: 'ARC',
        color: 'rgba(99, 102, 241, 0.25)',
        strokeColor: '#6366f1',
        capacity: 9000,
        centerX: 600,
        centerY: 460,
        innerRadius: 280,
        outerRadius: 360,
        startAngleDeg: 305,
        endAngleDeg: 415,
        rows: 25,
        seatsPerRow: 60,
        rowLabels: Array.from({ length: 25 }, (_, i) => `BEL-B${i + 1}`),
        aislesAfterColumns: [20, 40]
      },
      // Platea San Martín Alta (West lateral upper tier)
      {
        id: 'sec-monumental-platea-alta-sanmartin',
        name: 'Platea San Martín Alta',
        type: 'NUMBERED_SEATS',
        shapeType: 'ARC',
        color: 'rgba(168, 85, 247, 0.25)',
        strokeColor: '#a855f7',
        capacity: 11000,
        centerX: 600,
        centerY: 460,
        innerRadius: 380,
        outerRadius: 460,
        startAngleDeg: 125,
        endAngleDeg: 235,
        rows: 28,
        seatsPerRow: 65,
        rowLabels: Array.from({ length: 28 }, (_, i) => `SM-A${i + 1}`),
        aislesAfterColumns: [22, 44]
      },
      // Platea Belgrano Alta (East lateral upper tier)
      {
        id: 'sec-monumental-platea-alta-belgrano',
        name: 'Platea Belgrano Alta',
        type: 'NUMBERED_SEATS',
        shapeType: 'ARC',
        color: 'rgba(168, 85, 247, 0.25)',
        strokeColor: '#a855f7',
        capacity: 11000,
        centerX: 600,
        centerY: 460,
        innerRadius: 380,
        outerRadius: 460,
        startAngleDeg: 305,
        endAngleDeg: 415,
        rows: 28,
        seatsPerRow: 65,
        rowLabels: Array.from({ length: 28 }, (_, i) => `BEL-A${i + 1}`),
        aislesAfterColumns: [22, 44]
      },
      // Centenario Alta & Baja (South curve - facing stage directly)
      {
        id: 'sec-monumental-sivori-alta',
        name: 'Tribuna Centenario Sur',
        type: 'GENERAL_ADMISSION',
        shapeType: 'ARC',
        color: 'rgba(239, 68, 68, 0.22)',
        strokeColor: '#ef4444',
        capacity: 16567,
        centerX: 600,
        centerY: 460,
        innerRadius: 320,
        outerRadius: 465,
        startAngleDeg: 45,
        endAngleDeg: 135,
        rows: 35,
        seatsPerRow: 75,
        rowLabels: Array.from({ length: 35 }, (_, i) => `CEN-${i + 1}`),
        aislesAfterColumns: [25, 50]
      }
    ]
  };
}

/**
 * 2. ARENA BUENOS AIRES (Movistar Arena - 15,000 capacity)
 * Coordinates in canvas space: 1000 x 830
 */
export function generateArenaBuenosAires(): VenueSeatMapLayout {
  return {
    venueId: 'arena-buenos-aires',
    venueName: 'Arena Buenos Aires (Buenos Aires)',
    venueType: 'ARENA_CUBIERTO',
    canvasWidth: 1000,
    canvasHeight: 830,
    viewBox: { minX: 0, minY: 0, maxX: 1000, maxY: 830 },
    stage: {
      x: 330,
      y: 35,
      width: 340,
      height: 60,
      label: 'ESCENARIO PRINCIPAL',
      shape: 'RECTANGLE'
    },
    sectors: [
      {
        id: 'sec-arena-campo-vip',
        name: 'Campo VIP Frontal',
        type: 'VIP',
        shapeType: 'POLYGON',
        color: 'rgba(234, 179, 8, 0.25)',
        strokeColor: '#eab308',
        capacity: 3000,
        standingArea: true,
        points: [
          [330, 115],
          [670, 115],
          [670, 290],
          [330, 290]
        ],
        labelPosition: [500, 202]
      },
      {
        id: 'sec-arena-campo-general',
        name: 'Campo General',
        type: 'GENERAL_ADMISSION',
        shapeType: 'POLYGON',
        color: 'rgba(56, 189, 248, 0.20)',
        strokeColor: '#38bdf8',
        capacity: 3500,
        standingArea: true,
        points: [
          [330, 305],
          [670, 305],
          [670, 480],
          [330, 480]
        ],
        labelPosition: [500, 392]
      },
      {
        id: 'sec-arena-platea-baja-lateral-oeste',
        name: 'Platea Baja Oeste',
        type: 'NUMBERED_SEATS',
        shapeType: 'POLYGON',
        color: 'rgba(16, 185, 129, 0.25)',
        strokeColor: '#10b981',
        capacity: 1500,
        points: [
          [205, 115],
          [315, 115],
          [315, 480],
          [205, 480]
        ],
        labelPosition: [260, 297]
      },
      {
        id: 'sec-arena-platea-baja-lateral-este',
        name: 'Platea Baja Este',
        type: 'NUMBERED_SEATS',
        shapeType: 'POLYGON',
        color: 'rgba(16, 185, 129, 0.25)',
        strokeColor: '#10b981',
        capacity: 1500,
        points: [
          [685, 115],
          [795, 115],
          [795, 480],
          [685, 480]
        ],
        labelPosition: [740, 297]
      },
      {
        id: 'sec-arena-platea-alta-lateral-oeste',
        name: 'Platea Alta Oeste',
        type: 'NUMBERED_SEATS',
        shapeType: 'POLYGON',
        color: 'rgba(244, 63, 94, 0.22)',
        strokeColor: '#f43f5e',
        capacity: 1300,
        points: [
          [80, 115],
          [190, 115],
          [190, 615],
          [145, 615],
          [80, 520]
        ],
        labelPosition: [135, 365]
      },
      {
        id: 'sec-arena-platea-alta-lateral-este',
        name: 'Platea Alta Este',
        type: 'NUMBERED_SEATS',
        shapeType: 'POLYGON',
        color: 'rgba(244, 63, 94, 0.22)',
        strokeColor: '#f43f5e',
        capacity: 1300,
        points: [
          [810, 115],
          [920, 115],
          [920, 520],
          [855, 615],
          [810, 615]
        ],
        labelPosition: [865, 365]
      },
      {
        id: 'sec-arena-platea-baja-cabecera',
        name: 'Platea Baja Sur',
        type: 'NUMBERED_SEATS',
        shapeType: 'POLYGON',
        color: 'rgba(14, 165, 233, 0.25)',
        strokeColor: '#0ea5e9',
        capacity: 1500,
        points: [
          [205, 498],
          [795, 498],
          [795, 615],
          [205, 615]
        ],
        labelPosition: [500, 556]
      },
      {
        id: 'sec-arena-platea-alta-cabecera',
        name: 'Platea Alta Sur',
        type: 'NUMBERED_SEATS',
        shapeType: 'POLYGON',
        color: 'rgba(168, 85, 247, 0.25)',
        strokeColor: '#a855f7',
        capacity: 1400,
        points: [
          [205, 633],
          [795, 633],
          [795, 760],
          [205, 760]
        ],
        labelPosition: [500, 696]
      }
    ]
  };
}

/**
 * 3. TEATRO GRAN REX (Historic Theater - 3,262 capacity)
 * Straight/Trapezoidal tiers facing proscenium stage
 * Coordinates in canvas space: 900 x 950
 */
export function generateTeatroRex(): VenueSeatMapLayout {
  return {
    venueId: 'teatro-rex',
    venueName: 'Teatro Rex (Buenos Aires)',
    venueType: 'TEATRO_HISTORICO',
    canvasWidth: 900,
    canvasHeight: 950,
    viewBox: { minX: 0, minY: 0, maxX: 900, maxY: 950 },
    stage: {
      x: 200,
      y: 40,
      width: 500,
      height: 70,
      label: 'PROSCENIO / ESCENARIO TEATRAL',
      shape: 'RECTANGLE'
    },
    sectors: [
      // Platea Platino (Filas 1 a 12)
      {
        id: 'sec-rex-platea-platino',
        name: 'Platea Platino (Filas 1–12)',
        type: 'VIP',
        shapeType: 'GRID',
        color: 'rgba(234, 179, 8, 0.25)',
        strokeColor: '#eab308',
        capacity: 700,
        x: 175,
        y: 135,
        width: 550,
        height: 150,
        rows: 12,
        seatsPerRow: 34,
        rowLabels: ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L'],
        aislesAfterColumns: [10, 24]
      },
      // Platea Oro (Filas 13 a 25)
      {
        id: 'sec-rex-platea-oro',
        name: 'Platea Oro (Filas 13–25)',
        type: 'NUMBERED_SEATS',
        shapeType: 'GRID',
        color: 'rgba(56, 189, 248, 0.25)',
        strokeColor: '#38bdf8',
        capacity: 850,
        x: 160,
        y: 305,
        width: 580,
        height: 165,
        rows: 13,
        seatsPerRow: 36,
        rowLabels: ['M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y'],
        aislesAfterColumns: [10, 26]
      },
      // Super Pullman (Primer piso voladizo)
      {
        id: 'sec-rex-super-pullman',
        name: 'Super Pullman',
        type: 'NUMBERED_SEATS',
        shapeType: 'GRID',
        color: 'rgba(168, 85, 247, 0.25)',
        strokeColor: '#a855f7',
        capacity: 650,
        x: 140,
        y: 495,
        width: 620,
        height: 125,
        rows: 10,
        seatsPerRow: 38,
        rowLabels: ['SP-1', 'SP-2', 'SP-3', 'SP-4', 'SP-5', 'SP-6', 'SP-7', 'SP-8', 'SP-9', 'SP-10'],
        aislesAfterColumns: [12, 26]
      },
      // Pullman (Segundo nivel)
      {
        id: 'sec-rex-pullman',
        name: 'Pullman',
        type: 'NUMBERED_SEATS',
        shapeType: 'GRID',
        color: 'rgba(236, 72, 153, 0.25)',
        strokeColor: '#ec4899',
        capacity: 600,
        x: 120,
        y: 645,
        width: 660,
        height: 125,
        rows: 10,
        seatsPerRow: 40,
        rowLabels: ['P-1', 'P-2', 'P-3', 'P-4', 'P-5', 'P-6', 'P-7', 'P-8', 'P-9', 'P-10'],
        aislesAfterColumns: [12, 28]
      },
      // Tertulia Superior
      {
        id: 'sec-rex-tertulia',
        name: 'Tertulia Superior',
        type: 'NUMBERED_SEATS',
        shapeType: 'GRID',
        color: 'rgba(156, 163, 175, 0.25)',
        strokeColor: '#9ca3af',
        capacity: 462,
        x: 110,
        y: 795,
        width: 680,
        height: 100,
        rows: 8,
        seatsPerRow: 42,
        rowLabels: ['T-1', 'T-2', 'T-3', 'T-4', 'T-5', 'T-6', 'T-7', 'T-8'],
        aislesAfterColumns: [14, 28]
      }
    ]
  };
}

/**
 * Main execution runner
 */
function main() {
  const venues = [
    generateEstadioMonumental(),
    generateArenaBuenosAires(),
    generateTeatroRex()
  ];

  const targetDirs = [
    path.resolve(__dirname, '../infra/local-dev/seat-maps'),
    path.resolve(__dirname, '../ticketing-frontend/public/seat-maps'),
    path.resolve(__dirname, '../ticketing-frontend/src/seat-maps'),
    path.resolve(__dirname, '../ticketing-app-catalog/src/main/resources/seat-maps')
  ];

  targetDirs.forEach(dir => {
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }
  });

  venues.forEach(venue => {
    const filename = `${venue.venueId}.json`;
    const jsonContent = JSON.stringify(venue, null, 2);

    targetDirs.forEach(dir => {
      const filePath = path.join(dir, filename);
      fs.writeFileSync(filePath, jsonContent, 'utf-8');
      const stats = fs.statSync(filePath);
      console.log(`[GENERATED] ${filePath} (${(stats.size / 1024).toFixed(2)} KB)`);
    });
  });

  console.log('\nAll 3 venue seat-maps successfully generated with parametric optimization!');
}

main();
