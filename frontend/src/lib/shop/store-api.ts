import { error } from '@sveltejs/kit';
import createClient from 'openapi-fetch';
import { API_BASE } from '$lib/api/base';
import { ApiError, errorMessage } from '$lib/api/errors';
import type { components, paths } from '$lib/api/schema';

export type ProductSummary = components['schemas']['ProductSummary'];
export type ProductDetail = components['schemas']['ProductDetail'];
export type CategoryResponse = components['schemas']['CategoryResponse'];
export type CartQuote = components['schemas']['CartQuote'];
export type OrderStatus = components['schemas']['OrderResponse']['status'];
export type PaymentMethod = components['schemas']['OrderResponse']['paymentMethod'];

/**
 * A typed client for the public store endpoints, for universal `load` functions on
 * server-rendered pages. It goes through SvelteKit's `fetch` (so `hooks.server.ts` routes it
 * to the backend during SSR) and needs an absolute base URL there: pass the page's `url`.
 *
 *   const store = storeApi(fetch, url);
 *   const home = await loadOrError(store.GET('/api/v1/public/shop/home'));
 */
export function storeApi(fetch: typeof globalThis.fetch, url: URL) {
	return createClient<paths>({ baseUrl: API_BASE || url.origin, fetch: (request) => fetch(request) });
}

/** Like `callForPage`, for [storeApi] results: failures become SvelteKit error pages. */
export async function loadOrError<T>(request: Promise<{ data?: T; error?: unknown; response: Response }>): Promise<T> {
	let result: { data?: T; error?: unknown; response: Response };
	try {
		result = await request;
	} catch {
		const failure = ApiError.network();
		error(503, { message: errorMessage(failure), code: failure.code });
	}
	if (!result.response.ok) {
		const failure = ApiError.fromBody(result.response.status, result.error);
		error(failure.status, { message: errorMessage(failure), code: failure.code });
	}
	return result.data as T;
}

/** The store settings served in the public config (`shop.*`), typed, with safe fallbacks. */
export function shopSettings(settings: Record<string, unknown> | undefined) {
	const s = settings ?? {};
	return {
		currency: typeof s['shop.currency'] === 'string' && s['shop.currency'] ? s['shop.currency'] : 'EUR',
		shippingFlatCents: Number(s['shop.shippingFlatCents'] ?? 0),
		freeShippingThresholdCents: Number(s['shop.freeShippingThresholdCents'] ?? 0),
		bankTransferInstructions: String(s['shop.bankTransferInstructions'] ?? '').trim(),
	};
}

/** Units that can still be ordered: null stock means not tracked, so the cap is the cart's limit. */
export function orderableUnits(stock: number | null | undefined, cap: number): number {
	return stock == null ? cap : Math.max(0, Math.min(stock, cap));
}
