import { Routes } from '@angular/router';
import { CaseListComponent } from './case-list.component';import { CaseDetailComponent } from './case-detail.component';
export default [{path:'',component:CaseListComponent,data:{title:'Investigation Cases'}},{path:':id',component:CaseDetailComponent,data:{title:'Case Investigation'}}] satisfies Routes;
