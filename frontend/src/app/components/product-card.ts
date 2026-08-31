import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Product } from '../models/models';

@Component({
  selector: 'app-product-card',
  imports: [RouterLink],
  styleUrl: './product-card.css',
  templateUrl: './product-card.html',
})
export class ProductCard {
  readonly product = input.required<Product>();
  readonly showActions = input(false);
  readonly addToCart = output<Product>();
  readonly edit = output<Product>();

  priceLabel(): string {
    return (this.product()?.price ?? 0).toFixed(2);
  }
}