import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, delay, forkJoin, map, of } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse, PageResponse } from '../core/models/api-response.model';
import { DashboardOverview, DashboardSnapshot, RiskTrendPoint,
  StatusDistributionPoint, TimeSeriesPoint, TopRulePoint } from './dashboard.models';

interface SeriesResponse<T> { series: T[]; }
interface TopRulesResponse { rules: TopRulePoint[]; }
interface DashboardSummaryResponse {
  totalTransactions: number;
  totalAlerts: number;
  criticalAlerts: number;
  openCases: number;
}
interface CaseSummary { caseType: string; createdAt: string; }

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/dashboard`;

  snapshot(days = 14): Observable<DashboardSnapshot> {
    return forkJoin({ overview: this.overview(), riskTrend: this.riskTrend(days),
      alertStatusDistribution: this.alertStatusDistribution(), topFraudRules: this.topFraudRules(30, 6),
      amlCaseTrend: this.amlCaseTrend(days) });
  }

  overview(): Observable<DashboardOverview> {
    return this.mockOrApi(MOCK_OVERVIEW,
      this.http.get<ApiResponse<DashboardSummaryResponse>>(`${this.baseUrl}/summary`).pipe(map(({ data }) => ({
        transactionsMonitoredToday: data.totalTransactions,
        alertsGeneratedToday: data.totalAlerts,
        criticalAlerts: data.criticalAlerts,
        openCases: data.openCases,
        falsePositiveRate: 0,
        averageInvestigationHours: 0,
      }))));
  }

  riskTrend(days = 14): Observable<RiskTrendPoint[]> {
    const params = new HttpParams().set('days', days);
    return this.mockOrApi(MOCK_RISK_TREND,
      this.http.get<ApiResponse<SeriesResponse<RiskTrendPoint>>>(`${this.baseUrl}/risk-trends`, { params })
        .pipe(map(value => value.data.series)));
  }

  alertStatusDistribution(): Observable<StatusDistributionPoint[]> {
    return this.mockOrApi(MOCK_ALERT_STATUS,
      this.http.get<ApiResponse<SeriesResponse<StatusDistributionPoint>>>(`${this.baseUrl}/alert-status-count`)
        .pipe(map(value => value.data.series)));
  }

  topFraudRules(days = 30, limit = 6): Observable<TopRulePoint[]> {
    const params = new HttpParams().set('days', days).set('limit', limit);
    return this.mockOrApi(MOCK_TOP_RULES,
      this.http.get<ApiResponse<TopRulesResponse>>(`${this.baseUrl}/top-rules`, { params })
        .pipe(map(value => value.data.rules)));
  }

  amlCaseTrend(days = 14): Observable<TimeSeriesPoint[]> {
    const params = new HttpParams().set('page', 0).set('size', 100);
    return this.mockOrApi(MOCK_AML_TREND,
      this.http.get<ApiResponse<PageResponse<CaseSummary>>>(`${environment.apiBaseUrl}/cases`, { params })
        .pipe(map(value => this.caseTrend(value.data.content, days))));
  }

  private caseTrend(cases: CaseSummary[], days: number): TimeSeriesPoint[] {
    const dates = Array.from({ length: days }, (_, index) => {
      const date = new Date();
      date.setUTCHours(0, 0, 0, 0);
      date.setUTCDate(date.getUTCDate() - (days - index - 1));
      return date;
    });
    return dates.map(date => ({
      label: date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
      value: cases.filter(item => item.caseType === 'AML' && item.createdAt.slice(0, 10) === date.toISOString().slice(0, 10)).length,
    }));
  }

  private mockOrApi<T>(mockValue: T, apiRequest: Observable<T>): Observable<T> {
    return environment.useMockApi ? of(mockValue).pipe(delay(180)) : apiRequest;
  }
}

const labels = Array.from({ length: 14 }, (_, index) => {
  const date = new Date();
  date.setDate(date.getDate() - (13 - index));
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
});

const MOCK_OVERVIEW: DashboardOverview = {
  transactionsMonitoredToday: 128_442, alertsGeneratedToday: 284, criticalAlerts: 17,
  openCases: 93, falsePositiveRate: 12.4, averageInvestigationHours: 6.8,
};

const MOCK_RISK_TREND: RiskTrendPoint[] = labels.map((label, index) => ({
  label, low: 7600 + ((index * 191) % 900), medium: 1280 + ((index * 83) % 420),
  high: 230 + ((index * 37) % 130), critical: 18 + ((index * 11) % 27),
}));

const MOCK_ALERT_STATUS: StatusDistributionPoint[] = [
  { label: 'NEW', value: 41 }, { label: 'ASSIGNED', value: 27 },
  { label: 'IN_REVIEW', value: 36 }, { label: 'ESCALATED', value: 12 },
  { label: 'CLOSED_FRAUD', value: 58 }, { label: 'CLOSED_FALSE_POSITIVE', value: 39 },
];

const MOCK_TOP_RULES: TopRulePoint[] = [
  { ruleCode: 'HIGH_AMOUNT', ruleName: 'High-value transfer', matchCount: 184, totalScoreImpact: 3680 },
  { ruleCode: 'NEW_DEVICE_HIGH_VALUE', ruleName: 'New device and high value', matchCount: 143, totalScoreImpact: 3575 },
  { ruleCode: 'VELOCITY_5_MIN', ruleName: 'Transaction velocity', matchCount: 121, totalScoreImpact: 2420 },
  { ruleCode: 'GEO_LOCATION_MISMATCH', ruleName: 'Geo-location mismatch', matchCount: 96, totalScoreImpact: 1920 },
  { ruleCode: 'FIRST_TIME_BENEFICIARY', ruleName: 'First-time beneficiary', matchCount: 81, totalScoreImpact: 1215 },
  { ruleCode: 'REPEATED_ROUND_AMOUNTS', ruleName: 'Repeated round amounts', matchCount: 64, totalScoreImpact: 960 },
];

const MOCK_AML_TREND: TimeSeriesPoint[] = labels.map((label, index) => ({
  label, value: 8 + ((index * 7) % 16),
}));
