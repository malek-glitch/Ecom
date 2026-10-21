import { z } from 'zod';
import { required } from '$lib/forms/schemas';
import { APP_SLUG } from '$lib/config';
import { m } from '$lib/paraglide/messages';

// Limits mirror the backend's AddressRequest and PlaceOrderRequest (OrderDtos.kt).
const optional = (max: number) =>
	z
		.string()
		.trim()
		.max(max, { error: () => m.validation_too_long() });

export const checkoutSchema = z.object({
	fullName: required(120),
	phone: optional(40),
	line1: required(200),
	line2: optional(200),
	city: required(120),
	postalCode: required(20),
	country: required(80),
	paymentMethod: z.enum(['CASH_ON_DELIVERY', 'BANK_TRANSFER']),
	note: optional(1000),
});

export type CheckoutValues = z.infer<typeof checkoutSchema>;

export type SavedAddress = Omit<CheckoutValues, 'paymentMethod' | 'note'>;

const ADDRESS_KEY = `${APP_SLUG}.address`;

/** The last shipping address used on this device, to prefill the next checkout. */
export function loadSavedAddress(): Partial<SavedAddress> {
	try {
		const saved: unknown = JSON.parse(localStorage.getItem(ADDRESS_KEY) ?? '{}');
		return saved && typeof saved === 'object' ? (saved as Partial<SavedAddress>) : {};
	} catch {
		return {};
	}
}

export function saveAddress({ fullName, phone, line1, line2, city, postalCode, country }: CheckoutValues) {
	try {
		localStorage.setItem(ADDRESS_KEY, JSON.stringify({ fullName, phone, line1, line2, city, postalCode, country }));
	} catch {
		// Not worth failing a checkout over.
	}
}
