<script lang="ts">
	import Banknote from '@lucide/svelte/icons/banknote';
	import Landmark from '@lucide/svelte/icons/landmark';
	import { toast } from 'svelte-sonner';
	import { goto } from '$app/navigation';
	import { api, call } from '$lib/api/client';
	import { ApiError } from '$lib/api/errors';
	import { session } from '$lib/auth/session.svelte';
	import FormAlert from '$lib/components/app/FormAlert.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import SubmitButton from '$lib/components/app/SubmitButton.svelte';
	import OrderTotals from '$lib/components/shop/OrderTotals.svelte';
	import ProductImage from '$lib/components/shop/ProductImage.svelte';
	import { Button } from '$lib/components/ui/button';
	import * as Card from '$lib/components/ui/card';
	import * as Field from '$lib/components/ui/field';
	import { Input } from '$lib/components/ui/input';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { Textarea } from '$lib/components/ui/textarea';
	import { createForm } from '$lib/forms/form.svelte';
	import { formatMoney } from '$lib/i18n/format';
	import { m } from '$lib/paraglide/messages';
	import { cart } from '$lib/shop/cart.svelte';
	import { checkoutSchema, loadSavedAddress, saveAddress, type CheckoutValues } from '$lib/shop/checkout';
	import { paymentMethodLabel } from '$lib/shop/labels';
	import { createCartQuote } from '$lib/shop/quote.svelte';
	import { shopSettings } from '$lib/shop/store-api';

	let { data } = $props();

	const shop = $derived(shopSettings(data.config.settings));
	const quote = createCartQuote();
	const currency = $derived(quote.quote?.currency ?? shop.currency);
	let placing = false; // keeps the "cart is empty" redirect from firing while we leave the page

	const methods = $derived([
		{ value: 'CASH_ON_DELIVERY' as const, icon: Banknote, hint: m.payment_cash_on_delivery_hint() },
		...(shop.bankTransferInstructions
			? [{ value: 'BANK_TRANSFER' as const, icon: Landmark, hint: m.payment_bank_transfer_hint() }]
			: []),
	]);

	const saved = loadSavedAddress();
	const form = createForm({
		schema: checkoutSchema,
		initial: {
			fullName: saved.fullName ?? session.user?.fullName ?? '',
			phone: saved.phone ?? '',
			line1: saved.line1 ?? '',
			line2: saved.line2 ?? '',
			city: saved.city ?? '',
			postalCode: saved.postalCode ?? '',
			country: saved.country ?? '',
			paymentMethod: 'CASH_ON_DELIVERY',
			note: '',
		} satisfies CheckoutValues,
		onSubmit: async ({ paymentMethod, note, ...address }) => {
			if (!quote.quote?.orderable) return;
			placing = true;
			try {
				const order = await call(
					api.POST('/api/v1/orders', {
						body: {
							items: cart.requestItems,
							shippingAddress: address,
							paymentMethod,
							note,
							expectedTotalCents: quote.quote.totalCents,
						},
					}),
				);
				saveAddress({ ...address, paymentMethod, note });
				cart.clear();
				toast.success(m.checkout_placed({ number: order.number }));
				await goto(`/orders/${order.id}?placed=1`, { replaceState: true });
			} catch (error) {
				placing = false;
				// Stock or prices moved under us: show the fresh cart next to the message.
				if (error instanceof ApiError && error.status === 409) quote.refresh();
				throw error;
			}
		},
	});

	$effect(() => {
		if (cart.isEmpty && !placing) void goto('/cart', { replaceState: true });
	});

	const fields = [
		{ name: 'fullName', label: m.checkout_full_name, autocomplete: 'name', max: 120, required: true },
		{ name: 'phone', label: m.checkout_phone, autocomplete: 'tel', max: 40, type: 'tel' },
		{ name: 'line1', label: m.checkout_line1, autocomplete: 'address-line1', max: 200, required: true },
		{ name: 'line2', label: m.checkout_line2, autocomplete: 'address-line2', max: 200 },
		{
			name: 'postalCode',
			label: m.checkout_postal_code,
			autocomplete: 'postal-code',
			max: 20,
			required: true,
			half: true,
		},
		{ name: 'city', label: m.checkout_city, autocomplete: 'address-level2', max: 120, required: true, half: true },
		{ name: 'country', label: m.checkout_country, autocomplete: 'country-name', max: 80, required: true },
	] as const;
</script>

<Seo title={m.checkout_title()} />

<div class="mx-auto max-w-6xl px-4 py-10 sm:px-6">
	<h1 class="mb-8 text-3xl font-semibold tracking-tight">{m.checkout_title()}</h1>

	<form onsubmit={form.submit} novalidate class="grid gap-8 lg:grid-cols-[1fr_22rem]">
		<div class="space-y-6">
			<Card.Root>
				<Card.Header>
					<Card.Title>{m.checkout_shipping()}</Card.Title>
					<Card.Description>{m.checkout_email_note({ email: session.user?.email ?? '' })}</Card.Description>
				</Card.Header>
				<Card.Content>
					<Field.Group class="grid gap-4 sm:grid-cols-2">
						{#each fields as field (field.name)}
							<Field.Field data-invalid={!!form.errors[field.name]} class={'half' in field ? '' : 'sm:col-span-2'}>
								<Field.Label for="checkout-{field.name}">
									{field.label()}
									{#if !('required' in field)}<span class="font-normal text-muted-foreground"
											>({m.common_optional()})</span
										>{/if}
								</Field.Label>
								<Input
									id="checkout-{field.name}"
									type={'type' in field ? field.type : 'text'}
									autocomplete={field.autocomplete}
									bind:value={form.values[field.name]}
									aria-invalid={!!form.errors[field.name]}
									maxlength={field.max}
									required={'required' in field}
								/>
								<Field.Error errors={form.fieldErrors(field.name)} />
							</Field.Field>
						{/each}
					</Field.Group>
				</Card.Content>
			</Card.Root>

			<Card.Root>
				<Card.Header>
					<Card.Title>{m.checkout_payment()}</Card.Title>
				</Card.Header>
				<Card.Content>
					<fieldset class="grid gap-3">
						<legend class="sr-only">{m.checkout_payment()}</legend>
						{#each methods as method (method.value)}
							<label
								class="flex cursor-pointer items-start gap-3 rounded-lg border p-4 transition-colors has-[:checked]:border-primary has-[:checked]:bg-primary/5 has-[:focus-visible]:ring-[3px] has-[:focus-visible]:ring-ring/50"
							>
								<input
									type="radio"
									name="paymentMethod"
									value={method.value}
									bind:group={form.values.paymentMethod}
									class="mt-1 accent-primary"
								/>
								<method.icon class="mt-0.5 size-5 shrink-0 text-muted-foreground" />
								<span class="space-y-1">
									<span class="block font-medium">{paymentMethodLabel(method.value)}</span>
									<span class="block text-sm text-muted-foreground">{method.hint}</span>
								</span>
							</label>
						{/each}
					</fieldset>
				</Card.Content>
			</Card.Root>

			<Card.Root>
				<Card.Content>
					<Field.Field data-invalid={!!form.errors.note}>
						<Field.Label for="checkout-note">
							{m.checkout_note()} <span class="font-normal text-muted-foreground">({m.common_optional()})</span>
						</Field.Label>
						<Textarea id="checkout-note" bind:value={form.values.note} maxlength={1000} class="min-h-20" />
						<Field.Error errors={form.fieldErrors('note')} />
					</Field.Field>
				</Card.Content>
			</Card.Root>
		</div>

		<aside>
			<Card.Root class="lg:sticky lg:top-24">
				<Card.Header>
					<Card.Title>{m.cart_summary()}</Card.Title>
					<Card.Action>
						<Button variant="link" size="sm" href="/cart" class="h-auto p-0">{m.common_edit()}</Button>
					</Card.Action>
				</Card.Header>
				<Card.Content class="space-y-4">
					<ul class="space-y-3">
						{#each cart.items as item (item.productId)}
							{@const line = quote.line(item.productId)}
							<li class="flex items-center gap-3 text-sm">
								<ProductImage src={item.imageUrl} alt="" class="size-12 shrink-0 rounded-md border" />
								<span class="min-w-0 flex-1">
									<span class="line-clamp-1">{item.name}</span>
									<span class="text-muted-foreground">× {item.quantity}</span>
								</span>
								<span class="tabular-nums">
									{formatMoney(line?.lineTotalCents ?? item.priceCents * item.quantity, currency)}
								</span>
							</li>
						{/each}
					</ul>
					{#if quote.quote}
						<OrderTotals
							subtotalCents={quote.quote.subtotalCents}
							shippingCents={quote.quote.shippingCents}
							totalCents={quote.quote.totalCents}
							{currency}
						/>
						{#if !quote.quote.orderable}
							<FormAlert message={m.cart_stock_problem()} />
						{/if}
					{:else}
						<Skeleton class="h-24 w-full" />
					{/if}
					<FormAlert message={form.formError ?? quote.error} />
				</Card.Content>
				<Card.Footer class="flex-col gap-3">
					<SubmitButton
						busy={form.submitting}
						disabled={!quote.quote?.orderable || quote.loading}
						class="w-full"
						size="lg"
					>
						{m.checkout_place_order()}
					</SubmitButton>
					<p class="text-center text-xs text-muted-foreground">{m.checkout_terms()}</p>
				</Card.Footer>
			</Card.Root>
		</aside>
	</form>
</div>
