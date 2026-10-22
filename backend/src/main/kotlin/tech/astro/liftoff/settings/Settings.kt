package tech.astro.liftoff.settings

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

/**
 * How a value is stored and edited. COLOR, CHOICE, URL and TEXT are strings with extra rules
 * (and a matching input in the admin UI); the public config serves them as strings.
 */
enum class SettingType { BOOLEAN, STRING, INTEGER, TEXT, URL, COLOR, CHOICE }

/**
 * Every runtime setting, declared once with its type and default. A missing database row
 * means "use the default", so the table only holds deliberate changes and a fresh database
 * behaves exactly like a live one. `clientVisible` settings are served to the frontend
 * through `GET /api/v1/public/config`.
 */
enum class Setting(
    val key: String,
    val type: SettingType,
    val default: String,
    val clientVisible: Boolean,
    val description: String,
    /** Extra rule for STRING values (checked on save), or for INTEGER values: the minimum. */
    val pattern: Regex? = null,
    val min: Int? = null,
    /** The allowed values of a CHOICE setting, the first being the usual default. */
    val options: List<String> = emptyList(),
    /** Longest accepted value for string-like types. */
    val maxLength: Int = 2000,
) {
    REGISTRATION_ENABLED(
        key = "auth.registrationEnabled",
        type = SettingType.BOOLEAN,
        default = "true",
        clientVisible = true,
        description = "Allow new people to create an account. Turn off for invite-only apps.",
    ),
    ANNOUNCEMENT(
        key = "app.announcement",
        type = SettingType.STRING,
        default = "",
        clientVisible = true,
        description = "Banner shown to every signed-in user. Leave empty to hide it.",
    ),
    SUPPORT_EMAIL(
        key = "app.supportEmail",
        type = SettingType.STRING,
        default = "",
        clientVisible = true,
        description = "Contact address shown in the app footer.",
    ),
    SHOP_CURRENCY(
        key = "shop.currency",
        type = SettingType.STRING,
        default = "EUR",
        clientVisible = true,
        description = "ISO 4217 code of the store currency (EUR, USD, MAD...). Existing orders keep the currency they were placed in.",
        pattern = Regex("^[A-Z]{3}$"),
    ),
    SHOP_SHIPPING_FLAT(
        key = "shop.shippingFlatCents",
        type = SettingType.INTEGER,
        default = "500",
        clientVisible = true,
        description = "Flat shipping fee per order, in cents (500 = 5.00). 0 for free shipping.",
        min = 0,
    ),
    SHOP_FREE_SHIPPING_THRESHOLD(
        key = "shop.freeShippingThresholdCents",
        type = SettingType.INTEGER,
        default = "5000",
        clientVisible = true,
        description = "Orders from this subtotal, in cents, ship free. 0 turns the offer off.",
        min = 0,
    ),
    SHOP_BANK_TRANSFER_INSTRUCTIONS(
        key = "shop.bankTransferInstructions",
        type = SettingType.TEXT,
        default = "",
        clientVisible = true,
        description = "Bank details shown to customers who pay by transfer. Leave empty to hide that payment option.",
    ),

    // ------------------------------------------------------------------ inventory
    INVENTORY_ENABLED(
        key = "inventory.enabled",
        type = SettingType.BOOLEAN,
        default = "true",
        clientVisible = true,
        description = "Track stock: orders take items out of stock and sold-out products can't be ordered. Off: everything can always be ordered.",
    ),
    INVENTORY_LOW_STOCK_THRESHOLD(
        key = "inventory.lowStockThreshold",
        type = SettingType.INTEGER,
        default = "5",
        clientVisible = false,
        description = "Products at or below this many units count as low stock (\"only 3 left\", admin alerts). Each product can override it.",
        min = 0,
    ),
    INVENTORY_LOW_STOCK_EMAILS(
        key = "inventory.lowStockEmails",
        type = SettingType.BOOLEAN,
        default = "true",
        clientVisible = false,
        description = "Email the administrators when a product runs low or sells out.",
    ),

    // ------------------------------------------------------------------ appearance
    THEME_BRAND_COLOR(
        key = "theme.brandColor",
        type = SettingType.COLOR,
        default = "",
        clientVisible = true,
        description = "Main colour of buttons, links and highlights, as #rrggbb. Empty: the neutral black-and-white theme.",
    ),
    THEME_RADIUS(
        key = "theme.radius",
        type = SettingType.CHOICE,
        default = "medium",
        clientVisible = true,
        description = "Corner roundness of buttons, cards and fields.",
        options = listOf("none", "small", "medium", "large", "full"),
    ),
    THEME_HEADING_FONT(
        key = "theme.headingFont",
        type = SettingType.CHOICE,
        default = "sans",
        clientVisible = true,
        description = "Typeface of page titles and headings.",
        options = listOf("sans", "serif", "mono"),
    ),
    THEME_PRODUCT_IMAGE_RATIO(
        key = "theme.productImageRatio",
        type = SettingType.CHOICE,
        default = "square",
        clientVisible = true,
        description = "Shape of product photos in lists: square (1:1), portrait (3:4) or landscape (4:3).",
        options = listOf("square", "portrait", "landscape"),
    ),
    STORE_LOGO_URL(
        key = "store.logoUrl",
        type = SettingType.URL,
        default = "",
        clientVisible = true,
        description = "Address of the logo image shown in the header (ideally an SVG or a PNG about 64 px high). Empty: an icon and the app name.",
    ),
    STORE_ANNOUNCEMENT_BAR(
        key = "store.announcementBar",
        type = SettingType.STRING,
        default = "",
        clientVisible = true,
        description = "Short message in a bar above the store header (\"Free delivery this week\"). Empty hides the bar.",
        maxLength = 160,
    ),
    STORE_HERO_KICKER(
        key = "store.heroKicker",
        type = SettingType.STRING,
        default = "",
        clientVisible = true,
        description = "Small line above the home page title. Empty: the built-in, translated text.",
        maxLength = 120,
    ),
    STORE_HERO_TITLE(
        key = "store.heroTitle",
        type = SettingType.STRING,
        default = "",
        clientVisible = true,
        description = "Home page title. Empty: the built-in, translated text.",
        maxLength = 120,
    ),
    STORE_HERO_SUBTITLE(
        key = "store.heroSubtitle",
        type = SettingType.TEXT,
        default = "",
        clientVisible = true,
        description = "Text under the home page title. Empty: the built-in, translated text.",
        maxLength = 400,
    ),
    STORE_HERO_IMAGE_URL(
        key = "store.heroImageUrl",
        type = SettingType.URL,
        default = "",
        clientVisible = true,
        description = "Address of a large picture next to the home page title. Empty: a text-only banner.",
    ),
    STORE_SHOW_PERKS(
        key = "store.showPerks",
        type = SettingType.BOOLEAN,
        default = "true",
        clientVisible = true,
        description = "Show the delivery, payment and returns highlights on the home page.",
    ),
    STORE_FOOTER_ABOUT(
        key = "store.footerAbout",
        type = SettingType.TEXT,
        default = "",
        clientVisible = true,
        description = "A few words about the store, shown in the footer.",
        maxLength = 500,
    ),
    STORE_INSTAGRAM_URL(
        key = "store.instagramUrl",
        type = SettingType.URL,
        default = "",
        clientVisible = true,
        description = "Instagram page, linked from the footer.",
    ),
    STORE_FACEBOOK_URL(
        key = "store.facebookUrl",
        type = SettingType.URL,
        default = "",
        clientVisible = true,
        description = "Facebook page, linked from the footer.",
    ),
    STORE_TIKTOK_URL(
        key = "store.tiktokUrl",
        type = SettingType.URL,
        default = "",
        clientVisible = true,
        description = "TikTok account, linked from the footer.",
    ),
    STORE_X_URL(
        key = "store.xUrl",
        type = SettingType.URL,
        default = "",
        clientVisible = true,
        description = "X (Twitter) account, linked from the footer.",
    ),

    // ------------------------------------------------------------------ search engines
    SEO_META_DESCRIPTION(
        key = "seo.metaDescription",
        type = SettingType.TEXT,
        default = "",
        clientVisible = true,
        description = "Description of the store shown by search engines for the home page (about 150 characters). Empty: the home page subtitle.",
        maxLength = 300,
    ),
    SEO_SHARE_IMAGE_URL(
        key = "seo.shareImageUrl",
        type = SettingType.URL,
        default = "",
        clientVisible = true,
        description = "Picture shown when a store page without its own photo is shared on social networks (1200 × 630 px works best).",
    ),
    ;

    companion object {
        fun byKey(key: String): Setting? = entries.firstOrNull { it.key == key }
    }
}

@Entity
@Table(name = "app_settings")
class AppSetting(
    @Id
    @Column(name = "key", length = 100)
    val key: String,

    @Column(name = "value", nullable = false, columnDefinition = "text")
    var value: String,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,

    @Column(name = "updated_by")
    var updatedBy: UUID? = null,
)

interface AppSettingRepository : JpaRepository<AppSetting, String>
