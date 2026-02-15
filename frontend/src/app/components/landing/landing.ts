import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { AuthDialogComponent } from '../auth-dialog/auth-dialog';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatToolbarModule,
    MatDialogModule
  ],
  templateUrl: './landing.html',
  styleUrl: './landing.css'
})
export class LandingComponent {

  features = [
    {
      icon: 'upload_file',
      title: 'Receipt Upload',
      description: 'Automatically scan and parse receipt images with AI-powered OCR technology',
      color: '#2563eb'
    },
    {
      icon: 'edit_note',
      title: 'Manual Entry',
      description: 'Quick manual entry for non-receipt expenses with detailed line items',
      color: '#8b5cf6'
    },
    {
      icon: 'bar_chart',
      title: 'Visual Analytics',
      description: 'Beautiful D3.js charts showing spending by category and trends over time',
      color: '#ec4899'
    },
    {
      icon: 'smart_toy',
      title: 'FinBot AI Chat',
      description: 'Chat with FinBot, your AI-powered personal finance assistant for spending advice',
      color: '#10b981'
    },
    {
      icon: 'psychology',
      title: 'AI Suggestions',
      description: 'Receive personalized money-saving tips and alternative spending strategies',
      color: '#3b82f6'
    }
  ];

  howItWorks = [
    {
      step: 1,
      title: 'Sign Up',
      description: 'Create your account in seconds and get started right away',
      icon: 'person_add'
    },
    {
      step: 2,
      title: 'Upload Receipts',
      description: 'Drag & drop multiple receipts or enter expenses manually',
      icon: 'cloud_upload'
    },
    {
      step: 3,
      title: 'Track Spending',
      description: 'View real-time analytics with interactive charts and breakdowns',
      icon: 'analytics'
    },
    {
      step: 4,
      title: 'Get Smart Insights',
      description: 'Chat with FinBot for AI-powered spending advice and saving tips',
      icon: 'lightbulb'
    }
  ];

  benefits = [
    'Easy to use with minimal clicks',
    'Intelligent AI-powered categorization',
    'Beautiful visual insights',
    'AI chat assistant (FinBot)',
    'Receipt scanning with OCR',
    'Detailed spending analytics'
  ];

  constructor(
    private router: Router,
    private dialog: MatDialog
  ) {}

  openAuthDialog(defaultTab: 'login' | 'register' = 'login', returnUrl?: string): void {
    this.dialog.open(AuthDialogComponent, {
      width: '500px',
      maxWidth: '95vw',
      data: {
        defaultTab,
        returnUrl
      },
      disableClose: false,
      autoFocus: true
    });
  }

  navigateToLogin(returnUrl?: string): void {
    this.openAuthDialog('login', returnUrl);
  }

  navigateToRegister(): void {
    this.openAuthDialog('register');
  }

  scrollToSection(sectionId: string): void {
    const element = document.getElementById(sectionId);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }
}
