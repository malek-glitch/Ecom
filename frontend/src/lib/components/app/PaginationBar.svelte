<script lang="ts">
	import * as Pagination from '$lib/components/ui/pagination';

	interface Props {
		/** 0-based, like the API. */
		page: number;
		size: number;
		totalItems: number;
		onPageChange: (page: number) => void;
	}

	let { page, size, totalItems, onPageChange }: Props = $props();
</script>

{#if totalItems > size}
	<Pagination.Root count={totalItems} perPage={size} page={page + 1} onPageChange={(next) => onPageChange(next - 1)}>
		{#snippet children({ pages, currentPage })}
			<Pagination.Content>
				<Pagination.Item><Pagination.PrevButton /></Pagination.Item>
				{#each pages as item (item.key)}
					{#if item.type === 'ellipsis'}
						<Pagination.Item><Pagination.Ellipsis /></Pagination.Item>
					{:else}
						<Pagination.Item>
							<Pagination.Link page={item} isActive={currentPage === item.value}>{item.value}</Pagination.Link>
						</Pagination.Item>
					{/if}
				{/each}
				<Pagination.Item><Pagination.NextButton /></Pagination.Item>
			</Pagination.Content>
		{/snippet}
	</Pagination.Root>
{/if}
