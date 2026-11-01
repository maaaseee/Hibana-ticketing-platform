export interface BankOption {
  id: string
  name: string
  shortName: string
  icon: string
  popularPromo: string
}

export const SUPPORTED_BANKS: BankOption[] = [
  { id: 'GALICIA', name: 'Banco Galicia', shortName: 'Galicia', icon: 'account_balance', popularPromo: 'Hasta 6 cuotas sin interés' },
  { id: 'BBVA', name: 'Banco BBVA', shortName: 'BBVA', icon: 'account_balance', popularPromo: 'Hasta 6 cuotas sin interés' },
  { id: 'NACION', name: 'Banco Nación (BNA)', shortName: 'Nación', icon: 'account_balance', popularPromo: 'Hasta 12 cuotas sin interés' },
  { id: 'SANTANDER', name: 'Banco Santander', shortName: 'Santander', icon: 'account_balance', popularPromo: 'Hasta 3 cuotas sin interés' },
  { id: 'MACRO', name: 'Banco Macro', shortName: 'Macro', icon: 'account_balance', popularPromo: 'Hasta 3 cuotas sin interés' },
  { id: 'OTHER', name: 'Otros Bancos / Entidades', shortName: 'Otros', icon: 'credit_card', popularPromo: '1 cuota sin interés' },
]

export interface InstallmentOption {
  installments: number
  interestRatePercent: number
  totalAmount: number
  installmentAmount: number
  isInterestFree: boolean
  label: string
}

export function getBankInterestRate(bankId: string, installments: number): number {
  if (installments === 1) return 0

  switch (bankId.toUpperCase()) {
    case 'GALICIA':
      if (installments === 3 || installments === 6) return 0
      if (installments === 12) return 35
      return 20
    case 'BBVA':
      if (installments === 3 || installments === 6) return 0
      if (installments === 12) return 36
      return 20
    case 'NACION':
      return 0 // Promo BNA: 3, 6, 12 sin interés
    case 'SANTANDER':
      if (installments === 3) return 0
      if (installments === 6) return 18
      if (installments === 12) return 38
      return 25
    case 'MACRO':
      if (installments === 3) return 0
      if (installments === 6) return 15
      if (installments === 12) return 35
      return 22
    default:
      if (installments === 3) return 12
      if (installments === 6) return 25
      if (installments === 12) return 48
      return 30
  }
}

export function calculateInstallmentsForBank(baseAmount: number, bankId: string): InstallmentOption[] {
  const plans = [1, 3, 6, 12]

  return plans.map((n) => {
    const rate = getBankInterestRate(bankId, n)
    const surcharge = (baseAmount * rate) / 100
    const total = baseAmount + surcharge
    const perMonth = total / n
    const isFree = rate === 0

    const label = n === 1
      ? `1 pago de $ ${total.toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} (Sin interés)`
      : isFree
      ? `${n} cuotas sin interés de $ ${perMonth.toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} (Total: $ ${total.toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })})`
      : `${n} cuotas fijas de $ ${perMonth.toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} (CFT: ${rate}% • Total: $ ${total.toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })})`

    return {
      installments: n,
      interestRatePercent: rate,
      totalAmount: total,
      installmentAmount: perMonth,
      isInterestFree: isFree,
      label,
    }
  })
}
