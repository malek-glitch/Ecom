<script lang="ts">
	import { invalidate } from '$app/navigation';
	import { toast } from 'svelte-sonner';
	import { api, call } from '$lib/api/client';
	import { errorMessage } from '$lib/api/errors';
	import type { components } from '$lib/api/schema';
	import { Badge } from '$lib/components/ui/badge';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import * as Select from '$lib/components/ui/select';
	import { Spinner } from '$lib/components/ui/spinner';
	import { Switch } from '$lib/components/ui/switch';
	import { Textarea } from '$lib/components/ui/textarea';
	import { m } from '$lib/paraglide/messages';

	type Setting = components['schemas']['AdminSettingResponse'];

	let { setting }: { setting: Setting } = $props();

	// Follows the saved value; typing overrides it until the next save or reload.
	let draft = $derived(setting.value);
	let saving = $state(false);
	const dirty = $derived(draft !== setting.value);

	async function save(value: string) {
		saving = true;
		try {
			const saved = await call(
				api.PUT('/api/v1/admin/settings/{key}', { params: { path: { key: setting.key } }, body: { value } }),
			);
			draft = saved.value;
			toast.success(m.admin_setting_saved());
			// Client-visible settings reach the app through the public config: reload it.
			await Promise.all([invalidate('app:admin-settings'), saved.clientVisible ? invalidate('app:config') : null]);
		} catch (error) {
			draft = setting.value;
			toast.error(errorMessage(error));
		} finally {
			saving = false;
		}
	}
</script>

<div class="flex flex-col gap-3 py-4 first:pt-0 last:pb-0 sm:flex-row sm:items-start sm:justify-between">
	<div class="min-w-0 space-y-1">
		<p class="flex flex-wrap items-center gap-2">
			<code class="rounded bg-muted px-1.5 py-0.5 text-sm font-medium" dir="ltr">{setting.key}</code>
			{#if setting.clientVisible}<Badge variant="outline">{m.admin_setting_public()}</Badge>{/if}
		</p>
		<!-- Descriptions come from the backend's Setting enum (English). -->
		<p class="text-sm text-muted-foreground" dir="auto">{setting.description}</p>
		<p class="text-xs text-muted-foreground">
			{m.admin_setting_default({ value: setting.default === '' ? m.admin_setting_empty() : setting.default })}
		</p>
	</div>
	<div class="shrink-0 sm:w-80">
		{#if setting.type === 'BOOLEAN'}
			<div class="flex items-center gap-2 sm:justify-end">
				{#if saving}<Spinner class="text-muted-foreground" />{/if}
				<Switch
					checked={draft === 'true'}
					disabled={saving}
					aria-label={setting.key}
					onCheckedChange={(checked) => save(String(checked))}
				/>
			</div>
		{:else if setting.type === 'CHOICE'}
			<div class="flex items-center gap-2 sm:justify-end">
				{#if saving}<Spinner class="text-muted-foreground" />{/if}
				<Select.Root type="single" value={draft} disabled={saving} onValueChange={(value) => save(value)}>
					<Select.Trigger class="w-full sm:w-56" aria-label={setting.key}>{draft}</Select.Trigger>
					<Select.Content>
						{#each setting.options as option (option)}
							<Select.Item value={option} label={option} />
						{/each}
					</Select.Content>
				</Select.Root>
			</div>
		{:else}
			<form
				class="flex gap-2 {setting.type === 'TEXT' ? 'flex-col items-end' : ''}"
				onsubmit={(event) => {
					event.preventDefault();
					void save(draft);
				}}
			>
				{#if setting.type === 'TEXT'}
					<Textarea bind:value={draft} aria-label={setting.key} dir="auto" class="min-h-24" />
				{:else if setting.type === 'COLOR'}
					<input
						type="color"
						class="h-9 w-12 shrink-0 cursor-pointer rounded-md border bg-transparent p-1"
						value={draft || '#171717'}
						aria-label={setting.key}
						oninput={(event) => (draft = event.currentTarget.value)}
					/>
					<Input bind:value={draft} aria-label={setting.key} dir="ltr" placeholder="#2563eb" maxlength={7} />
				{:else}
					<Input
						bind:value={draft}
						type={setting.type === 'INTEGER' ? 'number' : setting.type === 'URL' ? 'url' : 'text'}
						aria-label={setting.key}
						dir={setting.type === 'URL' ? 'ltr' : 'auto'}
						placeholder={setting.type === 'URL' ? 'https://' : undefined}
					/>
				{/if}
				<Button type="submit" variant="outline" disabled={!dirty || saving}>
					{#if saving}<Spinner />{/if}
					{m.common_save()}
				</Button>
			</form>
		{/if}
	</div>
</div>
