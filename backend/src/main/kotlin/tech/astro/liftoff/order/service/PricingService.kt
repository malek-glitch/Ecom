package tech.astro.liftoff.order.service

import org.springframework.stereotype.Service
import tech.astro.liftoff.settings.Setting
import tech.astro.liftoff.settings.SettingsService

/** Store-wide money rules, read from the admin settings so they change without a redeploy. */
@Service
class PricingService(private val settings: SettingsService) {

    fun currency(): String = settings.string(Setting.SHOP_CURRENCY)

    /** Null when the free-shipping offer is off. */
    fun freeShippingThreshold(): Long? = settings.int(Setting.SHOP_FREE_SHIPPING_THRESHOLD).toLong().takeIf { it > 0 }

    fun shipping(subtotalCents: Long): Long {
        if (subtotalCents <= 0) return 0
        val threshold = freeShippingThreshold()
        if (threshold != null && subtotalCents >= threshold) return 0
        return settings.int(Setting.SHOP_SHIPPING_FLAT).toLong().coerceAtLeast(0)
    }

    /** Bank transfer is offered only once the admin has entered bank details. */
    fun bankTransferEnabled(): Boolean = settings.string(Setting.SHOP_BANK_TRANSFER_INSTRUCTIONS).isNotBlank()
}
