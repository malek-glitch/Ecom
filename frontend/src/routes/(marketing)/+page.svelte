<script lang="ts">
	import Globe from '@lucide/svelte/icons/globe';
	import HardDrive from '@lucide/svelte/icons/hard-drive';
	import Mail from '@lucide/svelte/icons/mail';
	import Rocket from '@lucide/svelte/icons/rocket';
	import ShieldCheck from '@lucide/svelte/icons/shield-check';
	import Users from '@lucide/svelte/icons/users';
	import { session } from '$lib/auth/session.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import { Button } from '$lib/components/ui/button';
	import * as Card from '$lib/components/ui/card';
	import { m } from '$lib/paraglide/messages';

	let { data } = $props();

	// Replace this page with your product's landing page.
	const features = [
		{ icon: ShieldCheck, title: m.landing_feature_auth_title, body: m.landing_feature_auth_body },
		{ icon: Users, title: m.landing_feature_admin_title, body: m.landing_feature_admin_body },
		{ icon: Mail, title: m.landing_feature_mail_title, body: m.landing_feature_mail_body },
		{ icon: Globe, title: m.landing_feature_i18n_title, body: m.landing_feature_i18n_body },
		{ icon: HardDrive, title: m.landing_feature_storage_title, body: m.landing_feature_storage_body },
		{ icon: Rocket, title: m.landing_feature_deploy_title, body: m.landing_feature_deploy_body },
	];
</script>

<Seo description={m.app_tagline()} />

<section class="relative overflow-hidden">
	<div
		aria-hidden="true"
		class="pointer-events-none absolute inset-x-0 -top-40 -z-10 h-[32rem] bg-gradient-to-b from-primary/10 to-transparent blur-2xl"
	></div>
	<div class="mx-auto max-w-3xl px-4 py-20 text-center sm:px-6 sm:py-28">
		<p class="mb-4 text-sm font-medium tracking-wide text-muted-foreground">{m.app_tagline()}</p>
		<h1 class="text-4xl font-semibold tracking-tight text-balance sm:text-5xl">{m.landing_title()}</h1>
		<p class="mx-auto mt-6 max-w-2xl text-lg text-pretty text-muted-foreground">
			{m.landing_subtitle({ app: data.config.appName })}
		</p>
		<div class="mt-10 flex flex-wrap items-center justify-center gap-3">
			{#if session.isAuthenticated}
				<Button size="lg" href="/dashboard">{m.nav_open_app()}</Button>
			{:else}
				{#if data.config.registrationEnabled}
					<Button size="lg" href="/register">{m.landing_cta_primary()}</Button>
				{/if}
				<Button size="lg" variant="outline" href="/login">{m.landing_cta_secondary()}</Button>
			{/if}
		</div>
	</div>
</section>

<section class="mx-auto max-w-6xl px-4 pb-24 sm:px-6">
	<h2 class="mb-10 text-center text-2xl font-semibold tracking-tight">{m.landing_features_title()}</h2>
	<div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
		{#each features as feature (feature.title)}
			<Card.Root>
				<Card.Header>
					<span class="mb-2 grid size-10 place-items-center rounded-lg bg-primary/10 text-primary">
						<feature.icon class="size-5" />
					</span>
					<Card.Title>{feature.title()}</Card.Title>
					<Card.Description class="leading-relaxed">{feature.body()}</Card.Description>
				</Card.Header>
			</Card.Root>
		{/each}
	</div>
</section>
