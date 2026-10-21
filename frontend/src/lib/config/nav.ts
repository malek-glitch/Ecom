import FileClock from '@lucide/svelte/icons/file-clock';
import Gauge from '@lucide/svelte/icons/gauge';
import LayoutDashboard from '@lucide/svelte/icons/layout-dashboard';
import NotebookPen from '@lucide/svelte/icons/notebook-pen'; // [example:notes]
import SlidersHorizontal from '@lucide/svelte/icons/sliders-horizontal';
import Users from '@lucide/svelte/icons/users';
import { m } from '$lib/paraglide/messages';

export interface NavItem {
	href: string;
	/** A function so the label is translated at render time. */
	label: () => string;
	icon: typeof LayoutDashboard;
	/** Match only this exact path (for section roots like /admin). */
	exact?: boolean;
}

export interface NavGroup {
	label: () => string;
	items: NavItem[];
	adminOnly?: boolean;
}

/** The sidebar. Add an entry here when you add a page. */
export const NAVIGATION: NavGroup[] = [
	{
		label: m.nav_workspace,
		items: [
			{ href: '/dashboard', label: m.nav_dashboard, icon: LayoutDashboard },
			// Add your pages here.
			{ href: '/notes', label: m.nav_notes, icon: NotebookPen }, // [example:notes]
		],
	},
	{
		label: m.nav_administration,
		adminOnly: true,
		items: [
			{ href: '/admin', label: m.nav_admin_overview, icon: Gauge, exact: true },
			{ href: '/admin/users', label: m.nav_admin_users, icon: Users },
			{ href: '/admin/settings', label: m.nav_admin_settings, icon: SlidersHorizontal },
			{ href: '/admin/audit', label: m.nav_admin_audit, icon: FileClock },
		],
	},
];
