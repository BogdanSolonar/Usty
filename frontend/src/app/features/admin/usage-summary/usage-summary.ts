import { Component, input } from '@angular/core';
import { AdminOverview } from '../../../core/models/admin.model';

// Короткі лічильники по всій системі над списком пар: пари, входи, Telegram і скільки створено записів
@Component({
  selector: 'app-usage-summary',
  templateUrl: './usage-summary.html',
  styleUrl: './usage-summary.css',
})
export class UsageSummary {
  overview = input.required<AdminOverview>();
}
