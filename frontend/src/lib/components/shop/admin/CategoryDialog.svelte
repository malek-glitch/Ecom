<script lang="ts">
	import { z } from 'zod';
	import { toast } from 'svelte-sonner';
	import { invalidate } from '$app/navigation';
	import { api, call } from '$lib/api/client';
	import type { components } from '$lib/api/schema';
	import FormAlert from '$lib/components/app/FormAlert.svelte';
	import SubmitButton from '$lib/components/app/SubmitButton.svelte';
	import { Button } from '$lib/components/ui/button';
	import * as Dialog from '$lib/components/ui/dialog';
	import * as Field from '$lib/components/ui/field';
	import { Input } from '$lib/components/ui/input';
	import { Textarea } from '$lib/components/ui/textarea';
	import { createForm } from '$lib/forms/form.svelte';
	import { required } from '$lib/forms/schemas';
	import { m } from '$lib/paraglide/messages';

	type Category = components['schemas']['CategoryResponse'];

	/** Creates a category, or edits [category]. Render inside {#key} so each opening starts fresh. */
	let { open = $bindable(false), category = null }: { open?: boolean; category?: Category | null } = $props();

	// Limits mirror SaveCategoryRequest (CatalogDtos.kt).
	const schema = z.object({
		name: required(120),
		slug: z
			.string()
			.trim()
			.max(80, { error: () => m.validation_too_long() })
			.refine((value) => value === '' || /^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(value), {
				error: () => m.validation_slug(),
			}),
		description: z.string().max(2000, { error: () => m.validation_too_long() }),
		seoTitle: z
			.string()
			.trim()
			.max(70, { error: () => m.validation_too_long() }),
		seoDescription: z
			.string()
			.trim()
			.max(200, { error: () => m.validation_too_long() }),
		position: z.string().refine((value) => /^\d{1,5}$/.test(value.trim()) && Number(value) <= 10_000, {
			error: () => m.validation_invalid(),
		}),
	});

	const form = createForm({
		schema,
		initial: () => ({
			name: category?.name ?? '',
			slug: category?.slug ?? '',
			description: category?.description ?? '',
			position: String(category?.position ?? 0),
			seoTitle: category?.seoTitle ?? '',
			seoDescription: category?.seoDescription ?? '',
		}),
		onSubmit: async (values) => {
			const body = { ...values, position: Number(values.position) };
			if (category) {
				await call(api.PUT('/api/v1/admin/shop/categories/{id}', { params: { path: { id: category.id } }, body }));
			} else {
				await call(api.POST('/api/v1/admin/shop/categories', { body }));
			}
			toast.success(m.category_saved());
			open = false;
			await invalidate('app:admin-categories');
		},
	});
</script>

<Dialog.Root bind:open>
	<Dialog.Content class="max-h-[90svh] overflow-y-auto sm:max-w-lg">
		<Dialog.Header>
			<Dialog.Title>{category ? m.category_edit() : m.category_new()}</Dialog.Title>
		</Dialog.Header>
		<form onsubmit={form.submit} novalidate class="space-y-6">
			<FormAlert message={form.formError} />
			<Field.Group>
				<Field.Field data-invalid={!!form.errors.name}>
					<Field.Label for="category-name">{m.category_field_name()}</Field.Label>
					<Input
						id="category-name"
						bind:value={form.values.name}
						aria-invalid={!!form.errors.name}
						maxlength={120}
						required
					/>
					<Field.Error errors={form.fieldErrors('name')} />
				</Field.Field>
				<Field.Field data-invalid={!!form.errors.description}>
					<Field.Label for="category-description">{m.product_field_description()}</Field.Label>
					<Textarea id="category-description" bind:value={form.values.description} maxlength={2000} />
					<Field.Error errors={form.fieldErrors('description')} />
				</Field.Field>
				<div class="grid gap-4 sm:grid-cols-2">
					<Field.Field data-invalid={!!form.errors.slug}>
						<Field.Label for="category-slug">{m.field_slug()}</Field.Label>
						<Input
							id="category-slug"
							dir="ltr"
							bind:value={form.values.slug}
							aria-invalid={!!form.errors.slug}
							maxlength={80}
						/>
						<Field.Description>{m.field_slug_hint()}</Field.Description>
						<Field.Error errors={form.fieldErrors('slug')} />
					</Field.Field>
					<Field.Field data-invalid={!!form.errors.position}>
						<Field.Label for="category-position">{m.category_field_position()}</Field.Label>
						<Input
							id="category-position"
							inputmode="numeric"
							dir="ltr"
							bind:value={form.values.position}
							aria-invalid={!!form.errors.position}
						/>
						<Field.Description>{m.category_field_position_hint()}</Field.Description>
						<Field.Error errors={form.fieldErrors('position')} />
					</Field.Field>
				</div>
				<Field.Field data-invalid={!!form.errors.seoTitle}>
					<Field.Label for="category-seo-title">{m.seo_title()}</Field.Label>
					<Input
						id="category-seo-title"
						bind:value={form.values.seoTitle}
						placeholder={form.values.name}
						maxlength={70}
					/>
					<Field.Error errors={form.fieldErrors('seoTitle')} />
				</Field.Field>
				<Field.Field data-invalid={!!form.errors.seoDescription}>
					<Field.Label for="category-seo-description">{m.seo_description()}</Field.Label>
					<Textarea
						id="category-seo-description"
						bind:value={form.values.seoDescription}
						placeholder={form.values.description.slice(0, 160)}
						maxlength={200}
						class="min-h-16"
					/>
					<Field.Description>{m.seo_section_hint()}</Field.Description>
					<Field.Error errors={form.fieldErrors('seoDescription')} />
				</Field.Field>
			</Field.Group>
			<Dialog.Footer>
				<Button type="button" variant="outline" onclick={() => (open = false)}>{m.common_cancel()}</Button>
				<SubmitButton busy={form.submitting}>{category ? m.common_save() : m.common_create()}</SubmitButton>
			</Dialog.Footer>
		</form>
	</Dialog.Content>
</Dialog.Root>
