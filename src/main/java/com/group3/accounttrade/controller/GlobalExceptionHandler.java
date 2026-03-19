package com.group3.accounttrade.controller;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.LazyInitializationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.exceptions.TemplateProcessingException;

@Slf4j
@ControllerAdvice(annotations = Controller.class)
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception,
                                              RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Request qua lon. Vui long giam kich thuoc anh hoac noi dung bai dang roi thu lai.");
        return "redirect:/seller/posts/new";
    }

    @ExceptionHandler(LazyInitializationException.class)
    public String handleLazyInitializationException(LazyInitializationException exception,
                                                    RedirectAttributes redirectAttributes) {
        log.error("[GLOBAL-ERROR] LazyInitializationException caught: {}", exception.getMessage(), exception);
        redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Lỗi tải dữ liệu. Vui lòng thử lại.");
        return "redirect:/buyer/dashboard";
    }

    @ExceptionHandler(TemplateProcessingException.class)
    public String handleTemplateProcessingException(TemplateProcessingException exception,
                                                    RedirectAttributes redirectAttributes) {
        log.error("[GLOBAL-ERROR] TemplateProcessingException caught: {}", exception.getMessage(), exception);
        redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Lỗi hiển thị trang. Vui lòng thử lại.");
        return "redirect:/buyer/dashboard";
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception exception,
                                         RedirectAttributes redirectAttributes) {
        log.error("[GLOBAL-ERROR] Unhandled exception caught: {}", exception.getMessage(), exception);
        redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau.");
        return "redirect:/buyer/dashboard";
    }
}
