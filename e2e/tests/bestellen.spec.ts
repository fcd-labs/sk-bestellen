import { expect, test, type Page } from '@playwright/test';

/**
 * The scenarios of the intent Bestellen (sk-1dp-mvp0.md), end to end:
 * browser -> frontend -> backend -> stubs of kitchen, bar and cash register.
 */

const stubs = {
  kitchen: process.env.KITCHEN_STUB_URL ?? 'http://stub-kitchen:8080',
  bar: process.env.BAR_STUB_URL ?? 'http://stub-bar:8080',
  'cash-register': process.env.CASH_REGISTER_STUB_URL ?? 'http://stub-cash-register:8080',
};

interface PlacedOrder {
  order: { orderId: string; tableNumber: number; [key: string]: unknown };
  deliveries: { receiver: string; status: string }[];
}

/** The guest picks items, enters a table number and places the order. Returns the API response. */
async function placeOrder(page: Page, tableNumber: number, picks: Record<string, number>): Promise<PlacedOrder> {
  await page.goto('/');
  for (const [name, quantity] of Object.entries(picks)) {
    for (let i = 0; i < quantity; i++) {
      await page.getByRole('button', { name: `${name} meer`, exact: true }).click();
    }
  }
  await page.getByLabel('Tafelnummer').fill(String(tableNumber));

  const response = page.waitForResponse(
    (r) => r.url().endsWith('/api/orders') && r.request().method() === 'POST',
  );
  await page.getByRole('button', { name: 'Bestelling plaatsen' }).click();
  return (await response).json();
}

test.describe('Intent Bestellen', () => {
  // The core path only: the guest places an order and gets a confirmation. Where the order arrives is checked
  // by the scenarios below. In the demo only this test runs (explain in the configuration of the showcase).
  test('core path: the guest places an order @kernpad', async ({ page }) => {
    await placeOrder(page, 3, { Tomatensoep: 1 });

    await expect(page.getByTestId('result')).toContainText('Bestelling geplaatst voor tafel 3');
  });

  test('scenario 1: the guest gets a menu to choose from, chooses, and the order goes to kitchen, bar and cash register', async ({ page }) => {
    await page.goto('/');
    for (const category of ['Voorgerechten', 'Hoofdgerechten', 'Desserts', 'Dranken']) {
      await expect(page.getByRole('heading', { name: category })).toBeVisible();
    }

    const placed = await placeOrder(page, 5, { Tomatensoep: 1, Cola: 2 });

    expect(placed.deliveries).toEqual([
      expect.objectContaining({ receiver: 'kitchen', status: 'delivered' }),
      expect.objectContaining({ receiver: 'bar', status: 'delivered' }),
      expect.objectContaining({ receiver: 'cash-register', status: 'delivered' }),
    ]);
  });

  test('scenario 2: given a menu, when the guest chooses and gives a table number, the order with that table number reaches kitchen, bar and cash register', async ({ page, request }) => {
    const tableNumber = 100 + Math.floor(Math.random() * 800);

    const placed = await placeOrder(page, tableNumber, { Biefstuk: 1, 'Bier van de tap': 2 });

    for (const [receiver, url] of Object.entries(stubs)) {
      const received: { orderId: string }[] = await (await request.get(`${url}/orders`)).json();
      const order = received.find((o) => o.orderId === placed.order.orderId);
      expect(order, `${receiver} did not receive the order`).toEqual(placed.order);
      expect(placed.order.tableNumber).toBe(tableNumber);
    }
  });

  test('scenario 3: given a placed order, the guest sees that it was sent to the stubs of kitchen, bar and cash register', async ({ page }) => {
    await placeOrder(page, 9, { 'Dame blanche': 1 });

    await expect(page.getByTestId('result')).toContainText('Bestelling geplaatst voor tafel 9');
    for (const [receiver, name] of [['kitchen', 'Keuken'], ['bar', 'Bar'], ['cash-register', 'Kassa']]) {
      const delivery = page.getByTestId(`delivery-${receiver}`);
      await expect(delivery).toContainText(name);
      await expect(delivery).toContainText('verstuurd');
    }
  });
});
