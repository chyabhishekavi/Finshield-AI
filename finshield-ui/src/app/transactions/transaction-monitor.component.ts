import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatNativeDateModule } from '@angular/material/core';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { EMPTY, Subject, catchError, debounceTime, finalize, merge, startWith, switchMap, tap } from 'rxjs';
import { RiskBand, RiskExplanation, TransactionChannel, TransactionRecord, TransactionStatus } from './transaction.models';
import { TransactionService } from './transaction.service';

@Component({
  selector: 'app-transaction-monitor',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, ReactiveFormsModule, MatButtonModule, MatDatepickerModule,
    MatFormFieldModule, MatInputModule, MatNativeDateModule, MatPaginatorModule,
    MatProgressBarModule, MatSelectModule, MatTableModule],
  template: `
    <section class="page monitor-page">
      <header class="page-header"><div><p class="eyebrow">REAL-TIME TRANSACTION SURVEILLANCE</p>
        <h1 class="page-title">Transaction Monitor</h1><p>Search, filter, and investigate scored banking activity.</p></div>
        <div class="stream-state"><span></span>Monitoring live</div></header>

      <form class="filters" [formGroup]="filters">
        <mat-form-field appearance="outline" class="search"><mat-label>Reference or customer</mat-label>
          <input matInput formControlName="query" placeholder="TXN-2026… or customer name"></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Risk band</mat-label><mat-select formControlName="riskBand">
          <mat-option value="">All risk bands</mat-option>@for (value of riskBands; track value) { <mat-option [value]="value">{{ pretty(value) }}</mat-option> }
        </mat-select></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Status</mat-label><mat-select formControlName="status">
          <mat-option value="">All statuses</mat-option>@for (value of statuses; track value) { <mat-option [value]="value">{{ pretty(value) }}</mat-option> }
        </mat-select></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Channel</mat-label><mat-select formControlName="channel">
          <mat-option value="">All channels</mat-option>@for (value of channels; track value) { <mat-option [value]="value">{{ pretty(value) }}</mat-option> }
        </mat-select></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Minimum amount</mat-label><input matInput type="number" min="0" formControlName="minAmount"></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Maximum amount</mat-label><input matInput type="number" min="0" formControlName="maxAmount"></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>From date</mat-label><input matInput [matDatepicker]="fromPicker" formControlName="fromDate">
          <mat-datepicker-toggle matIconSuffix [for]="fromPicker"/><mat-datepicker #fromPicker/></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>To date</mat-label><input matInput [matDatepicker]="toPicker" formControlName="toDate">
          <mat-datepicker-toggle matIconSuffix [for]="toPicker"/><mat-datepicker #toPicker/></mat-form-field>
        <button mat-stroked-button type="button" (click)="clearFilters()">Clear filters</button>
      </form>

      <div class="table-card">
        @if (loading()) { <mat-progress-bar mode="indeterminate"/> }
        <div class="table-wrap">
          <table mat-table [dataSource]="transactions()">
            <ng-container matColumnDef="reference"><th mat-header-cell *matHeaderCellDef>Transaction</th><td mat-cell *matCellDef="let row">
              <strong>{{ row.transactionReference }}</strong><small>{{ row.initiatedAt | date:'MMM d, y · HH:mm' }}</small></td></ng-container>
            <ng-container matColumnDef="customer"><th mat-header-cell *matHeaderCellDef>Customer</th><td mat-cell *matCellDef="let row">
              <strong>{{ row.customerName }}</strong><small>{{ row.customerNumber }}</small></td></ng-container>
            <ng-container matColumnDef="channel"><th mat-header-cell *matHeaderCellDef>Channel</th><td mat-cell *matCellDef="let row">{{ pretty(row.channel) }}</td></ng-container>
            <ng-container matColumnDef="amount"><th mat-header-cell *matHeaderCellDef>Amount</th><td mat-cell *matCellDef="let row" class="amount">
              {{ row.amount | currency:row.currency:'symbol-narrow':'1.2-2' }}</td></ng-container>
            <ng-container matColumnDef="status"><th mat-header-cell *matHeaderCellDef>Status</th><td mat-cell *matCellDef="let row">
              <span class="status-badge">{{ pretty(row.status) }}</span></td></ng-container>
            <ng-container matColumnDef="risk"><th mat-header-cell *matHeaderCellDef>Risk</th><td mat-cell *matCellDef="let row">
              <span [class]="'risk-badge risk-' + row.riskBand.toLowerCase()">{{ row.riskBand }} · {{ row.riskScore }}</span></td></ng-container>
            <ng-container matColumnDef="action"><th mat-header-cell *matHeaderCellDef></th><td mat-cell *matCellDef="let row">
              <button mat-button (click)="openDetails(row); $event.stopPropagation()">Review</button></td></ng-container>
            <tr mat-header-row *matHeaderRowDef="columns"></tr>
            <tr mat-row *matRowDef="let row; columns: columns" (click)="openDetails(row)"></tr>
          </table>
          @if (!loading() && transactions().length === 0) { <div class="empty">No transactions match the selected filters.</div> }
        </div>
        <mat-paginator [length]="totalElements()" [pageIndex]="pageIndex()" [pageSize]="pageSize()"
          [pageSizeOptions]="[10, 20, 50, 100]" showFirstLastButtons (page)="pageChanged($event)"/>
      </div>
    </section>

    @if (selected(); as transaction) {
      <div class="drawer-backdrop" (click)="closeDetails()"></div>
      <aside class="detail-drawer">
        <header><div><p>TRANSACTION REVIEW</p><h2>{{ transaction.transactionReference }}</h2></div>
          <button mat-button (click)="closeDetails()" aria-label="Close transaction details">✕</button></header>
        <div class="drawer-content">
          <section class="risk-hero"><span [class]="'risk-badge risk-' + transaction.riskBand.toLowerCase()">{{ transaction.riskBand }}</span>
            <strong>{{ transaction.riskScore }}</strong><small>Composite risk score</small></section>
          <section class="detail-grid">
            <div><span>Customer</span><strong>{{ transaction.customerName }}</strong><small>{{ transaction.customerNumber }}</small></div>
            <div><span>Amount</span><strong>{{ transaction.amount | currency:transaction.currency }}</strong></div>
            <div><span>Source account</span><strong>{{ transaction.maskedSourceAccountNumber }}</strong></div>
            <div><span>Destination</span><strong>{{ transaction.maskedDestinationAccountNumber }}</strong></div>
            <div><span>Channel</span><strong>{{ pretty(transaction.channel) }}</strong></div>
            <div><span>Decision</span><strong>{{ pretty(transaction.decision) }}</strong></div>
            <div><span>Device</span><strong>{{ transaction.deviceId || 'Not captured' }}</strong></div>
            <div><span>Location</span><strong>{{ transaction.geoLocation || 'Not captured' }}</strong></div>
          </section>
          <section class="explanation"><h3>Risk explanation</h3>
            @if (explanationLoading()) { <mat-progress-bar mode="indeterminate"/> }
            @if (explanation(); as risk) {
              <p>{{ risk.explanationSummary }}</p>
              <div class="components">@for (item of riskComponents(risk); track item.label) {
                <div><span>{{ item.label }}</span><div class="bar"><i [style.width.%]="item.value"></i></div><strong>{{ item.value }}</strong></div>
              }</div>
              <h4>Matched rules</h4>
              @for (rule of risk.matchedRules; track rule.ruleCode) {
                <article><div><strong>{{ rule.ruleName }}</strong><span>+{{ rule.scoreImpact }}</span></div><p>{{ rule.reason }}</p></article>
              } @empty { <p>No configurable fraud rules matched this transaction.</p> }
            } @else if (!explanationLoading()) { <p>Risk scoring details are not available yet.</p> }
          </section>
        </div>
      </aside>
    }
  `,
  styles: [`
    .monitor-page { position: relative; }.page-header { display: flex; justify-content: space-between; align-items: end; margin-bottom: 18px; }
    .page-header p { margin: 0; color: #748095; font-size: .78rem; }.eyebrow { color: #2e67cc !important; font-size: .62rem !important; font-weight: 750; letter-spacing: .13em; }
    .page-title { margin: 4px 0; }.stream-state { padding: 9px 12px; color: #486078; background: white; border: 1px solid #e1e7ef; border-radius: 9px; font-size: .67rem; }
    .stream-state span { display: inline-block; width: 7px; height: 7px; margin-right: 7px; background: #1db27a; border-radius: 50%; }
    .filters { display: grid; grid-template-columns: 2fr repeat(7, minmax(125px, 1fr)) auto; gap: 9px; align-items: center; padding: 14px;
      margin-bottom: 14px; background: white; border: 1px solid #e1e7ef; border-radius: 11px; }.filters mat-form-field { width: 100%; margin-bottom: -20px; }
    .table-card { overflow: hidden; background: white; border: 1px solid #e1e7ef; border-radius: 11px; }.table-wrap { overflow-x: auto; }
    table { width: 100%; min-width: 980px; } th { color: #758195; background: #f8fafc !important; font-size: .65rem; text-transform: uppercase; letter-spacing: .07em; }
    td { color: #35435a; font-size: .75rem; } td strong, td small { display: block; } td small { margin-top: 4px; color: #8994a5; font-size: .63rem; }
    tr[mat-row] { cursor: pointer; } tr[mat-row]:hover { background: #f7faff; }.amount { font-weight: 700; color: #1d2e4a; }
    .status-badge,.risk-badge { display: inline-flex; padding: 5px 8px; border-radius: 20px; font-size: .62rem; font-weight: 720; }
    .status-badge { color: #4e627c; background: #eef2f7; }.risk-low { color: #16775a; background: #e7f8f1; }.risk-medium { color: #946010; background: #fff4dc; }
    .risk-high { color: #b44c1c; background: #ffeadf; }.risk-critical { color: #b4232c; background: #ffe8ea; }.empty { padding: 50px; text-align: center; color: #7d899c; }
    .drawer-backdrop { position: fixed; inset: 0; z-index: 50; background: #07132966; backdrop-filter: blur(2px); }.detail-drawer { position: fixed; z-index: 51; top: 0; right: 0;
      width: min(620px, 94vw); height: 100vh; overflow: hidden; background: #f7f9fc; box-shadow: -20px 0 70px #08152f35; }.detail-drawer > header { display: flex; justify-content: space-between;
      align-items: center; height: 78px; padding: 0 22px; color: white; background: #0c1c43; }.detail-drawer header p { margin: 0; color: #89a1d1; font-size: .58rem; letter-spacing: .13em; }
    .detail-drawer h2 { margin: 5px 0 0; font-size: 1rem; }.detail-drawer header button { color: white; }.drawer-content { height: calc(100vh - 78px); overflow: auto; padding: 18px; }
    .risk-hero { display: grid; grid-template-columns: 1fr auto; align-items: center; padding: 17px; margin-bottom: 14px; background: white; border: 1px solid #e2e7ef; border-radius: 11px; }
    .risk-hero > strong { grid-row: span 2; font-size: 2.3rem; color: #182743; }.risk-hero > small { color: #7d899b; font-size: .65rem; }
    .detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1px; overflow: hidden; margin-bottom: 14px; background: #e5eaf1; border: 1px solid #e2e7ef; border-radius: 11px; }
    .detail-grid > div { min-width: 0; padding: 13px; background: white; }.detail-grid span,.detail-grid small { display: block; color: #8490a2; font-size: .61rem; }
    .detail-grid strong { display: block; overflow: hidden; margin-top: 5px; color: #31405a; font-size: .73rem; text-overflow: ellipsis; }.detail-grid small { margin-top: 3px; }
    .explanation { padding: 17px; background: white; border: 1px solid #e2e7ef; border-radius: 11px; }.explanation h3 { margin: 0 0 12px; font-size: .86rem; }.explanation > p { color: #5f6e84; font-size: .7rem; line-height: 1.6; }
    .components > div { display: grid; grid-template-columns: 90px 1fr 30px; gap: 8px; align-items: center; margin: 8px 0; font-size: .62rem; }.bar { height: 6px; overflow: hidden; background: #edf1f6; border-radius: 5px; }.bar i { display: block; height: 100%; background: #4577d4; border-radius: 5px; }
    .explanation h4 { margin: 18px 0 8px; font-size: .7rem; }.explanation article { padding: 10px 0; border-top: 1px solid #edf0f4; }.explanation article div { display: flex; justify-content: space-between; font-size: .68rem; }
    .explanation article div span { color: #c14747; font-weight: 700; }.explanation article p { margin: 5px 0 0; color: #758195; font-size: .64rem; }
    @media (max-width: 1500px) { .filters { grid-template-columns: repeat(4, 1fr); }.search { grid-column: span 2; } }
    @media (max-width: 750px) { .page { padding: 15px; }.filters { grid-template-columns: 1fr 1fr; }.search { grid-column: span 2; }.stream-state { display: none; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TransactionMonitorComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(TransactionService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload$ = new Subject<void>();
  readonly columns = ['reference', 'customer', 'channel', 'amount', 'status', 'risk', 'action'];
  readonly riskBands: RiskBand[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
  readonly statuses: TransactionStatus[] = ['RECEIVED', 'PROCESSING', 'PENDING_REVIEW', 'AUTHORIZED', 'DECLINED', 'COMPLETED', 'FAILED', 'REVERSED', 'CANCELLED'];
  readonly channels: TransactionChannel[] = ['MOBILE_BANKING', 'INTERNET_BANKING', 'ATM', 'BRANCH', 'POS', 'API'];
  readonly transactions = signal<TransactionRecord[]>([]);
  readonly totalElements = signal(0);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(20);
  readonly loading = signal(false);
  readonly selected = signal<TransactionRecord | null>(null);
  readonly explanation = signal<RiskExplanation | null>(null);
  readonly explanationLoading = signal(false);
  readonly filters = this.fb.group({
    query: this.fb.nonNullable.control(''), riskBand: this.fb.nonNullable.control<RiskBand | ''>(''),
    status: this.fb.nonNullable.control<TransactionStatus | ''>(''), channel: this.fb.nonNullable.control<TransactionChannel | ''>(''),
    minAmount: this.fb.control<number | null>(null), maxAmount: this.fb.control<number | null>(null),
    fromDate: this.fb.control<Date | null>(null), toDate: this.fb.control<Date | null>(null),
  });

  ngOnInit(): void {
    const filterChanges = this.filters.valueChanges.pipe(debounceTime(300), tap(() => this.pageIndex.set(0)));
    merge(filterChanges, this.reload$).pipe(startWith(null), switchMap(() => {
      this.loading.set(true);
      return this.service.search(this.filters.getRawValue(), this.pageIndex(), this.pageSize()).pipe(
        catchError(() => { this.transactions.set([]); this.totalElements.set(0); return EMPTY; }),
        finalize(() => this.loading.set(false)),
      );
    }), takeUntilDestroyed(this.destroyRef)).subscribe(page => {
      this.transactions.set(page.content); this.totalElements.set(page.totalElements);
    });
  }

  pageChanged(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex); this.pageSize.set(event.pageSize); this.reload$.next();
  }

  clearFilters(): void { this.filters.reset({ query: '', riskBand: '', status: '', channel: '', minAmount: null, maxAmount: null, fromDate: null, toDate: null }); }

  openDetails(transaction: TransactionRecord): void {
    this.selected.set(transaction); this.explanation.set(null); this.explanationLoading.set(true);
    this.service.getRiskExplanation(transaction.id).pipe(takeUntilDestroyed(this.destroyRef),
      catchError(() => EMPTY), finalize(() => {
        if (this.selected()?.id === transaction.id) this.explanationLoading.set(false);
      }))
      .subscribe(value => {
        if (this.selected()?.id === transaction.id) this.explanation.set(value);
      });
  }

  closeDetails(): void { this.selected.set(null); this.explanation.set(null); }

  riskComponents(value: RiskExplanation) {
    return [{ label: 'Rules', value: value.ruleScore }, { label: 'ML model', value: value.mlScore },
      { label: 'Customer', value: value.customerRiskScore }, { label: 'Device', value: value.deviceRiskScore },
      { label: 'AML', value: value.amlScore }];
  }

  pretty(value: string): string {
    return value.toLowerCase().split('_').map(part => part[0]?.toUpperCase() + part.slice(1)).join(' ');
  }
}
