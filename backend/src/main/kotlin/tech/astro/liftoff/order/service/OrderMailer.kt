package tech.astro.liftoff.order.service

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import tech.astro.liftoff.mail.MailKind
import tech.astro.liftoff.mail.OutgoingMail
import tech.astro.liftoff.order.model.ShopOrder
import tech.astro.liftoff.shared.config.AppProperties
import tech.astro.liftoff.shared.i18n.SupportedLocales
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Order emails, sent after the transaction commits. Texts: `mail.order_*` in i18n/messages*.properties. */
@Component
class OrderMailer(
    private val events: ApplicationEventPublisher,
    private val props: AppProperties,
) {
    fun placed(order: ShopOrder, name: String, locale: String) = send(order, name, locale, MailKind.ORDER_PLACED)

    fun shipped(order: ShopOrder, name: String, locale: String) = send(order, name, locale, MailKind.ORDER_SHIPPED)

    fun cancelled(order: ShopOrder, name: String, locale: String) = send(order, name, locale, MailKind.ORDER_CANCELLED)

    private fun send(order: ShopOrder, name: String, locale: String, kind: MailKind) {
        val link = UriComponentsBuilder.fromUriString(props.frontendUrl.trimEnd('/')).path("/orders/{id}").buildAndExpand(order.id).toUriString()
        val args = listOf("#${order.number}", formatMoney(order.totalCents, order.currency, locale))
        events.publishEvent(OutgoingMail(order.email, name, locale, kind, link, args))
    }

    companion object {
        /** Amounts are in minor units: cents for EUR, but whole yen for JPY and millimes for TND. */
        fun formatMoney(minor: Long, currency: String, locale: String): String {
            val code = runCatching { Currency.getInstance(currency) }.getOrNull() ?: return "${BigDecimal.valueOf(minor, 2)} $currency"
            // Latin digits in Arabic, like the web app (see frontend/src/lib/i18n/format.ts).
            val tag = SupportedLocales.normalize(locale).let { if (it == "ar") "ar-u-nu-latn" else it }
            val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag(tag))
            format.currency = code
            return format.format(BigDecimal.valueOf(minor, code.defaultFractionDigits.coerceAtLeast(0)))
        }
    }
}
