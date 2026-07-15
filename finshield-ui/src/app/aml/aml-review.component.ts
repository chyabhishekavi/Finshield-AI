import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { finalize, forkJoin } from 'rxjs';
import { InvestigationCase } from '../cases/case.models';
import { AmlScreening, WatchlistEntry } from './aml.models';
import { AmlService } from './aml.service';

@Component({
  selector: 'app-aml-review',
  standalone: true,
  imports: [DatePipe, DecimalPipe, RouterLink, ReactiveFormsModule, MatButtonModule, MatFormFieldModule,
    MatInputModule, MatProgressSpinnerModule, MatSelectModule],
  template: `
    <main class="aml-page">
      <header>
        <div><p>FINANCIAL CRIME OPERATIONS</p><h1>AML Review</h1><span>Screen subjects, investigate behavioral patterns, and prepare defensible escalation records.</span></div>
        <div class="metrics"><b>{{ openCases() }}</b><small>Open reviews</small><b>{{ criticalCases() }}</b><small>High priority</small></div>
      </header>

      <section class="workspace">
        <aside class="queue">
          <div class="section-title"><div><h2>Review queue</h2><span>{{ cases().length }} active records</span></div></div>
          @if (loading()) { <mat-spinner diameter="28" /> }
          @for (item of cases(); track item.id) {
            <button class="case-row" [class.selected]="selected()?.id === item.id" (click)="select(item)">
              <div><strong>{{ item.caseNumber }}</strong><span class="type">{{ pretty(item.caseType) }}</span></div>
              <p>{{ item.customerName }}</p>
              <div><span class="priority" [attr.data-priority]="item.priority">{{ item.priority }}</span><time>{{ item.createdAt | date:'dd MMM, HH:mm' }}</time></div>
            </button>
          } @empty { @if (!loading()) { <p class="empty">No AML cases are waiting for review.</p> } }
        </aside>

        <section class="investigation">
          @if (selected(); as current) {
            <div class="case-head">
              <div><span>{{ current.caseNumber }} · {{ pretty(current.caseType) }}</span><h2>{{ current.customerName }}</h2><p>{{ current.customerNumber }} · {{ current.transactionReference }}</p></div>
              <div><button mat-stroked-button [routerLink]="['/cases', current.id]">Open case file</button><button mat-flat-button color="primary" [disabled]="current.status === 'ESCALATED_TO_COMPLIANCE'" (click)="escalate()">Escalate to compliance</button></div>
            </div>
            <div class="status-grid"><div><span>Case status</span><b>{{ pretty(current.status) }}</b></div><div><span>Decision</span><b>{{ pretty(current.decision) }}</b></div><div><span>Owner</span><b>{{ current.assignedToName || 'Unassigned' }}</b></div></div>

            <article class="panel"><div class="panel-title"><h3>Suspicious pattern assessment</h3><span>AUTOMATED + INVESTIGATOR REVIEW</span></div><p>{{ current.summary }}</p><div class="pattern-list">@for (pattern of patterns(); track pattern) { <div><i></i><span>{{ pattern }}</span></div> }</div></article>

            <article class="panel screening">
              <div class="panel-title"><div><h3>Customer / beneficiary screening</h3><small>Exact and fuzzy matching against active watchlists</small></div></div>
              <form [formGroup]="screeningForm" (ngSubmit)="runScreening()">
                <mat-form-field appearance="outline"><mat-label>Subject</mat-label><mat-select formControlName="subjectType"><mat-option value="CUSTOMER">Customer ID</mat-option><mat-option value="BENEFICIARY">Beneficiary ID</mat-option></mat-select></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Subject UUID</mat-label><input matInput formControlName="subjectId"></mat-form-field>
                <button mat-flat-button color="primary" [disabled]="screeningForm.invalid || screeningBusy()">Run screening</button>
              </form>
              @if (screening(); as result) {
                <div class="screen-result" [class.hit]="result.matched"><div><b>{{ result.matched ? 'Potential match found' : 'No active watchlist match' }}</b><span>{{ result.subjectName }} · screened {{ result.screenedAt | date:'medium' }}</span></div><strong>{{ result.results.length }} result(s)</strong></div>
                @for (match of result.results; track match.resultId) { <div class="match"><div><b>{{ match.watchlistName }}</b><span>{{ pretty(match.listType) }} · {{ match.riskCategory }}</span></div><div><strong>{{ match.matchScore | number:'1.0-1' }}%</strong><span>{{ pretty(match.matchType) }}</span></div><p>{{ match.reason }}</p></div> }
              }
            </article>

            <article class="panel report"><div class="panel-title"><div><h3>Report-ready case summary</h3><small>Prepared from the case, transaction, and latest screening outcome</small></div><button mat-stroked-button (click)="copySummary()">{{ copied() ? 'Copied' : 'Copy summary' }}</button></div><pre>{{ reportSummary() }}</pre></article>
          } @else { <div class="empty-detail"><span>AML</span><h2>Select a review from the queue</h2><p>The subject profile, suspicious patterns, screening results, and escalation controls will appear here.</p></div> }
        </section>

        <aside class="watchlist">
          <div class="section-title"><div><h2>Active watchlist</h2><span>Sanctions, PEP and internal records</span></div></div>
          <mat-form-field appearance="outline"><mat-label>Search name or identifier</mat-label><input matInput [value]="watchQuery()" (input)="searchWatchlist($event)"></mat-form-field>
          @for (entry of filteredWatchlist(); track entry.id) { <article><div><b>{{ entry.name }}</b><span>{{ entry.identifier }}</span></div><em [attr.data-risk]="entry.riskCategory">{{ entry.riskCategory }}</em><p>{{ pretty(entry.listType) }} · {{ entry.country }}</p></article> } @empty { <p class="empty">No active entries match this search.</p> }
        </aside>
      </section>
    </main>
  `,
  styles: [`
    .aml-page{padding:24px;color:#17243e}.aml-page>header{display:flex;justify-content:space-between;align-items:end;margin-bottom:18px}.aml-page>header p{margin:0 0 5px;color:#3269c8;font-size:.61rem;font-weight:800;letter-spacing:.14em}.aml-page h1{margin:0;font-size:1.55rem}.aml-page>header span{color:#748096;font-size:.69rem}.metrics{display:grid;grid-template-columns:auto auto;align-items:center;gap:1px 9px;padding:10px 14px;background:#10234a;color:white;border-radius:10px}.metrics b{font-size:1rem}.metrics small{color:#a7b7d7;font-size:.59rem}.workspace{display:grid;grid-template-columns:260px minmax(440px,1fr) 270px;min-height:690px;background:#fff;border:1px solid #dde4ed;border-radius:12px;overflow:hidden}.queue,.watchlist{background:#f8fafc}.queue{border-right:1px solid #e1e7ef}.watchlist{padding:15px;border-left:1px solid #e1e7ef}.section-title{display:flex;justify-content:space-between;padding:16px;border-bottom:1px solid #e1e7ef}.section-title h2{margin:0;font-size:.78rem}.section-title span{display:block;margin-top:3px;color:#8a95a6;font-size:.57rem}.queue mat-spinner{margin:25px auto}.case-row{width:100%;padding:14px 15px;text-align:left;background:transparent;border:0;border-bottom:1px solid #e6ebf1;cursor:pointer}.case-row:hover,.case-row.selected{background:#edf3ff}.case-row.selected{box-shadow:inset 3px 0 #316bd0}.case-row>div{display:flex;justify-content:space-between;align-items:center}.case-row strong{font-size:.67rem}.case-row .type{font-size:.53rem;color:#61718a}.case-row p{margin:8px 0;color:#40516a;font-size:.68rem}.case-row time{color:#8d98a8;font-size:.54rem}.priority{padding:3px 5px;color:#925515;background:#fff0d5;font-size:.51rem;font-weight:750;border-radius:4px}.priority[data-priority=CRITICAL]{color:#a42626;background:#ffe1e1}.investigation{padding:20px}.case-head{display:flex;justify-content:space-between;gap:15px}.case-head>div:last-child{display:flex;gap:7px;align-items:start}.case-head span{color:#3a6bc0;font-size:.59rem;font-weight:750}.case-head h2{margin:4px 0;font-size:1.1rem}.case-head p{margin:0;color:#818c9c;font-size:.62rem}.status-grid{display:grid;grid-template-columns:repeat(3,1fr);margin:17px 0;background:#f5f8fc;border-radius:8px}.status-grid div{padding:11px;border-right:1px solid #e1e6ed}.status-grid span,.status-grid b{display:block}.status-grid span{color:#8793a4;font-size:.54rem}.status-grid b{margin-top:5px;font-size:.65rem}.panel{margin-top:13px;padding:16px;border:1px solid #e1e6ed;border-radius:9px}.panel-title{display:flex;justify-content:space-between;align-items:start}.panel h3{margin:0;font-size:.76rem}.panel-title>span{padding:3px 6px;color:#557298;background:#eaf1fa;font-size:.48rem;border-radius:4px}.panel-title small{display:block;margin-top:3px;color:#919bac;font-size:.55rem}.panel>p{color:#596980;font-size:.68rem;line-height:1.55}.pattern-list{display:grid;gap:6px}.pattern-list div{display:flex;gap:8px;padding:8px;background:#fff8eb;color:#695331;font-size:.63rem;border-radius:6px}.pattern-list i{width:6px;height:6px;margin-top:4px;background:#d58b25;border-radius:50%}.screening form{display:grid;grid-template-columns:150px 1fr auto;gap:8px;align-items:center;margin-top:13px}.screening mat-form-field{margin-bottom:-20px}.screen-result{display:flex;justify-content:space-between;margin-top:15px;padding:10px;background:#edf8f0;border-left:3px solid #42a35e}.screen-result.hit{background:#fff0f0;border-color:#d14343}.screen-result b,.screen-result span{display:block;font-size:.62rem}.screen-result span{margin-top:4px;color:#778497;font-size:.55rem}.match{display:grid;grid-template-columns:1fr auto;gap:6px;margin-top:8px;padding:10px;background:#f7f9fc;border-radius:7px}.match b,.match span{display:block;font-size:.61rem}.match span{margin-top:3px;color:#7c899b;font-size:.53rem}.match p{grid-column:1/-1;margin:1px 0;color:#56657a;font-size:.58rem}.report pre{white-space:pre-wrap;margin:12px 0 0;padding:12px;background:#101d35;color:#d5dfef;font: .61rem/1.65 ui-monospace,monospace;border-radius:7px}.watchlist mat-form-field{width:100%;margin:12px 0 -6px}.watchlist article{display:grid;grid-template-columns:1fr auto;gap:4px;padding:11px 2px;border-bottom:1px solid #e1e6ed}.watchlist b,.watchlist span{display:block}.watchlist b{font-size:.63rem}.watchlist span{margin-top:3px;color:#8a96a7;font-size:.53rem}.watchlist em{align-self:start;padding:3px 5px;color:#805911;background:#ffedc7;font-size:.48rem;font-style:normal;border-radius:4px}.watchlist em[data-risk=CRITICAL],.watchlist em[data-risk=HIGH]{color:#982e2e;background:#ffe1e1}.watchlist p{grid-column:1/-1;margin:4px 0 0;color:#5e6e85;font-size:.55rem}.empty{padding:14px;color:#8b96a5;font-size:.63rem}.empty-detail{display:grid;place-items:center;align-content:center;height:100%;text-align:center}.empty-detail span{display:grid;place-items:center;width:55px;height:55px;color:#416eb9;background:#edf3ff;font-weight:800;border-radius:50%}.empty-detail h2{margin:15px 0 5px;font-size:.9rem}.empty-detail p{max-width:340px;color:#8490a1;font-size:.65rem;line-height:1.5}@media(max-width:1200px){.workspace{grid-template-columns:235px 1fr}.watchlist{display:none}}@media(max-width:800px){.workspace{grid-template-columns:1fr}.queue{border:0}.investigation{min-height:600px}.case-head{display:block}.case-head>div:last-child{margin-top:10px}.screening form{grid-template-columns:1fr}.metrics{display:none}}
  `],
})
export class AmlReviewComponent {
  private readonly service = inject(AmlService);
  private readonly fb = inject(FormBuilder);
  readonly cases = signal<InvestigationCase[]>([]);
  readonly watchlist = signal<WatchlistEntry[]>([]);
  readonly selected = signal<InvestigationCase | null>(null);
  readonly screening = signal<AmlScreening | null>(null);
  readonly loading = signal(true);
  readonly screeningBusy = signal(false);
  readonly copied = signal(false);
  readonly watchQuery = signal('');
  readonly screeningForm = this.fb.nonNullable.group({ subjectType: 'CUSTOMER', subjectId: ['', [Validators.required, Validators.minLength(36)]] });
  readonly filteredWatchlist = computed(() => { const query = this.watchQuery().trim().toLowerCase(); return query ? this.watchlist().filter(item => `${item.name} ${item.identifier}`.toLowerCase().includes(query)) : this.watchlist(); });
  readonly openCases = computed(() => this.cases().filter(item => item.status !== 'CLOSED').length);
  readonly criticalCases = computed(() => this.cases().filter(item => ['CRITICAL', 'HIGH'].includes(item.priority)).length);
  readonly patterns = computed(() => { const value = this.selected()?.summary ?? ''; const extracted = value.split(/[;\n]|\.\s+/).map(item => item.replace(/^[-•]\s*/, '').trim()).filter(item => item.length > 12); return extracted.length ? extracted.slice(0, 5) : ['Behavior requires investigator validation against expected customer activity.']; });
  readonly reportSummary = computed(() => { const item = this.selected(); if (!item) return ''; const result = this.screening(); return [`CASE: ${item.caseNumber}`, `SUBJECT: ${item.customerName} (${item.customerNumber})`, `CASE TYPE: ${this.pretty(item.caseType)}`, `PRIMARY TRANSACTION: ${item.transactionReference}`, `ASSESSMENT: ${item.summary}`, `WATCHLIST SCREENING: ${result ? (result.matched ? `${result.results.length} potential match(es)` : 'No active match') : 'Pending investigator screening'}`, `CURRENT STATUS: ${this.pretty(item.status)}`, `RECOMMENDATION: ${item.status === 'ESCALATED_TO_COMPLIANCE' ? 'Compliance review initiated' : 'Complete screening and determine whether escalation/SAR review is warranted'}`].join('\n'); });

  constructor() { forkJoin({ cases: this.service.reviewQueue(), watchlist: this.service.watchlist() }).pipe(finalize(() => this.loading.set(false))).subscribe(({ cases, watchlist }) => { this.cases.set(cases); this.watchlist.set(watchlist); if (cases.length) this.select(cases[0]); }); }
  select(item: InvestigationCase) { this.selected.set(item); this.screening.set(null); this.screeningForm.patchValue({ subjectType: 'CUSTOMER', subjectId: item.customerId }); }
  runScreening() { if (this.screeningForm.invalid) return; const value = this.screeningForm.getRawValue(); this.screeningBusy.set(true); const request = value.subjectType === 'CUSTOMER' ? this.service.screenCustomer(value.subjectId) : this.service.screenBeneficiary(value.subjectId); request.pipe(finalize(() => this.screeningBusy.set(false))).subscribe(result => this.screening.set(result)); }
  escalate() { const item = this.selected(); if (!item) return; this.service.escalate(item.id).subscribe(updated => { this.selected.set(updated); this.cases.update(items => items.map(value => value.id === updated.id ? updated : value)); }); }
  searchWatchlist(event: Event) { this.watchQuery.set((event.target as HTMLInputElement).value); }
  copySummary() { navigator.clipboard.writeText(this.reportSummary()).then(() => { this.copied.set(true); setTimeout(() => this.copied.set(false), 1500); }); }
  pretty(value: string | null) { if (!value) return 'Pending'; return value.toLowerCase().split('_').map(part => `${part[0]?.toUpperCase() ?? ''}${part.slice(1)}`).join(' '); }
}
