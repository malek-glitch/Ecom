<script lang="ts">
	import * as Avatar from '$lib/components/ui/avatar';
	import { cn } from '$lib/utils';

	let { name, src = null, class: className }: { name: string; src?: string | null; class?: string } = $props();

	const initials = $derived(
		name
			.split(/\s+/)
			.filter(Boolean)
			.slice(0, 2)
			.map((part) => part[0]?.toUpperCase())
			.join('') || '?',
	);
</script>

<Avatar.Root class={cn('size-8', className)}>
	{#if src}
		<Avatar.Image {src} alt={name} />
	{/if}
	<Avatar.Fallback class="text-xs font-medium">{initials}</Avatar.Fallback>
</Avatar.Root>
