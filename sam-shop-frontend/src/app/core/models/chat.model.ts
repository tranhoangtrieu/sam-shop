export interface ChatConversation {
  id: number;
  customerUserId: number;
  customerUsername: string;
  status: string;
  createdAt?: string;
  updatedAt?: string;
  lastMessagePreview?: string;
}

export interface ChatMessage {
  id: number;
  conversationId: number;
  senderUserId: number;
  senderUsername: string;
  senderRole: string;
  content: string;
  sentAt: string;
}

export interface WsServerMessage {
  type: string;
  payload?: ChatMessage;
  message?: string;
}
