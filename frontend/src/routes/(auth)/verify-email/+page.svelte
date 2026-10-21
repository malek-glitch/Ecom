<script lang="ts">
	import CircleCheck from '@lucide/svelte/icons/circle-check';
	import CircleX from '@lucide/svelte/icons/circle-x';
	import { page } from '$app/state';
	import { onMount } from 'svelte';
	import { api, call } from '$lib/api/client';
	import { session } from '$lib/auth/session.svelte';
	import AuthCard from '$lib/components/app/AuthCard.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import { Button } from '$lib/components/ui/button';
	import { Spinner } from '$lib/components/ui/spinner';
	import { m } from '$lib/paraglide/messages';

	let status = $state<'pending' | 'success' | 'failed'>('pending');

	// In onMount, not a load function: hovering a link preloads its data, and that must not
	// spend a single-use token. Email scanners that only follow links never run this either.
	onMount(async () => {
		const token = page.url.searchParams.get('token');
		if (!token) {
			status = 'failed';
			return;
		}
		try {
			await call(api.POST('/api/v1/auth/verify-email', { body: { token } }));
			status = 'success';
			await session.init();
			if (session.user) await session.reloadUser(); // clears the "unconfirmed" banner
		} catch {
			status = 'failed';
		}
	});
</script>

<Seo title={m.auth_verify_title()} />

<AuthCard title={m.auth_verify_title()}>
	<div class="space-y-4 text-center" aria-live="polite">
		{#if status === 'pending'}
			<Spinner class="mx-auto size-8 text-muted-foreground" />
			<p class="text-sm text-muted-foreground">{m.auth_verify_pending()}</p>
		{:else if status === 'success'}
			<CircleCheck class="mx-auto size-10 text-emerald-600 dark:text-emerald-400" />
			<p class="text-sm">{m.auth_verify_success()}</p>
			{#if session.user}
				<Button class="w-full" href="/dashboard">{m.nav_open_app()}</Button>
			{:else}
				<Button class="w-full" href="/login">{m.nav_sign_in()}</Button>
			{/if}
		{:else}
			<CircleX class="mx-auto size-10 text-destructive" />
			<p class="text-sm">{m.auth_verify_failed()}</p>
			<Button variant="outline" class="w-full" href="/login">{m.auth_back_to_login()}</Button>
		{/if}
	</div>
</AuthCard>
