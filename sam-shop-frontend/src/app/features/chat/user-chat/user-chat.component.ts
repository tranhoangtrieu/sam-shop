import { DatePipe } from '@angular/common';
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ChatMessage } from '../../../core/models/chat.model';
import { LoadingComponent } from '../../../shared/components/loading/loading.component';
import { ChatApiService, ChatWebSocketService } from '../chat.service';

@Component({
  selector: 'app-user-chat',
  standalone: true,
  imports: [FormsModule, DatePipe, LoadingComponent],
  templateUrl: './user-chat.component.html',
  styleUrl: './user-chat.component.scss'
})
export class UserChatComponent implements OnInit, OnDestroy {
  private chatApi = inject(ChatApiService);
  private chatWs = inject(ChatWebSocketService);

  loading = true;
  conversationId: number | null = null;
  messages: ChatMessage[] = [];
  draft = '';
  error = '';

  ngOnInit(): void {
    this.chatApi.myConversation().subscribe({
      next: res => {
        this.conversationId = res.data?.id ?? null;
        if (!this.conversationId) {
          this.loading = false;
          this.error = 'Không mở được hội thoại';
          return;
        }
        this.loadMessages();
        const convId = this.conversationId;
        this.chatWs.connect(
          msg => this.onWsMessage(msg),
          err => { this.error = err ?? ''; },
          () => this.chatWs.join(convId)
        );
      },
      error: err => {
        this.loading = false;
        this.error = err.error?.message || 'Lỗi tải chat';
      }
    });
  }

  ngOnDestroy(): void {
    this.chatWs.disconnect();
  }

  send(): void {
    if (!this.conversationId || !this.draft.trim()) return;
    this.chatWs.sendChat(this.conversationId, this.draft.trim());
    this.draft = '';
  }

  private loadMessages(): void {
    if (!this.conversationId) return;
    this.chatApi.getMessages(this.conversationId).subscribe({
      next: res => {
        this.messages = res.data || [];
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  private onWsMessage(msg: { type: string; payload?: ChatMessage; message?: string }): void {
    if (msg.type === 'MESSAGE' && msg.payload) {
      if (!this.messages.some(m => m.id === msg.payload!.id)) {
        this.messages = [...this.messages, msg.payload];
      }
    }
    if (msg.type === 'ERROR' && msg.message) {
      this.error = msg.message;
    }
  }
}
