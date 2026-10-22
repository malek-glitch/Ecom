<script lang="ts">
	import { cn } from '$lib/utils';

	/** A row of toggle buttons for picking one value (a segmented control). */
	let {
		label,
		options,
		labels,
		value = $bindable(),
	}: { label: string; options: readonly string[]; labels: Record<string, () => string>; value: string } = $props();

	const id = $props.id();
</script>

<div class="space-y-2">
	<p id="{id}-label" class="text-sm font-medium">{label}</p>
	<div
		role="radiogroup"
		aria-labelledby="{id}-label"
		class="inline-flex flex-wrap gap-1 rounded-lg border bg-muted/40 p-1"
	>
		{#each options as option (option)}
			<button
				type="button"
				role="radio"
				aria-checked={value === option}
				class={cn(
					'rounded-md px-3 py-1.5 text-sm transition-colors outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50',
					value === option ? 'bg-background font-medium shadow-sm' : 'text-muted-foreground hover:text-foreground',
				)}
				onclick={() => (value = option)}
			>
				{labels[option]?.() ?? option}
			</button>
		{/each}
	</div>
</div>
