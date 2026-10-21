import { z } from 'zod';
import type { components } from '$lib/api/schema';
import { required } from '$lib/forms/schemas';
import { m } from '$lib/paraglide/messages';
import { parseAmount, toAmountInput } from '$lib/shop/money';

export type AdminProduct = components['schemas']['AdminProductResponse'];
export type SaveProductRequest = components['schemas']['SaveProductRequest'];

const SLUG = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;

/** Prices are typed as text ("12.50") and converted to minor units on submit. Limits mirror CatalogDtos.kt. */
export const productSchema = (currency: string) =>
	z
		.object({
			name: required(200),
			slug: z
				.string()
				.trim()
				.max(120, { error: () => m.validation_too_long() })
				.refine((value) => value === '' || SLUG.test(value), { error: () => m.validation_slug() }),
			description: z.string().max(20_000, { error: () => m.validation_too_long() }),
			price: z
				.string()
				.refine((value) => parseAmount(value, currency) !== null, { error: () => m.validation_amount() }),
			compareAtPrice: z.string().refine((value) => value.trim() === '' || parseAmount(value, currency) !== null, {
				error: () => m.validation_amount(),
			}),
			sku: z
				.string()
				.trim()
				.max(64, { error: () => m.validation_too_long() }),
			stock: z.string().refine((value) => /^\d{1,7}$/.test(value.trim()) && Number(value) <= 1_000_000, {
				error: () => m.validation_stock(),
			}),
			active: z.boolean(),
			featured: z.boolean(),
			categoryId: z.string(),
		})
		.refine(
			(values) => {
				const compareAt = parseAmount(values.compareAtPrice, currency);
				const price = parseAmount(values.price, currency);
				return values.compareAtPrice.trim() === '' || compareAt === null || price === null || compareAt > price;
			},
			{ path: ['compareAtPrice'], error: () => m.validation_compare_at() },
		);

export type ProductValues = z.infer<ReturnType<typeof productSchema>>;

export const emptyProduct = (): ProductValues => ({
	name: '',
	slug: '',
	description: '',
	price: '',
	compareAtPrice: '',
	sku: '',
	stock: '0',
	active: true,
	featured: false,
	categoryId: '',
});

export const productValues = (product: AdminProduct, currency: string): ProductValues => ({
	name: product.name,
	slug: product.slug,
	description: product.description,
	price: toAmountInput(product.priceCents, currency),
	compareAtPrice: toAmountInput(product.compareAtPriceCents, currency),
	sku: product.sku ?? '',
	stock: String(product.stock),
	active: product.active,
	featured: product.featured,
	categoryId: product.category?.id ?? '',
});

export const toRequest = (values: ProductValues, currency: string): SaveProductRequest => ({
	name: values.name,
	slug: values.slug,
	description: values.description,
	priceCents: parseAmount(values.price, currency)!,
	compareAtPriceCents: values.compareAtPrice.trim() ? parseAmount(values.compareAtPrice, currency) : null,
	sku: values.sku,
	stock: Number(values.stock),
	active: values.active,
	featured: values.featured,
	categoryId: values.categoryId || null,
});
