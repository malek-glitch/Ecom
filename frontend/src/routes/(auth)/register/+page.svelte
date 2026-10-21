<script lang="ts">
	import MailCheck from '@lucide/svelte/icons/mail-check';
	import { goto } from '$app/navigation';
	import { z } from 'zod';
	import { api, call } from '$lib/api/client';
	import { session } from '$lib/auth/session.svelte';
	import AuthCard from '$lib/components/app/AuthCard.svelte';
	import FormAlert from '$lib/components/app/FormAlert.svelte';
	import GoogleSignInButton from '$lib/components/app/GoogleSignInButton.svelte';
	import PasswordInput from '$lib/components/app/PasswordInput.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import SubmitButton from '$lib/components/app/SubmitButton.svelte';
	import { Button } from '$lib/components/ui/button';
	import * as Field from '$lib/components/ui/field';
	import { Input } from '$lib/components/ui/input';
	import { createForm } from '$lib/forms/form.svelte';
	import { email, password, required } from '$lib/forms/schemas';
	import { m } from '$lib/paraglide/messages';
	import { getLocale } from '$lib/paraglide/runtime';

	let { data } = $props();

	/** Set once the account exists and a confirmation email is on its way. */
	let sentTo = $state<string | null>(null);

	const form = createForm({
		schema: z.object({ fullName: required(100), email: email(), password: password() }),
		initial: { fullName: '', email: '', password: '' },
		onSubmit: async (values) => {
			const result = await call(api.POST('/api/v1/auth/register', { body: { ...values, locale: getLocale() } }));
			if (result.session) {
				session.signIn(result.session);
				await goto('/dashboard', { replaceState: true });
			} else {
				sentTo = values.email;
			}
		},
	});
</script>

<Seo title={m.auth_register_title()} />

{#if sentTo}
	<AuthCard title={m.auth_check_inbox_title()}>
		<div class="space-y-4 text-center">
			<MailCheck class="mx-auto size-10 text-primary" />
			<p class="text-sm text-muted-foreground">{m.auth_check_inbox_body({ email: sentTo })}</p>
			<Button variant="outline" class="w-full" href="/login">{m.auth_back_to_login()}</Button>
		</div>
	</AuthCard>
{:else if !data.config.registrationEnabled}
	<AuthCard title={m.auth_register_title()}>
		<div class="space-y-4 text-center">
			<p class="text-sm text-muted-foreground">{m.auth_registration_closed()}</p>
			<Button variant="outline" class="w-full" href="/login">{m.auth_back_to_login()}</Button>
		</div>
	</AuthCard>
{:else}
	<AuthCard title={m.auth_register_title()} description={m.auth_register_subtitle()}>
		<form onsubmit={form.submit} novalidate class="space-y-6">
			<FormAlert message={form.formError} />
			<Field.Group>
				<Field.Field data-invalid={!!form.errors.fullName}>
					<Field.Label for="fullName">{m.auth_full_name()}</Field.Label>
					<Input
						id="fullName"
						autocomplete="name"
						bind:value={form.values.fullName}
						aria-invalid={!!form.errors.fullName}
						required
					/>
					<Field.Error errors={form.fieldErrors('fullName')} />
				</Field.Field>
				<Field.Field data-invalid={!!form.errors.email}>
					<Field.Label for="email">{m.auth_email()}</Field.Label>
					<Input
						id="email"
						type="email"
						autocomplete="email"
						bind:value={form.values.email}
						aria-invalid={!!form.errors.email}
						required
					/>
					<Field.Error errors={form.fieldErrors('email')} />
				</Field.Field>
				<Field.Field data-invalid={!!form.errors.password}>
					<Field.Label for="password">{m.auth_password()}</Field.Label>
					<PasswordInput
						id="password"
						autocomplete="new-password"
						bind:value={form.values.password}
						aria-invalid={!!form.errors.password}
						aria-describedby="password-hint"
						required
					/>
					<Field.Description id="password-hint">{m.auth_password_hint()}</Field.Description>
					<Field.Error errors={form.fieldErrors('password')} />
				</Field.Field>
			</Field.Group>
			<SubmitButton class="w-full" busy={form.submitting}>{m.auth_register_submit()}</SubmitButton>
		</form>

		{#if data.config.googleClientId}
			<Field.Separator class="my-6">{m.auth_or()}</Field.Separator>
			<GoogleSignInButton
				clientId={data.config.googleClientId}
				onSuccess={() => goto('/dashboard', { replaceState: true })}
				onError={(message) => form.setFormError(message || m.error_google())}
			/>
		{/if}
	</AuthCard>
	<p class="mt-6 text-center text-sm text-muted-foreground">
		{m.auth_have_account()}
		<a href="/login" class="font-medium text-foreground underline-offset-4 hover:underline">{m.auth_login_link()}</a>
	</p>
{/if}
