<script lang="ts">
	import Plus from '@lucide/svelte/icons/plus';
	import SearchX from '@lucide/svelte/icons/search-x';
	import TrendingUp from '@lucide/svelte/icons/trending-up';
	import type { components } from '$lib/api/schema';
	import { Button } from '$lib/components/ui/button';
	import * as Card from '$lib/components/ui/card';
	import * as Table from '$lib/components/ui/table';
	import { formatNumber, formatRelative } from '$lib/i18n/format';
	import { m } from '$lib/paraglide/messages';

	/** What shoppers search for. [missing]: queries that found nothing, with a shortcut to add the product. */
	let {
		title,
		description,
		rows,
		empty,
		missing = false,
	}: {
		title: string;
		description: string;
		rows: components['schemas']['SearchInsight'][];
		empty: string;
		missing?: boolean;
	} = $props();
</script>

<Card.Root>
	<Card.Header>
		<Card.Title class="flex items-center gap-2">
			{#if missing}<SearchX class="size-4 text-destructive" />{:else}<TrendingUp class="size-4 text-primary" />{/if}
			{title}
		</Card.Title>
		<Card.Description>{description}</Card.Description>
	</Card.Header>
	<Card.Content>
		{#if rows.length === 0}
			<p class="py-6 text-center text-sm text-muted-foreground">{empty}</p>
		{:else}
			<Table.Root>
				<Table.Header>
					<Table.Row>
						<Table.Head>{m.insights_col_query()}</Table.Head>
						<Table.Head class="text-end">{m.insights_col_searches()}</Table.Head>
						<Table.Head class="hidden text-end sm:table-cell"
							>{missing ? m.insights_col_last() : m.insights_col_results()}</Table.Head
						>
						{#if missing}<Table.Head class="w-10"><span class="sr-only">{m.common_actions()}</span></Table.Head>{/if}
					</Table.Row>
				</Table.Header>
				<Table.Body>
					{#each rows.slice(0, 10) as row (row.query)}
						<Table.Row>
							<Table.Cell class="max-w-48 truncate">
								<a
									href="/products?q={encodeURIComponent(row.query)}"
									target="_blank"
									rel="noopener"
									class="hover:underline"
									dir="auto"
								>
									{row.query}
								</a>
							</Table.Cell>
							<Table.Cell class="text-end tabular-nums">{formatNumber(row.searches)}</Table.Cell>
							<Table.Cell class="hidden text-end text-muted-foreground tabular-nums sm:table-cell">
								{missing ? formatRelative(row.lastSearchedAt) : formatNumber(row.lastResultCount)}
							</Table.Cell>
							{#if missing}
								<Table.Cell class="text-end">
									<Button variant="ghost" size="icon" href="/admin/products/new" aria-label={m.admin_product_new()}>
										<Plus />
									</Button>
								</Table.Cell>
							{/if}
						</Table.Row>
					{/each}
				</Table.Body>
			</Table.Root>
		{/if}
	</Card.Content>
</Card.Root>
