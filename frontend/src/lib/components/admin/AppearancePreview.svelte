<script lang="ts">
	import Search from '@lucide/svelte/icons/search';
	import ShoppingBag from '@lucide/svelte/icons/shopping-bag';
	import { mode } from 'mode-watcher';
	import Logo from '$lib/components/app/Logo.svelte';
	import ProductImage from '$lib/components/shop/ProductImage.svelte';
	import { Badge } from '$lib/components/ui/badge';
	import { Button } from '$lib/components/ui/button';
	import { m } from '$lib/paraglide/messages';
	import { IMAGE_RATIO_CLASS, type StoreSettings } from '$lib/theme/store-settings';
	import { themeVariables } from '$lib/theme/theme';
	import { cn } from '$lib/utils';

	/** A miniature store page drawn with the draft settings, before they are saved. */
	let { settings, appName }: { settings: StoreSettings; appName: string } = $props();

	const style = $derived.by(() => {
		const { light, dark } = themeVariables(settings);
		const vars = mode.current === 'dark' ? { ...light, ...dark } : light;
		return Object.entries(vars)
			.map(([name, value]) => `${name}:${value}`)
			.join(';');
	});
	const ratio = $derived(IMAGE_RATIO_CLASS[settings.productImageRatio]);
	const samples = [
		{ name: 'Ceramic Lamp', price: '€69.00' },
		{ name: 'Wool Blanket', price: '€89.00', sale: true },
	];
</script>

<div class="overflow-hidden rounded-xl border bg-background text-foreground shadow-sm" {style} aria-hidden="true" inert>
	{#if settings.announcementBar}
		<div class="truncate bg-primary px-3 py-1.5 text-center text-xs font-medium text-primary-foreground">
			{settings.announcementBar}
		</div>
	{/if}
	<div class="flex items-center gap-2 border-b px-3 py-2">
		<Logo name={appName} logoUrl={settings.logoUrl} class="text-sm" />
		<div class="ms-auto flex items-center gap-1 rounded-md border px-2 py-1 text-xs text-muted-foreground">
			<Search class="size-3" />{m.search_placeholder()}
		</div>
		<ShoppingBag class="size-4" />
	</div>
	<div class={cn('grid gap-3 p-4', settings.heroImageUrl && 'grid-cols-[1fr_6rem] items-center')}>
		<div class="space-y-2">
			<p class="text-[10px] font-medium tracking-wide text-muted-foreground uppercase">
				{settings.heroKicker || m.shop_hero_kicker()}
			</p>
			<h2 class="text-lg leading-tight font-semibold tracking-tight">{settings.heroTitle || m.shop_hero_title()}</h2>
			<p class="line-clamp-2 text-xs text-muted-foreground">
				{settings.heroSubtitle || m.shop_hero_subtitle({ app: appName })}
			</p>
			<div class="flex gap-2 pt-1">
				<Button size="sm">{m.shop_hero_cta()}</Button>
				<Button size="sm" variant="outline">{m.shop_categories()}</Button>
			</div>
		</div>
		{#if settings.heroImageUrl}
			<img src={settings.heroImageUrl} alt="" class="aspect-square w-full rounded-lg object-cover" />
		{/if}
	</div>
	<div class="grid grid-cols-2 gap-3 px-4 pb-4">
		{#each samples as sample (sample.name)}
			<div class="space-y-1.5">
				<div class="relative">
					<ProductImage alt="" class={cn('rounded-lg border', ratio)} />
					{#if sample.sale}<Badge class="absolute start-1.5 top-1.5 text-[10px]">{m.shop_sale()}</Badge>{/if}
				</div>
				<h3 class="text-xs font-medium">{sample.name}</h3>
				<p class="text-xs font-semibold">{sample.price}</p>
			</div>
		{/each}
	</div>
	<div class="border-t bg-muted/40 px-4 py-3 text-[11px] text-muted-foreground">
		{settings.footerAbout || `© ${new Date().getFullYear()} ${appName}`}
	</div>
</div>
