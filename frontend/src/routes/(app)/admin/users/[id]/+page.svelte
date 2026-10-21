<script lang="ts">
	import ArrowLeft from '@lucide/svelte/icons/arrow-left';
	import KeyRound from '@lucide/svelte/icons/key-round';
	import LogOut from '@lucide/svelte/icons/log-out';
	import Trash2 from '@lucide/svelte/icons/trash-2';
	import { goto, invalidate } from '$app/navigation';
	import { toast } from 'svelte-sonner';
	import { api, call } from '$lib/api/client';
	import { errorMessage } from '$lib/api/errors';
	import { session } from '$lib/auth/session.svelte';
	import UserDetailsForm from '$lib/components/admin/UserDetailsForm.svelte';
	import UserStatusBadge from '$lib/components/admin/UserStatusBadge.svelte';
	import ConfirmDialog from '$lib/components/app/ConfirmDialog.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import UserAvatar from '$lib/components/app/UserAvatar.svelte';
	import { Badge } from '$lib/components/ui/badge';
	import { Button } from '$lib/components/ui/button';
	import * as Card from '$lib/components/ui/card';
	import { formatDate, formatDateTime, formatNumber, formatRelative } from '$lib/i18n/format';
	import { LOCALE_NAMES, type Locale } from '$lib/i18n/locale';
	import { m } from '$lib/paraglide/messages';

	let { data } = $props();

	const user = $derived(data.user);
	// The API refuses these on your own account (and for the last admin); the UI just doesn't offer them.
	const isSelf = $derived(user.id === session.user?.id);
	const roles = { USER: m.admin_role_user, ADMIN: m.admin_role_admin };

	let confirmingDelete = $state(false);

	async function act(request: Promise<unknown>, success: string) {
		try {
			await request;
			toast.success(success);
			await invalidate('app:admin-user');
		} catch (error) {
			toast.error(errorMessage(error));
		}
	}

	const revokeSessions = () =>
		act(
			call(api.POST('/api/v1/admin/users/{id}/revoke-sessions', { params: { path: { id: user.id } } })),
			m.admin_user_revoked(),
		);

	const sendPasswordEmail = () =>
		act(
			call(api.POST('/api/v1/admin/users/{id}/password-email', { params: { path: { id: user.id } } })),
			m.admin_user_password_sent(),
		);

	async function remove() {
		try {
			await call(api.DELETE('/api/v1/admin/users/{id}', { params: { path: { id: user.id } } }));
		} catch (error) {
			toast.error(errorMessage(error));
			throw error;
		}
		toast.success(m.admin_user_deleted());
		await goto('/admin/users', { replaceState: true });
	}
</script>

<Seo title={user.fullName} />

<Button variant="ghost" size="sm" href="/admin/users" class="-ms-2 mb-4">
	<ArrowLeft class="rtl:rotate-180" />
	{m.admin_user_back()}
</Button>

<div class="mb-6 flex flex-wrap items-center gap-4">
	<UserAvatar name={user.fullName} src={user.avatarUrl} class="size-14 text-base" />
	<div class="min-w-0">
		<h1 class="truncate text-2xl font-semibold tracking-tight">{user.fullName}</h1>
		<p class="truncate text-sm text-muted-foreground" dir="ltr">{user.email}</p>
	</div>
	<div class="flex gap-2 sm:ms-auto">
		<Badge variant={user.role === 'ADMIN' ? 'default' : 'outline'}>{roles[user.role]()}</Badge>
		<UserStatusBadge {user} />
	</div>
</div>

<div class="grid gap-6 lg:grid-cols-3">
	<Card.Root class="lg:col-span-2">
		<Card.Header>
			<Card.Title>{m.admin_user_details()}</Card.Title>
			<Card.Description>{m.admin_user_joined({ date: formatDate(user.createdAt) })}</Card.Description>
		</Card.Header>
		<Card.Content>
			{#key user.id}
				<UserDetailsForm {user} {isSelf} />
			{/key}
		</Card.Content>
	</Card.Root>

	<div class="space-y-6">
		<Card.Root>
			<Card.Content>
				<dl class="space-y-3 text-sm">
					<div>
						<dt class="text-muted-foreground">{m.admin_user_sign_in_methods()}</dt>
						<dd class="flex flex-wrap gap-1 pt-1">
							{#if user.hasPassword}<Badge variant="outline">{m.admin_method_password()}</Badge>{/if}
							{#if user.googleLinked}<Badge variant="outline">{m.admin_method_google()}</Badge>{/if}
							{#if !user.hasPassword && !user.googleLinked}<span>{m.admin_method_none()}</span>{/if}
						</dd>
					</div>
					<div>
						<dt class="text-muted-foreground">{m.admin_col_last_login()}</dt>
						<dd title={user.lastLoginAt ? formatDateTime(user.lastLoginAt) : undefined}>
							{user.lastLoginAt ? formatRelative(user.lastLoginAt) : m.common_never()}
						</dd>
					</div>
					<div>
						<dt class="text-muted-foreground">{m.admin_stat_sessions()}</dt>
						<dd>{formatNumber(user.activeSessions)}</dd>
					</div>
					<div>
						<dt class="text-muted-foreground">{m.admin_user_language()}</dt>
						<dd>{LOCALE_NAMES[user.locale as Locale] ?? user.locale}</dd>
					</div>
				</dl>
			</Card.Content>
		</Card.Root>

		<Card.Root>
			<Card.Content class="flex flex-col gap-2">
				<Button variant="outline" onclick={sendPasswordEmail} disabled={!user.enabled}>
					<KeyRound />{m.admin_user_send_password()}
				</Button>
				<Button variant="outline" onclick={revokeSessions} disabled={isSelf || user.activeSessions === 0}>
					<LogOut />{m.admin_user_revoke()}
				</Button>
				<Button variant="destructive" onclick={() => (confirmingDelete = true)} disabled={isSelf}>
					<Trash2 />{m.admin_user_delete()}
				</Button>
			</Card.Content>
		</Card.Root>
	</div>
</div>

<ConfirmDialog
	bind:open={confirmingDelete}
	title={m.admin_user_delete_title({ name: user.fullName })}
	description={m.admin_user_delete_body()}
	confirmLabel={m.common_delete()}
	onConfirm={remove}
/>
