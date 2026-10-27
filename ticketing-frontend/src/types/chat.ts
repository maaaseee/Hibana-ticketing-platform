export interface ChatMessage {
  id: string
  sender: 'ai' | 'user' | 'operator' | 'system'
  senderName: string
  text: string
  timestamp: string
  isInternalNote?: boolean
  quickActions?: { label: string; action: string; icon?: string }[]
}
