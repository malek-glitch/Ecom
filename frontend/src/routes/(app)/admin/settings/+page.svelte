<script lang="ts">
	import Palette from '@lucide/svelte/icons/palette';
	import SettingRow from '$lib/components/admin/SettingRow.svelte';
	import PageHeader from '$lib/components/app/PageHeader.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import { Button } from '$lib/components/ui/button';
	import * as Card from '$lib/components/ui/card';
	import { m } from '$lib/paraglide/messages';

	let { data } = $props();

	/** Settings are grouped by the first part of their key (`shop.currency` → shop). */
	const GROUPS = [
		{ prefixes: ['auth', 'app'], title: m.admin_settings_group_general },
		{ prefixes: ['shop'], title: m.admin_settings_group_shop },
		{ prefixes: ['theme', 'store'], title: m.admin_settings_group_appearance, appearance: true },
		{ prefixes: ['seo'], title: m.admin_settings_group_seo },
	];

	const prefix = (key: string) => key.split('.')[0];
	const groups = $derived.by(() => {
		const known = GROUPS.flatMap((group) => group.prefixes);
		const result = GROUPS.map((group) => ({
			...group,
			settings: data.settings.filter((setting) => group.prefixes.includes(prefix(setting.key))),
		}));
		const others = data.settings.filter((setting) => !known.includes(prefix(setting.key)));
		if (others.length) result.push({ prefixes: [], title: m.admin_settings_group_other, settings: others });
		return result.filter((group) => group.settings.length);
	});
</script>

<Seo title={m.admin_settings_title()} />

<PageHeader title={m.admin_settings_title()} description={m.admin_settings_subtitle()} />

<div class="space-y-6">
	{#each groups as group (group.title)}
		<Card.Root>
			<Card.Header>
				<Card.Title>{group.title()}</Card.Title>
				{#if group.appearance}
					<Card.Description>{m.admin_settings_appearance_hint()}</Card.Description>
					<Card.Action>
						<Button variant="outline" size="sm" href="/admin/appearance"><Palette />{m.nav_admin_appearance()}</Button>
					</Card.Action>
				{/if}
			</Card.Header>
			<Card.Content class="divide-y">
				{#each group.settings as setting (setting.key)}
					<SettingRow {setting} />
				{/each}
			</Card.Content>
		</Card.Root>
	{/each}
</div>
