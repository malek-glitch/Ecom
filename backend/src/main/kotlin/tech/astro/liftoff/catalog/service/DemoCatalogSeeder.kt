package tech.astro.liftoff.catalog.service

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.astro.liftoff.catalog.model.Category
import tech.astro.liftoff.catalog.model.Product
import tech.astro.liftoff.catalog.repository.CategoryRepository
import tech.astro.liftoff.catalog.repository.ProductRepository
import tech.astro.liftoff.shared.config.AppProperties

/**
 * With SHOP_SEED_DEMO=true, fills an empty catalog with a few categories and products so the
 * store has something to show. Does nothing once any product exists. Replace or delete
 * the data from the admin area; turn the variable off in production.
 */
@Component
class DemoCatalogSeeder(
    private val categories: CategoryRepository,
    private val products: ProductRepository,
    private val props: AppProperties,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (!props.shop.seedDemo || products.count() > 0) return
        val bySlug = DEMO_CATEGORIES.mapIndexed { index, (slug, name, description) ->
            categories.save(Category(slug, name, description, position = index))
        }.associateBy { it.slug }
        DEMO_PRODUCTS.forEach { demo ->
            products.save(
                Product(
                    slug = Slugs.from(demo.name),
                    name = demo.name,
                    description = demo.description,
                    priceCents = demo.priceCents,
                    compareAtPriceCents = demo.compareAtPriceCents,
                    sku = demo.sku,
                    stock = demo.stock,
                    featured = demo.featured,
                    category = bySlug.getValue(demo.category),
                ),
            )
        }
        log.info("Seeded the demo catalog: {} categories, {} products (SHOP_SEED_DEMO=true)", bySlug.size, DEMO_PRODUCTS.size)
    }

    private data class DemoProduct(
        val name: String,
        val category: String,
        val priceCents: Long,
        val stock: Int,
        val sku: String,
        val description: String,
        val compareAtPriceCents: Long? = null,
        val featured: Boolean = false,
    )

    private companion object {
        val DEMO_CATEGORIES = listOf(
            Triple("home-living", "Home & Living", "Small things that make a home."),
            Triple("kitchen", "Kitchen", "Tools and tableware for everyday cooking."),
            Triple("apparel", "Apparel", "Simple, durable clothing."),
            Triple("stationery", "Stationery", "Paper goods for writing and planning."),
        )

        val DEMO_PRODUCTS = listOf(
            DemoProduct("Ceramic Table Lamp", "home-living", 6900, 12, "HL-LAMP-01", "A hand-glazed ceramic base with a linen shade. Warm light for reading corners.", featured = true),
            DemoProduct("Wool Throw Blanket", "home-living", 8900, 8, "HL-THROW-01", "Soft merino wool in a herringbone weave, 130 × 170 cm.", compareAtPriceCents = 11000, featured = true),
            DemoProduct("Scented Soy Candle", "home-living", 2400, 40, "HL-CANDLE-01", "Cedar and bergamot, about 45 hours of burn time."),
            DemoProduct("Linen Cushion Cover", "home-living", 2900, 3, "HL-CUSH-01", "Stonewashed linen, 45 × 45 cm, hidden zip."),
            DemoProduct("Cast Iron Skillet", "kitchen", 4900, 15, "KT-SKILLET-01", "Pre-seasoned 26 cm skillet that goes from stovetop to oven.", featured = true),
            DemoProduct("Pour-Over Coffee Set", "kitchen", 3900, 20, "KT-COFFEE-01", "Glass carafe and stainless steel dripper for two cups."),
            DemoProduct("Olive Wood Cutting Board", "kitchen", 3400, 0, "KT-BOARD-01", "Each board has its own grain. Oil it now and then."),
            DemoProduct("Stoneware Dinner Plates (Set of 4)", "kitchen", 5600, 10, "KT-PLATES-04", "Matte glaze, dishwasher and microwave safe.", compareAtPriceCents = 6400),
            DemoProduct("Organic Cotton T-Shirt", "apparel", 2500, 50, "AP-TEE-01", "Heavyweight jersey, relaxed fit, made to last."),
            DemoProduct("Canvas Tote Bag", "apparel", 1900, 60, "AP-TOTE-01", "Sturdy 12 oz canvas with an inside pocket.", featured = true),
            DemoProduct("Hardcover Dotted Notebook", "stationery", 1800, 35, "ST-NOTE-01", "A5, 192 numbered pages of 100 g paper that takes fountain pens well."),
            DemoProduct("Brass Ballpoint Pen", "stationery", 3200, 4, "ST-PEN-01", "Solid brass body that ages with use. Takes standard refills."),
        )
    }
}
