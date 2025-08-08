/*
 * Copyright (c) 2025 JP Morgan Chase
 *
 */

package com.jpmchase.bank.controller;

import com.jpmchase.bank.dto.TransferRequest;
import com.jpmchase.bank.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<String> transfer(@RequestBody TransferRequest request) {
        transferService.transfer(request.getFromId(), request.getToId(), request.getAmount());
        return ResponseEntity.ok("Transfer successful");
    }
}
