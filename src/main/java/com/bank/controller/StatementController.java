package com.bank.controller;

import com.bank.service.StatementService;
import com.itextpdf.text.DocumentException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/statements")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Statements", description = "PDF bank statement generation")
public class StatementController {

    @Autowired
    private StatementService statementService;

    @GetMapping(value = "/{accountNumber}", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Download PDF bank statement for an account")
    public ResponseEntity<byte[]> downloadStatement(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) throws DocumentException {

        byte[] pdfBytes = statementService.generateStatement(accountNumber, page, size);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "statement-" + accountNumber + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}
