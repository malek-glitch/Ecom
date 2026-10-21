<script lang="ts">
	import Eye from '@lucide/svelte/icons/eye';
	import EyeOff from '@lucide/svelte/icons/eye-off';
	import type { HTMLInputAttributes } from 'svelte/elements';
	import { Input } from '$lib/components/ui/input';
	import { m } from '$lib/paraglide/messages';

	let { value = $bindable(''), ...rest }: Omit<HTMLInputAttributes, 'type' | 'files' | 'value'> & { value?: string } =
		$props();
	let visible = $state(false);
</script>

<div class="relative">
	<Input {...rest} type={visible ? 'text' : 'password'} bind:value class="pe-10" />
	<button
		type="button"
		class="absolute inset-y-0 end-0 flex w-10 items-center justify-center rounded-e-md text-muted-foreground outline-none hover:text-foreground focus-visible:ring-[3px] focus-visible:ring-ring/50"
		onclick={() => (visible = !visible)}
		aria-label={visible ? m.auth_hide_password() : m.auth_show_password()}
		aria-pressed={visible}
	>
		{#if visible}
			<EyeOff class="size-4" />
		{:else}
			<Eye class="size-4" />
		{/if}
	</button>
</div>
