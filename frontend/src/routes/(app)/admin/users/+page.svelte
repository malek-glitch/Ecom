<script lang="ts">
	import Search from '@lucide/svelte/icons/search';
	import UserPlus from '@lucide/svelte/icons/user-plus';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import InviteUserDialog from '$lib/components/admin/InviteUserDialog.svelte';
	import UserStatusBadge from '$lib/components/admin/UserStatusBadge.svelte';
	import PageHeader from '$lib/components/app/PageHeader.svelte';
	import PaginationBar from '$lib/components/app/PaginationBar.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import UserAvatar from '$lib/components/app/UserAvatar.svelte';
	import { Badge } from '$lib/components/ui/badge';
	import { Button } from '$lib/components/ui/button';
	import * as Card from '$lib/components/ui/card';
	import { Input } from '$lib/components/ui/input';
	import * as Select from '$lib/components/ui/select';
	import * as Table from '$lib/components/ui/table';
	import { debounce } from '$lib/debounce';
	import { formatDate, formatRelative } from '$lib/i18n/format';
	import { withQuery } from '$lib/navigation';
	import { m } from '$lib/paraglide/messages';

	let { data } = $props();

	let inviting = $state(false);

	const ALL = 'ALL';
	const roleLabels: Record<string, () => string> = {
		ALL: m.admin_filter_all,
		USER: m.admin_role_user,
		ADMIN: m.admin_role_admin,
	};
	const statusLabels: Record<string, () => string> = {
		ALL: m.admin_filter_all,
		ACTIVE: m.admin_status_active,
		DISABLED: m.admin_status_disabled,
		UNVERIFIED: m.admin_status_unverified,
	};

	function filter(changes: Record<string, string | null>) {
		void goto(withQuery(page.url, { ...changes, page: null }), { keepFocus: true, noScroll: true, replaceState: true });
	}

	const search = debounce((value: string) => filter({ q: value.trim() }));
</script>

<Seo title={m.admin_users_title()} />

<PageHeader title={m.admin_users_title()} description={m.admin_users_subtitle()}>
	{#snippet actions()}
		<Button onclick={() => (inviting = true)}><UserPlus />{m.admin_invite()}</Button>
	{/snippet}
</PageHeader>

<div class="mb-4 flex flex-wrap items-center gap-3">
	<div class="relative w-full sm:max-w-xs">
		<Search class="pointer-events-none absolute start-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
		<Input
			type="search"
			class="ps-9"
			placeholder={m.admin_users_search()}
			aria-label={m.admin_users_search()}
			value={data.filters.q}
			oninput={(event) => search(event.currentTarget.value)}
		/>
	</div>
	<Select.Root
		type="single"
		value={data.filters.role ?? ALL}
		onValueChange={(value) => filter({ role: value === ALL ? null : value })}
	>
		<Select.Trigger class="w-40" aria-label={m.admin_filter_role()}>
			<span class="text-muted-foreground">{m.admin_filter_role()}:</span>
			{roleLabels[data.filters.role ?? ALL]()}
		</Select.Trigger>
		<Select.Content>
			{#each Object.entries(roleLabels) as [value, label] (value)}
				<Select.Item {value} label={label()} />
			{/each}
		</Select.Content>
	</Select.Root>
	<Select.Root
		type="single"
		value={data.filters.status ?? ALL}
		onValueChange={(value) => filter({ status: value === ALL ? null : value })}
	>
		<Select.Trigger class="w-48" aria-label={m.admin_filter_status()}>
			<span class="text-muted-foreground">{m.admin_filter_status()}:</span>
			{statusLabels[data.filters.status ?? ALL]()}
		</Select.Trigger>
		<Select.Content>
			{#each Object.entries(statusLabels) as [value, label] (value)}
				<Select.Item {value} label={label()} />
			{/each}
		</Select.Content>
	</Select.Root>
	<p class="ms-auto text-sm text-muted-foreground">{m.users_count({ count: data.users.totalItems })}</p>
</div>

<Card.Root class="py-0">
	<Table.Root>
		<Table.Header>
			<Table.Row>
				<Table.Head>{m.admin_col_user()}</Table.Head>
				<Table.Head>{m.admin_col_role()}</Table.Head>
				<Table.Head>{m.admin_col_status()}</Table.Head>
				<Table.Head class="hidden md:table-cell">{m.admin_col_last_login()}</Table.Head>
				<Table.Head class="hidden lg:table-cell">{m.admin_col_created()}</Table.Head>
			</Table.Row>
		</Table.Header>
		<Table.Body>
			{#each data.users.items as user (user.id)}
				<Table.Row class="relative">
					<Table.Cell>
						<div class="flex items-center gap-3">
							<UserAvatar name={user.fullName} src={user.avatarUrl} />
							<div class="min-w-0">
								<!-- The link covers the whole row (after:absolute), so any cell opens the user. -->
								<a
									href="/admin/users/{user.id}"
									class="block truncate font-medium after:absolute after:inset-0 hover:underline"
								>
									{user.fullName}
								</a>
								<span class="block truncate text-xs text-muted-foreground" dir="ltr">{user.email}</span>
							</div>
						</div>
					</Table.Cell>
					<Table.Cell>
						<Badge variant={user.role === 'ADMIN' ? 'default' : 'outline'}>
							{user.role === 'ADMIN' ? m.admin_role_admin() : m.admin_role_user()}
						</Badge>
					</Table.Cell>
					<Table.Cell><UserStatusBadge {user} /></Table.Cell>
					<Table.Cell class="hidden text-muted-foreground md:table-cell">
						{user.lastLoginAt ? formatRelative(user.lastLoginAt) : m.common_never()}
					</Table.Cell>
					<Table.Cell class="hidden text-muted-foreground lg:table-cell">{formatDate(user.createdAt)}</Table.Cell>
				</Table.Row>
			{:else}
				<Table.Row>
					<Table.Cell colspan={5} class="h-24 text-center text-muted-foreground">{m.admin_no_users()}</Table.Cell>
				</Table.Row>
			{/each}
		</Table.Body>
	</Table.Root>
</Card.Root>

<div class="mt-6">
	<PaginationBar
		page={data.users.page}
		size={data.users.size}
		totalItems={data.users.totalItems}
		onPageChange={(next) => goto(withQuery(page.url, { page: next || null }))}
	/>
</div>

<InviteUserDialog bind:open={inviting} onInvited={(id) => goto(`/admin/users/${id}`)} />
