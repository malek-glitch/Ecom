<script lang="ts">
	import { invalidate } from '$app/navigation';
	import { toast } from 'svelte-sonner';
	import { z } from 'zod';
	import { api, call } from '$lib/api/client';
	import type { components } from '$lib/api/schema';
	import FormAlert from '$lib/components/app/FormAlert.svelte';
	import SubmitButton from '$lib/components/app/SubmitButton.svelte';
	import * as Field from '$lib/components/ui/field';
	import { Input } from '$lib/components/ui/input';
	import * as Select from '$lib/components/ui/select';
	import { Switch } from '$lib/components/ui/switch';
	import { createForm } from '$lib/forms/form.svelte';
	import { required } from '$lib/forms/schemas';
	import { m } from '$lib/paraglide/messages';

	interface Props {
		user: components['schemas']['AdminUserDetail'];
		/** Your own account: the API refuses role and status changes on it, so they're not offered. */
		isSelf: boolean;
	}

	let { user, isSelf }: Props = $props();

	const roles = { USER: m.admin_role_user, ADMIN: m.admin_role_admin };

	const form = createForm({
		schema: z.object({ fullName: required(100), role: z.enum(['USER', 'ADMIN']), enabled: z.boolean() }),
		initial: () => ({ fullName: user.fullName, role: user.role, enabled: user.enabled }),
		onSubmit: async (values) => {
			await call(api.PATCH('/api/v1/admin/users/{id}', { params: { path: { id: user.id } }, body: values }));
			toast.success(m.admin_user_saved());
			await invalidate('app:admin-user');
			form.reset();
		},
	});
</script>

<form onsubmit={form.submit} novalidate class="space-y-6">
	<FormAlert message={form.formError} />
	<Field.Group>
		<Field.Field data-invalid={!!form.errors.fullName}>
			<Field.Label for="fullName">{m.auth_full_name()}</Field.Label>
			<Input id="fullName" bind:value={form.values.fullName} aria-invalid={!!form.errors.fullName} />
			<Field.Error errors={form.fieldErrors('fullName')} />
		</Field.Field>
		<Field.Field>
			<Field.Label for="role">{m.admin_col_role()}</Field.Label>
			<Select.Root type="single" bind:value={form.values.role} disabled={isSelf}>
				<Select.Trigger id="role" class="w-full sm:w-56">{roles[form.values.role]()}</Select.Trigger>
				<Select.Content>
					<Select.Item value="USER" label={m.admin_role_user()} />
					<Select.Item value="ADMIN" label={m.admin_role_admin()} />
				</Select.Content>
			</Select.Root>
		</Field.Field>
		<Field.Field orientation="horizontal">
			<Field.Content>
				<Field.Label for="enabled">{m.admin_user_enabled()}</Field.Label>
				<Field.Description>{m.admin_user_enabled_hint()}</Field.Description>
			</Field.Content>
			<Switch id="enabled" bind:checked={form.values.enabled} disabled={isSelf} />
		</Field.Field>
	</Field.Group>
	<div class="flex justify-end">
		<SubmitButton busy={form.submitting}>{m.common_save()}</SubmitButton>
	</div>
</form>
