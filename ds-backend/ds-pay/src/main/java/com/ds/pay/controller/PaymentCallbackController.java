package com.ds.pay.controller;

import com.ds.pay.core.PayChannel;
import com.ds.pay.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/pay/callback")
@RequiredArgsConstructor
public class PaymentCallbackController {

    private final PaymentService paymentService;

    @PostMapping("/{channel}")
    public String callback(@PathVariable String channel, HttpServletRequest request, @RequestBody(required = false) String body) {
        PayChannel payChannel = PayChannel.from(channel);
        Map<String, Object> params = extractParams(request);
        return paymentService.handleCallback(payChannel, params, body);
    }

    @GetMapping("/{channel}/return")
    public String returnUrl(@PathVariable String channel, HttpServletRequest request) {
        PayChannel payChannel = PayChannel.from(channel);
        Map<String, Object> params = extractParams(request);
        return paymentService.handleCallback(payChannel, params, null);
    }

    private Map<String, Object> extractParams(HttpServletRequest request) {
        Map<String, Object> params = new HashMap<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            params.put(name, request.getParameter(name));
        }
        return params;
    }
}
