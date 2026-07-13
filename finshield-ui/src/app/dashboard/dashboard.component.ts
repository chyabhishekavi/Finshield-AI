import { AfterViewInit, ChangeDetectionStrategy, Component, DestroyRef, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { EMPTY, catchError, finalize, switchMap, timer } from 'rxjs';
import { DashboardOverview, DashboardSnapshot } from './dashboard.models';
import { DashboardService } from './dashboard.service';

Chart.register(...registerables);

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [MatCardModule, MatButtonModule],
  template: `
    <section class="page">
      <header class="dashboard-header">
        <div><p class="eyebrow">REAL-TIME MONITORING</p><h1 class="page-title">Financial Crime Command Center</h1>
          <p>Enterprise fraud and AML signals across the monitored estate.</p></div>
        <div class="live-state"><span></span><div><strong>Live monitoring</strong><small>
          @if (lastUpdated()) { Updated {{ lastUpdated()!.toLocaleTimeString() }} } @else { Connecting… }
        </small></div></div>
      </header>
      @if (snapshot(); as data) {
        <div class="metrics">
          @for (metric of metricCards(data.overview); track metric.label) {
            <mat-card [class]="'metric ' + metric.tone">
              <div class="metric-mark">{{ metric.mark }}</div><div><span>{{ metric.label }}</span>
              <strong>{{ metric.value }}</strong><small>{{ metric.context }}</small></div>
            </mat-card>
          }
        </div>
      }
      @if (error()) {
        <div class="error-state" role="alert">Dashboard data could not be refreshed.
          <button mat-button (click)="refresh()">Try again</button></div>
      }
      <div class="chart-grid" [class.loading]="loading()">
        <section class="chart-panel risk-panel"><div class="panel-heading"><div><h2>Risk trend</h2><p>Daily transaction risk bands</p></div><span>14 days</span></div>
          <div class="canvas-wrap"><canvas #riskChart aria-label="Daily transaction risk trend"></canvas></div></section>
        <section class="chart-panel"><div class="panel-heading"><div><h2>Alert status distribution</h2><p>Current operational workload</p></div></div>
          <div class="canvas-wrap"><canvas #alertChart aria-label="Alert status distribution"></canvas></div></section>
        <section class="chart-panel"><div class="panel-heading"><div><h2>Top fraud rules</h2><p>Rules ranked by match volume</p></div><span>30 days</span></div>
          <div class="canvas-wrap"><canvas #rulesChart aria-label="Top matching fraud rules"></canvas></div></section>
        <section class="chart-panel"><div class="panel-heading"><div><h2>AML case trend</h2><p>New AML investigations opened</p></div><span>14 days</span></div>
          <div class="canvas-wrap"><canvas #amlChart aria-label="AML case trend"></canvas></div></section>
      </div>
    </section>
  `,
  styles: [`
    .dashboard-header { display: flex; justify-content: space-between; align-items: end; margin-bottom: 22px; }
    .dashboard-header h1 { margin-bottom: 5px; }.dashboard-header p { margin: 0; color: #6b778c; font-size: .84rem; }
    .eyebrow { color: #2d66cc !important; font-size: .64rem !important; font-weight: 750; letter-spacing: .14em; }
    .live-state { display: flex; align-items: center; gap: 10px; min-width: 155px; padding: 10px 13px; background: white;
      border: 1px solid #e1e7f0; border-radius: 10px; }.live-state > span { width: 8px; height: 8px; border-radius: 50%;
      background: #18ad72; box-shadow: 0 0 0 4px #18ad7218; }.live-state strong,.live-state small { display: block; }
    .live-state strong { color: #31405a; font-size: .7rem; }.live-state small { color: #8a95a6; font-size: .62rem; margin-top: 2px; }
    .metrics { display: grid; grid-template-columns: repeat(6, minmax(145px, 1fr)); gap: 13px; margin-bottom: 18px; }
    .metric { display: grid; grid-template-columns: 34px 1fr; gap: 11px; padding: 16px; border: 1px solid #e2e8f1;
      border-radius: 11px; box-shadow: 0 5px 18px #203a6910; }.metric-mark { display: grid; place-items: center; width: 34px;
      height: 34px; border-radius: 9px; background: #edf3ff; color: #2d65c8; font-size: .62rem; font-weight: 800; }
    .metric span,.metric small { display: block; color: #748095; }.metric span { min-height: 28px; font-size: .69rem; line-height: 1.25; }
    .metric strong { display: block; margin: 4px 0; color: #14213c; font-size: 1.4rem; letter-spacing: -.03em; }
    .metric small { font-size: .58rem; }.metric.danger .metric-mark { background: #fff0f0; color: #d43c3c; }
    .metric.warning .metric-mark { background: #fff6e6; color: #bc7414; }.metric.positive .metric-mark { background: #eaf9f3; color: #158660; }
    .chart-grid { display: grid; grid-template-columns: 1.35fr .85fr; gap: 16px; transition: opacity .2s; }.chart-grid.loading { opacity: .55; }
    .chart-panel { min-width: 0; padding: 18px; background: white; border: 1px solid #e1e7f0; border-radius: 12px;
      box-shadow: 0 5px 20px #203a690a; }.panel-heading { display: flex; justify-content: space-between; align-items: start; margin-bottom: 12px; }
    .panel-heading h2 { margin: 0; color: #26344d; font-size: .86rem; }.panel-heading p { margin: 4px 0 0; color: #8994a6; font-size: .65rem; }
    .panel-heading > span { padding: 5px 8px; color: #64758e; background: #f4f7fb; border-radius: 6px; font-size: .6rem; }
    .canvas-wrap { position: relative; height: 270px; }.error-state { margin-bottom: 14px; padding: 10px 14px;
      color: #9e2d2d; background: #fff0f0; border: 1px solid #ffd3d3; border-radius: 9px; font-size: .76rem; }
    @media (max-width: 1250px) { .metrics { grid-template-columns: repeat(3, 1fr); } }
    @media (max-width: 850px) { .chart-grid { grid-template-columns: 1fr; }.dashboard-header { align-items: start; gap: 16px; }
      .metrics { grid-template-columns: repeat(2, 1fr); } }
    @media (max-width: 520px) { .page { padding: 16px; }.metrics { grid-template-columns: 1fr; }.live-state { display: none; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent implements AfterViewInit {
  @ViewChild('riskChart', { static: true }) private chartCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('alertChart', { static: true }) private alertCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('rulesChart', { static: true }) private rulesCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('amlChart', { static: true }) private amlCanvas!: ElementRef<HTMLCanvasElement>;
  private readonly dashboard = inject(DashboardService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly charts = new Map<string, Chart>();
  readonly snapshot = signal<DashboardSnapshot | null>(null);
  readonly loading = signal(true);
  readonly error = signal(false);
  readonly lastUpdated = signal<Date | null>(null);

  constructor() {
    this.destroyRef.onDestroy(() => this.charts.forEach(chart => chart.destroy()));
  }

  ngAfterViewInit(): void {
    timer(0, 30_000).pipe(switchMap(() => {
      this.loading.set(true); this.error.set(false);
      return this.dashboard.snapshot().pipe(
        catchError(() => { this.error.set(true); return EMPTY; }),
        finalize(() => this.loading.set(false)),
      );
    }), takeUntilDestroyed(this.destroyRef)).subscribe(data => {
      this.snapshot.set(data); this.lastUpdated.set(new Date()); this.renderCharts(data);
    });
  }

  refresh(): void {
    this.loading.set(true);
    this.dashboard.snapshot().pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.loading.set(false)))
      .subscribe({ next: data => { this.snapshot.set(data); this.lastUpdated.set(new Date()); this.renderCharts(data); },
        error: () => this.error.set(true) });
  }

  metricCards(value: DashboardOverview) {
    return [
      { label: 'Transactions monitored today', value: value.transactionsMonitoredToday.toLocaleString(), context: 'Across all channels', mark: 'TX', tone: '' },
      { label: 'Alerts generated today', value: value.alertsGeneratedToday.toLocaleString(), context: 'Automated detections', mark: 'AL', tone: 'warning' },
      { label: 'Critical alerts', value: value.criticalAlerts.toLocaleString(), context: 'Immediate action', mark: 'CR', tone: 'danger' },
      { label: 'Open cases', value: value.openCases.toLocaleString(), context: 'Investigation queue', mark: 'CS', tone: '' },
      { label: 'False positive rate', value: `${value.falsePositiveRate.toFixed(1)}%`, context: 'Trailing 30 days', mark: 'FP', tone: 'positive' },
      { label: 'Average investigation time', value: `${value.averageInvestigationHours.toFixed(1)}h`, context: 'Closed cases', mark: 'AT', tone: 'positive' },
    ];
  }

  private renderCharts(data: DashboardSnapshot): void {
    this.draw('risk', this.chartCanvas, { type: 'line', data: { labels: data.riskTrend.map(item => item.label), datasets: [
      { label: 'Medium', data: data.riskTrend.map(item => item.medium), borderColor: '#4d7bd6', backgroundColor: '#4d7bd620', tension: .35 },
      { label: 'High', data: data.riskTrend.map(item => item.high), borderColor: '#e99a2f', backgroundColor: '#e99a2f20', tension: .35 },
      { label: 'Critical', data: data.riskTrend.map(item => item.critical), borderColor: '#d74c4c', backgroundColor: '#d74c4c20', tension: .35 },
    ] }, options: this.chartOptions() });
    this.draw('alerts', this.alertCanvas, { type: 'doughnut', data: { labels: data.alertStatusDistribution.map(item => this.pretty(item.label)),
      datasets: [{ data: data.alertStatusDistribution.map(item => item.value), backgroundColor: ['#336fd5','#76a0ed','#e9a23b','#d84a4a','#36a67a','#a6b2c4'], borderWidth: 0 }] },
      options: this.chartOptions() });
    this.draw('rules', this.rulesCanvas, { type: 'bar', data: { labels: data.topFraudRules.map(item => item.ruleName),
      datasets: [{ label: 'Matches', data: data.topFraudRules.map(item => item.matchCount), backgroundColor: '#376fd2', borderRadius: 5 }] },
      options: { ...this.chartOptions(), indexAxis: 'y' } });
    this.draw('aml', this.amlCanvas, { type: 'line', data: { labels: data.amlCaseTrend.map(item => item.label),
      datasets: [{ label: 'AML cases', data: data.amlCaseTrend.map(item => item.value), borderColor: '#7257c8',
        backgroundColor: '#7257c820', fill: true, tension: .35 }] }, options: this.chartOptions() });
  }

  private draw(key: string, canvas: ElementRef<HTMLCanvasElement>, config: ChartConfiguration): void {
    this.charts.get(key)?.destroy();
    this.charts.set(key, new Chart(canvas.nativeElement, config));
  }

  private chartOptions(): ChartConfiguration['options'] {
    return { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom', labels: { boxWidth: 9, usePointStyle: true } } },
      scales: { x: { grid: { display: false }, ticks: { font: { size: 10 } } }, y: { beginAtZero: true, grid: { color: '#edf0f5' }, ticks: { font: { size: 10 } } } } };
  }

  private pretty(value: string): string {
    return value.toLowerCase().split('_').map(part => part[0]?.toUpperCase() + part.slice(1)).join(' ');
  }
}
