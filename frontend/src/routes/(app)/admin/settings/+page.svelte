<script lang="ts">
	import SettingRow from '$lib/components/admin/SettingRow.svelte';
	import PageHeader from '$lib/components/app/PageHeader.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import * as Card from '$lib/components/ui/card';
	import { m } from '$lib/paraglide/messages';

	let { data } = $props();
</script>

<Seo title={m.admin_settings_title()} />

<PageHeader title={m.admin_settings_title()} description={m.admin_settings_subtitle()} />

<Card.Root>
	<Card.Content class="divide-y">
		{#each data.settings as setting (setting.key)}
			<SettingRow {setting} />
		{/each}
	</Card.Content>
</Card.Root>
