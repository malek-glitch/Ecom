<script lang="ts">
	import Trash2 from '@lucide/svelte/icons/trash-2';
	import Upload from '@lucide/svelte/icons/upload';
	import { toast } from 'svelte-sonner';
	import { z } from 'zod';
	import { api, call } from '$lib/api/client';
	import { errorMessage } from '$lib/api/errors';
	import { session } from '$lib/auth/session.svelte';
	import FormAlert from '$lib/components/app/FormAlert.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import SubmitButton from '$lib/components/app/SubmitButton.svelte';
	import UserAvatar from '$lib/components/app/UserAvatar.svelte';
	import { Button } from '$lib/components/ui/button';
	import * as Card from '$lib/components/ui/card';
	import * as Field from '$lib/components/ui/field';
	import { Input } from '$lib/components/ui/input';
	import * as Select from '$lib/components/ui/select';
	import { Spinner } from '$lib/components/ui/spinner';
	import { createForm } from '$lib/forms/form.svelte';
	import { required } from '$lib/forms/schemas';
	import { LOCALE_NAMES, getLocale, locales, type Locale } from '$lib/i18n/locale';
	import { m } from '$lib/paraglide/messages';
	import { setLocale } from '$lib/paraglide/runtime';

	let { data } = $props();

	const user = $derived(session.user!);
	const MAX_AVATAR_BYTES = 5 * 1024 * 1024; // the backend's app.storage.max-avatar-size
	let fileInput = $state<HTMLInputElement>();
	let avatarBusy = $state(false);

	const form = createForm({
		schema: z.object({ fullName: required(100), locale: z.enum(locales) }),
		initial: { fullName: session.user!.fullName, locale: getLocale() },
		onSubmit: async (values) => {
			session.user = await call(api.PATCH('/api/v1/me', { body: values }));
			toast.success(m.profile_saved());
			// The account language is also the interface language.
			if (values.locale !== getLocale()) setLocale(values.locale as Locale);
		},
	});

	async function uploadAvatar(event: Event & { currentTarget: HTMLInputElement }) {
		const file = event.currentTarget.files?.[0];
		event.currentTarget.value = ''; // so picking the same file again still triggers a change
		if (!file) return;
		if (file.size > MAX_AVATAR_BYTES) {
			toast.error(m.error_file_too_large());
			return;
		}
		const body = new FormData();
		body.append('file', file);
		avatarBusy = true;
		try {
			// FormData passes through openapi-fetch untouched; the browser sets the multipart boundary.
			session.user = await call(api.POST('/api/v1/me/avatar', { body: body as unknown as { file: string } }));
			toast.success(m.profile_avatar_saved());
		} catch (error) {
			toast.error(errorMessage(error));
		} finally {
			avatarBusy = false;
		}
	}

	async function removeAvatar() {
		avatarBusy = true;
		try {
			session.user = await call(api.DELETE('/api/v1/me/avatar'));
			toast.success(m.profile_avatar_removed());
		} catch (error) {
			toast.error(errorMessage(error));
		} finally {
			avatarBusy = false;
		}
	}
</script>

<Seo title={m.profile_title()} />

<Card.Root>
	<Card.Header>
		<Card.Title>{m.profile_title()}</Card.Title>
		<Card.Description>{m.profile_subtitle()}</Card.Description>
	</Card.Header>
	<Card.Content class="space-y-8">
		{#if data.config.storageEnabled}
			<div class="flex flex-wrap items-center gap-4">
				<UserAvatar name={user.fullName} src={user.avatarUrl} class="size-16 text-base" />
				<div class="space-y-2">
					<div class="flex flex-wrap gap-2">
						<Button variant="outline" size="sm" disabled={avatarBusy} onclick={() => fileInput?.click()}>
							{#if avatarBusy}<Spinner />{:else}<Upload />{/if}
							{m.profile_avatar_upload()}
						</Button>
						{#if user.avatarUrl}
							<Button variant="ghost" size="sm" disabled={avatarBusy} onclick={removeAvatar}>
								<Trash2 />{m.profile_avatar_remove()}
							</Button>
						{/if}
					</div>
					<p class="text-xs text-muted-foreground">{m.profile_avatar_hint()}</p>
				</div>
				<input
					bind:this={fileInput}
					type="file"
					accept="image/jpeg,image/png,image/webp,image/gif"
					class="hidden"
					onchange={uploadAvatar}
				/>
			</div>
		{/if}

		<form onsubmit={form.submit} novalidate class="space-y-6">
			<FormAlert message={form.formError} />
			<Field.Group>
				<Field.Field data-invalid={!!form.errors.fullName}>
					<Field.Label for="fullName">{m.profile_name()}</Field.Label>
					<Input
						id="fullName"
						autocomplete="name"
						bind:value={form.values.fullName}
						aria-invalid={!!form.errors.fullName}
					/>
					<Field.Error errors={form.fieldErrors('fullName')} />
				</Field.Field>
				<Field.Field>
					<Field.Label for="email">{m.profile_email()}</Field.Label>
					<Input id="email" value={user.email} readonly disabled dir="ltr" class="text-start" />
					<Field.Description>{m.profile_email_hint()}</Field.Description>
				</Field.Field>
				<Field.Field>
					<Field.Label for="locale">{m.profile_language()}</Field.Label>
					<Select.Root type="single" bind:value={form.values.locale}>
						<Select.Trigger id="locale" class="w-full sm:w-64"
							>{LOCALE_NAMES[form.values.locale as Locale]}</Select.Trigger
						>
						<Select.Content>
							{#each locales as locale (locale)}
								<Select.Item value={locale} label={LOCALE_NAMES[locale]} lang={locale} />
							{/each}
						</Select.Content>
					</Select.Root>
					<Field.Description>{m.profile_language_hint()}</Field.Description>
				</Field.Field>
			</Field.Group>
			<div class="flex justify-end">
				<SubmitButton busy={form.submitting}>{m.common_save()}</SubmitButton>
			</div>
		</form>
	</Card.Content>
</Card.Root>
