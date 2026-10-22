<script lang="ts">
	import { toast } from 'svelte-sonner';
	import { invalidate } from '$app/navigation';
	import { api, call } from '$lib/api/client';
	import { errorMessage } from '$lib/api/errors';
	import type { components } from '$lib/api/schema';
	import { Button } from '$lib/components/ui/button';
	import * as Card from '$lib/components/ui/card';
	import { Input } from '$lib/components/ui/input';
	import { Label } from '$lib/components/ui/label';
	import { Switch } from '$lib/components/ui/switch';
	import { m } from '$lib/paraglide/messages';

	/** The module's own switches (runtime settings inventory.*), at hand on the inventory page. */
	let { summary }: { summary: components['schemas']['InventorySummary'] } = $props();

	let threshold = $derived(String(summary.lowStockThreshold));
	let saving = $state(false);

	async function set(key: string, value: string, done: string) {
		saving = true;
		try {
			await call(api.PUT('/api/v1/admin/settings/{key}', { params: { path: { key } }, body: { value } }));
			toast.success(done);
			// inventory.enabled is public: the store's config changes too.
			await Promise.all([invalidate('app:inventory'), invalidate('app:config')]);
		} catch (error) {
			toast.error(errorMessage(error));
		} finally {
			saving = false;
		}
	}
</script>

<Card.Root>
	<Card.Header>
		<Card.Title>{m.inventory_module()}</Card.Title>
		<Card.Description>{m.inventory_module_hint()}</Card.Description>
	</Card.Header>
	<Card.Content class="space-y-5">
		<div class="flex items-start justify-between gap-4">
			<div class="space-y-1">
				<Label for="inventory-enabled">{m.inventory_enabled()}</Label>
				<p class="text-sm text-muted-foreground">{m.inventory_enabled_hint()}</p>
			</div>
			<Switch
				id="inventory-enabled"
				checked={summary.enabled}
				disabled={saving}
				onCheckedChange={(checked) =>
					set('inventory.enabled', String(checked), checked ? m.inventory_turned_on() : m.inventory_turned_off())}
			/>
		</div>
		<div class="flex items-start justify-between gap-4">
			<div class="space-y-1">
				<Label for="inventory-emails">{m.inventory_emails()}</Label>
				<p class="text-sm text-muted-foreground">{m.inventory_emails_hint()}</p>
			</div>
			<Switch
				id="inventory-emails"
				checked={summary.lowStockEmails}
				disabled={saving || !summary.enabled}
				onCheckedChange={(checked) => set('inventory.lowStockEmails', String(checked), m.admin_setting_saved())}
			/>
		</div>
		<form
			class="flex flex-wrap items-end gap-2"
			onsubmit={(event) => {
				event.preventDefault();
				void set('inventory.lowStockThreshold', threshold.trim(), m.admin_setting_saved());
			}}
		>
			<div class="space-y-1">
				<Label for="inventory-threshold">{m.inventory_threshold()}</Label>
				<Input
					id="inventory-threshold"
					type="number"
					min={0}
					class="w-28"
					bind:value={threshold}
					disabled={!summary.enabled}
				/>
			</div>
			<Button
				type="submit"
				variant="outline"
				disabled={saving || !summary.enabled || threshold === String(summary.lowStockThreshold)}
			>
				{m.common_save()}
			</Button>
			<p class="basis-full text-xs text-muted-foreground">{m.inventory_threshold_hint()}</p>
		</form>
	</Card.Content>
</Card.Root>
