import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { AlertAssignee } from './alert.models';

export interface AlertDialogData { mode:'assign'|'status'|'close'; assignees?:AlertAssignee[]; }
@Component({selector:'app-alert-action-dialog',standalone:true,
  imports:[ReactiveFormsModule,MatDialogModule,MatButtonModule,MatFormFieldModule,MatInputModule,MatSelectModule],
  template:`<h2 mat-dialog-title>{{data.mode==='assign'?'Assign alert':data.mode==='status'?'Update status':'Close alert'}}</h2>
  <mat-dialog-content><form [formGroup]="form">
    @if(data.mode==='assign'){<mat-form-field appearance="outline"><mat-label>Analyst</mat-label><mat-select formControlName="assignedTo">
      @for(user of data.assignees ?? [];track user.id){<mat-option [value]="user.id">{{user.fullName}} · {{user.email}}</mat-option>}</mat-select></mat-form-field>}
    @if(data.mode==='status'){<mat-form-field appearance="outline"><mat-label>Next status</mat-label><mat-select formControlName="status">
      <mat-option value="IN_REVIEW">In review</mat-option></mat-select></mat-form-field>}
    @if(data.mode==='close'){<mat-form-field appearance="outline"><mat-label>Outcome</mat-label><mat-select formControlName="status">
      <mat-option value="CLOSED_FRAUD">Confirmed fraud</mat-option><mat-option value="CLOSED_FALSE_POSITIVE">False positive</mat-option></mat-select></mat-form-field>
      <mat-form-field appearance="outline"><mat-label>Resolution rationale</mat-label><textarea matInput rows="4" formControlName="resolution"></textarea></mat-form-field>}
  </form></mat-dialog-content><mat-dialog-actions align="end"><button mat-button mat-dialog-close>Cancel</button>
    <button mat-flat-button color="primary" [disabled]="form.invalid" (click)="save()">Confirm</button></mat-dialog-actions>`,
  styles:[`form{display:grid;min-width:min(430px,75vw);padding-top:8px}mat-form-field{width:100%}`]})
export class AlertActionDialogComponent{
  readonly data=inject<AlertDialogData>(MAT_DIALOG_DATA); private readonly ref=inject(MatDialogRef<AlertActionDialogComponent>); private readonly fb=inject(FormBuilder);
  readonly form=this.fb.group({assignedTo:['',this.data.mode==='assign'?Validators.required:[]],status:['',this.data.mode!=='assign'?Validators.required:[]],resolution:['',this.data.mode==='close'?Validators.required:[]]});
  save(){if(this.form.valid)this.ref.close(this.form.getRawValue());}
}
