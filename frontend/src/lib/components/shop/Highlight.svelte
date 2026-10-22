<script lang="ts">
	import { highlight } from '$lib/shop/highlight';

	/** [text] with the words of [query] in bold. */
	let { text, query = '' }: { text: string; query?: string } = $props();

	const segments = $derived(highlight(text, query));
</script>

{#each segments as segment, index (index)}{#if segment.match}<mark class="bg-transparent font-semibold text-foreground"
			>{segment.text}</mark
		>{:else}{segment.text}{/if}{/each}
