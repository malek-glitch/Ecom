<script lang="ts">
	import MailCheck from '@lucide/svelte/icons/mail-check';
	import { z } from 'zod';
	import { api, call } from '$lib/api/client';
	import AuthCard from '$lib/components/app/AuthCard.svelte';
	import FormAlert from '$lib/components/app/FormAlert.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import SubmitButton from '$lib/components/app/SubmitButton.svelte';
	import { Button } from '$lib/components/ui/button';
	import * as Field from '$lib/components/ui/field';
	import { Input } from '$lib/components/ui/input';
	import { createForm } from '$lib/forms/form.svelte';
	import { email } from '$lib/forms/schemas';
	import { m } from '$lib/paraglide/messages';

	let sentTo = $state<string | null>(null);

	const form = createForm({
		schema: z.object({ email: email() }),
		initial: { email: '' },
		onSubmit: async (values) => {
			// Always "sent": the API answers the same whether or not the account exists.
			await call(api.POST('/api/v1/auth/password/forgot', { body: values }));
			sentTo = values.email;
		},
	});
</script>

<Seo title={m.auth_forgot_title()} />

{#if sentTo}
	<AuthCard title={m.auth_check_inbox_title()}>
		<div class="space-y-4 text-center">
			<MailCheck class="mx-auto size-10 text-primary" />
			<p class="text-sm text-muted-foreground">{m.auth_forgot_sent({ email: sentTo })}</p>
			<Button variant="outline" class="w-full" href="/login">{m.auth_back_to_login()}</Button>
		</div>
	</AuthCard>
{:else}
	<AuthCard title={m.auth_forgot_title()} description={m.auth_forgot_subtitle()}>
		<form onsubmit={form.submit} novalidate class="space-y-6">
			<FormAlert message={form.formError} />
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
			<SubmitButton class="w-full" busy={form.submitting}>{m.auth_forgot_submit()}</SubmitButton>
		</form>
	</AuthCard>
	<p class="mt-6 text-center text-sm">
		<a href="/login" class="text-muted-foreground underline-offset-4 hover:text-foreground hover:underline">
			{m.auth_back_to_login()}
		</a>
	</p>
{/if}
