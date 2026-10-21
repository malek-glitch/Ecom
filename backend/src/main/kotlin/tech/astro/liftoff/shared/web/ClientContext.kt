package tech.astro.liftoff.shared.web

import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/**
 * Who is calling, network-wise. Behind Traefik, `server.forward-headers-strategy` makes
 * `remoteAddr` the real client address. Declare it as a controller parameter to receive it.
 */
data class ClientContext(val ipAddress: String?, val userAgent: String?) {
    companion object {
        fun from(request: HttpServletRequest) = ClientContext(
            ipAddress = request.remoteAddr,
            userAgent = request.getHeader("User-Agent")?.take(512),
        )
    }
}

class ClientContextArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter) = parameter.parameterType == ClientContext::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): ClientContext = ClientContext.from(webRequest.getNativeRequest(HttpServletRequest::class.java)!!)
}
