<script lang="ts">
	import FileQuestion from '@lucide/svelte/icons/file-question';
	import ShieldX from '@lucide/svelte/icons/shield-x';
	import TriangleAlert from '@lucide/svelte/icons/triangle-alert';
	import { page } from '$app/state';
	import { Button } from '$lib/components/ui/button';
	import * as Empty from '$lib/components/ui/empty';
	import { m } from '$lib/paraglide/messages';
	import Seo from './Seo.svelte';

	let { home = '/' }: { home?: string } = $props();

	const kind = $derived(page.status === 404 ? 'not-found' : page.status === 403 ? 'forbidden' : 'error');
	const title = $derived(
		kind === 'not-found'
			? m.error_page_not_found_title()
			: kind === 'forbidden'
				? m.error_page_forbidden_title()
				: m.error_page_generic_title(),
	);
	// Expected errors (thrown with error(status, message)) carry a translated message; unexpected ones don't.
	const body = $derived(
		kind === 'not-found'
			? m.error_page_not_found_body()
			: kind === 'forbidden'
				? m.error_forbidden()
				: page.status < 500 || page.error?.code
					? (page.error?.message ?? m.error_generic())
					: m.error_server(),
	);
</script>

<Seo {title} />

<Empty.Root class="min-h-[60vh]">
	<Empty.Header>
		<Empty.Media variant="icon">
			{#if kind === 'not-found'}
				<FileQuestion />
			{:else if kind === 'forbidden'}
				<ShieldX />
			{:else}
				<TriangleAlert />
			{/if}
		</Empty.Media>
		<Empty.Title>{title}</Empty.Title>
		<Empty.Description>{body}</Empty.Description>
	</Empty.Header>
	<Empty.Content>
		<div class="flex gap-2">
			{#if kind === 'error'}
				<Button variant="outline" onclick={() => location.reload()}>{m.common_retry()}</Button>
			{/if}
			<Button href={home}>{m.error_page_home()}</Button>
		</div>
		<p class="text-xs text-muted-foreground" dir="ltr">{page.status}</p>
	</Empty.Content>
</Empty.Root>
