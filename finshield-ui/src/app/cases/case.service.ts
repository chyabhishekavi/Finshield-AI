import{HttpClient,HttpParams}from'@angular/common/http';import{inject,Injectable}from'@angular/core';import{map}from'rxjs';import{environment}from'../../environments/environment';import{ApiResponse,PageResponse}from'../core/models/api-response.model';import{CaseEvidence,CaseHistory,CaseNote,InvestigationCase}from'./case.models';
@Injectable({providedIn:'root'})export class CaseService{private readonly http=inject(HttpClient);private readonly url=`${environment.apiBaseUrl}/cases`;
list(page=0,size=20){return this.http.get<ApiResponse<PageResponse<InvestigationCase>>>(this.url,{params:new HttpParams().set('page',page).set('size',size)}).pipe(map(r=>r.data))}
get(id:string){return this.http.get<ApiResponse<InvestigationCase>>(`${this.url}/${id}`).pipe(map(r=>r.data))}notes(id:string){return this.http.get<ApiResponse<CaseNote[]>>(`${this.url}/${id}/notes`).pipe(map(r=>r.data))}
evidence(id:string){return this.http.get<ApiResponse<CaseEvidence[]>>(`${this.url}/${id}/evidence`).pipe(map(r=>r.data))}history(id:string){return this.http.get<ApiResponse<CaseHistory[]>>(`${this.url}/${id}/status-history`).pipe(map(r=>r.data))}
addNote(id:string,noteText:string,internalOnly:boolean){return this.http.post<ApiResponse<CaseNote>>(`${this.url}/${id}/notes`,{noteText,internalOnly}).pipe(map(r=>r.data))}
addEvidence(id:string,body:object){return this.http.post<ApiResponse<CaseEvidence>>(`${this.url}/${id}/evidence`,body).pipe(map(r=>r.data))}
status(id:string,status:string){return this.http.patch<ApiResponse<InvestigationCase>>(`${this.url}/${id}/status`,{status}).pipe(map(r=>r.data))}
close(id:string,decision:string,rationale:string){return this.http.patch<ApiResponse<InvestigationCase>>(`${this.url}/${id}/close`,{decision,rationale}).pipe(map(r=>r.data))}}
