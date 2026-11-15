import { Component, computed, inject, signal } from '@angular/core';
import { OrderApi } from './order/order-api';
import { Menu, MenuItem, PlacedOrder, Receiver } from './order/order.model';

const receiverNames: Record<Receiver, string> = {
  kitchen: 'Keuken',
  bar: 'Bar',
  'cash-register': 'Kassa',
};

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly api = inject(OrderApi);

  protected readonly menu = signal<Menu | null>(null);
  protected readonly menuError = signal(false);
  protected readonly quantities = signal<Record<string, number>>({});
  protected readonly tableNumber = signal<number | null>(null);
  protected readonly placing = signal(false);
  protected readonly placeError = signal(false);
  protected readonly placed = signal<PlacedOrder | null>(null);

  private readonly items = computed(() => this.menu()?.categories.flatMap((c) => c.items) ?? []);

  protected readonly chosenLines = computed(() =>
    this.items()
      .filter((item) => (this.quantities()[item.id] ?? 0) > 0)
      .map((item) => ({ menuItemId: item.id, quantity: this.quantities()[item.id] })),
  );

  protected readonly totalCents = computed(() =>
    this.items().reduce((sum, item) => sum + (this.quantities()[item.id] ?? 0) * item.priceCents, 0),
  );

  protected readonly tableValid = computed(() => {
    const table = this.tableNumber();
    return table !== null && Number.isInteger(table) && table >= 1 && table <= 999;
  });

  protected readonly canPlace = computed(
    () => this.tableValid() && this.chosenLines().length > 0 && !this.placing(),
  );

  constructor() {
    this.api.getMenu().subscribe({
      next: (menu) => this.menu.set(menu),
      error: () => this.menuError.set(true),
    });
  }

  protected quantity(item: MenuItem): number {
    return this.quantities()[item.id] ?? 0;
  }

  protected add(item: MenuItem): void {
    this.changeQuantity(item, 1);
  }

  protected remove(item: MenuItem): void {
    this.changeQuantity(item, -1);
  }

  protected onTableInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value.trim();
    this.tableNumber.set(value === '' ? null : Number(value));
  }

  protected placeOrder(): void {
    if (!this.canPlace()) return;
    this.placing.set(true);
    this.placeError.set(false);
    this.api.placeOrder({ tableNumber: this.tableNumber()!, lines: this.chosenLines() }).subscribe({
      next: (placed) => {
        this.placed.set(placed);
        this.placing.set(false);
      },
      error: () => {
        this.placeError.set(true);
        this.placing.set(false);
      },
    });
  }

  protected newOrder(): void {
    this.placed.set(null);
    this.quantities.set({});
    this.placeError.set(false);
  }

  protected receiverName(receiver: Receiver): string {
    return receiverNames[receiver];
  }

  protected euro(cents: number): string {
    return (cents / 100).toLocaleString('nl-NL', { style: 'currency', currency: 'EUR' });
  }

  private changeQuantity(item: MenuItem, delta: number): void {
    const next = Math.min(99, Math.max(0, this.quantity(item) + delta));
    this.quantities.update((q) => ({ ...q, [item.id]: next }));
  }
}
