<script lang="ts">
	import { page } from '$app/state';
	import { m } from '$lib/paraglide/messages';

	/** Roughly how a search engine result would show this page (titles are cut near 60 characters, descriptions near 160). */
	let { title, path, description }: { title: string; path: string; description: string } = $props();

	const appName = $derived(page.data.config?.appName ?? '');
	const fullTitle = $derived(appName ? `${title} · ${appName}` : title);
	const clip = (text: string, max: number) => (text.length > max ? `${text.slice(0, max - 1).trimEnd()}…` : text);
</script>

<figure class="space-y-1 rounded-lg border bg-background p-4" aria-label={m.seo_preview()}>
	<figcaption class="mb-2 text-xs font-medium text-muted-foreground">{m.seo_preview()}</figcaption>
	<p class="truncate text-xs text-muted-foreground" dir="ltr">{page.url.host}{path}</p>
	<p class="text-lg leading-snug text-[#1a0dab] dark:text-[#8ab4f8]" dir="auto">{clip(fullTitle, 62)}</p>
	<p class="line-clamp-2 text-sm text-muted-foreground" dir="auto">
		{clip(description || m.seo_no_description(), 160)}
	</p>
</figure>
