import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiService } from '../../core/services/api.service';
import { TokenService } from '../../core/services/token.service';
import { ChatConversation, ChatMessage, WsServerMessage } from '../../core/models/chat.model';

@Injectable({ providedIn: 'root' })
export class ChatApiService {
  private api = inject(ApiService);

  myConversation() {
    return this.api.get<ChatConversation>('/api/chat/conversations/me');
  }

  listConversations() {
    return this.api.get<ChatConversation[]>('/api/chat/conversations');
  }

  getMessages(conversationId: number) {
    return this.api.get<ChatMessage[]>(`/api/chat/conversations/${conversationId}/messages`, {
      page: 0,
      size: 100
    });
  }

  sendMessage(conversationId: number, content: string) {
    return this.api.post<ChatMessage>(`/api/chat/conversations/${conversationId}/messages`, { content });
  }
}

@Injectable({ providedIn: 'root' })
export class ChatWebSocketService {
  private tokenService = inject(TokenService);
  private socket: WebSocket | null = null;
  private pending: object[] = [];

  connect(
    onMessage: (msg: WsServerMessage) => void,
    onError?: (err: string) => void,
    onOpen?: () => void
  ): void {
    this.disconnect();
    const token = this.tokenService.getToken();
    if (!token) {
      onError?.('Chưa đăng nhập');
      return;
    }
    if (this.isTokenExpired(token)) {
      this.tokenService.clear();
      onError?.('Phiên đăng nhập hết hạn — vui lòng đăng nhập lại');
      return;
    }
    const wsBase = this.resolveWsBase();
    const url = `${wsBase}/api/chat/ws`;
    this.pending = [];
    this.socket = new WebSocket(url, [token]);
    this.socket.onopen = () => {
      this.flushPending();
      onOpen?.();
    };
    this.socket.onmessage = event => {
      try {
        onMessage(JSON.parse(event.data) as WsServerMessage);
      } catch {
        onError?.('Invalid message from server');
      }
    };
    this.socket.onerror = () => onError?.('WebSocket lỗi kết nối — kiểm tra chat-service đang chạy');
    this.socket.onclose = ev => {
      if (ev.code !== 1000 && ev.code !== 1001) {
        onError?.('Mất kết nối chat — thử đăng nhập lại hoặc tải lại trang');
      }
    };
  }

  join(conversationId: number): void {
    this.send({ type: 'JOIN', conversationId });
  }

  sendChat(conversationId: number, content: string): void {
    this.send({ type: 'SEND', conversationId, content });
  }

  disconnect(): void {
    this.socket?.close();
    this.socket = null;
    this.pending = [];
  }

  private send(body: object): void {
    if (this.socket?.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify(body));
    } else if (this.socket) {
      this.pending.push(body);
    }
  }

  private flushPending(): void {
    if (this.socket?.readyState !== WebSocket.OPEN) {
      return;
    }
    for (const body of this.pending) {
      this.socket.send(JSON.stringify(body));
    }
    this.pending = [];
  }

  private isTokenExpired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const exp = payload.exp as number | undefined;
      return !exp || Date.now() >= exp * 1000;
    } catch {
      return true;
    }
  }

  private resolveWsBase(): string {
    const configured = (environment as { chatWsUrl?: string }).chatWsUrl;
    if (configured) {
      return configured.replace(/\/$/, '');
    }
    if (!environment.apiUrl) {
      const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      return `${proto}//${window.location.host}`;
    }
    return environment.apiUrl.replace(/^http/, 'ws');
  }
}
