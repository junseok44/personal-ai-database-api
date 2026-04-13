package com.junseok.personal_data_ai.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class SecurityInterceptor(
    @Value("\${auth.api-key}") private val apiKey: String,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val requestKey = request.getHeader("X-API-KEY")
        if (requestKey == null || requestKey != apiKey) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            return false
        }
        return true
    }
}
