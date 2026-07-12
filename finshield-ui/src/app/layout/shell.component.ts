import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { AuthService } from '../core/auth/auth.service';
import { RoleName } from '../core/auth/auth.models';
import { filter, map } from 'rxjs';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatButtonModule,
    MatDividerModule, MatListModule, MatMenuModule, MatSidenavModule, MatToolbarModule],
  template: `
    <mat-sidenav-container class="shell">
      <mat-sidenav #drawer [mode]="isMobile() ? 'over' : 'side'" [opened]="!isMobile()" class="sidebar">
        <a class="brand" routerLink="/dashboard" (click)="isMobile() && drawer.close()">
          <span class="shield">FS</span><span class="brand-copy">FinShield <b>AI</b><small>Risk Operations</small></span>
        </a>
        <p class="section-label">WORKSPACE</p>
        <mat-nav-list>
          @for (item of visibleNavigation(); track item.path) {
            <a mat-list-item [routerLink]="item.path" routerLinkActive="active"
              (click)="isMobile() && drawer.close()">
              <span class="nav-mark">{{ item.mark }}</span>{{ item.label }}
            </a>
          }
        </mat-nav-list>
        <div class="security-state"><span></span><div><strong>Systems operational</strong><small>Protected session</small></div></div>
      </mat-sidenav>
      <mat-sidenav-content>
        <mat-toolbar class="topbar">
          @if (isMobile()) { <button mat-button class="menu-button" (click)="drawer.toggle()">☰</button> }
          <div class="page-context"><small>FINSHIELD OPERATIONS</small><strong>{{ pageTitle() }}</strong></div>
          <span class="spacer"></span>
          <button mat-button class="profile" [matMenuTriggerFor]="profileMenu">
            <span class="avatar">{{ initials() }}</span>
            <span class="profile-copy"><strong>{{ auth.currentUser()?.fullName }}</strong><small>{{ primaryRole() }}</small></span>
          </button>
          <mat-menu #profileMenu="matMenu" xPosition="before">
            <button mat-menu-item (click)="auth.logout()">Sign out securely</button>
          </mat-menu>
        </mat-toolbar>
        <router-outlet />
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [`
    .shell { height: 100vh; background: #f3f6fa; } .sidebar { width: 264px; background: #0b1739; color: #dbe7ff; border: 0; }
    .brand { display: flex; align-items: center; gap: 12px; padding: 25px 22px; color: white; text-decoration: none; }
    .shield { display: grid; place-items: center; width: 40px; height: 44px; background: #2f6fed;
      clip-path: polygon(50% 0, 96% 17%, 87% 72%, 50% 100%, 13% 72%, 4% 17%); font-size: .72rem; font-weight: 800; }
    .brand-copy { font-size: 1.08rem; font-weight: 700; line-height: 1.05; } .brand-copy b { color: #73a6ff; }
    .brand-copy small { display: block; margin-top: 6px; color: #91a4cc; font-size: .62rem; letter-spacing: .12em; }
    .section-label { margin: 8px 24px; color: #7183aa; font-size: .65rem; letter-spacing: .13em; }
    mat-nav-list { padding: 0 12px; } a[mat-list-item] { color: #c1cdec; margin: 4px 0; border-radius: 9px; }
    .active { background: #18346e !important; color: white !important; }
    .nav-mark { display: inline-grid; place-items: center; width: 28px; margin-right: 8px; color: #79a7ff;
      font-size: .66rem; font-weight: 750; }
    .security-state { position: absolute; left: 20px; right: 20px; bottom: 22px; display: flex; gap: 10px;
      align-items: center; padding: 12px; background: #101f47; border: 1px solid #203566; border-radius: 10px; }
    .security-state > span { width: 8px; height: 8px; background: #37d996; border-radius: 50%; box-shadow: 0 0 0 4px #37d99620; }
    .security-state strong, .security-state small { display: block; } .security-state strong { font-size: .72rem; }
    .security-state small { margin-top: 3px; color: #8297c2; font-size: .65rem; }
    .topbar { min-height: 68px; height: 68px; background: white; border-bottom: 1px solid #e1e7f0; padding: 0 22px; }
    .page-context { display: grid; line-height: 1.15; } .page-context small { color: #7b879b; font-size: .6rem; letter-spacing: .12em; }
    .page-context strong { margin-top: 5px; font-size: 1rem; color: #17233d; } .spacer { flex: 1; }
    .profile { height: 52px; border-left: 1px solid #e6eaf0; border-radius: 0; padding-left: 18px; }
    .avatar { display: inline-grid; place-items: center; width: 34px; height: 34px; margin-right: 9px;
      border-radius: 9px; background: #e8efff; color: #1e55b3; font-weight: 750; }
    .profile-copy { display: inline-grid; text-align: left; line-height: 1.15; }
    .profile-copy strong { font-size: .78rem; color: #26344d; } .profile-copy small { margin-top: 4px; color: #7c8799; font-size: .62rem; }
    .menu-button { min-width: 38px; margin-right: 8px; font-size: 1.2rem; }
    @media (max-width: 599px) { .profile-copy { display: none; } .page-context small { display: none; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShellComponent {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly breakpointObserver = inject(BreakpointObserver);
  readonly navigation: ReadonlyArray<{ path: string; label: string; mark: string; roles?: readonly RoleName[] }> = [
    { path: '/dashboard', label: 'Executive Dashboard', mark: 'DB' },
    { path: '/customers', label: 'Customer 360', mark: 'CU' },
    { path: '/transactions', label: 'Transactions', mark: 'TX' },
    { path: '/alerts', label: 'Fraud Alerts', mark: 'FA', roles: ['ADMIN', 'FRAUD_ANALYST', 'RISK_MANAGER'] },
    { path: '/cases', label: 'Investigations', mark: 'IC', roles: ['ADMIN', 'FRAUD_ANALYST', 'AML_INVESTIGATOR', 'COMPLIANCE_OFFICER'] },
    { path: '/aml', label: 'AML Monitoring', mark: 'AM', roles: ['ADMIN', 'AML_INVESTIGATOR', 'COMPLIANCE_OFFICER', 'RISK_MANAGER'] },
    { path: '/rules', label: 'Risk Rules', mark: 'RR', roles: ['ADMIN', 'RISK_MANAGER'] },
    { path: '/audit', label: 'Audit Trail', mark: 'AT', roles: ['ADMIN', 'COMPLIANCE_OFFICER'] },
  ];
  readonly visibleNavigation = computed(() => this.navigation.filter(item =>
    !item.roles || this.auth.hasAnyRole(item.roles),
  ));
  readonly isMobile = toSignal(this.breakpointObserver.observe(Breakpoints.Handset)
    .pipe(map(result => result.matches)), { initialValue: false });
  readonly pageTitle = toSignal(this.router.events.pipe(
    filter((event): event is NavigationEnd => event instanceof NavigationEnd),
    map(event => this.navigation.find(item => event.urlAfterRedirects.startsWith(item.path))?.label ?? 'Operations'),
  ), { initialValue: 'Executive Dashboard' });
  readonly initials = computed(() => this.auth.currentUser()?.fullName.split(/\s+/)
    .slice(0, 2).map(part => part[0]).join('').toUpperCase() ?? 'FS');
  readonly primaryRole = computed(() => (this.auth.currentUser()?.roles[0] ?? 'USER').replaceAll('_', ' '));
}
