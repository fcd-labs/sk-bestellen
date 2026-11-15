// Types of the Bestellen API (bestellen/contracts/bestellen-api.openapi.yaml)
// and the menu data model (bestellen/menu/menu.schema.json).

export type Station = 'kitchen' | 'bar';
export type Receiver = 'kitchen' | 'bar' | 'cash-register';
export type DeliveryStatus = 'delivered' | 'failed';

export interface Menu {
  version: string;
  currency: 'EUR';
  categories: Category[];
}

export interface Category {
  id: string;
  name: string;
  items: MenuItem[];
}

export interface MenuItem {
  id: string;
  name: string;
  description?: string;
  priceCents: number;
  station: Station;
}

export interface PlaceOrderRequest {
  tableNumber: number;
  lines: PlaceOrderLine[];
}

export interface PlaceOrderLine {
  menuItemId: string;
  quantity: number;
}

export interface PlacedOrder {
  order: Order;
  deliveries: Delivery[];
}

export interface Order {
  orderId: string;
  placedAt: string;
  tableNumber: number;
  currency: 'EUR';
  lines: OrderLine[];
  totalCents: number;
}

export interface OrderLine {
  menuItemId: string;
  name: string;
  station: Station;
  quantity: number;
  unitPriceCents: number;
  lineTotalCents: number;
}

export interface Delivery {
  receiver: Receiver;
  status: DeliveryStatus;
  receivedAt?: string;
}
