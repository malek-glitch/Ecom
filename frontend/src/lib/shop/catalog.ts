import { intParam } from '$lib/navigation';
import { currencyDigits } from '$lib/i18n/format';
import type { components } from '$lib/api/schema';
import { loadOrError, storeApi } from './store-api';

export type SearchResults = components['schemas']['ProductSearchResponse'];
export type Sort = SearchResults['sort'];
export const SORTS: Sort[] = ['RELEVANCE', 'NEWEST', 'PRICE_ASC', 'PRICE_DESC', 'NAME'];

/** What the URL asks for. Prices are in whole currency units in the URL (?min=10&max=50) for readability. */
export interface CatalogFilters {
	q: string;
	sort: Sort | undefined;
	min: number | undefined;
	max: number | undefined;
	inStock: boolean;
	page: number;
}

const amount = (value: string | null) => {
	const n = Number(value);
	return value && Number.isFinite(n) && n >= 0 ? n : undefined;
};

export function parseFilters(url: URL): CatalogFilters {
	return {
		q: url.searchParams.get('q')?.trim().slice(0, 200) ?? '',
		sort: SORTS.find((sort) => sort === url.searchParams.get('sort')),
		min: amount(url.searchParams.get('min')),
		max: amount(url.searchParams.get('max')),
		inStock: url.searchParams.get('stock') === '1',
		page: intParam(url, 'page'),
	};
}

/** Whether anything besides the page narrows or reorders the list (such pages aren't indexed). */
export const isFiltered = (f: CatalogFilters) =>
	!!f.q || f.sort !== undefined || f.min !== undefined || f.max !== undefined || f.inStock;

export const PAGE_SIZE = 24;

/** Runs the store search for a catalog page. Works during SSR (see storeApi). */
export async function loadCatalog(fetch: typeof globalThis.fetch, url: URL, currency: string, category?: string) {
	const filters = parseFilters(url);
	const scale = 10 ** currencyDigits(currency);
	const results = await loadOrError(
		storeApi(fetch, url).GET('/api/v1/public/shop/products', {
			params: {
				query: {
					q: filters.q || undefined,
					category,
					minPrice: filters.min !== undefined ? Math.round(filters.min * scale) : undefined,
					maxPrice: filters.max !== undefined ? Math.round(filters.max * scale) : undefined,
					inStock: filters.inStock || undefined,
					sort: filters.sort,
					page: filters.page,
					size: PAGE_SIZE,
				},
			},
		}),
	);
	return { results, filters };
}
