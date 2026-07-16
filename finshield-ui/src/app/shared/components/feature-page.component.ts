import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-feature-page',
  standalone: true,
  template: `
    <section class="page">
      <h1 class="page-title">{{ title }}</h1>
      <div class="card">This workspace is ready for its feature implementation.</div>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeaturePageComponent {
  readonly title = inject(ActivatedRoute).snapshot.data['title'] as string;
}
