<script lang="ts">
	import Menu from '@lucide/svelte/icons/menu';
	import Search from '@lucide/svelte/icons/search';
	import UserRound from '@lucide/svelte/icons/user-round';
	import { onMount } from 'svelte';
	import { afterNavigate } from '$app/navigation';
	import { page } from '$app/state';
	import { session } from '$lib/auth/session.svelte';
	import LocaleSwitcher from '$lib/components/app/LocaleSwitcher.svelte';
	import Logo from '$lib/components/app/Logo.svelte';
	import ThemeToggle from '$lib/components/app/ThemeToggle.svelte';
	import CartButton from '$lib/components/shop/CartButton.svelte';
	import SearchBox from '$lib/components/shop/SearchBox.svelte';
	import SocialIcon from '$lib/components/shop/SocialIcon.svelte';
	import { Button } from '$lib/components/ui/button';
	import * as Dialog from '$lib/components/ui/dialog';
	import * as Sheet from '$lib/components/ui/sheet';
	import { m } from '$lib/paraglide/messages';
	import { storeSettings } from '$lib/theme/store-settings';
	import { cn } from '$lib/utils';

	let { data, children } = $props();

	const store = $derived(storeSettings(data.config.settings));
	const supportEmail = $derived(String(data.config.settings['app.supportEmail'] ?? '').trim());
	const socials = $derived(
		(['instagram', 'facebook', 'tiktok', 'x'] as const)
			.map((network) => ({ network, href: store.social[network] }))
			.filter((social) => social.href),
	);
	const NETWORK_NAMES = { instagram: 'Instagram', facebook: 'Facebook', tiktok: 'TikTok', x: 'X' };

	let menuOpen = $state(false);
	let searchOpen = $state(false);

	const isCurrent = (href: string) => page.url.pathname === href;

	// The server can't know who is signed in, so the header adapts after hydration.
	onMount(() => void session.init());
	afterNavigate(() => {
		menuOpen = false;
		searchOpen = false;
	});
</script>

<a
	href="#main"
	class="sr-only z-50 rounded-md bg-background px-3 py-2 text-sm font-medium shadow focus:not-sr-only focus:fixed focus:start-4 focus:top-4"
>
	{m.nav_skip()}
</a>

<div class="flex min-h-svh flex-col">
	{#if store.announcementBar}
		<div class="bg-primary px-4 py-2 text-center text-sm font-medium text-balance text-primary-foreground" dir="auto">
			{store.announcementBar}
		</div>
	{/if}

	<header class="sticky top-0 z-30 border-b bg-background/85 backdrop-blur supports-[backdrop-filter]:bg-background/70">
		<div class="mx-auto flex h-16 max-w-7xl items-center gap-2 px-4 sm:gap-4 sm:px-6">
			<Button
				variant="ghost"
				size="icon"
				class="-ms-2 lg:hidden"
				aria-label={m.nav_menu()}
				onclick={() => (menuOpen = true)}
			>
				<Menu />
			</Button>
			<a href="/" class="shrink-0 rounded-md outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50">
				<Logo name={data.config.appName} />
			</a>
			<nav aria-label={m.nav_main()} class="hidden items-center gap-1 lg:flex">
				<Button href="/products" variant="ghost" size="sm" aria-current={isCurrent('/products') ? 'page' : undefined}>
					{m.shop_all_products()}
				</Button>
				{#each data.menu.slice(0, 4) as category (category.id)}
					<Button
						href="/categories/{category.slug}"
						variant="ghost"
						size="sm"
						aria-current={isCurrent(`/categories/${category.slug}`) ? 'page' : undefined}
					>
						{category.name}
					</Button>
				{/each}
			</nav>
			<SearchBox class="ms-auto hidden max-w-sm md:block" />
			<div class="ms-auto flex items-center gap-0.5 md:ms-0">
				<Button
					variant="ghost"
					size="icon"
					class="md:hidden"
					aria-label={m.search_label()}
					onclick={() => (searchOpen = true)}
				>
					<Search />
				</Button>
				<div class="hidden items-center gap-0.5 sm:flex">
					<LocaleSwitcher />
					<ThemeToggle />
				</div>
				<CartButton />
				{#if session.isAuthenticated}
					<Button href="/orders" variant="ghost" size="icon" class="hidden sm:inline-flex" aria-label={m.nav_orders()}>
						<UserRound />
					</Button>
					<Button href="/dashboard" size="sm" class="ms-1 hidden sm:inline-flex">{m.nav_open_app()}</Button>
				{:else}
					<Button href="/login" variant="ghost" size="sm" class="ms-1 hidden sm:inline-flex">{m.nav_sign_in()}</Button>
					{#if data.config.registrationEnabled}
						<Button href="/register" size="sm" class="hidden xl:inline-flex">{m.nav_create_account()}</Button>
					{/if}
				{/if}
			</div>
		</div>
	</header>

	<main id="main" class="flex-1">
		{@render children()}
	</main>

	<footer class="mt-16 border-t bg-muted/30">
		<div class="mx-auto grid max-w-7xl gap-10 px-4 py-12 sm:grid-cols-2 sm:px-6 lg:grid-cols-4">
			<div class="space-y-4 lg:col-span-2">
				<Logo name={data.config.appName} />
				{#if store.footerAbout}
					<p class="max-w-md text-sm leading-relaxed whitespace-pre-line text-muted-foreground" dir="auto">
						{store.footerAbout}
					</p>
				{/if}
				{#if socials.length}
					<ul class="flex gap-2">
						{#each socials as social (social.network)}
							<li>
								<a
									href={social.href}
									target="_blank"
									rel="noopener me"
									aria-label={NETWORK_NAMES[social.network]}
									class="grid size-9 place-items-center rounded-full border bg-background text-muted-foreground transition-colors hover:text-foreground"
								>
									<SocialIcon network={social.network} />
								</a>
							</li>
						{/each}
					</ul>
				{/if}
			</div>
			<nav aria-label={m.footer_shop()}>
				<h2 class="mb-3 text-sm font-semibold">{m.footer_shop()}</h2>
				<ul class="space-y-2 text-sm text-muted-foreground">
					<li><a href="/products" class="hover:text-foreground">{m.shop_all_products()}</a></li>
					{#each data.menu.slice(0, 6) as category (category.id)}
						<li><a href="/categories/{category.slug}" class="hover:text-foreground">{category.name}</a></li>
					{/each}
				</ul>
			</nav>
			<nav aria-label={m.footer_help()}>
				<h2 class="mb-3 text-sm font-semibold">{m.footer_help()}</h2>
				<ul class="space-y-2 text-sm text-muted-foreground">
					<li><a href="/orders" class="hover:text-foreground">{m.nav_orders()}</a></li>
					<li><a href="/cart" class="hover:text-foreground">{m.cart_title()}</a></li>
					<li>
						<a href={session.isAuthenticated ? '/settings/profile' : '/login'} class="hover:text-foreground">
							{m.footer_account()}
						</a>
					</li>
					{#if supportEmail}
						<li><a href="mailto:{supportEmail}" class="hover:text-foreground" dir="ltr">{supportEmail}</a></li>
					{/if}
				</ul>
			</nav>
		</div>
		<div class="border-t">
			<div
				class="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-2 px-4 py-4 text-sm text-muted-foreground sm:px-6"
			>
				<span>{m.footer_rights({ year: new Date().getFullYear(), app: data.config.appName })}</span>
				<div class="flex items-center gap-1 sm:hidden">
					<LocaleSwitcher />
					<ThemeToggle />
				</div>
			</div>
		</div>
	</footer>
</div>

<Sheet.Root bind:open={menuOpen}>
	<Sheet.Content side="left" class="flex flex-col gap-0 p-0">
		<Sheet.Header class="border-b">
			<Sheet.Title><Logo name={data.config.appName} /></Sheet.Title>
		</Sheet.Header>
		<nav aria-label={m.nav_main()} class="flex-1 overflow-y-auto p-2">
			<ul class="space-y-0.5">
				<li>
					<a
						href="/"
						class={cn('block rounded-md px-3 py-2 text-sm hover:bg-accent', isCurrent('/') && 'font-semibold')}
						>{m.nav_home()}</a
					>
				</li>
				<li>
					<a
						href="/products"
						class={cn('block rounded-md px-3 py-2 text-sm hover:bg-accent', isCurrent('/products') && 'font-semibold')}
					>
						{m.shop_all_products()}
					</a>
				</li>
			</ul>
			{#if data.menu.length}
				<p class="mt-4 px-3 pb-1 text-xs font-medium text-muted-foreground">{m.shop_categories()}</p>
				<ul class="space-y-0.5">
					{#each data.menu as category (category.id)}
						<li>
							<a
								href="/categories/{category.slug}"
								class={cn(
									'flex justify-between rounded-md px-3 py-2 text-sm hover:bg-accent',
									isCurrent(`/categories/${category.slug}`) && 'font-semibold',
								)}
							>
								{category.name}<span class="text-muted-foreground tabular-nums">{category.productCount}</span>
							</a>
						</li>
					{/each}
				</ul>
			{/if}
		</nav>
		<div class="space-y-2 border-t p-4">
			{#if session.isAuthenticated}
				<Button href="/orders" variant="outline" class="w-full">{m.nav_orders()}</Button>
				<Button href="/dashboard" class="w-full">{m.nav_open_app()}</Button>
			{:else}
				<Button href="/login" variant="outline" class="w-full">{m.nav_sign_in()}</Button>
				{#if data.config.registrationEnabled}
					<Button href="/register" class="w-full">{m.nav_create_account()}</Button>
				{/if}
			{/if}
			<div class="flex justify-center gap-1 pt-1">
				<LocaleSwitcher />
				<ThemeToggle />
			</div>
		</div>
	</Sheet.Content>
</Sheet.Root>

<Dialog.Root bind:open={searchOpen}>
	<Dialog.Content class="top-4 translate-y-0 p-4 sm:top-[15%]" showCloseButton={false}>
		<Dialog.Title class="sr-only">{m.search_label()}</Dialog.Title>
		<SearchBox autofocus onNavigate={() => (searchOpen = false)} />
	</Dialog.Content>
</Dialog.Root>
