package tech.astro.liftoff.mail

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import tech.astro.liftoff.shared.config.AppProperties
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Delivers [OutgoingMail] after the surrounding transaction commits, so a rolled-back signup
 * never sends a verification email. Sending happens on a small private pool (so a slow SMTP
 * server never holds a request thread) with three attempts and backoff.
 */
@Component
class MailDispatcher(
    private val renderer: MailRenderer,
    private val gateway: MailGateway,
    private val props: AppProperties,
) : DisposableBean {

    private val log = LoggerFactory.getLogger(javaClass)
    private val threadIds = AtomicInteger()
    private val executor = ThreadPoolExecutor(2, 2, 60, TimeUnit.SECONDS, LinkedBlockingQueue(1_000)) { runnable ->
        Thread(runnable, "mail-${threadIds.incrementAndGet()}").apply { isDaemon = true }
    }.apply { allowCoreThreadTimeOut(true) }

    @TransactionalEventListener(fallbackExecution = true)
    fun on(mail: OutgoingMail) {
        if (props.mail.async) executor.execute { deliver(mail) } else deliver(mail)
    }

    private fun deliver(mail: OutgoingMail) {
        val rendered = try {
            renderer.render(mail)
        } catch (e: Exception) {
            log.error("Could not render {} mail for {}", mail.kind, mail.to, e)
            return
        }
        var delayMillis = 1_000L
        for (attempt in 1..MAX_ATTEMPTS) {
            try {
                gateway.send(rendered)
                log.info("Sent {} mail to {}", mail.kind, mail.to)
                return
            } catch (e: Exception) {
                if (attempt == MAX_ATTEMPTS) {
                    log.error("Giving up on {} mail to {} after {} attempts", mail.kind, mail.to, attempt, e)
                    return
                }
                log.warn("Sending {} mail to {} failed (attempt {}): {}", mail.kind, mail.to, attempt, e.message)
                Thread.sleep(delayMillis)
                delayMillis *= 4
            }
        }
    }

    override fun destroy() {
        executor.shutdown()
        executor.awaitTermination(15, TimeUnit.SECONDS)
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
