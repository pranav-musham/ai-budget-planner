import { Component, OnInit, ElementRef, ViewChild, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ChatService, ChatMessage } from '../../services/chat.service';

@Component({
  selector: 'app-ai-chat',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule
  ],
  templateUrl: './ai-chat.component.html',
  styleUrls: ['./ai-chat.component.css']
})
export class AiChatComponent implements OnInit, AfterViewChecked {
  @ViewChild('chatContainer') private chatContainer!: ElementRef;

  isOpen = false;
  messages: Array<{ type: 'user' | 'ai', content: string, timestamp: Date }> = [];
  userInput = '';
  isLoading = false;
  shouldScroll = false;
  historyLoaded = false;

  suggestedQuestions = [
    'How much did I spend this month?',
    'What\'s my top spending category?',
    'Tips to save money',
    'Show my spending this week'
  ];

  constructor(
    private chatService: ChatService,
    private snackBar: MatSnackBar
  ) { }

  ngOnInit(): void { }

  toggleChat(): void {
    this.isOpen = !this.isOpen;
    if (this.isOpen && !this.historyLoaded) {
      this.loadChatHistory();
      this.historyLoaded = true;
    }
    if (this.isOpen) {
      this.shouldScroll = true;
    }
  }

  ngAfterViewChecked(): void {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  /**
   * Load chat history from backend
   */
  loadChatHistory(): void {
    this.chatService.getChatHistory(50).subscribe({
      next: (history: ChatMessage[]) => {
        this.messages = history.flatMap(msg => [
          {
            type: 'user' as const,
            content: msg.message,
            timestamp: new Date(msg.timestamp)
          },
          {
            type: 'ai' as const,
            content: msg.response,
            timestamp: new Date(msg.timestamp)
          }
        ]);
        this.shouldScroll = true;
      },
      error: (error) => {
        console.error('Failed to load chat history:', error);
      }
    });
  }

  /**
   * Send a message
   */
  sendMessage(): void {
    if (!this.userInput.trim() || this.isLoading) {
      return;
    }

    const messageText = this.userInput.trim();
    this.userInput = '';

    // Add user message to chat
    this.messages.push({
      type: 'user',
      content: messageText,
      timestamp: new Date()
    });
    this.shouldScroll = true;
    this.isLoading = true;

    // Send to backend
    this.chatService.sendMessage(messageText).subscribe({
      next: (response: ChatMessage) => {
        // Add AI response to chat
        this.messages.push({
          type: 'ai',
          content: response.response,
          timestamp: new Date(response.timestamp)
        });
        this.isLoading = false;
        this.shouldScroll = true;
      },
      error: (error) => {
        console.error('Failed to send message:', error);
        this.messages.push({
          type: 'ai',
          content: 'Sorry, I encountered an error processing your request. Please try again.',
          timestamp: new Date()
        });
        this.isLoading = false;
        this.shouldScroll = true;
        this.snackBar.open('Failed to send message', 'Close', { duration: 3000 });
      }
    });
  }

  /**
   * Send a suggested question
   */
  sendSuggestedQuestion(question: string): void {
    this.userInput = question;
    this.sendMessage();
  }

  /**
   * Clear chat history
   */
  clearHistory(): void {
    if (!confirm('Are you sure you want to clear your chat history? This cannot be undone.')) {
      return;
    }

    this.chatService.clearChatHistory().subscribe({
      next: () => {
        this.messages = [];
        this.snackBar.open('Chat history cleared', 'Close', { duration: 3000 });
      },
      error: (error) => {
        console.error('Failed to clear history:', error);
        this.snackBar.open('Failed to clear history', 'Close', { duration: 3000 });
      }
    });
  }

  /**
   * Handle Enter key press
   */
  onEnterPress(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  /**
   * Scroll to bottom of chat
   */
  private scrollToBottom(): void {
    try {
      if (this.chatContainer) {
        this.chatContainer.nativeElement.scrollTop = this.chatContainer.nativeElement.scrollHeight;
      }
    } catch (err) {
      console.error('Scroll error:', err);
    }
  }

  /**
   * Format timestamp
   */
  formatTime(date: Date): string {
    return date.toLocaleTimeString('en-US', {
      hour: 'numeric',
      minute: '2-digit',
      hour12: true
    });
  }
}
