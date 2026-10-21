package tech.astro.liftoff.shared.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration(proxyBeanMethods = false)
class ClockConfig {
    /** Injected instead of calling Instant.now() so tests can pin time. */
    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
