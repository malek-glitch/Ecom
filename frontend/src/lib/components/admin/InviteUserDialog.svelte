<script lang="ts">
	import { toast } from 'svelte-sonner';
	import { z } from 'zod';
	import { api, call } from '$lib/api/client';
	import FormAlert from '$lib/components/app/FormAlert.svelte';
	import SubmitButton from '$lib/components/app/SubmitButton.svelte';
	import { Button } from '$lib/components/ui/button';
	import * as Dialog from '$lib/components/ui/dialog';
	import * as Field from '$lib/components/ui/field';
	import { Input } from '$lib/components/ui/input';
	import * as Select from '$lib/components/ui/select';
	import { createForm } from '$lib/forms/form.svelte';
	import { email, required } from '$lib/forms/schemas';
	import { LOCALE_NAMES, getLocale, locales, type Locale } from '$lib/i18n/locale';
	import { m } from '$lib/paraglide/messages';

	let { open = $bindable(false), onInvited }: { open?: boolean; onInvited: (id: string) => void } = $props();

	const roles = { USER: m.admin_role_user, ADMIN: m.admin_role_admin };

	const form = createForm({
		schema: z.object({
			fullName: required(100),
			email: email(),
			role: z.enum(['USER', 'ADMIN']),
			locale: z.enum(locales),
		}),
		initial: { fullName: '', email: '', role: 'USER' as 'USER' | 'ADMIN', locale: getLocale() as Locale },
		onSubmit: async (values) => {
			const user = await call(api.POST('/api/v1/admin/users/invite', { body: values }));
			toast.success(m.admin_invited({ email: user.email }));
			open = false;
			form.reset();
			onInvited(user.id);
		},
	});
</script>

<Dialog.Root bind:open>
	<Dialog.Content class="sm:max-w-md">
		<Dialog.Header>
			<Dialog.Title>{m.admin_invite_title()}</Dialog.Title>
			<Dialog.Description>{m.admin_invite_subtitle()}</Dialog.Description>
		</Dialog.Header>
		<form onsubmit={form.submit} novalidate class="space-y-6">
			<FormAlert message={form.formError} />
			<Field.Group>
				<Field.Field data-invalid={!!form.errors.fullName}>
					<Field.Label for="invite-name">{m.auth_full_name()}</Field.Label>
					<Input id="invite-name" bind:value={form.values.fullName} aria-invalid={!!form.errors.fullName} />
					<Field.Error errors={form.fieldErrors('fullName')} />
				</Field.Field>
				<Field.Field data-invalid={!!form.errors.email}>
					<Field.Label for="invite-email">{m.auth_email()}</Field.Label>
					<Input id="invite-email" type="email" bind:value={form.values.email} aria-invalid={!!form.errors.email} />
					<Field.Error errors={form.fieldErrors('email')} />
				</Field.Field>
				<div class="grid gap-4 sm:grid-cols-2">
					<Field.Field>
						<Field.Label for="invite-role">{m.admin_col_role()}</Field.Label>
						<Select.Root type="single" bind:value={form.values.role}>
							<Select.Trigger id="invite-role" class="w-full">{roles[form.values.role]()}</Select.Trigger>
							<Select.Content>
								<Select.Item value="USER" label={m.admin_role_user()} />
								<Select.Item value="ADMIN" label={m.admin_role_admin()} />
							</Select.Content>
						</Select.Root>
					</Field.Field>
					<Field.Field>
						<Field.Label for="invite-locale">{m.admin_user_language()}</Field.Label>
						<Select.Root type="single" bind:value={form.values.locale}>
							<Select.Trigger id="invite-locale" class="w-full">{LOCALE_NAMES[form.values.locale]}</Select.Trigger>
							<Select.Content>
								{#each locales as locale (locale)}
									<Select.Item value={locale} label={LOCALE_NAMES[locale]} lang={locale} />
								{/each}
							</Select.Content>
						</Select.Root>
					</Field.Field>
				</div>
			</Field.Group>
			<Dialog.Footer>
				<Button type="button" variant="outline" onclick={() => (open = false)}>{m.common_cancel()}</Button>
				<SubmitButton busy={form.submitting}>{m.admin_invite_submit()}</SubmitButton>
			</Dialog.Footer>
		</form>
	</Dialog.Content>
</Dialog.Root>
