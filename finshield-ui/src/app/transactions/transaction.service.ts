import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse, PageResponse } from '../core/models/api-response.model';
import { RiskExplanation, TransactionFilters, TransactionRecord } from './transaction.models';

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/transactions`;

  search(filters: TransactionFilters, page: number, size: number) {
    let params = new HttpParams().set('page', page).set('size', size);
    const values: Record<string, string | number | null | undefined> = {
      query: filters.query.trim() || null,
      riskBand: filters.riskBand || null,
      status: filters.status || null,
      channel: filters.channel || null,
      minAmount: filters.minAmount,
      maxAmount: filters.maxAmount,
      fromTime: filters.fromDate?.toISOString(),
      toTime: filters.toDate ? this.endOfDay(filters.toDate).toISOString() : null,
    };
    Object.entries(values).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== '') params = params.set(key, value);
    });
    return this.http.get<ApiResponse<PageResponse<TransactionRecord>>>(this.baseUrl, { params })
      .pipe(map(response => response.data));
  }

  getRiskExplanation(transactionId: string) {
    return this.http.get<ApiResponse<RiskExplanation>>(`${this.baseUrl}/${transactionId}/risk-explanation`)
      .pipe(map(response => response.data));
  }

  private endOfDay(value: Date): Date {
    const result = new Date(value);
    result.setHours(23, 59, 59, 999);
    return result;
  }
}
