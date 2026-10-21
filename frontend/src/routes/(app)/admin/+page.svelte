<script lang="ts">
	import KeyRound from '@lucide/svelte/icons/key-round';
	import MailWarning from '@lucide/svelte/icons/mail-warning';
	import ShieldCheck from '@lucide/svelte/icons/shield-check';
	import UserPlus from '@lucide/svelte/icons/user-plus';
	import UserX from '@lucide/svelte/icons/user-x';
	import Users from '@lucide/svelte/icons/users';
	import SignupChart from '$lib/components/admin/SignupChart.svelte';
	import PageHeader from '$lib/components/app/PageHeader.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import * as Card from '$lib/components/ui/card';
	import { formatNumber } from '$lib/i18n/format';
	import { m } from '$lib/paraglide/messages';

	let { data } = $props();

	const cards = $derived([
		{ label: m.admin_stat_users(), value: data.stats.totalUsers, icon: Users, href: '/admin/users' },
		{ label: m.admin_stat_new_users(), value: data.stats.newUsersLast7Days, icon: UserPlus, href: '/admin/users' },
		{ label: m.admin_stat_sessions(), value: data.stats.activeSessions, icon: KeyRound },
		{ label: m.admin_stat_admins(), value: data.stats.admins, icon: ShieldCheck, href: '/admin/users?role=ADMIN' },
		{
			label: m.admin_stat_unverified(),
			value: data.stats.unverifiedUsers,
			icon: MailWarning,
			href: '/admin/users?status=UNVERIFIED',
		},
		{
			label: m.admin_stat_disabled(),
			value: data.stats.disabledUsers,
			icon: UserX,
			href: '/admin/users?status=DISABLED',
		},
	]);
</script>

<Seo title={m.admin_overview_title()} />

<PageHeader title={m.admin_overview_title()} description={m.admin_overview_subtitle()} />

<div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
	{#each cards as card (card.label)}
		{@const Icon = card.icon}
		<svelte:element
			this={card.href ? 'a' : 'div'}
			href={card.href}
			class="group rounded-xl outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50"
		>
			<Card.Root class={card.href ? 'transition-colors group-hover:border-primary/40' : ''}>
				<Card.Header>
					<Card.Description class="flex items-center gap-2"><Icon class="size-4" />{card.label}</Card.Description>
					<Card.Title class="text-3xl tabular-nums">{formatNumber(card.value)}</Card.Title>
				</Card.Header>
			</Card.Root>
		</svelte:element>
	{/each}
</div>

<Card.Root class="mt-4">
	<Card.Header>
		<Card.Title>{m.admin_signups_title()}</Card.Title>
	</Card.Header>
	<Card.Content>
		<SignupChart data={data.stats.signupsPerDay} label={m.admin_signups_title()} />
	</Card.Content>
</Card.Root>
