import { Routes } from '@angular/router';
import { LandingComponent } from './components/landing/landing';
import { AnalyticsDashboardComponent } from './components/analytics-dashboard/analytics-dashboard.component';
import { ReceiptListComponent } from './components/receipt-list/receipt-list';
import { ReceiptDetail } from './components/receipt-detail/receipt-detail';
import { ManualReceiptForm } from './components/manual-receipt-form/manual-receipt-form';
import { LoginComponent } from './components/login/login';
import { RegisterComponent } from './components/register/register';
import { IncomeSettingsComponent } from './components/income-settings/income-settings.component';
import { authGuard } from './guards/auth.guard';
import { guestGuard } from './guards/guest.guard';

export const routes: Routes = [
  // Public routes (redirect to dashboard if already logged in)
  { path: '', component: LandingComponent, canActivate: [guestGuard] },
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  { path: 'register', component: RegisterComponent, canActivate: [guestGuard] },

  // Protected routes - Main Dashboard with Analytics
  { path: 'dashboard', component: AnalyticsDashboardComponent, canActivate: [authGuard] },

  // Transaction routes (renamed from receipts)
  { path: 'transactions', component: ReceiptListComponent, canActivate: [authGuard] },
  { path: 'transactions/create', component: ManualReceiptForm, canActivate: [authGuard] },
  { path: 'transactions/scan', redirectTo: 'transactions/create', pathMatch: 'full' },
  { path: 'transactions/:id', component: ReceiptDetail, canActivate: [authGuard] },

  // Income routes
  { path: 'income', component: IncomeSettingsComponent, canActivate: [authGuard] },

  // Legacy redirects (keep old URLs working)
  { path: 'receipts', redirectTo: 'transactions', pathMatch: 'full' },
  { path: 'receipts/create', redirectTo: 'transactions/create', pathMatch: 'full' },
  { path: 'receipts/upload', redirectTo: 'transactions/scan', pathMatch: 'full' },

  // Wildcard - redirect to landing
  { path: '**', redirectTo: '' }
];
