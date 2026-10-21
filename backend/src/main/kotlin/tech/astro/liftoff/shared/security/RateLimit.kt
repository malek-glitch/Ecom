package tech.astro.liftoff.shared.security

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.bucket4j.Bucket
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import tech.astro.liftoff.shared.config.AppProperties
import tech.astro.liftoff.shared.config.AppProperties.RateLimit.Policy
import tech.astro.liftoff.shared.error.TooManyRequestsException
import java.time.Duration
import kotlin.math.ceil

/** Limits a controller method per client IP, using a named policy (see [RateLimitInterceptor.DEFAULT_POLICIES]). */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RateLimited(val policy: String)

/**
 * In-memory token buckets (Bucket4j), keyed by policy + client IP. Fine for one instance;
 * with several replicas each keeps its own counters (see docs/DEPLOYMENT.md).
 * Policies can be overridden with `app.rate-limit.policies.<name>.capacity/period`.
 */
@Component
class RateLimitInterceptor(private val props: AppProperties) : HandlerInterceptor {

    private val buckets = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofHours(1))
        .maximumSize(100_000)
        .build<String, Bucket>()

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (!props.rateLimit.enabled || handler !is HandlerMethod) return true
        val limited = handler.getMethodAnnotation(RateLimited::class.java) ?: return true
        val policy = props.rateLimit.policies[limited.policy] ?: DEFAULT_POLICIES[limited.policy] ?: return true
        val bucket = buckets.get("${limited.policy}:${request.remoteAddr}") { newBucket(policy) }
        val probe = bucket.tryConsumeAndReturnRemaining(1)
        if (probe.isConsumed) return true
        throw TooManyRequestsException(ceil(probe.nanosToWaitForRefill / 1_000_000_000.0).toLong().coerceAtLeast(1))
    }

    private fun newBucket(policy: Policy): Bucket =
        Bucket.builder()
            .addLimit { limit -> limit.capacity(policy.capacity).refillGreedy(policy.capacity, policy.period) }
            .build()

    companion object {
        val DEFAULT_POLICIES = mapOf(
            "login" to Policy(10, Duration.ofMinutes(1)),
            "register" to Policy(10, Duration.ofMinutes(10)),
            "refresh" to Policy(60, Duration.ofMinutes(1)),
            "email" to Policy(5, Duration.ofMinutes(15)),
            "google" to Policy(20, Duration.ofMinutes(1)),
            "upload" to Policy(20, Duration.ofMinutes(10)),
            "checkout" to Policy(10, Duration.ofMinutes(10)),
            "cart" to Policy(120, Duration.ofMinutes(1)),
        )
    }
}
