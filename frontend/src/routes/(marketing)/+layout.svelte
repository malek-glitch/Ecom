<script lang="ts">
	import { onMount } from 'svelte';
	import { session } from '$lib/auth/session.svelte';
	import LocaleSwitcher from '$lib/components/app/LocaleSwitcher.svelte';
	import Logo from '$lib/components/app/Logo.svelte';
	import ThemeToggle from '$lib/components/app/ThemeToggle.svelte';
	import CartButton from '$lib/components/shop/CartButton.svelte';
	import { Button } from '$lib/components/ui/button';
	import { m } from '$lib/paraglide/messages';

	let { data, children } = $props();

	const supportEmail = $derived(String(data.config.settings['app.supportEmail'] ?? '').trim());

	// The server can't know who is signed in, so the header adapts after hydration.
	onMount(() => void session.init());
</script>

<div class="flex min-h-svh flex-col">
	<header class="sticky top-0 z-20 border-b bg-background/80 backdrop-blur">
		<div class="mx-auto flex h-16 max-w-6xl items-center gap-4 px-4 sm:px-6">
			<a href="/" class="rounded-md outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50">
				<Logo name={data.config.appName} />
			</a>
			<Button href="/products" variant="ghost" class="hidden sm:inline-flex">{m.nav_shop()}</Button>
			<nav class="ms-auto flex items-center gap-1">
				<LocaleSwitcher />
				<ThemeToggle />
				<CartButton />
				{#if session.isAuthenticated}
					<Button href="/orders" variant="ghost" class="ms-2 hidden sm:inline-flex">{m.nav_orders()}</Button>
					<Button href="/dashboard">{m.nav_open_app()}</Button>
				{:else}
					<Button href="/login" variant="ghost" class="ms-2 hidden sm:inline-flex">{m.nav_sign_in()}</Button>
					<Button href="/login" class="sm:hidden">{m.nav_sign_in()}</Button>
					{#if data.config.registrationEnabled}
						<Button href="/register" class="hidden sm:inline-flex">{m.nav_create_account()}</Button>
					{/if}
				{/if}
			</nav>
		</div>
	</header>

	<main id="main" class="flex-1">
		{@render children()}
	</main>

	<footer class="border-t">
		<div
			class="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-2 px-4 py-6 text-sm text-muted-foreground sm:px-6"
		>
			<span>{m.footer_rights({ year: new Date().getFullYear(), app: data.config.appName })}</span>
			{#if supportEmail}
				<a href="mailto:{supportEmail}" class="hover:text-foreground">{m.footer_contact({ email: supportEmail })}</a>
			{/if}
		</div>
	</footer>
</div>
