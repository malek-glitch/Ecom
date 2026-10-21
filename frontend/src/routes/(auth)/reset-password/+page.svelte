<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { z } from 'zod';
	import { api, call } from '$lib/api/client';
	import { session } from '$lib/auth/session.svelte';
	import AuthCard from '$lib/components/app/AuthCard.svelte';
	import FormAlert from '$lib/components/app/FormAlert.svelte';
	import PasswordInput from '$lib/components/app/PasswordInput.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import SubmitButton from '$lib/components/app/SubmitButton.svelte';
	import { Button } from '$lib/components/ui/button';
	import * as Field from '$lib/components/ui/field';
	import { createForm } from '$lib/forms/form.svelte';
	import { password } from '$lib/forms/schemas';
	import { m } from '$lib/paraglide/messages';

	// One page for both emails: "reset your password" and "you're invited, choose a password".
	const token = $derived(page.url.searchParams.get('token') ?? '');
	const invite = $derived(page.url.searchParams.get('invite') === '1');
	const title = $derived(invite ? m.auth_invite_title() : m.auth_reset_title());

	const form = createForm({
		schema: z
			.object({ password: password(), confirm: z.string() })
			.refine((values) => values.password === values.confirm, {
				path: ['confirm'],
				error: () => m.validation_passwords_mismatch(),
			}),
		initial: { password: '', confirm: '' },
		onSubmit: async (values) => {
			// A valid link also confirms the email address, so this signs the user straight in.
			session.signIn(
				await call(api.POST('/api/v1/auth/password/reset', { body: { token, password: values.password } })),
			);
			await goto('/dashboard', { replaceState: true });
		},
	});
</script>

<Seo {title} />

<AuthCard {title} description={token ? (invite ? m.auth_invite_subtitle() : m.auth_reset_subtitle()) : undefined}>
	{#if !token}
		<div class="space-y-4 text-center">
			<p class="text-sm text-muted-foreground">{m.auth_reset_missing_token()}</p>
			<Button variant="outline" class="w-full" href="/forgot-password">{m.auth_forgot_title()}</Button>
		</div>
	{:else}
		<form onsubmit={form.submit} novalidate class="space-y-6">
			<FormAlert message={form.formError} />
			<Field.Group>
				<Field.Field data-invalid={!!form.errors.password}>
					<Field.Label for="password">{m.auth_new_password()}</Field.Label>
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
				<Field.Field data-invalid={!!form.errors.confirm}>
					<Field.Label for="confirm">{m.auth_confirm_password()}</Field.Label>
					<PasswordInput
						id="confirm"
						autocomplete="new-password"
						bind:value={form.values.confirm}
						aria-invalid={!!form.errors.confirm}
						required
					/>
					<Field.Error errors={form.fieldErrors('confirm')} />
				</Field.Field>
			</Field.Group>
			<SubmitButton class="w-full" busy={form.submitting}>{m.auth_reset_submit()}</SubmitButton>
		</form>
	{/if}
</AuthCard>
