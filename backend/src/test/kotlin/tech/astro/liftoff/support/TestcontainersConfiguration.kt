package tech.astro.liftoff.support

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.postgresql.PostgreSQLContainer

/** Real PostgreSQL 18 for integration tests. One container is shared by every Spring test context. */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    fun postgres(): PostgreSQLContainer = POSTGRES

    companion object {
        val POSTGRES: PostgreSQLContainer = PostgreSQLContainer("postgres:18-alpine")
    }
}
