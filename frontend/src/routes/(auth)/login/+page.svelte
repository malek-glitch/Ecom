<script lang="ts">
	import MailCheck from '@lucide/svelte/icons/mail-check';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { z } from 'zod';
	import { api, call } from '$lib/api/client';
	import { ApiError } from '$lib/api/errors';
	import { session, type TokenResponse } from '$lib/auth/session.svelte';
	import AuthCard from '$lib/components/app/AuthCard.svelte';
	import FormAlert from '$lib/components/app/FormAlert.svelte';
	import GoogleSignInButton from '$lib/components/app/GoogleSignInButton.svelte';
	import PasswordInput from '$lib/components/app/PasswordInput.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import SubmitButton from '$lib/components/app/SubmitButton.svelte';
	import * as Alert from '$lib/components/ui/alert';
	import { Button } from '$lib/components/ui/button';
	import * as Field from '$lib/components/ui/field';
	import { Input } from '$lib/components/ui/input';
	import { createForm } from '$lib/forms/form.svelte';
	import { anyPassword, email } from '$lib/forms/schemas';
	import { syncAccountLocale } from '$lib/i18n/locale';
	import { safeRedirect } from '$lib/navigation';
	import { m } from '$lib/paraglide/messages';

	let { data } = $props();

	/** Set when the account exists but its email isn't confirmed yet. */
	let unverifiedEmail = $state<string | null>(null);
	let resent = $state(false);

	const form = createForm({
		schema: z.object({ email: email(), password: anyPassword() }),
		initial: { email: '', password: '' },
		onSubmit: async (values) => {
			let tokens: TokenResponse;
			try {
				tokens = await call(api.POST('/api/v1/auth/login', { body: values }));
			} catch (error) {
				if (error instanceof ApiError && error.code === 'auth.email_not_verified') {
					unverifiedEmail = values.email;
					return;
				}
				throw error;
			}
			session.signIn(tokens);
			await signedIn();
		},
	});

	async function signedIn() {
		if (session.user) syncAccountLocale(session.user);
		await goto(safeRedirect(page.url.searchParams.get('redirect')), { replaceState: true });
	}

	async function resend() {
		if (!unverifiedEmail) return;
		await api.POST('/api/v1/auth/verify-email/resend', { body: { email: unverifiedEmail } }).catch(() => undefined);
		resent = true;
	}
</script>

<Seo title={m.auth_login_submit()} />

{#if unverifiedEmail}
	<AuthCard title={m.auth_not_verified_title()}>
		<div class="space-y-4 text-center">
			<MailCheck class="mx-auto size-10 text-primary" />
			<p class="text-sm text-muted-foreground">{m.auth_not_verified_body({ email: unverifiedEmail })}</p>
			{#if resent}
				<Alert.Root>
					<Alert.Description>{m.auth_resent()}</Alert.Description>
				</Alert.Root>
			{:else}
				<Button variant="outline" class="w-full" onclick={resend}>{m.auth_resend()}</Button>
			{/if}
			<Button variant="link" onclick={() => ((unverifiedEmail = null), (resent = false))}
				>{m.auth_back_to_login()}</Button
			>
		</div>
	</AuthCard>
{:else}
	<AuthCard title={m.auth_login_title({ app: data.config.appName })} description={m.auth_login_subtitle()}>
		<form onsubmit={form.submit} novalidate class="space-y-6">
			<FormAlert message={form.formError} />
			<Field.Group>
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
					<div class="flex items-center">
						<Field.Label for="password">{m.auth_password()}</Field.Label>
						<a href="/forgot-password" class="ms-auto text-sm underline-offset-4 hover:underline">
							{m.auth_forgot_link()}
						</a>
					</div>
					<PasswordInput
						id="password"
						autocomplete="current-password"
						bind:value={form.values.password}
						aria-invalid={!!form.errors.password}
						required
					/>
					<Field.Error errors={form.fieldErrors('password')} />
				</Field.Field>
			</Field.Group>
			<SubmitButton class="w-full" busy={form.submitting}>{m.auth_login_submit()}</SubmitButton>
		</form>

		{#if data.config.googleClientId}
			<Field.Separator class="my-6">{m.auth_or()}</Field.Separator>
			<GoogleSignInButton
				clientId={data.config.googleClientId}
				onSuccess={signedIn}
				onError={(message) => form.setFormError(message || m.error_google())}
			/>
		{/if}
	</AuthCard>
	{#if data.config.registrationEnabled}
		<p class="mt-6 text-center text-sm text-muted-foreground">
			{m.auth_login_no_account()}
			<a href="/register" class="font-medium text-foreground underline-offset-4 hover:underline"
				>{m.auth_register_link()}</a
			>
		</p>
	{/if}
{/if}
