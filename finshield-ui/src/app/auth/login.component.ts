import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatProgressSpinnerModule],
  template: `
    <main class="login-page">
      <section class="welcome-panel">
        <div class="logo"><span>FS</span> FinShield <b>AI</b></div>
        <div class="welcome-copy">
          <p class="eyebrow">FINANCIAL CRIME OPERATIONS</p>
          <h1>Make risk visible.<br>Act before loss.</h1>
          <p>Real-time fraud detection, AML monitoring, and defensible investigation workflows in one secure workspace.</p>
        </div>
        <div class="trust-row"><span>● Encrypted session</span><span>● Full audit trail</span></div>
      </section>
      <mat-card class="login-card">
        <mat-card-header>
          <p class="eyebrow dark">AUTHORIZED ACCESS</p>
          <mat-card-title>Welcome back</mat-card-title>
          <mat-card-subtitle>Sign in to the risk operations console</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="submit()">
            <mat-form-field appearance="outline">
              <mat-label>Email</mat-label>
              <input matInput type="email" autocomplete="username" formControlName="email">
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Password</mat-label>
              <input matInput type="password" autocomplete="current-password" formControlName="password">
            </mat-form-field>
            @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
            <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || loading()">
              @if (loading()) { <mat-spinner diameter="20" /> } @else { Sign in }
            </button>
          </form>
        </mat-card-content>
      </mat-card>
    </main>
  `,
  styles: [`
    .login-page { min-height: 100vh; display: grid; grid-template-columns: minmax(360px, 1.05fr) minmax(420px, .95fr); background: #f8fafc; }
    .welcome-panel { display: flex; flex-direction: column; justify-content: space-between; padding: clamp(36px, 6vw, 82px);
      color: white; background: radial-gradient(circle at 10% 0, #214a93, transparent 42%), linear-gradient(145deg, #07132f, #102b5d); }
    .logo { font-size: 1.2rem; font-weight: 720; }.logo span { display: inline-grid; place-items: center; width: 40px; height: 40px;
      margin-right: 10px; background: #3679f5; border-radius: 11px; font-size: .72rem; }.logo b { color: #72a7ff; }
    .welcome-copy { max-width: 590px; }.welcome-copy h1 { margin: 12px 0 22px; font-size: clamp(2.4rem, 4vw, 4.4rem);
      line-height: 1.04; letter-spacing: -.045em; }.welcome-copy > p:last-child { max-width: 530px; color: #b7c7e7; line-height: 1.75; }
    .eyebrow { color: #7fb0ff; font-size: .68rem; font-weight: 750; letter-spacing: .16em; }.eyebrow.dark { color: #2f67ca; margin: 0 0 8px; }
    .trust-row { display: flex; gap: 24px; color: #93a8d0; font-size: .72rem; }
    .login-card { align-self: center; justify-self: center; width: min(440px, calc(100% - 48px)); padding: 28px;
      border: 1px solid #e3e9f2; border-radius: 16px; box-shadow: 0 24px 70px #18376b16; }
    form { display: grid; gap: 8px; margin-top: 24px; }
    button { height: 46px; } mat-spinner { margin: auto; }
    .error { color: #b42318; margin: 0 0 8px; }
    @media (max-width: 850px) { .login-page { grid-template-columns: 1fr; }.welcome-panel { display: none; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  submit(): void {
    if (this.form.invalid || this.loading()) return;
    this.loading.set(true);
    this.error.set(null);
    const { email, password } = this.form.getRawValue();
    this.auth.login(email, password).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: () => void this.router.navigateByUrl(this.route.snapshot.queryParamMap.get('returnUrl') ?? '/dashboard'),
      error: () => this.error.set('Sign-in failed. Check your credentials and try again.'),
    });
  }
}
