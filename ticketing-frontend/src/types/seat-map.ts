// Parametric Seat Map Layout Interfaces (from /seat-maps/*.json)
export type SectorShapeType = 'GRID' | 'ARC' | 'POLYGON' | 'PATH'

export interface BaseSectorGeometry {
  id: string
  name: string
  shortLabel?: string
  subLabel?: string
  type: 'VIP' | 'NUMBERED_SEATS' | 'GENERAL_ADMISSION' | 'TECHNICAL'
  shapeType: SectorShapeType
  color: string
  strokeColor: string
  capacity: number
  price?: number
  labelPosition?: [number, number]
  vendible?: boolean
  selectable?: boolean
}

export interface GridSectorLayout extends BaseSectorGeometry {
  shapeType: 'GRID'
  x: number
  y: number
  width: number
  height: number
  rows: number
  seatsPerRow: number
  rowLabels?: string[]
}

export interface ArcSectorLayout extends BaseSectorGeometry {
  shapeType: 'ARC'
  centerX: number
  centerY: number
  innerRadius: number
  outerRadius: number
  startAngleDeg: number
  endAngleDeg: number
  rows?: number
  seatsPerRow?: number
  rowLabels?: string[]
}

export interface PolygonSectorLayout extends BaseSectorGeometry {
  shapeType: 'POLYGON'
  points: [number, number][]
  labelPosition: [number, number]
  standingArea?: boolean
}

export interface PathSectorLayout extends BaseSectorGeometry {
  shapeType: 'PATH'
  pathData: string
  labelPosition: [number, number]
  standingArea?: boolean
}

export type SectorGeometry = GridSectorLayout | ArcSectorLayout | PolygonSectorLayout | PathSectorLayout

export interface StageGeometry {
  x: number
  y: number
  width: number
  height: number
  label: string
  shape: 'RECTANGLE' | 'THRUST'
}

export interface VenueSeatMapLayout {
  venueId: string
  venueName: string
  venueType: 'ESTADIO_MASIVO' | 'ARENA_CUBIERTO' | 'TEATRO_HISTORICO'
  canvasWidth: number
  canvasHeight: number
  viewBox: { minX: number; minY: number; maxX: number; maxY: number }
  stage: StageGeometry
  sectors: SectorGeometry[]
}
