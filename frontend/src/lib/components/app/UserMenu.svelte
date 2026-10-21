<script lang="ts">
	import ChevronsUpDown from '@lucide/svelte/icons/chevrons-up-down';
	import LogOut from '@lucide/svelte/icons/log-out';
	import ShieldCheck from '@lucide/svelte/icons/shield-check';
	import UserRound from '@lucide/svelte/icons/user-round';
	import { goto } from '$app/navigation';
	import { session } from '$lib/auth/session.svelte';
	import * as DropdownMenu from '$lib/components/ui/dropdown-menu';
	import * as Sidebar from '$lib/components/ui/sidebar';
	import { textDirection } from '$lib/i18n/locale';
	import { m } from '$lib/paraglide/messages';
	import UserAvatar from './UserAvatar.svelte';

	const sidebar = Sidebar.useSidebar();
	// Popovers position physically, so "beside the sidebar" means left in right-to-left layouts.
	const side = $derived(sidebar.isMobile ? 'bottom' : textDirection() === 'rtl' ? 'left' : 'right');

	// The app layout notices the session ended and goes to the login page.
	const signOut = () => session.signOut();
</script>

{#if session.user}
	{@const user = session.user}
	<Sidebar.Menu>
		<Sidebar.MenuItem>
			<DropdownMenu.Root>
				<DropdownMenu.Trigger>
					{#snippet child({ props })}
						<Sidebar.MenuButton size="lg" class="data-[state=open]:bg-sidebar-accent" {...props}>
							<UserAvatar name={user.fullName} src={user.avatarUrl} class="rounded-lg" />
							<span class="grid flex-1 text-start text-sm leading-tight">
								<span class="truncate font-medium">{user.fullName}</span>
								<span class="truncate text-xs text-muted-foreground">{user.email}</span>
							</span>
							<ChevronsUpDown class="ms-auto size-4" />
						</Sidebar.MenuButton>
					{/snippet}
				</DropdownMenu.Trigger>
				<DropdownMenu.Content class="min-w-56 rounded-lg" {side} align="end" sideOffset={4}>
					<DropdownMenu.Label class="font-normal">
						<span class="block truncate font-medium">{user.fullName}</span>
						<span class="block truncate text-xs text-muted-foreground">{user.email}</span>
					</DropdownMenu.Label>
					<DropdownMenu.Separator />
					<DropdownMenu.Item onSelect={() => goto('/settings/profile')}>
						<UserRound />
						{m.nav_profile()}
					</DropdownMenu.Item>
					<DropdownMenu.Item onSelect={() => goto('/settings/security')}>
						<ShieldCheck />
						{m.nav_security()}
					</DropdownMenu.Item>
					<DropdownMenu.Separator />
					<DropdownMenu.Item onSelect={signOut}>
						<LogOut />
						{m.nav_sign_out()}
					</DropdownMenu.Item>
				</DropdownMenu.Content>
			</DropdownMenu.Root>
		</Sidebar.MenuItem>
	</Sidebar.Menu>
{/if}
