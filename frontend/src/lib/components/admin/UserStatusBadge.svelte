<script lang="ts">
	import { Badge } from '$lib/components/ui/badge';
	import { m } from '$lib/paraglide/messages';

	let { user }: { user: { enabled: boolean; emailVerified: boolean } } = $props();
</script>

{#if !user.enabled}
	<Badge variant="destructive">{m.admin_status_disabled()}</Badge>
{:else if !user.emailVerified}
	<Badge variant="outline">{m.admin_status_unverified()}</Badge>
{:else}
	<Badge variant="secondary">{m.admin_status_active()}</Badge>
{/if}
