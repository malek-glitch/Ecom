package tech.astro.liftoff.mail

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.astro.liftoff.shared.i18n.SupportedLocales
import tech.astro.liftoff.support.IntegrationTest
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MailRenderingIntegrationTest : IntegrationTest() {

    @Autowired
    lateinit var renderer: MailRenderer

    @Test
    fun `every email renders in every locale`() {
        for (locale in SupportedLocales.ALL) {
            for (kind in MailKind.entries) {
                val url = "https://app.example/path?token=abc123"
                val rendered = renderer.render(OutgoingMail("ada@example.com", "Ada", locale, kind, url, listOf("Grace")))
                val label = "$kind/$locale"
                assertTrue(rendered.subject.isNotBlank(), label)
                assertTrue(rendered.html.contains(url), "$label: link missing from HTML")
                assertTrue(rendered.text.contains(url), "$label: link missing from text")
                assertFalse(Regex("\\{\\d}").containsMatchIn(rendered.html + rendered.text + rendered.subject), "$label: unfilled placeholder")
                assertFalse(rendered.text.contains("''"), "$label: doubled apostrophe leaked")
                assertTrue(rendered.html.contains("dir=\"${if (locale == "ar") "rtl" else "ltr"}\""), "$label: text direction")
            }
        }
    }
}
