<script lang="ts">
	import type { Snippet } from 'svelte';
	import * as Card from '$lib/components/ui/card';

	let { title, description, children }: { title: string; description?: string; children: Snippet } = $props();
</script>

<Card.Root>
	<Card.Header class="text-center">
		<Card.Title class="text-xl">{title}</Card.Title>
		{#if description}
			<Card.Description>{description}</Card.Description>
		{/if}
	</Card.Header>
	<Card.Content>
		{@render children()}
	</Card.Content>
</Card.Root>
