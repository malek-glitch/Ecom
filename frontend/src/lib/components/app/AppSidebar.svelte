<script lang="ts">
	import { page } from '$app/state';
	import { session } from '$lib/auth/session.svelte';
	import * as Sidebar from '$lib/components/ui/sidebar';
	import { NAVIGATION } from '$lib/config/nav';
	import Logo from './Logo.svelte';
	import UserMenu from './UserMenu.svelte';

	let { appName }: { appName: string } = $props();

	const groups = $derived(NAVIGATION.filter((group) => !group.adminOnly || session.isAdmin));

	function isActive(href: string, exact = false) {
		const path = page.url.pathname;
		return exact ? path === href : path === href || path.startsWith(`${href}/`);
	}
</script>

<!-- side="left" means the start edge: the component uses logical CSS, so it sits on the right in Arabic. -->
<Sidebar.Root collapsible="icon">
	<Sidebar.Header>
		<Sidebar.Menu>
			<Sidebar.MenuItem>
				<Sidebar.MenuButton size="lg">
					{#snippet child({ props })}
						<a href="/dashboard" {...props}><Logo name={appName} /></a>
					{/snippet}
				</Sidebar.MenuButton>
			</Sidebar.MenuItem>
		</Sidebar.Menu>
	</Sidebar.Header>
	<Sidebar.Content>
		{#each groups as group (group.label)}
			<Sidebar.Group>
				<Sidebar.GroupLabel>{group.label()}</Sidebar.GroupLabel>
				<Sidebar.GroupContent>
					<Sidebar.Menu>
						{#each group.items as item (item.href)}
							<Sidebar.MenuItem>
								<Sidebar.MenuButton isActive={isActive(item.href, item.exact)} tooltipContent={item.label()}>
									{#snippet child({ props })}
										<a href={item.href} {...props}>
											<item.icon />
											<span>{item.label()}</span>
										</a>
									{/snippet}
								</Sidebar.MenuButton>
							</Sidebar.MenuItem>
						{/each}
					</Sidebar.Menu>
				</Sidebar.GroupContent>
			</Sidebar.Group>
		{/each}
	</Sidebar.Content>
	<Sidebar.Footer>
		<UserMenu />
	</Sidebar.Footer>
	<Sidebar.Rail />
</Sidebar.Root>
