export type RoleName =
  | 'ADMIN'
  | 'FRAUD_ANALYST'
  | 'AML_INVESTIGATOR'
  | 'COMPLIANCE_OFFICER'
  | 'RISK_MANAGER';

export interface AuthUser {
  id: string;
  fullName: string;
  email: string;
  status: 'PENDING_ACTIVATION' | 'ACTIVE' | 'SUSPENDED' | 'LOCKED' | 'DISABLED';
  lastLoginAt: string | null;
  roles: RoleName[];
}

export interface AuthResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresInSeconds: number;
  user: AuthUser;
}
