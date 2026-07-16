import { Routes } from '@angular/router';
import{CustomerListComponent}from'./customer-list.component';import{CustomerDetailComponent}from'./customer-detail.component';
export default[{path:'',component:CustomerListComponent,data:{title:'Customer 360'}},{path:':id',component:CustomerDetailComponent,data:{title:'Customer Profile'}}]satisfies Routes;
