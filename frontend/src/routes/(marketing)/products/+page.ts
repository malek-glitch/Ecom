import { redirect } from '@sveltejs/kit';
import { loadCatalog } from '$lib/shop/catalog';
import { shopSettings } from '$lib/shop/store-api';
import type { PageLoad } from './$types';

/** All products, or search results with `?q=`. Search, filters, sort and page live in the URL. */
export const load = (async ({ fetch, url, parent }) => {
	// Old-style category links (?category=x) move to the category's own page.
	const category = url.searchParams.get('category');
	if (category) {
		const params = new URLSearchParams(url.searchParams);
		params.delete('category');
		const query = params.toString();
		redirect(301, `/categories/${encodeURIComponent(category)}${query ? `?${query}` : ''}`);
	}
	const { config } = await parent();
	return loadCatalog(fetch, url, shopSettings(config.settings).currency);
}) satisfies PageLoad;
