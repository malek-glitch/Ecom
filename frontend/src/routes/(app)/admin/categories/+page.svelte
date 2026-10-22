<script lang="ts">
	import Pencil from '@lucide/svelte/icons/pencil';
	import Plus from '@lucide/svelte/icons/plus';
	import Trash2 from '@lucide/svelte/icons/trash-2';
	import { toast } from 'svelte-sonner';
	import { invalidate } from '$app/navigation';
	import { api, call } from '$lib/api/client';
	import { errorMessage } from '$lib/api/errors';
	import type { components } from '$lib/api/schema';
	import ConfirmDialog from '$lib/components/app/ConfirmDialog.svelte';
	import PageHeader from '$lib/components/app/PageHeader.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import CategoryDialog from '$lib/components/shop/admin/CategoryDialog.svelte';
	import { Button } from '$lib/components/ui/button';
	import * as Card from '$lib/components/ui/card';
	import * as Table from '$lib/components/ui/table';
	import { m } from '$lib/paraglide/messages';

	type Category = components['schemas']['CategoryResponse'];

	let { data } = $props();

	let editing = $state(false);
	let editingCategory = $state<Category | null>(null);
	let editKey = $state(0);
	let deleting = $state<Category | null>(null);
	let confirmingDelete = $state(false);

	function open(category: Category | null) {
		editingCategory = category;
		editKey++;
		editing = true;
	}

	async function remove() {
		if (!deleting) return;
		try {
			await call(api.DELETE('/api/v1/admin/shop/categories/{id}', { params: { path: { id: deleting.id } } }));
		} catch (error) {
			toast.error(errorMessage(error));
			throw error;
		}
		toast.success(m.category_deleted());
		await invalidate('app:admin-categories');
	}
</script>

<Seo title={m.admin_categories_title()} />

<PageHeader title={m.admin_categories_title()} description={m.admin_categories_subtitle()}>
	{#snippet actions()}
		<Button onclick={() => open(null)}><Plus />{m.category_new()}</Button>
	{/snippet}
</PageHeader>

<Card.Root class="py-0">
	<Table.Root>
		<Table.Header>
			<Table.Row>
				<Table.Head>{m.category_field_name()}</Table.Head>
				<Table.Head class="hidden sm:table-cell">{m.field_slug()}</Table.Head>
				<Table.Head class="text-end">{m.admin_products_title()}</Table.Head>
				<Table.Head class="w-24"><span class="sr-only">{m.common_actions()}</span></Table.Head>
			</Table.Row>
		</Table.Header>
		<Table.Body>
			{#each data.categories as category (category.id)}
				<Table.Row>
					<Table.Cell>
						<a href="/admin/products?category={category.id}" class="font-medium hover:underline">{category.name}</a>
						{#if category.description}
							<span class="block max-w-md truncate text-xs text-muted-foreground">{category.description}</span>
						{/if}
					</Table.Cell>
					<Table.Cell class="hidden text-muted-foreground sm:table-cell" dir="ltr">{category.slug}</Table.Cell>
					<Table.Cell class="text-end tabular-nums">{category.productCount}</Table.Cell>
					<Table.Cell class="text-end">
						<Button variant="ghost" size="icon" aria-label={m.common_edit()} onclick={() => open(category)}
							><Pencil /></Button
						>
						<Button
							variant="ghost"
							size="icon"
							aria-label={m.common_delete()}
							onclick={() => ((deleting = category), (confirmingDelete = true))}
						>
							<Trash2 />
						</Button>
					</Table.Cell>
				</Table.Row>
			{:else}
				<Table.Row>
					<Table.Cell colspan={4} class="h-24 text-center text-muted-foreground">{m.category_empty()}</Table.Cell>
				</Table.Row>
			{/each}
		</Table.Body>
	</Table.Root>
</Card.Root>

{#key editKey}
	<CategoryDialog bind:open={editing} category={editingCategory} />
{/key}

<ConfirmDialog
	bind:open={confirmingDelete}
	title={m.category_delete_title({ name: deleting?.name ?? '' })}
	description={m.category_delete_body()}
	confirmLabel={m.common_delete()}
	onConfirm={remove}
/>
