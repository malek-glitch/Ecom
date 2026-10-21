<script lang="ts">
	import type { Snippet } from 'svelte';
	import { Button, type ButtonProps } from '$lib/components/ui/button';
	import { Spinner } from '$lib/components/ui/spinner';

	let { busy = false, children, disabled, ...rest }: ButtonProps & { busy?: boolean; children: Snippet } = $props();
</script>

<Button type="submit" disabled={busy || disabled} aria-busy={busy} {...rest}>
	{#if busy}<Spinner />{/if}
	{@render children()}
</Button>
