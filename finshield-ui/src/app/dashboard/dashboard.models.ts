export interface DashboardOverview {
  transactionsMonitoredToday: number;
  alertsGeneratedToday: number;
  criticalAlerts: number;
  openCases: number;
  falsePositiveRate: number;
  averageInvestigationHours: number;
}

export interface TimeSeriesPoint {
  label: string;
  value: number;
}

export interface RiskTrendPoint {
  label: string;
  low: number;
  medium: number;
  high: number;
  critical: number;
}

export interface StatusDistributionPoint {
  label: string;
  value: number;
}

export interface TopRulePoint {
  ruleCode: string;
  ruleName: string;
  matchCount: number;
  totalScoreImpact: number;
}

export interface DashboardSnapshot {
  overview: DashboardOverview;
  riskTrend: RiskTrendPoint[];
  alertStatusDistribution: StatusDistributionPoint[];
  topFraudRules: TopRulePoint[];
  amlCaseTrend: TimeSeriesPoint[];
}
