<script lang="ts">
	import Laptop from '@lucide/svelte/icons/laptop';
	import Smartphone from '@lucide/svelte/icons/smartphone';
	import { goto, invalidate } from '$app/navigation';
	import { toast } from 'svelte-sonner';
	import { z } from 'zod';
	import { api, call } from '$lib/api/client';
	import { ApiError, errorMessage, fieldErrorMessage } from '$lib/api/errors';
	import { session } from '$lib/auth/session.svelte';
	import ConfirmDialog from '$lib/components/app/ConfirmDialog.svelte';
	import FormAlert from '$lib/components/app/FormAlert.svelte';
	import PasswordInput from '$lib/components/app/PasswordInput.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import SubmitButton from '$lib/components/app/SubmitButton.svelte';
	import { Badge } from '$lib/components/ui/badge';
	import { Button } from '$lib/components/ui/button';
	import * as Card from '$lib/components/ui/card';
	import * as Field from '$lib/components/ui/field';
	import { Input } from '$lib/components/ui/input';
	import { createForm } from '$lib/forms/form.svelte';
	import { password } from '$lib/forms/schemas';
	import { formatDateTime, formatRelative } from '$lib/i18n/format';
	import { m } from '$lib/paraglide/messages';
	import { describeUserAgent } from '$lib/user-agent';

	let { data } = $props();

	const user = $derived(session.user!);
	const otherSessions = $derived(data.sessions.filter((s) => !s.current).length);

	// --- Password: change it, or set a first one for accounts created with Google or an invitation.
	const passwordForm = createForm({
		schema: z.object({ currentPassword: z.string(), newPassword: password() }),
		initial: { currentPassword: '', newPassword: '' },
		onSubmit: async (values) => {
			await call(
				api.PUT('/api/v1/me/password', {
					body: { currentPassword: user.hasPassword ? values.currentPassword : null, newPassword: values.newPassword },
				}),
			);
			// The change signs out other devices and invalidates this access token; refresh it now.
			await session.refresh();
			await session.reloadUser();
			passwordForm.reset();
			toast.success(m.security_password_saved());
			await invalidate('app:sessions');
		},
	});

	// --- Sessions
	async function revoke(id: string) {
		try {
			await call(api.DELETE('/api/v1/me/sessions/{id}', { params: { path: { id } } }));
			toast.success(m.security_revoked());
			await invalidate('app:sessions');
		} catch (error) {
			toast.error(errorMessage(error));
		}
	}

	async function revokeOthers() {
		try {
			await call(api.DELETE('/api/v1/me/sessions'));
			toast.success(m.security_revoked());
			await invalidate('app:sessions');
		} catch (error) {
			toast.error(errorMessage(error));
		}
	}

	// --- Delete account: confirmed with the password, or the email for password-less accounts.
	let deleting = $state(false);
	let confirmation = $state('');
	let deleteError = $state<string | null>(null);

	async function deleteAccount() {
		deleteError = null;
		try {
			await call(
				api.DELETE('/api/v1/me', {
					body: user.hasPassword ? { password: confirmation } : { confirmEmail: confirmation },
				}),
			);
		} catch (error) {
			const field = error instanceof ApiError ? Object.values(error.fields)[0] : undefined;
			deleteError = field ? fieldErrorMessage(field) : errorMessage(error);
			throw error;
		}
		await goto('/', { replaceState: true });
		session.clear();
		toast.success(m.security_deleted());
	}
</script>

<Seo title={m.settings_security()} />

<Card.Root>
	<Card.Header>
		<Card.Title>{user.hasPassword ? m.security_password_title() : m.security_set_password()}</Card.Title>
		<Card.Description>
			{user.hasPassword ? m.security_password_subtitle() : m.security_set_password_subtitle()}
		</Card.Description>
	</Card.Header>
	<Card.Content>
		<form onsubmit={passwordForm.submit} novalidate class="space-y-6">
			<FormAlert message={passwordForm.formError} />
			<!-- Lets password managers pair the new password with this account. -->
			<input
				type="email"
				autocomplete="username"
				value={user.email}
				class="hidden"
				readonly
				tabindex="-1"
				aria-hidden="true"
			/>
			<Field.Group>
				{#if user.hasPassword}
					<Field.Field data-invalid={!!passwordForm.errors.currentPassword}>
						<Field.Label for="currentPassword">{m.security_current_password()}</Field.Label>
						<PasswordInput
							id="currentPassword"
							autocomplete="current-password"
							bind:value={passwordForm.values.currentPassword}
							aria-invalid={!!passwordForm.errors.currentPassword}
						/>
						<Field.Error errors={passwordForm.fieldErrors('currentPassword')} />
					</Field.Field>
				{/if}
				<Field.Field data-invalid={!!passwordForm.errors.newPassword}>
					<Field.Label for="newPassword">{m.security_new_password()}</Field.Label>
					<PasswordInput
						id="newPassword"
						autocomplete="new-password"
						bind:value={passwordForm.values.newPassword}
						aria-invalid={!!passwordForm.errors.newPassword}
						aria-describedby="new-password-hint"
					/>
					<Field.Description id="new-password-hint">{m.auth_password_hint()}</Field.Description>
					<Field.Error errors={passwordForm.fieldErrors('newPassword')} />
				</Field.Field>
			</Field.Group>
			<div class="flex justify-end">
				<SubmitButton busy={passwordForm.submitting}>
					{user.hasPassword ? m.security_change_password() : m.security_set_password()}
				</SubmitButton>
			</div>
		</form>
	</Card.Content>
</Card.Root>

<Card.Root>
	<Card.Header>
		<Card.Title>{m.security_sessions_title()}</Card.Title>
		<Card.Description>{m.security_sessions_subtitle()}</Card.Description>
		{#if otherSessions > 0}
			<Card.Action>
				<Button variant="outline" size="sm" onclick={revokeOthers}>{m.security_revoke_others()}</Button>
			</Card.Action>
		{/if}
	</Card.Header>
	<Card.Content>
		<ul class="divide-y">
			{#each data.sessions as item (item.id)}
				{@const mobile = /Android|iPhone|iPad|Mobile/.test(item.userAgent ?? '')}
				<li class="flex items-center gap-3 py-3 first:pt-0 last:pb-0">
					<span class="grid size-9 shrink-0 place-items-center rounded-md bg-muted text-muted-foreground">
						{#if mobile}<Smartphone class="size-4" />{:else}<Laptop class="size-4" />{/if}
					</span>
					<div class="min-w-0 flex-1">
						<p class="flex flex-wrap items-center gap-2 text-sm font-medium">
							<span class="truncate" dir="auto">{describeUserAgent(item.userAgent) ?? m.security_unknown_device()}</span
							>
							{#if item.current}<Badge variant="secondary">{m.security_this_device()}</Badge>{/if}
						</p>
						<p class="text-xs text-muted-foreground">
							<span title={formatDateTime(item.lastUsedAt)}
								>{m.security_last_active({ time: formatRelative(item.lastUsedAt) })}</span
							>
							{#if item.ipAddress}· <span dir="ltr">{item.ipAddress}</span>{/if}
						</p>
					</div>
					{#if !item.current}
						<Button variant="ghost" size="sm" onclick={() => revoke(item.id)}>{m.security_revoke()}</Button>
					{/if}
				</li>
			{/each}
		</ul>
	</Card.Content>
</Card.Root>

<Card.Root class="border-destructive/40">
	<Card.Header>
		<Card.Title>{m.security_danger_title()}</Card.Title>
		<Card.Description>{m.security_danger_subtitle()}</Card.Description>
	</Card.Header>
	<Card.Footer>
		<Button
			variant="destructive"
			onclick={() => {
				confirmation = '';
				deleteError = null;
				deleting = true;
			}}
		>
			{m.security_delete_button()}
		</Button>
	</Card.Footer>
</Card.Root>

<ConfirmDialog
	bind:open={deleting}
	title={m.security_delete_confirm_title()}
	description={m.security_delete_confirm_body()}
	confirmLabel={m.security_delete_button()}
	confirmDisabled={!confirmation}
	onConfirm={deleteAccount}
>
	<Field.Field data-invalid={!!deleteError}>
		{#if user.hasPassword}
			<Field.Label for="delete-confirmation">{m.security_delete_password()}</Field.Label>
			<PasswordInput
				id="delete-confirmation"
				autocomplete="current-password"
				bind:value={confirmation}
				aria-invalid={!!deleteError}
			/>
		{:else}
			<Field.Label for="delete-confirmation">{m.security_delete_email()}</Field.Label>
			<Input
				id="delete-confirmation"
				type="email"
				dir="ltr"
				bind:value={confirmation}
				aria-invalid={!!deleteError}
				placeholder={user.email}
			/>
		{/if}
		{#if deleteError}<Field.Error>{deleteError}</Field.Error>{/if}
	</Field.Field>
</ConfirmDialog>
