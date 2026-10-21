<script lang="ts">
	import ArrowRight from '@lucide/svelte/icons/arrow-right';
	import BookOpen from '@lucide/svelte/icons/book-open';
	import NotebookPen from '@lucide/svelte/icons/notebook-pen'; // [example:notes]
	import Receipt from '@lucide/svelte/icons/receipt';
	import ShieldCheck from '@lucide/svelte/icons/shield-check';
	import { session } from '$lib/auth/session.svelte';
	import PageHeader from '$lib/components/app/PageHeader.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import StatValue from '$lib/components/app/StatValue.svelte';
	import { Badge } from '$lib/components/ui/badge';
	import { Button } from '$lib/components/ui/button';
	import * as Card from '$lib/components/ui/card';
	import { m } from '$lib/paraglide/messages';

	let { data } = $props();

	const firstName = $derived(session.user?.fullName.split(/\s+/)[0] ?? '');
</script>

<Seo title={m.dashboard_title()} />

<PageHeader title={m.dashboard_welcome({ name: firstName })} description={m.dashboard_subtitle()} />

<div class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
	<Card.Root>
		<Card.Header>
			<Card.Description class="flex items-center gap-2">
				<ShieldCheck class="size-4" />
				{m.dashboard_security_title()}
			</Card.Description>
			<StatValue value={data.sessionCount} label={(count) => m.sessions_count({ count })} />
			<Card.Action>
				{#if session.user?.emailVerified}
					<Badge variant="secondary">{m.dashboard_email_verified()}</Badge>
				{:else}
					<Badge variant="destructive">{m.dashboard_email_unverified()}</Badge>
				{/if}
			</Card.Action>
		</Card.Header>
		<Card.Footer>
			<Button variant="outline" size="sm" href="/settings/security">
				{m.dashboard_security_link()}
				<ArrowRight class="rtl:rotate-180" />
			</Button>
		</Card.Footer>
	</Card.Root>

	<Card.Root>
		<Card.Header>
			<Card.Description class="flex items-center gap-2">
				<Receipt class="size-4" />
				{m.dashboard_orders_title()}
			</Card.Description>
			<StatValue value={data.orderCount} label={(count) => m.orders_count({ count })} />
		</Card.Header>
		<Card.Footer class="flex flex-wrap gap-2">
			<Button variant="outline" size="sm" href="/orders">
				{m.dashboard_orders_link()}
				<ArrowRight class="rtl:rotate-180" />
			</Button>
			<Button variant="ghost" size="sm" href="/products">{m.nav_shop()}</Button>
		</Card.Footer>
	</Card.Root>

	<!-- [example:notes] -->
	<Card.Root>
		<Card.Header>
			<Card.Description class="flex items-center gap-2">
				<NotebookPen class="size-4" />
				{m.dashboard_notes_title()}
			</Card.Description>
			<StatValue value={data.noteCount} label={(count) => m.notes_count({ count })} />
		</Card.Header>
		<Card.Footer>
			<Button variant="outline" size="sm" href="/notes">
				{m.dashboard_notes_link()}
				<ArrowRight class="rtl:rotate-180" />
			</Button>
		</Card.Footer>
	</Card.Root>
	<!-- [/example:notes] -->

	<Card.Root>
		<Card.Header>
			<Card.Description class="flex items-center gap-2">
				<BookOpen class="size-4" />
				{data.config.appName}
			</Card.Description>
			<Card.Title>{m.dashboard_dev_title({ app: data.config.appName })}</Card.Title>
		</Card.Header>
		<Card.Content>
			<p class="text-sm leading-relaxed text-muted-foreground">{m.dashboard_dev_body()}</p>
		</Card.Content>
	</Card.Root>
</div>
