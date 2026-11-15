import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { firstValueFrom } from 'rxjs';
import { OrderApi } from './order-api';
import { Menu, PlacedOrder } from './order.model';

describe('OrderApi', () => {
  let api: OrderApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(OrderApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('gets the menu from /api/menu', async () => {
    const menu: Menu = { version: '0.1.0', currency: 'EUR', categories: [] };

    const result = firstValueFrom(api.getMenu());
    const request = http.expectOne('/api/menu');
    expect(request.request.method).toBe('GET');
    request.flush(menu);

    expect(await result).toEqual(menu);
  });

  it('places an order with POST /api/orders', async () => {
    const placed = { order: { totalCents: 650 }, deliveries: [] } as unknown as PlacedOrder;

    const result = firstValueFrom(
      api.placeOrder({ tableNumber: 12, lines: [{ menuItemId: 'tomato-soup', quantity: 1 }] }),
    );
    const request = http.expectOne('/api/orders');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      tableNumber: 12,
      lines: [{ menuItemId: 'tomato-soup', quantity: 1 }],
    });
    request.flush(placed, { status: 201, statusText: 'Created' });

    expect(await result).toEqual(placed);
  });
});
