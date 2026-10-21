<script lang="ts">
	import { Skeleton } from '$lib/components/ui/skeleton';

	/** A number that may still be loading (a promise) or unavailable (null). */
	let { value, label }: { value: Promise<number | null> | number | null; label: (count: number) => string } = $props();
</script>

{#await value}
	<Skeleton class="h-7 w-28" />
{:then count}
	<p class="text-2xl font-semibold tracking-tight">{count === null ? '—' : label(count)}</p>
{/await}
