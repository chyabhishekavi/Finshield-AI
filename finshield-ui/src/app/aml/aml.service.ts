import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { forkJoin, map } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse, PageResponse } from '../core/models/api-response.model';
import { InvestigationCase } from '../cases/case.models';
import { AmlScreening, WatchlistEntry } from './aml.models';

@Injectable({ providedIn: 'root' })
export class AmlService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  reviewQueue() {
    const types = ['AML', 'MULE_ACCOUNT', 'SANCTIONS_MATCH'];
    return forkJoin(types.map(type => this.http.get<ApiResponse<PageResponse<InvestigationCase>>>(
      `${this.base}/cases`, { params: new HttpParams().set('type', type).set('size', 100) }
    ).pipe(map(response => response.data.content)))).pipe(
      map(groups => groups.flat().sort((a, b) => b.createdAt.localeCompare(a.createdAt)))
    );
  }

  watchlist(query = '') {
    let params = new HttpParams().set('active', true).set('size', 100);
    if (query.trim()) params = params.set('query', query.trim());
    return this.http.get<ApiResponse<PageResponse<WatchlistEntry>>>(`${this.base}/aml/watchlist`, { params })
      .pipe(map(response => response.data.content));
  }

  screenCustomer(id: string) {
    return this.http.post<ApiResponse<AmlScreening>>(`${this.base}/aml/screenings/customers/${id}`, {})
      .pipe(map(response => response.data));
  }

  screenBeneficiary(id: string) {
    return this.http.post<ApiResponse<AmlScreening>>(`${this.base}/aml/screenings/beneficiaries/${id}`, {})
      .pipe(map(response => response.data));
  }

  escalate(caseId: string) {
    return this.http.patch<ApiResponse<InvestigationCase>>(`${this.base}/cases/${caseId}/status`, {
      status: 'ESCALATED_TO_COMPLIANCE',
    }).pipe(map(response => response.data));
  }
}
