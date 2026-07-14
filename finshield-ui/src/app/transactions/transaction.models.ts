export type RiskBand = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type TransactionStatus = 'RECEIVED' | 'PROCESSING' | 'PENDING_REVIEW' | 'AUTHORIZED'
  | 'DECLINED' | 'COMPLETED' | 'FAILED' | 'REVERSED' | 'CANCELLED';
export type TransactionChannel = 'MOBILE_BANKING' | 'INTERNET_BANKING' | 'ATM' | 'POS'
  | 'BRANCH' | 'API';

export interface TransactionRecord {
  id: string;
  transactionReference: string;
  sourceAccountId: string;
  customerNumber: string;
  customerName: string;
  maskedSourceAccountNumber: string;
  maskedDestinationAccountNumber: string;
  beneficiaryName: string;
  amount: number;
  currency: string;
  transactionType: string;
  channel: TransactionChannel;
  status: TransactionStatus;
  deviceId: string | null;
  ipAddress: string | null;
  geoLocation: string | null;
  initiatedAt: string;
  riskScore: number;
  riskBand: RiskBand;
  decision: string;
  createdAt: string;
  updatedAt: string;
}

export interface TransactionFilters {
  query: string;
  riskBand: RiskBand | '';
  status: TransactionStatus | '';
  channel: TransactionChannel | '';
  minAmount: number | null;
  maxAmount: number | null;
  fromDate: Date | null;
  toDate: Date | null;
}

export interface RiskExplanation {
  transactionId: string;
  ruleScore: number;
  mlScore: number;
  customerRiskScore: number;
  deviceRiskScore: number;
  amlScore: number;
  finalScore: number;
  riskBand: RiskBand;
  decision: string;
  explanationSummary: string;
  mlFallbackUsed: boolean;
  scoredAt: string;
  matchedRules: Array<{
    ruleCode: string;
    ruleName: string;
    scoreImpact: number;
    severity: string;
    reason: string;
  }>;
}
