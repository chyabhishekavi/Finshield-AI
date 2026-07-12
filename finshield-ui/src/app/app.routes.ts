import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { guestGuard } from './core/guards/guest.guard';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./auth/login.component').then(module => module.LoginComponent),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell.component').then(module => module.ShellComponent),
    children: [
      { path: 'dashboard', loadComponent: () => import('./dashboard/dashboard.component').then(module => module.DashboardComponent) },
      { path: 'customers', loadChildren: () => import('./customers/customers.routes') },
      { path: 'transactions', loadChildren: () => import('./transactions/transactions.routes') },
      { path: 'alerts', canActivate: [roleGuard], data: { roles: ['ADMIN', 'FRAUD_ANALYST', 'RISK_MANAGER'] }, loadChildren: () => import('./alerts/alerts.routes') },
      { path: 'cases', canActivate: [roleGuard], data: { roles: ['ADMIN', 'FRAUD_ANALYST', 'AML_INVESTIGATOR', 'COMPLIANCE_OFFICER'] }, loadChildren: () => import('./cases/cases.routes') },
      { path: 'aml', canActivate: [roleGuard], data: { roles: ['ADMIN', 'AML_INVESTIGATOR', 'COMPLIANCE_OFFICER', 'RISK_MANAGER'] }, loadChildren: () => import('./aml/aml.routes') },
      { path: 'rules', canActivate: [roleGuard], data: { roles: ['ADMIN', 'RISK_MANAGER'] }, loadChildren: () => import('./rules/rules.routes') },
      { path: 'audit', canActivate: [roleGuard], data: { roles: ['ADMIN', 'COMPLIANCE_OFFICER'] }, loadChildren: () => import('./audit/audit.routes') },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
    ],
  },
  { path: '**', redirectTo: '' },
];
