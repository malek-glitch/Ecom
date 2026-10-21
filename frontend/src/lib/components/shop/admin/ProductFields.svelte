<script lang="ts">
	import type { components } from '$lib/api/schema';
	import { Checkbox } from '$lib/components/ui/checkbox';
	import * as Field from '$lib/components/ui/field';
	import { Input } from '$lib/components/ui/input';
	import * as Select from '$lib/components/ui/select';
	import { Textarea } from '$lib/components/ui/textarea';
	import type { Form } from '$lib/forms/form.svelte';
	import { m } from '$lib/paraglide/messages';
	import type { ProductValues } from './product-schema';

	/** Shared by the "new product" page and the edit page. */
	let {
		form,
		categories,
		currency,
		idPrefix = 'product',
	}: {
		form: Form<ProductValues>;
		categories: components['schemas']['CategoryResponse'][];
		currency: string;
		idPrefix?: string;
	} = $props();

	const NONE = 'none'; // the select's value for "no category" (the form keeps '')
	const categoryName = $derived(categories.find((category) => category.id === form.values.categoryId)?.name);
</script>

<Field.Group>
	<Field.Field data-invalid={!!form.errors.name}>
		<Field.Label for="{idPrefix}-name">{m.product_field_name()}</Field.Label>
		<Input
			id="{idPrefix}-name"
			bind:value={form.values.name}
			aria-invalid={!!form.errors.name}
			maxlength={200}
			required
		/>
		<Field.Error errors={form.fieldErrors('name')} />
	</Field.Field>

	<Field.Field data-invalid={!!form.errors.description}>
		<Field.Label for="{idPrefix}-description">{m.product_field_description()}</Field.Label>
		<Textarea
			id="{idPrefix}-description"
			bind:value={form.values.description}
			aria-invalid={!!form.errors.description}
			class="min-h-32"
			maxlength={20000}
		/>
		<Field.Error errors={form.fieldErrors('description')} />
	</Field.Field>

	<div class="grid gap-4 sm:grid-cols-3">
		<Field.Field data-invalid={!!form.errors.price}>
			<Field.Label for="{idPrefix}-price">{m.product_field_price({ currency })}</Field.Label>
			<Input
				id="{idPrefix}-price"
				inputmode="decimal"
				dir="ltr"
				bind:value={form.values.price}
				aria-invalid={!!form.errors.price}
				placeholder="0.00"
				required
			/>
			<Field.Error errors={form.fieldErrors('price')} />
		</Field.Field>
		<Field.Field data-invalid={!!form.errors.compareAtPrice}>
			<Field.Label for="{idPrefix}-compare">{m.product_field_compare_at({ currency })}</Field.Label>
			<Input
				id="{idPrefix}-compare"
				inputmode="decimal"
				dir="ltr"
				bind:value={form.values.compareAtPrice}
				aria-invalid={!!form.errors.compareAtPrice}
			/>
			<Field.Description>{m.product_field_compare_at_hint()}</Field.Description>
			<Field.Error errors={form.fieldErrors('compareAtPrice')} />
		</Field.Field>
		<Field.Field data-invalid={!!form.errors.stock}>
			<Field.Label for="{idPrefix}-stock">{m.product_field_stock()}</Field.Label>
			<Input
				id="{idPrefix}-stock"
				inputmode="numeric"
				dir="ltr"
				bind:value={form.values.stock}
				aria-invalid={!!form.errors.stock}
				required
			/>
			<Field.Error errors={form.fieldErrors('stock')} />
		</Field.Field>
	</div>

	<div class="grid gap-4 sm:grid-cols-3">
		<Field.Field>
			<Field.Label for="{idPrefix}-category">{m.product_field_category()}</Field.Label>
			<Select.Root
				type="single"
				value={form.values.categoryId || NONE}
				onValueChange={(value) => (form.values.categoryId = value === NONE ? '' : value)}
			>
				<Select.Trigger id="{idPrefix}-category" class="w-full">
					{categoryName ?? m.product_no_category()}
				</Select.Trigger>
				<Select.Content>
					<Select.Item value={NONE} label={m.product_no_category()} />
					{#each categories as category (category.id)}
						<Select.Item value={category.id} label={category.name} />
					{/each}
				</Select.Content>
			</Select.Root>
		</Field.Field>
		<Field.Field data-invalid={!!form.errors.sku}>
			<Field.Label for="{idPrefix}-sku">{m.shop_sku()}</Field.Label>
			<Input
				id="{idPrefix}-sku"
				dir="ltr"
				bind:value={form.values.sku}
				aria-invalid={!!form.errors.sku}
				maxlength={64}
			/>
			<Field.Error errors={form.fieldErrors('sku')} />
		</Field.Field>
		<Field.Field data-invalid={!!form.errors.slug}>
			<Field.Label for="{idPrefix}-slug">{m.field_slug()}</Field.Label>
			<Input
				id="{idPrefix}-slug"
				dir="ltr"
				bind:value={form.values.slug}
				aria-invalid={!!form.errors.slug}
				maxlength={120}
			/>
			<Field.Description>{m.field_slug_hint()}</Field.Description>
			<Field.Error errors={form.fieldErrors('slug')} />
		</Field.Field>
	</div>

	<Field.Field orientation="horizontal">
		<Checkbox id="{idPrefix}-active" bind:checked={form.values.active} />
		<Field.Content>
			<Field.Label for="{idPrefix}-active" class="font-normal">{m.product_field_active()}</Field.Label>
			<Field.Description>{m.product_field_active_hint()}</Field.Description>
		</Field.Content>
	</Field.Field>
	<Field.Field orientation="horizontal">
		<Checkbox id="{idPrefix}-featured" bind:checked={form.values.featured} />
		<Field.Content>
			<Field.Label for="{idPrefix}-featured" class="font-normal">{m.product_field_featured()}</Field.Label>
			<Field.Description>{m.product_field_featured_hint()}</Field.Description>
		</Field.Content>
	</Field.Field>
</Field.Group>
