<script lang="ts">
	import Rocket from '@lucide/svelte/icons/rocket';
	import { cn } from '$lib/utils';

	let { name, class: className }: { name: string; class?: string } = $props();
</script>

<span class={cn('inline-flex items-center gap-2 font-semibold tracking-tight', className)}>
	<span class="grid size-8 shrink-0 place-items-center rounded-lg bg-primary text-primary-foreground">
		<Rocket class="size-4" />
	</span>
	<span class="truncate">{name}</span>
</span>
