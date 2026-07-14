import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse, PageResponse } from '../core/models/api-response.model';
import { AlertAssignee, AlertStatus, FraudAlert } from './alert.models';

@Injectable({providedIn:'root'})
export class AlertService {
  private readonly http=inject(HttpClient); private readonly url=`${environment.apiBaseUrl}/fraud/alerts`;
  list(status:AlertStatus|'',page:number,size:number,sortDirection:'ASC'|'DESC') {
    let params=new HttpParams().set('page',page).set('size',size).set('sortBy','riskScore').set('sortDirection',sortDirection);
    if(status) params=params.set('status',status);
    return this.http.get<ApiResponse<PageResponse<FraudAlert>>>(this.url,{params}).pipe(map(r=>r.data));
  }
  assignees(){return this.http.get<ApiResponse<AlertAssignee[]>>(`${this.url}/assignees`).pipe(map(r=>r.data));}
  assign(id:string,assignedTo:string,dueAt:string|null){return this.patch(id,'assignment',{assignedTo,dueAt});}
  updateStatus(id:string,status:AlertStatus){return this.patch(id,'status',{status});}
  close(id:string,status:'CLOSED_FRAUD'|'CLOSED_FALSE_POSITIVE',resolution:string){return this.patch(id,'close',{status,resolution});}
  private patch(id:string,path:string,body:object){return this.http.patch<ApiResponse<FraudAlert>>(`${this.url}/${id}/${path}`,body).pipe(map(r=>r.data));}
}
