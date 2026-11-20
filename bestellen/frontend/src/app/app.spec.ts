import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { App } from './app';
import { Menu, PlacedOrder } from './order/order.model';

const menu: Menu = {
  version: '0.1.0',
  currency: 'EUR',
  categories: [
    {
      id: 'starters',
      name: 'Voorgerechten',
      items: [
        {
          id: 'tomato-soup',
          name: 'Tomatensoep',
          description: 'Met basilicum',
          priceCents: 650,
          station: 'kitchen',
        },
      ],
    },
    {
      id: 'drinks',
      name: 'Dranken',
      items: [{ id: 'draught-beer', name: 'Bier van de tap', priceCents: 380, station: 'bar' }],
    },
  ],
};

function placedOrder(barStatus: 'delivered' | 'failed'): PlacedOrder {
  return {
    order: {
      orderId: '3f2b8c1e-6a4d-4e7b-9c0a-1d2e3f4a5b6c',
      placedAt: '2026-11-15T19:42:00Z',
      tableNumber: 12,
      currency: 'EUR',
      lines: [
        {
          menuItemId: 'tomato-soup',
          name: 'Tomatensoep',
          station: 'kitchen',
          quantity: 2,
          unitPriceCents: 650,
          lineTotalCents: 1300,
        },
        {
          menuItemId: 'draught-beer',
          name: 'Bier van de tap',
          station: 'bar',
          quantity: 1,
          unitPriceCents: 380,
          lineTotalCents: 380,
        },
      ],
      totalCents: 1680,
    },
    deliveries: [
      { receiver: 'kitchen', status: 'delivered', receivedAt: '2026-11-15T19:42:01Z' },
      barStatus === 'delivered'
        ? { receiver: 'bar', status: 'delivered', receivedAt: '2026-11-15T19:42:01Z' }
        : { receiver: 'bar', status: 'failed' },
      { receiver: 'cash-register', status: 'delivered', receivedAt: '2026-11-15T19:42:01Z' },
    ],
  };
}

describe('App (bestellen)', () => {
  let fixture: ComponentFixture<App>;
  let http: HttpTestingController;
  let page: HTMLElement;

  const byTestId = (id: string) => page.querySelector<HTMLElement>(`[data-testid="${id}"]`);
  const click = async (id: string) => {
    byTestId(id)!.click();
    await render();
  };
  const render = async () => {
    fixture.detectChanges();
    await fixture.whenStable();
  };
  const enterTable = async (value: string) => {
    const input = byTestId('table-number') as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    await render();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(App);
    http = TestBed.inject(HttpTestingController);
    page = fixture.nativeElement;
    await render();
    http.expectOne('/api/menu').flush(menu);
    await render();
  });

  afterEach(() => http.verify());

  it('shows the menu per category with prices', () => {
    expect(page.textContent).toContain('Voorgerechten');
    expect(page.textContent).toContain('Dranken');
    expect(byTestId('item-tomato-soup')!.textContent).toContain('Tomatensoep');
    expect(byTestId('item-tomato-soup')!.textContent).toContain('6,50');
  });

  it('only allows placing an order with a table number and at least one item', async () => {
    const button = () => byTestId('place-order') as HTMLButtonElement;
    expect(button().disabled).toBe(true);

    await click('add-tomato-soup');
    expect(button().disabled).toBe(true);

    await enterTable('12');
    expect(button().disabled).toBe(false);

    await enterTable('1000');
    expect(button().disabled).toBe(true);
  });

  it('lets the guest change quantities', async () => {
    await click('add-tomato-soup');
    await click('add-tomato-soup');
    await click('remove-tomato-soup');

    expect(byTestId('quantity-tomato-soup')!.textContent?.trim()).toBe('1');
  });

  it('sends the table number and chosen items, then shows that the order went to kitchen, bar and cash register', async () => {
    await click('add-tomato-soup');
    await click('add-tomato-soup');
    await click('add-draught-beer');
    await enterTable('12');

    await click('place-order');
    const request = http.expectOne('/api/orders');
    expect(request.request.body).toEqual({
      tableNumber: 12,
      lines: [
        { menuItemId: 'tomato-soup', quantity: 2 },
        { menuItemId: 'draught-beer', quantity: 1 },
      ],
    });
    request.flush(placedOrder('delivered'), { status: 201, statusText: 'Created' });
    await render();

    const result = byTestId('result')!;
    expect(result.textContent).toContain('tafel 12');
    expect(result.textContent).toContain('16,80');
    expect(byTestId('delivery-kitchen')!.textContent).toContain('Keuken');
    expect(byTestId('delivery-kitchen')!.textContent).toContain('verstuurd');
    expect(byTestId('delivery-bar')!.textContent).toContain('Bar');
    expect(byTestId('delivery-cash-register')!.textContent).toContain('Kassa');
  });

  it('shows when the order did not reach a receiver', async () => {
    await click('add-tomato-soup');
    await enterTable('12');
    await click('place-order');
    http
      .expectOne('/api/orders')
      .flush(placedOrder('failed'), { status: 201, statusText: 'Created' });
    await render();

    expect(byTestId('delivery-bar')!.textContent).toContain('niet gelukt');
    expect(byTestId('delivery-kitchen')!.textContent).toContain('verstuurd');
  });

  it('shows an error when placing the order fails', async () => {
    await click('add-tomato-soup');
    await enterTable('12');
    await click('place-order');
    http
      .expectOne('/api/orders')
      .flush({ status: 400 }, { status: 400, statusText: 'Bad Request' });
    await render();

    expect(byTestId('error')!.textContent).toContain('niet gelukt');
    expect(byTestId('result')).toBeNull();
  });

  it('starts a new order after placing one', async () => {
    await click('add-tomato-soup');
    await enterTable('12');
    await click('place-order');
    http
      .expectOne('/api/orders')
      .flush(placedOrder('delivered'), { status: 201, statusText: 'Created' });
    await render();

    await click('new-order');

    expect(byTestId('result')).toBeNull();
    expect(byTestId('quantity-tomato-soup')!.textContent?.trim()).toBe('0');
  });
});
