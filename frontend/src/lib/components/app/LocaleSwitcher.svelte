<script lang="ts">
	import Languages from '@lucide/svelte/icons/languages';
	import { buttonVariants } from '$lib/components/ui/button';
	import * as DropdownMenu from '$lib/components/ui/dropdown-menu';
	import { LOCALE_NAMES, changeLocale, getLocale, locales, type Locale } from '$lib/i18n/locale';
	import { m } from '$lib/paraglide/messages';
</script>

<DropdownMenu.Root>
	<DropdownMenu.Trigger class={buttonVariants({ variant: 'ghost', size: 'icon' })} aria-label={m.locale_switch()}>
		<Languages class="size-4" />
	</DropdownMenu.Trigger>
	<DropdownMenu.Content align="end">
		<DropdownMenu.RadioGroup value={getLocale()} onValueChange={(value) => changeLocale(value as Locale)}>
			{#each locales as locale (locale)}
				<DropdownMenu.RadioItem value={locale} lang={locale}>{LOCALE_NAMES[locale]}</DropdownMenu.RadioItem>
			{/each}
		</DropdownMenu.RadioGroup>
	</DropdownMenu.Content>
</DropdownMenu.Root>
