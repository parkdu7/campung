package com.example.Campung.Global.phpmyadmin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * phpMyAdmin 접속을 위한 리다이렉트 컨트롤러
 * 단일 책임 원칙(SRP)을 준수하여 phpMyAdmin 리다이렉트만 담당
 */
@Tag(name = "📊 phpMyAdmin", description = "phpMyAdmin 접속 관련 API")
@Controller
public class PhpMyAdminController {
    
    /**
     * phpMyAdmin 접속을 위한 리다이렉트
     * @return phpMyAdmin(포트 9012)으로 리다이렉트
     */
    @Operation(summary = "📊 phpMyAdmin 리다이렉트", description = "phpMyAdmin(포트 9012)으로 리다이렉트합니다.")
    @ApiResponse(responseCode = "302", description = "리다이렉트")
    @GetMapping("/phpmyadmin")
    public String redirectToPhpMyAdmin() {
        return "redirect:http://localhost:9012";
    }
}
