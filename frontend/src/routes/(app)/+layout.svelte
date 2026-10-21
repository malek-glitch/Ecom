<script lang="ts">
	import MailWarning from '@lucide/svelte/icons/mail-warning';
	import Megaphone from '@lucide/svelte/icons/megaphone';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { api } from '$lib/api/client';
	import { session } from '$lib/auth/session.svelte';
	import AppSidebar from '$lib/components/app/AppSidebar.svelte';
	import LocaleSwitcher from '$lib/components/app/LocaleSwitcher.svelte';
	import ThemeToggle from '$lib/components/app/ThemeToggle.svelte';
	import * as Alert from '$lib/components/ui/alert';
	import { Button } from '$lib/components/ui/button';
	import { Separator } from '$lib/components/ui/separator';
	import * as Sidebar from '$lib/components/ui/sidebar';
	import { SIDEBAR_COOKIE_NAME } from '$lib/components/ui/sidebar/constants';
	import { loginUrl } from '$lib/navigation';
	import { m } from '$lib/paraglide/messages';
	import { toast } from 'svelte-sonner';

	let { data, children } = $props();

	// The sidebar remembers whether it was collapsed (a cookie written by the component).
	let sidebarOpen = $state(!document.cookie.split('; ').includes(`${SIDEBAR_COOKIE_NAME}=false`));
	let resending = $state(false);

	const announcement = $derived(String(data.config.settings['app.announcement'] ?? '').trim());

	// Signed out here, in another tab, or the session expired: back to the login page.
	$effect(() => {
		if (!session.isAuthenticated) void goto(loginUrl(page.url), { replaceState: true });
	});

	async function resendVerification() {
		if (!session.user) return;
		resending = true;
		await api.POST('/api/v1/auth/verify-email/resend', { body: { email: session.user.email } }).catch(() => undefined);
		resending = false;
		toast.success(m.auth_resent());
	}
</script>

<a
	href="#main"
	class="sr-only z-50 rounded-md bg-background px-3 py-2 text-sm font-medium shadow focus:not-sr-only focus:fixed focus:start-4 focus:top-4"
>
	{m.nav_skip()}
</a>

<Sidebar.Provider bind:open={sidebarOpen}>
	<AppSidebar appName={data.config.appName} />
	<Sidebar.Inset>
		<header
			class="sticky top-0 z-10 flex h-14 shrink-0 items-center gap-2 border-b bg-background/95 px-4 backdrop-blur"
		>
			<Sidebar.Trigger class="-ms-1" aria-label={m.nav_toggle_sidebar()} />
			<Separator orientation="vertical" class="me-2 data-[orientation=vertical]:h-4" />
			<div class="ms-auto flex items-center gap-1">
				<LocaleSwitcher />
				<ThemeToggle />
			</div>
		</header>

		{#if announcement || session.user?.emailVerified === false}
			<div class="space-y-2 px-4 pt-4 md:px-6 lg:px-8">
				{#if announcement}
					<Alert.Root>
						<Megaphone />
						<Alert.Description class="text-foreground">{announcement}</Alert.Description>
					</Alert.Root>
				{/if}
				{#if session.user?.emailVerified === false}
					<Alert.Root>
						<MailWarning />
						<Alert.Description class="flex flex-wrap items-center justify-between gap-2">
							<span>{m.dashboard_verify_banner()}</span>
							<Button size="sm" variant="outline" disabled={resending} onclick={resendVerification}>
								{m.auth_resend()}
							</Button>
						</Alert.Description>
					</Alert.Root>
				{/if}
			</div>
		{/if}

		<main id="main" tabindex="-1" class="flex-1 px-4 py-6 outline-none md:px-6 lg:px-8">
			<div class="mx-auto w-full max-w-6xl">
				{@render children()}
			</div>
		</main>
	</Sidebar.Inset>
</Sidebar.Provider>
