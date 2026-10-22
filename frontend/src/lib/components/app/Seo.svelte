<script lang="ts">
	import { page } from '$app/state';
	import { APP_NAME } from '$lib/config';
	import { getLocale } from '$lib/paraglide/runtime';
	import { absoluteUrl, jsonLdTag, type JsonLd } from '$lib/seo/json-ld';
	import { storeSettings } from '$lib/theme/store-settings';

	interface Props {
		title?: string;
		description?: string;
		/** A photo for link previews; the store's share image otherwise. */
		image?: string | null;
		type?: 'website' | 'product';
		/** Path (or URL) search engines should index this page under. Defaults to the path without its query. */
		canonical?: string;
		/** Keep the page out of search results (signed-in pages always are). */
		noindex?: boolean;
		jsonLd?: JsonLd | JsonLd[];
	}

	let { title, description, image, type = 'website', canonical, noindex = false, jsonLd }: Props = $props();

	const OG_LOCALES: Record<string, string> = { fr: 'fr_FR', en: 'en_GB', ar: 'ar_AR' };

	const appName = $derived(page.data.config?.appName ?? APP_NAME);
	const fullTitle = $derived(title ? `${title} · ${appName}` : appName);
	const origin = $derived(page.url.origin);
	const canonicalUrl = $derived(absoluteUrl(origin, canonical ?? page.url.pathname));
	const shareImage = $derived(absoluteUrl(origin, image ?? storeSettings(page.data.config?.settings).shareImageUrl));
	// Accounts, the admin area and auth forms have nothing for search engines.
	const hidden = $derived(noindex || /^\/\((app|auth)\)/.test(page.route.id ?? ''));
</script>

<svelte:head>
	<title>{fullTitle}</title>
	{#if description}
		<meta name="description" content={description} />
		<meta property="og:description" content={description} />
		<meta name="twitter:description" content={description} />
	{/if}
	{#if hidden}
		<meta name="robots" content="noindex, follow" />
	{:else if canonicalUrl}
		<link rel="canonical" href={canonicalUrl} />
		<meta property="og:url" content={canonicalUrl} />
	{/if}
	<meta property="og:title" content={fullTitle} />
	<meta property="og:site_name" content={appName} />
	<meta property="og:type" content={type} />
	<meta property="og:locale" content={OG_LOCALES[getLocale()] ?? getLocale()} />
	<meta name="twitter:title" content={fullTitle} />
	{#if shareImage}
		<meta property="og:image" content={shareImage} />
		<meta name="twitter:card" content="summary_large_image" />
		<meta name="twitter:image" content={shareImage} />
	{:else}
		<meta name="twitter:card" content="summary" />
	{/if}
	{#if jsonLd && !hidden}
		<!-- JSON data, escaped by jsonLdTag: never executed (see lib/seo/json-ld.ts). -->
		<!-- eslint-disable-next-line svelte/no-at-html-tags -->
		{@html jsonLdTag(jsonLd)}
	{/if}
</svelte:head>
