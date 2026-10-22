<script lang="ts">
	import { z } from 'zod';
	import { toast } from 'svelte-sonner';
	import { invalidate } from '$app/navigation';
	import { api, call } from '$lib/api/client';
	import FormAlert from '$lib/components/app/FormAlert.svelte';
	import SubmitButton from '$lib/components/app/SubmitButton.svelte';
	import * as Card from '$lib/components/ui/card';
	import * as Field from '$lib/components/ui/field';
	import { Input } from '$lib/components/ui/input';
	import { Switch } from '$lib/components/ui/switch';
	import { createForm } from '$lib/forms/form.svelte';
	import { m } from '$lib/paraglide/messages';
	import { parseAmount, toAmountInput } from '$lib/shop/money';
	import type { InventoryRow } from './types';

	/** Tracking, low-stock level and unit cost of one product. Render inside {#key row.version}. */
	let { row, currency, globalThreshold }: { row: InventoryRow; currency: string; globalThreshold: number } = $props();

	const form = createForm({
		schema: z.object({
			trackInventory: z.boolean(),
			threshold: z.string().refine((v) => v.trim() === '' || (/^\d{1,7}$/.test(v.trim()) && Number(v) <= 1_000_000), {
				error: () => m.validation_stock(),
			}),
			cost: z.string().refine((v) => v.trim() === '' || parseAmount(v, currency) !== null, {
				error: () => m.validation_amount(),
			}),
		}),
		initial: () => ({
			trackInventory: row.trackInventory,
			threshold: row.lowStockThresholdOverride?.toString() ?? '',
			cost: toAmountInput(row.costCents, currency),
		}),
		onSubmit: async (values) => {
			await call(
				api.PUT('/api/v1/admin/inventory/products/{id}/settings', {
					params: { path: { id: row.productId } },
					body: {
						trackInventory: values.trackInventory,
						lowStockThreshold: values.threshold.trim() ? Number(values.threshold) : null,
						costCents: values.cost.trim() ? parseAmount(values.cost, currency) : null,
					},
				}),
			);
			toast.success(m.product_saved());
			await invalidate('app:inventory');
		},
	});
</script>

<Card.Root>
	<Card.Header>
		<Card.Title>{m.inventory_product_settings()}</Card.Title>
	</Card.Header>
	<Card.Content>
		<form onsubmit={form.submit} novalidate class="space-y-5">
			<FormAlert message={form.formError} />
			<Field.Group>
				<Field.Field orientation="horizontal">
					<Switch id="track-inventory" bind:checked={form.values.trackInventory} />
					<Field.Content>
						<Field.Label for="track-inventory" class="font-normal">{m.inventory_track()}</Field.Label>
						<Field.Description>{m.inventory_track_hint()}</Field.Description>
					</Field.Content>
				</Field.Field>
				<Field.Field data-invalid={!!form.errors.threshold}>
					<Field.Label for="threshold">{m.inventory_threshold()}</Field.Label>
					<Input
						id="threshold"
						inputmode="numeric"
						dir="ltr"
						bind:value={form.values.threshold}
						placeholder={String(globalThreshold)}
						disabled={!form.values.trackInventory}
						aria-invalid={!!form.errors.threshold}
					/>
					<Field.Description>{m.inventory_threshold_product_hint({ count: globalThreshold })}</Field.Description>
					<Field.Error errors={form.fieldErrors('threshold')} />
				</Field.Field>
				<Field.Field data-invalid={!!form.errors.cost}>
					<Field.Label for="cost">{m.inventory_cost({ currency })}</Field.Label>
					<Input
						id="cost"
						inputmode="decimal"
						dir="ltr"
						bind:value={form.values.cost}
						placeholder="0.00"
						aria-invalid={!!form.errors.cost}
					/>
					<Field.Description>{m.inventory_cost_hint()}</Field.Description>
					<Field.Error errors={form.fieldErrors('cost')} />
				</Field.Field>
			</Field.Group>
			<SubmitButton busy={form.submitting} class="w-full">{m.common_save()}</SubmitButton>
		</form>
	</Card.Content>
</Card.Root>
