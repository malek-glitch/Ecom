<script lang="ts">
	import type { Snippet } from 'svelte';
	import * as AlertDialog from '$lib/components/ui/alert-dialog';
	import { Button } from '$lib/components/ui/button';
	import { Spinner } from '$lib/components/ui/spinner';
	import { m } from '$lib/paraglide/messages';

	interface Props {
		open?: boolean;
		title: string;
		description: string;
		confirmLabel: string;
		destructive?: boolean;
		/** Rejecting keeps the dialog open; the caller shows the error. */
		onConfirm: () => Promise<void> | void;
		/** Extra content, e.g. a password field. */
		children?: Snippet;
		confirmDisabled?: boolean;
	}

	let {
		open = $bindable(false),
		title,
		description,
		confirmLabel,
		destructive = true,
		onConfirm,
		children,
		confirmDisabled = false,
	}: Props = $props();
	let busy = $state(false);

	async function confirm() {
		busy = true;
		try {
			await onConfirm();
			open = false;
		} catch {
			// handled by the caller
		} finally {
			busy = false;
		}
	}
</script>

<AlertDialog.Root bind:open>
	<AlertDialog.Content>
		<AlertDialog.Header>
			<AlertDialog.Title>{title}</AlertDialog.Title>
			<AlertDialog.Description>{description}</AlertDialog.Description>
		</AlertDialog.Header>
		{@render children?.()}
		<AlertDialog.Footer>
			<AlertDialog.Cancel disabled={busy}>{m.common_cancel()}</AlertDialog.Cancel>
			<Button variant={destructive ? 'destructive' : 'default'} disabled={busy || confirmDisabled} onclick={confirm}>
				{#if busy}<Spinner />{/if}
				{confirmLabel}
			</Button>
		</AlertDialog.Footer>
	</AlertDialog.Content>
</AlertDialog.Root>
