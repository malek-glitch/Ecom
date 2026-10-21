<script lang="ts">
	import CircleAlert from '@lucide/svelte/icons/circle-alert';
	import * as Alert from '$lib/components/ui/alert';

	/** A form-level error (wrong password, network down...). Renders nothing when empty. */
	let { message }: { message: string | null | undefined } = $props();
</script>

{#if message}
	<Alert.Root variant="destructive" aria-live="polite">
		<CircleAlert />
		<Alert.Description>{message}</Alert.Description>
	</Alert.Root>
{/if}
