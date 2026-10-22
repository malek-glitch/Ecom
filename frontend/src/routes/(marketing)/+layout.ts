import { storeApi } from '$lib/shop/store-api';
import type { LayoutLoad } from './$types';

/** Categories for the header menu and the footer. A failure only hides them: pages still render. */
export const load = (async ({ fetch, url }) => {
	try {
		const { data } = await storeApi(fetch, url).GET('/api/v1/public/shop/categories');
		return { menu: (data ?? []).filter((category) => category.productCount > 0) };
	} catch {
		return { menu: [] };
	}
}) satisfies LayoutLoad;
