package com.hojun.interviewnote.interviewnoteapi.exception

import org.slf4j.LoggerFactory
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(e: NotFoundException, model: Model): String {
        logger.warn("Resource not found: ${e.message}")
        model.addAttribute("errorMessage", e.message)
        return "error/404"
    }
}
