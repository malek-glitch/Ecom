<script lang="ts">
	import ArrowRight from '@lucide/svelte/icons/arrow-right';
	import { z } from 'zod';
	import { toast } from 'svelte-sonner';
	import { invalidate } from '$app/navigation';
	import { api, call } from '$lib/api/client';
	import ChoiceGroup from '$lib/components/admin/ChoiceGroup.svelte';
	import FormAlert from '$lib/components/app/FormAlert.svelte';
	import SubmitButton from '$lib/components/app/SubmitButton.svelte';
	import { Button } from '$lib/components/ui/button';
	import * as Dialog from '$lib/components/ui/dialog';
	import * as Field from '$lib/components/ui/field';
	import { Input } from '$lib/components/ui/input';
	import { Textarea } from '$lib/components/ui/textarea';
	import { createForm } from '$lib/forms/form.svelte';
	import { m } from '$lib/paraglide/messages';
	import { stockAfter, type AdjustmentKind, type InventoryRow } from './types';

	/** Receive, remove or count the stock of one product. Render inside {#key} so each opening starts fresh. */
	let {
		open = $bindable(false),
		product,
		kind = 'RECEIVE',
	}: { open?: boolean; product: InventoryRow; kind?: AdjustmentKind } = $props();

	const KINDS = ['RECEIVE', 'REMOVE', 'COUNT'] as const;
	const kindLabels: Record<string, () => string> = {
		RECEIVE: m.adjust_receive,
		REMOVE: m.adjust_remove,
		COUNT: m.adjust_count,
	};
	const hints: Record<AdjustmentKind, () => string> = {
		RECEIVE: m.adjust_receive_hint,
		REMOVE: m.adjust_remove_hint,
		COUNT: m.adjust_count_hint,
	};

	// Limits mirror AdjustStockRequest (InventoryDtos.kt).
	const schema = z.object({
		kind: z.enum(KINDS),
		quantity: z.string().refine((value) => /^\d{1,7}$/.test(value.trim()) && Number(value) <= 1_000_000, {
			error: () => m.validation_stock(),
		}),
		note: z.string().max(300, { error: () => m.validation_too_long() }),
	});

	const form = createForm({
		schema,
		initial: () => ({ kind, quantity: kind === 'COUNT' ? String(product.stock) : '', note: '' }),
		onSubmit: async (values) => {
			await call(
				api.POST('/api/v1/admin/inventory/products/{id}/adjustments', {
					params: { path: { id: product.productId } },
					body: { kind: values.kind, quantity: Number(values.quantity), note: values.note },
				}),
			);
			toast.success(m.adjust_saved({ name: product.name }));
			open = false;
			await invalidate('app:inventory');
		},
	});

	const after = $derived(
		/^\d+$/.test(form.values.quantity.trim())
			? stockAfter(product.stock, form.values.kind, Number(form.values.quantity))
			: undefined,
	);
</script>

<Dialog.Root bind:open>
	<Dialog.Content class="sm:max-w-md">
		<Dialog.Header>
			<Dialog.Title>{m.adjust_title()}</Dialog.Title>
			<Dialog.Description class="line-clamp-1">{product.name}</Dialog.Description>
		</Dialog.Header>
		<form onsubmit={form.submit} novalidate class="space-y-5">
			<FormAlert message={form.formError} />
			<ChoiceGroup label={m.adjust_kind()} options={KINDS} labels={kindLabels} bind:value={form.values.kind} />
			<p class="-mt-2 text-sm text-muted-foreground">{hints[form.values.kind]()}</p>
			<Field.Group>
				<Field.Field data-invalid={!!form.errors.quantity}>
					<Field.Label for="adjust-quantity">
						{form.values.kind === 'COUNT' ? m.adjust_counted() : m.cart_quantity()}
					</Field.Label>
					<Input
						id="adjust-quantity"
						inputmode="numeric"
						dir="ltr"
						bind:value={form.values.quantity}
						aria-invalid={!!form.errors.quantity}
						required
					/>
					<Field.Error errors={form.fieldErrors('quantity')} />
				</Field.Field>
				<Field.Field data-invalid={!!form.errors.note}>
					<Field.Label for="adjust-note">
						{m.adjust_note()} <span class="font-normal text-muted-foreground">({m.common_optional()})</span>
					</Field.Label>
					<Textarea
						id="adjust-note"
						bind:value={form.values.note}
						maxlength={300}
						class="min-h-16"
						placeholder={m.adjust_note_placeholder()}
					/>
					<Field.Error errors={form.fieldErrors('note')} />
				</Field.Field>
			</Field.Group>
			<div class="flex items-center justify-center gap-3 rounded-lg bg-muted/50 py-3 text-sm" aria-live="polite">
				<span class="text-muted-foreground">{m.inventory_stock()}</span>
				<span class="font-semibold tabular-nums">{product.stock}</span>
				<ArrowRight class="size-4 text-muted-foreground rtl:rotate-180" />
				{#if after === null}
					<span class="font-semibold text-destructive">{m.adjust_too_many()}</span>
				{:else}
					<span class="font-semibold tabular-nums">{after ?? '…'}</span>
				{/if}
			</div>
			<Dialog.Footer>
				<Button type="button" variant="outline" onclick={() => (open = false)}>{m.common_cancel()}</Button>
				<SubmitButton busy={form.submitting} disabled={after === null}>{m.adjust_submit()}</SubmitButton>
			</Dialog.Footer>
		</form>
	</Dialog.Content>
</Dialog.Root>
