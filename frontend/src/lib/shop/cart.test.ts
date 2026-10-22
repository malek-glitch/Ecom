import { describe, expect, it } from 'vitest';
import { Cart, MAX_LINE_QUANTITY, type CartItem, type CartStorage } from './cart.svelte';

class MemoryCartStorage implements CartStorage {
	items: CartItem[] = [];
	load = () => structuredClone(this.items);
	save = (items: CartItem[]) => void (this.items = structuredClone(items));
	subscribe = () => () => undefined;
}

const mug = { productId: 'p1', slug: 'mug', name: 'Mug', priceCents: 1200 };
const pen = { productId: 'p2', slug: 'pen', name: 'Pen', priceCents: 300 };

describe('Cart', () => {
	it('adds, merges and counts lines, and persists them', () => {
		const storage = new MemoryCartStorage();
		const cart = new Cart(storage);
		cart.add(mug);
		cart.add(mug, 2);
		cart.add(pen, 4);
		expect(cart.count).toBe(7);
		expect(cart.quantityOf('p1')).toBe(3);
		expect(storage.items).toHaveLength(2);
		expect(cart.requestItems).toEqual([
			{ productId: 'p1', quantity: 3 },
			{ productId: 'p2', quantity: 4 },
		]);
	});

	it('caps quantities and removes a line set to zero', () => {
		const cart = new Cart(new MemoryCartStorage());
		cart.add(mug, 500);
		expect(cart.quantityOf('p1')).toBe(MAX_LINE_QUANTITY);
		cart.setQuantity('p1', 0);
		expect(cart.isEmpty).toBe(true);
	});

	it('refreshes names and prices and drops unavailable products', () => {
		const storage = new MemoryCartStorage();
		const cart = new Cart(storage);
		cart.add(mug);
		cart.add(pen);
		cart.refresh([{ productId: 'p1', slug: 'mug', name: 'Big Mug', unitPriceCents: 1500, imageUrl: null }]);
		cart.removeMany(['p2']);
		expect(cart.items).toEqual([{ ...mug, name: 'Big Mug', priceCents: 1500, imageUrl: null, quantity: 1 }]);
		expect(storage.items[0].name).toBe('Big Mug');
	});
});
