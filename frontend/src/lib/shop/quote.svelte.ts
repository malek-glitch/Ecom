import { toast } from 'svelte-sonner';
import { api, call } from '$lib/api/client';
import { errorMessage } from '$lib/api/errors';
import { m } from '$lib/paraglide/messages';
import { cart } from './cart.svelte';
import type { CartQuote } from './store-api';

const DEBOUNCE_MS = 250;

/**
 * Keeps an up-to-date server quote for the cart: refetched (debounced) whenever products or
 * quantities change. Products the store stopped selling are dropped from the cart, and the
 * remembered names and prices are refreshed. Call it during component initialisation.
 */
export function createCartQuote() {
	let quote = $state<CartQuote | null>(null);
	let loading = $state(true);
	let error = $state<string | null>(null);
	let version = $state(0);
	let sequence = 0;

	cart.load();
	$effect(() => {
		void version; // refresh() bumps it
		const items = cart.requestItems;
		const id = ++sequence;
		if (items.length === 0) {
			quote = null;
			loading = false;
			error = null;
			return;
		}
		loading = true;
		const timer = setTimeout(async () => {
			try {
				const result = await call(api.POST('/api/v1/public/shop/cart/quote', { body: { items } }));
				if (id !== sequence) return;
				quote = result;
				error = null;
				cart.refresh(result.lines);
				if (result.unavailableProductIds.length) {
					cart.removeMany(result.unavailableProductIds);
					toast.info(m.cart_removed_unavailable());
				}
			} catch (failure) {
				if (id === sequence) error = errorMessage(failure);
			} finally {
				if (id === sequence) loading = false;
			}
		}, DEBOUNCE_MS);
		return () => clearTimeout(timer);
	});

	return {
		get quote() {
			return quote;
		},
		get loading() {
			return loading;
		},
		get error() {
			return error;
		},
		/** Fetches a fresh quote, e.g. after checkout was refused because stock or prices changed. */
		refresh() {
			version++;
		},
		/** The line of the quote for a product, if the server priced it. */
		line(productId: string) {
			return quote?.lines.find((line) => line.productId === productId);
		},
	};
}
