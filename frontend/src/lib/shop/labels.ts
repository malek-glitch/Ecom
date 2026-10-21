import { m } from '$lib/paraglide/messages';
import type { OrderStatus, PaymentMethod } from './store-api';

const STATUS: Record<OrderStatus, () => string> = {
	PENDING: m.order_status_pending,
	PAID: m.order_status_paid,
	SHIPPED: m.order_status_shipped,
	DELIVERED: m.order_status_delivered,
	CANCELLED: m.order_status_cancelled,
};

const PAYMENT: Record<PaymentMethod, () => string> = {
	CASH_ON_DELIVERY: m.payment_cash_on_delivery,
	BANK_TRANSFER: m.payment_bank_transfer,
};

export const ORDER_STATUSES = Object.keys(STATUS) as OrderStatus[];

export const orderStatusLabel = (status: OrderStatus) => STATUS[status]();

export const paymentMethodLabel = (method: PaymentMethod) => PAYMENT[method]();

/** "#1001": how people refer to an order. */
export const orderNumber = (number: number) => `#${number}`;
