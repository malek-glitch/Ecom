<script lang="ts">
	import { page } from '$app/state';
	import { APP_NAME } from '$lib/config';

	let { title, description }: { title?: string; description?: string } = $props();

	const appName = $derived(page.data.config?.appName ?? APP_NAME);
	const fullTitle = $derived(title ? `${title} · ${appName}` : appName);
</script>

<svelte:head>
	<title>{fullTitle}</title>
	{#if description}
		<meta name="description" content={description} />
		<meta property="og:description" content={description} />
	{/if}
	<meta property="og:title" content={fullTitle} />
	<meta property="og:site_name" content={appName} />
</svelte:head>
