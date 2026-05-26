import { DatePipe } from '@angular/common';
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ChatConversation, ChatMessage } from '../../../core/models/chat.model';
import { LoadingComponent } from '../../../shared/components/loading/loading.component';
import { ChatApiService, ChatWebSocketService } from '../chat.service';

@Component({
  selector: 'app-staff-chat',
  standalone: true,
  imports: [FormsModule, DatePipe, LoadingComponent],
  templateUrl: './staff-chat.component.html',
  styleUrl: './staff-chat.component.scss'
})
export class StaffChatComponent implements OnInit, OnDestroy {
  private chatApi = inject(ChatApiService);
  private chatWs = inject(ChatWebSocketService);

  loading = true;
  conversations: ChatConversation[] = [];
  selectedId: number | null = null;
  messages: ChatMessage[] = [];
  draft = '';
  error = '';

  ngOnInit(): void {
    this.chatWs.connect(
      msg => this.onWsMessage(msg),
      err => { this.error = err ?? ''; },
      () => {
        if (this.selectedId) {
          this.chatWs.join(this.selectedId);
        }
      }
    );
    this.loadInbox();
  }

  ngOnDestroy(): void {
    this.chatWs.disconnect();
  }

  selectConversation(c: ChatConversation): void {
    this.selectedId = c.id;
    this.messages = [];
    this.loading = true;
    this.error = '';
    this.chatWs.join(c.id);
    this.chatApi.getMessages(c.id).subscribe({
      next: res => {
        this.messages = res.data || [];
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  send(): void {
    if (!this.selectedId || !this.draft.trim()) return;
    this.chatWs.sendChat(this.selectedId, this.draft.trim());
    this.draft = '';
  }

  private loadInbox(): void {
    this.chatApi.listConversations().subscribe({
      next: res => {
        this.conversations = res.data || [];
        this.loading = false;
        if (this.conversations.length && !this.selectedId) {
          this.selectConversation(this.conversations[0]);
        }
      },
      error: err => {
        this.loading = false;
        this.error = err.error?.message || 'Lỗi tải hội thoại';
      }
    });
  }

  private onWsMessage(msg: { type: string; payload?: ChatMessage; message?: string }): void {
    if (msg.type === 'MESSAGE' && msg.payload) {
      const p = msg.payload;
      if (p.conversationId === this.selectedId && !this.messages.some(m => m.id === p.id)) {
        this.messages = [...this.messages, p];
      }
      this.conversations = this.conversations.map(c =>
        c.id === p.conversationId
          ? { ...c, lastMessagePreview: p.content, updatedAt: p.sentAt }
          : c
      );
    }
    if (msg.type === 'ERROR' && msg.message) {
      this.error = msg.message;
    }
  }
}
