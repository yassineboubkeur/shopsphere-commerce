import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-policy',
  imports: [RouterLink],
  styleUrl: './policy.css',
  templateUrl: './policy.html',
})
export class PolicyComponent {
  protected readonly year = new Date().getFullYear();
}
