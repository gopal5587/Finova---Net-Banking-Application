package com.finova.integration.currency;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/currency")
@Validated
public class CurrencyController {

    private final CurrencyExchangeService currencyExchangeService;

    public CurrencyController(CurrencyExchangeService currencyExchangeService) {
        this.currencyExchangeService = currencyExchangeService;
    }

    @GetMapping("/convert")
    public ResponseEntity<CurrencyExchangeService.ConversionResult> convert(
            @RequestParam @NotNull @DecimalMin("0.00") BigDecimal amount,
            @RequestParam(defaultValue = "INR") @NotBlank String from,
            @RequestParam @NotBlank String to) {
        return ResponseEntity.ok(currencyExchangeService.convert(amount, from, to));
    }
}
