<script lang="ts">
	import Search from '@lucide/svelte/icons/search';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import PageHeader from '$lib/components/app/PageHeader.svelte';
	import PaginationBar from '$lib/components/app/PaginationBar.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import { Badge } from '$lib/components/ui/badge';
	import * as Card from '$lib/components/ui/card';
	import { Input } from '$lib/components/ui/input';
	import * as Table from '$lib/components/ui/table';
	import * as Tabs from '$lib/components/ui/tabs';
	import { debounce } from '$lib/debounce';
	import { formatDateTime, formatRelative } from '$lib/i18n/format';
	import { withQuery } from '$lib/navigation';
	import { m } from '$lib/paraglide/messages';

	let { data } = $props();

	const groups = [
		{ value: 'all', label: m.admin_audit_all },
		{ value: 'security.', label: m.admin_audit_security },
		{ value: 'user.', label: m.admin_audit_user },
		{ value: 'admin.', label: m.admin_audit_admin },
		{ value: 'system.', label: m.admin_audit_system },
	];

	function filter(changes: Record<string, string | null>) {
		void goto(withQuery(page.url, { ...changes, page: null }), { keepFocus: true, noScroll: true, replaceState: true });
	}

	const search = debounce((value: string) => filter({ q: value.trim() }));

	/** `{"count":2}` → `count: 2`, for a compact details column. */
	function describe(details: string | null | undefined) {
		if (!details) return '';
		try {
			return Object.entries(JSON.parse(details) as Record<string, unknown>)
				.map(([key, value]) => `${key}: ${typeof value === 'string' ? value : JSON.stringify(value)}`)
				.join(' · ');
		} catch {
			return details;
		}
	}
</script>

<Seo title={m.admin_audit_title()} />

<PageHeader title={m.admin_audit_title()} description={m.admin_audit_subtitle()} />

<div class="mb-4 flex flex-wrap items-center gap-3">
	<Tabs.Root
		value={data.filters.action ?? 'all'}
		onValueChange={(value) => filter({ action: value === 'all' ? null : value })}
	>
		<Tabs.List class="flex-wrap">
			{#each groups as group (group.value)}
				<Tabs.Trigger value={group.value}>{group.label()}</Tabs.Trigger>
			{/each}
		</Tabs.List>
	</Tabs.Root>
	<div class="relative w-full sm:ms-auto sm:max-w-xs">
		<Search class="pointer-events-none absolute start-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
		<Input
			type="search"
			class="ps-9"
			placeholder={m.admin_audit_search()}
			aria-label={m.admin_audit_search()}
			value={data.filters.q}
			oninput={(event) => search(event.currentTarget.value)}
		/>
	</div>
</div>

<Card.Root class="py-0">
	<Table.Root>
		<Table.Header>
			<Table.Row>
				<Table.Head>{m.admin_audit_col_when()}</Table.Head>
				<Table.Head>{m.admin_audit_col_action()}</Table.Head>
				<Table.Head>{m.admin_audit_col_actor()}</Table.Head>
				<Table.Head>{m.admin_audit_col_target()}</Table.Head>
				<Table.Head class="hidden lg:table-cell">{m.admin_audit_col_ip()}</Table.Head>
			</Table.Row>
		</Table.Header>
		<Table.Body>
			{#each data.events.items as event (event.id)}
				<Table.Row>
					<Table.Cell class="whitespace-nowrap text-muted-foreground" title={formatDateTime(event.occurredAt)}>
						{formatRelative(event.occurredAt)}
					</Table.Cell>
					<Table.Cell>
						<!-- Action codes are stable identifiers, shown as-is so they can be searched in logs. -->
						<Badge variant="outline" class="font-mono" dir="ltr">{event.action}</Badge>
						{#if event.details}
							<p class="mt-1 max-w-xs truncate text-xs text-muted-foreground" dir="ltr" title={event.details}>
								{describe(event.details)}
							</p>
						{/if}
					</Table.Cell>
					<Table.Cell class="max-w-48 truncate" dir="ltr">
						{#if event.actorId}
							<a href="/admin/users/{event.actorId}" class="hover:underline">{event.actorEmail}</a>
						{:else}
							{event.actorEmail ?? '—'}
						{/if}
					</Table.Cell>
					<Table.Cell class="max-w-48 truncate" dir="ltr">
						{#if event.targetType === 'user' && event.targetId}
							<a href="/admin/users/{event.targetId}" class="hover:underline">{event.targetLabel ?? event.targetId}</a>
						{:else}
							{event.targetLabel ?? event.targetId ?? '—'}
						{/if}
					</Table.Cell>
					<Table.Cell class="hidden font-mono text-xs text-muted-foreground lg:table-cell" dir="ltr">
						{event.ipAddress ?? '—'}
					</Table.Cell>
				</Table.Row>
			{:else}
				<Table.Row>
					<Table.Cell colspan={5} class="h-24 text-center text-muted-foreground">{m.admin_audit_empty()}</Table.Cell>
				</Table.Row>
			{/each}
		</Table.Body>
	</Table.Root>
</Card.Root>

<div class="mt-6">
	<PaginationBar
		page={data.events.page}
		size={data.events.size}
		totalItems={data.events.totalItems}
		onPageChange={(next) => goto(withQuery(page.url, { page: next || null }))}
	/>
</div>
