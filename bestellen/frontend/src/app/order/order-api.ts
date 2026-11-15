import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Menu, PlacedOrder, PlaceOrderRequest } from './order.model';

/** Client for the Bestellen API. nginx forwards /api to the backend. */
@Injectable({ providedIn: 'root' })
export class OrderApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api';

  getMenu(): Observable<Menu> {
    return this.http.get<Menu>(`${this.baseUrl}/menu`);
  }

  placeOrder(request: PlaceOrderRequest): Observable<PlacedOrder> {
    return this.http.post<PlacedOrder>(`${this.baseUrl}/orders`, request);
  }
}
