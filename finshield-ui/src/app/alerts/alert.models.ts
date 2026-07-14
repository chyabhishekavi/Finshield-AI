export type AlertStatus = 'NEW'|'ASSIGNED'|'IN_REVIEW'|'ESCALATED'|'CLOSED_FRAUD'|'CLOSED_FALSE_POSITIVE'|'CONVERTED_TO_CASE';
export type AlertSeverity = 'LOW'|'MEDIUM'|'HIGH'|'CRITICAL';
export interface FraudAlert { id:string; alertNumber:string; transactionId:string; transactionReference:string;
  customerId:string; customerNumber:string; customerName:string; riskScore:number; riskBand:string;
  severity:AlertSeverity; status:AlertStatus; assignedToId:string|null; assignedToName:string|null;
  createdAt:string; updatedAt:string; dueAt:string; overdue:boolean; alertSummary:string; version:number; }
export interface AlertAssignee { id:string; fullName:string; email:string; }
