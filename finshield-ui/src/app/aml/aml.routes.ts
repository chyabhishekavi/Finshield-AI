import { Routes } from '@angular/router';
export default [{
  path: '',
  loadComponent: () => import('./aml-review.component').then(module => module.AmlReviewComponent),
}] satisfies Routes;
