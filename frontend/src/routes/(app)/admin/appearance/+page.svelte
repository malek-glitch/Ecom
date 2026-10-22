<script lang="ts">
	import Check from '@lucide/svelte/icons/check';
	import RotateCcw from '@lucide/svelte/icons/rotate-ccw';
	import { untrack } from 'svelte';
	import { toast } from 'svelte-sonner';
	import { invalidate } from '$app/navigation';
	import { api, call } from '$lib/api/client';
	import { ApiError, errorMessage, fieldErrorMessage } from '$lib/api/errors';
	import AppearancePreview from '$lib/components/admin/AppearancePreview.svelte';
	import ChoiceGroup from '$lib/components/admin/ChoiceGroup.svelte';
	import PageHeader from '$lib/components/app/PageHeader.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import SubmitButton from '$lib/components/app/SubmitButton.svelte';
	import { Button } from '$lib/components/ui/button';
	import * as Card from '$lib/components/ui/card';
	import * as Field from '$lib/components/ui/field';
	import { Input } from '$lib/components/ui/input';
	import { Switch } from '$lib/components/ui/switch';
	import { Textarea } from '$lib/components/ui/textarea';
	import { m } from '$lib/paraglide/messages';
	import { HEADING_FONTS, IMAGE_RATIOS, RADII, isHexColor, storeSettings } from '$lib/theme/store-settings';
	import { BRAND_PRESETS } from '$lib/theme/theme';

	let { data } = $props();

	/** The settings this page edits (keys of the backend's Setting enum). */
	const KEYS = [
		'theme.brandColor',
		'theme.radius',
		'theme.headingFont',
		'theme.productImageRatio',
		'store.logoUrl',
		'store.announcementBar',
		'store.heroKicker',
		'store.heroTitle',
		'store.heroSubtitle',
		'store.heroImageUrl',
		'store.showPerks',
		'store.footerAbout',
		'store.instagramUrl',
		'store.facebookUrl',
		'store.tiktokUrl',
		'store.xUrl',
	] as const;
	type Key = (typeof KEYS)[number];

	const saved = $derived(
		Object.fromEntries(KEYS.map((key) => [key, data.settings.find((s) => s.key === key)?.value ?? ''])) as Record<
			Key,
			string
		>,
	);
	let draft = $state(Object.fromEntries(KEYS.map((key) => [key, ''])) as Record<Key, string>);
	let errors = $state<Partial<Record<Key, string>>>({});
	let saving = $state(false);

	// Start from (and, after saving, follow) the stored values; a value the API refused stays as typed.
	$effect.pre(() => {
		const next = saved;
		untrack(() => {
			for (const key of KEYS) if (!errors[key]) draft[key] = next[key];
		});
	});

	const changed = $derived(KEYS.filter((key) => draft[key].trim() !== saved[key]));
	/** The draft as the store would read it, for the preview. */
	const preview = $derived(
		storeSettings(
			Object.fromEntries(KEYS.map((key) => [key, key === 'store.showPerks' ? draft[key] === 'true' : draft[key]])),
		),
	);

	const URL_KEYS: Key[] = [
		'store.logoUrl',
		'store.heroImageUrl',
		'store.instagramUrl',
		'store.facebookUrl',
		'store.tiktokUrl',
		'store.xUrl',
	];

	function validate(): boolean {
		const next: Partial<Record<Key, string>> = {};
		const color = draft['theme.brandColor'].trim();
		if (color && !isHexColor(color)) next['theme.brandColor'] = m.validation_color();
		for (const key of URL_KEYS) {
			const value = draft[key].trim();
			if (value && !/^https?:\/\/[^\s"'<>]+$/.test(value)) next[key] = m.validation_url();
		}
		errors = next;
		return Object.keys(next).length === 0;
	}

	async function save(event: Event) {
		event.preventDefault();
		if (!validate() || changed.length === 0) return;
		saving = true;
		const failed: Partial<Record<Key, string>> = {};
		for (const key of changed) {
			try {
				await call(
					api.PUT('/api/v1/admin/settings/{key}', { params: { path: { key } }, body: { value: draft[key].trim() } }),
				);
			} catch (error) {
				const problem = error instanceof ApiError ? error.fields.value : undefined;
				failed[key] = problem ? fieldErrorMessage(problem) : errorMessage(error);
			}
		}
		errors = failed;
		await Promise.all([invalidate('app:admin-settings'), invalidate('app:config')]);
		saving = false;
		if (Object.keys(failed).length) toast.error(m.appearance_save_partial());
		else toast.success(m.appearance_saved());
	}

	function discard() {
		Object.assign(draft, saved);
		errors = {};
	}

	const radiusLabels: Record<string, () => string> = {
		none: m.appearance_radius_none,
		small: m.appearance_radius_small,
		medium: m.appearance_radius_medium,
		large: m.appearance_radius_large,
		full: m.appearance_radius_full,
	};
	const fontLabels: Record<string, () => string> = {
		sans: m.appearance_font_sans,
		serif: m.appearance_font_serif,
		mono: m.appearance_font_mono,
	};
	const ratioLabels: Record<string, () => string> = {
		square: m.appearance_ratio_square,
		portrait: m.appearance_ratio_portrait,
		landscape: m.appearance_ratio_landscape,
	};
</script>

{#snippet text(
	key: Key,
	label: string,
	options: { placeholder?: string; hint?: string; url?: boolean; long?: boolean; max?: number } = {},
)}
	<Field.Field data-invalid={!!errors[key]}>
		<Field.Label for={key}>{label}</Field.Label>
		{#if options.long}
			<Textarea
				id={key}
				bind:value={draft[key]}
				placeholder={options.placeholder}
				maxlength={options.max}
				class="min-h-20"
				dir="auto"
			/>
		{:else}
			<Input
				id={key}
				type={options.url ? 'url' : 'text'}
				dir={options.url ? 'ltr' : 'auto'}
				bind:value={draft[key]}
				placeholder={options.url ? 'https://' : options.placeholder}
				maxlength={options.max}
				aria-invalid={!!errors[key]}
			/>
		{/if}
		{#if options.hint}<Field.Description>{options.hint}</Field.Description>{/if}
		{#if errors[key]}<Field.Error errors={[{ message: errors[key] }]} />{/if}
	</Field.Field>
{/snippet}

<Seo title={m.nav_admin_appearance()} />

<PageHeader title={m.nav_admin_appearance()} description={m.appearance_subtitle()} />

<form onsubmit={save} novalidate class="grid gap-6 xl:grid-cols-[minmax(0,1fr)_26rem]">
	<div class="min-w-0 space-y-6">
		<Card.Root>
			<Card.Header>
				<Card.Title>{m.appearance_brand()}</Card.Title>
				<Card.Description>{m.appearance_brand_hint()}</Card.Description>
			</Card.Header>
			<Card.Content class="space-y-4">
				<div class="flex flex-wrap gap-2" role="group" aria-label={m.appearance_brand()}>
					{#each BRAND_PRESETS as preset (preset.name)}
						{@const selected = draft['theme.brandColor'].trim().toLowerCase() === preset.value}
						<button
							type="button"
							title={preset.name}
							aria-label={preset.name}
							aria-pressed={selected}
							class="grid size-9 place-items-center rounded-full border-2 outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50 {selected
								? 'border-foreground'
								: 'border-transparent'}"
							onclick={() => (draft['theme.brandColor'] = preset.value)}
						>
							<span
								class="grid size-7 place-items-center rounded-full border {preset.value ? '' : 'bg-foreground'}"
								style:background-color={preset.value || undefined}
							>
								{#if selected}<Check class="size-4 text-white mix-blend-difference" />{/if}
							</span>
						</button>
					{/each}
				</div>
				<Field.Field data-invalid={!!errors['theme.brandColor']}>
					<Field.Label for="brand-hex">{m.appearance_custom_color()}</Field.Label>
					<div class="flex max-w-xs gap-2">
						<input
							type="color"
							class="h-9 w-12 shrink-0 cursor-pointer rounded-md border bg-transparent p-1"
							value={isHexColor(draft['theme.brandColor']) ? draft['theme.brandColor'] : '#171717'}
							aria-label={m.appearance_custom_color()}
							oninput={(event) => (draft['theme.brandColor'] = event.currentTarget.value)}
						/>
						<Input
							id="brand-hex"
							dir="ltr"
							bind:value={draft['theme.brandColor']}
							placeholder="#2563eb"
							maxlength={7}
							aria-invalid={!!errors['theme.brandColor']}
						/>
					</div>
					{#if errors['theme.brandColor']}<Field.Error errors={[{ message: errors['theme.brandColor'] }]} />{/if}
				</Field.Field>
			</Card.Content>
		</Card.Root>

		<Card.Root>
			<Card.Header>
				<Card.Title>{m.appearance_style()}</Card.Title>
			</Card.Header>
			<Card.Content class="space-y-5">
				<ChoiceGroup
					label={m.appearance_radius()}
					options={RADII}
					labels={radiusLabels}
					bind:value={draft['theme.radius']}
				/>
				<ChoiceGroup
					label={m.appearance_heading_font()}
					options={HEADING_FONTS}
					labels={fontLabels}
					bind:value={draft['theme.headingFont']}
				/>
				<ChoiceGroup
					label={m.appearance_image_ratio()}
					options={IMAGE_RATIOS}
					labels={ratioLabels}
					bind:value={draft['theme.productImageRatio']}
				/>
			</Card.Content>
		</Card.Root>

		<Card.Root>
			<Card.Header>
				<Card.Title>{m.appearance_header()}</Card.Title>
			</Card.Header>
			<Card.Content>
				<Field.Group>
					{@render text('store.logoUrl', m.appearance_logo(), { url: true, hint: m.appearance_logo_hint() })}
					{@render text('store.announcementBar', m.appearance_announcement(), {
						max: 160,
						hint: m.appearance_announcement_hint(),
					})}
				</Field.Group>
			</Card.Content>
		</Card.Root>

		<Card.Root>
			<Card.Header>
				<Card.Title>{m.appearance_home()}</Card.Title>
				<Card.Description>{m.appearance_home_hint()}</Card.Description>
			</Card.Header>
			<Card.Content>
				<Field.Group>
					{@render text('store.heroKicker', m.appearance_hero_kicker(), {
						placeholder: m.shop_hero_kicker(),
						max: 120,
					})}
					{@render text('store.heroTitle', m.appearance_hero_title(), { placeholder: m.shop_hero_title(), max: 120 })}
					{@render text('store.heroSubtitle', m.appearance_hero_subtitle(), {
						placeholder: m.shop_hero_subtitle({ app: data.config.appName }),
						long: true,
						max: 400,
					})}
					{@render text('store.heroImageUrl', m.appearance_hero_image(), {
						url: true,
						hint: m.appearance_hero_image_hint(),
					})}
					<Field.Field orientation="horizontal">
						<Switch
							id="store.showPerks"
							checked={draft['store.showPerks'] === 'true'}
							onCheckedChange={(checked) => (draft['store.showPerks'] = String(checked))}
						/>
						<Field.Label for="store.showPerks" class="font-normal">{m.appearance_show_perks()}</Field.Label>
					</Field.Field>
				</Field.Group>
			</Card.Content>
		</Card.Root>

		<Card.Root>
			<Card.Header>
				<Card.Title>{m.appearance_footer()}</Card.Title>
			</Card.Header>
			<Card.Content>
				<Field.Group>
					{@render text('store.footerAbout', m.appearance_footer_about(), { long: true, max: 500 })}
					<div class="grid gap-4 sm:grid-cols-2">
						{@render text('store.instagramUrl', 'Instagram', { url: true })}
						{@render text('store.facebookUrl', 'Facebook', { url: true })}
						{@render text('store.tiktokUrl', 'TikTok', { url: true })}
						{@render text('store.xUrl', 'X (Twitter)', { url: true })}
					</div>
				</Field.Group>
			</Card.Content>
		</Card.Root>
	</div>

	<aside class="min-w-0">
		<div class="space-y-3 xl:sticky xl:top-20">
			<p class="text-sm font-medium text-muted-foreground">{m.appearance_preview()}</p>
			<AppearancePreview settings={preview} appName={data.config.appName} />
		</div>
	</aside>

	<div
		class="sticky bottom-0 z-10 -mx-4 flex flex-wrap items-center justify-end gap-2 border-t bg-background/95 px-4 py-3 backdrop-blur md:-mx-6 md:px-6 lg:-mx-8 lg:px-8 xl:col-span-2"
	>
		<p class="me-auto text-sm text-muted-foreground" aria-live="polite">
			{changed.length ? m.appearance_unsaved({ count: changed.length }) : m.appearance_all_saved()}
		</p>
		<Button type="button" variant="ghost" disabled={!changed.length || saving} onclick={discard}>
			<RotateCcw />{m.appearance_discard()}
		</Button>
		<SubmitButton busy={saving} disabled={!changed.length}>{m.appearance_save()}</SubmitButton>
	</div>
</form>
