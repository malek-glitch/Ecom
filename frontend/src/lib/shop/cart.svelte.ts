import { browser } from '$app/environment';
import { APP_SLUG } from '$lib/config';

export const CART_STORAGE_KEY = `${APP_SLUG}.cart`;

/** The backend's limits (OrderDtos.kt). */
export const MAX_CART_LINES = 50;
export const MAX_LINE_QUANTITY = 99;

/** What the cart remembers about a product, so it renders at once; prices are re-checked by the API. */
export interface CartProduct {
	productId: string;
	slug: string;
	name: string;
	priceCents: number;
	imageUrl?: string | null;
}

export interface CartItem extends CartProduct {
	quantity: number;
}

export interface CartStorage {
	load(): CartItem[];
	save(items: CartItem[]): void;
	subscribe(listener: () => void): () => void;
}

function isCartItem(value: unknown): value is CartItem {
	const item = value as CartItem;
	return (
		typeof item?.productId === 'string' &&
		typeof item.slug === 'string' &&
		typeof item.name === 'string' &&
		typeof item.priceCents === 'number' &&
		Number.isInteger(item.quantity) &&
		item.quantity > 0
	);
}

export class LocalCartStorage implements CartStorage {
	constructor(private readonly key = CART_STORAGE_KEY) {}

	load(): CartItem[] {
		try {
			const parsed: unknown = JSON.parse(localStorage.getItem(this.key) ?? '[]');
			return Array.isArray(parsed) ? parsed.filter(isCartItem) : [];
		} catch {
			return [];
		}
	}

	save(items: CartItem[]) {
		try {
			if (items.length) localStorage.setItem(this.key, JSON.stringify(items));
			else localStorage.removeItem(this.key);
		} catch {
			// Storage full or blocked (private mode): the cart still works for this page view.
		}
	}

	subscribe(listener: () => void) {
		const handler = (event: StorageEvent) => {
			if (event.key === this.key) listener();
		};
		window.addEventListener('storage', handler);
		return () => window.removeEventListener('storage', handler);
	}
}

/**
 * The shopping cart, as reactive state kept in the browser (no account needed). Other tabs
 * see changes through the storage event. The server never trusts these prices: the cart page
 * and checkout ask `POST /api/v1/public/shop/cart/quote` for the real ones.
 */
export class Cart {
	items = $state<CartItem[]>([]);

	readonly count = $derived(this.items.reduce((sum, item) => sum + item.quantity, 0));
	readonly isEmpty = $derived(this.items.length === 0);

	#storage: CartStorage | null;
	#loaded = false;

	constructor(storage: CartStorage | null) {
		this.#storage = storage;
	}

	/** Reads the stored cart once (in the browser). Safe to call repeatedly. */
	load() {
		if (this.#loaded || !this.#storage) return;
		this.#loaded = true;
		this.items = this.#storage.load();
		this.#storage.subscribe(() => (this.items = this.#storage!.load()));
	}

	quantityOf(productId: string) {
		return this.items.find((item) => item.productId === productId)?.quantity ?? 0;
	}

	/** Adds to the line for this product (or creates it). Returns false when the cart is full. */
	add(product: CartProduct, quantity = 1): boolean {
		this.load();
		const existing = this.items.find((item) => item.productId === product.productId);
		if (existing) {
			Object.assign(existing, product); // fresher name and price
			existing.quantity = clamp(existing.quantity + quantity);
		} else {
			if (this.items.length >= MAX_CART_LINES) return false;
			this.items.push({ ...product, quantity: clamp(quantity) });
		}
		this.#save();
		return true;
	}

	setQuantity(productId: string, quantity: number) {
		if (quantity <= 0) return this.remove(productId);
		const item = this.items.find((line) => line.productId === productId);
		if (!item) return;
		item.quantity = clamp(quantity);
		this.#save();
	}

	remove(productId: string) {
		this.items = this.items.filter((item) => item.productId !== productId);
		this.#save();
	}

	/** Drops products the store no longer sells. */
	removeMany(productIds: string[]) {
		if (!productIds.length) return;
		this.items = this.items.filter((item) => !productIds.includes(item.productId));
		this.#save();
	}

	/** Refreshes the remembered names and prices from a quote. */
	refresh(
		lines: { productId: string; name: string; slug: string; unitPriceCents: number; imageUrl?: string | null }[],
	) {
		let changed = false;
		for (const line of lines) {
			const item = this.items.find((entry) => entry.productId === line.productId);
			if (!item) continue;
			if (item.name !== line.name || item.priceCents !== line.unitPriceCents || item.slug !== line.slug) changed = true;
			item.name = line.name;
			item.slug = line.slug;
			item.priceCents = line.unitPriceCents;
			item.imageUrl = line.imageUrl ?? null;
		}
		if (changed) this.#save();
	}

	clear() {
		this.items = [];
		this.#save();
	}

	/** The request body for quotes and checkout. */
	get requestItems() {
		return this.items.map(({ productId, quantity }) => ({ productId, quantity }));
	}

	#save() {
		this.#storage?.save($state.snapshot(this.items) as CartItem[]);
	}
}

const clamp = (quantity: number) => Math.min(MAX_LINE_QUANTITY, Math.max(1, Math.floor(quantity)));

export const cart = new Cart(browser ? new LocalCartStorage() : null);
