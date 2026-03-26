package com.bank.service;

import com.bank.domain.Account;
import com.bank.domain.Transaction;
import com.bank.exception.AccountNotFoundException;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StatementService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, new BaseColor(0, 51, 102));
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BaseColor.BLACK);
    private static final Font BOLD_FONT = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.BLACK);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @Transactional(readOnly = true)
    public byte[] generateStatement(String accountNumber, int page, int size) throws DocumentException {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        List<Transaction> transactions = transactionRepository
                .findByAccountNumber(accountNumber, PageRequest.of(page, size))
                .getContent();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 60, 40);
        PdfWriter.getInstance(document, out);
        document.open();

        // Header
        addHeader(document, account);

        // Account summary table
        addAccountSummary(document, account);

        // Transactions table
        addTransactionsTable(document, transactions, accountNumber);

        // Footer
        addFooter(document);

        document.close();
        return out.toByteArray();
    }

    private void addHeader(Document document, Account account) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{2f, 1f});

        PdfPCell bankCell = new PdfPCell();
        bankCell.setBorder(Rectangle.NO_BORDER);
        Paragraph bankName = new Paragraph("SecureBank Online", TITLE_FONT);
        Paragraph tagline = new Paragraph("Your trusted banking partner", new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, BaseColor.GRAY));
        bankCell.addElement(bankName);
        bankCell.addElement(tagline);
        headerTable.addCell(bankCell);

        PdfPCell statementCell = new PdfPCell();
        statementCell.setBorder(Rectangle.NO_BORDER);
        statementCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        statementCell.addElement(new Paragraph("ACCOUNT STATEMENT", new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD)));
        statementCell.addElement(new Paragraph("Generated: " + java.time.LocalDateTime.now().format(FORMATTER), NORMAL_FONT));
        headerTable.addCell(statementCell);

        document.add(headerTable);

        LineSeparator separator = new LineSeparator();
        separator.setLineColor(new BaseColor(0, 51, 102));
        document.add(new Chunk(separator));
        document.add(Chunk.NEWLINE);
    }

    private void addAccountSummary(Document document, Account account) throws DocumentException {
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingBefore(10f);
        summaryTable.setSpacingAfter(15f);

        addSummaryRow(summaryTable, "Account Holder", account.getUser().getFullName());
        addSummaryRow(summaryTable, "Account Number", account.getAccountNumber());
        addSummaryRow(summaryTable, "Account Type", account.getAccountType().name());
        addSummaryRow(summaryTable, "Currency", account.getCurrency());
        addSummaryRow(summaryTable, "Status", account.getStatus().name());
        addSummaryRow(summaryTable, "Current Balance", account.getCurrency() + " " + account.getBalance().toPlainString());

        document.add(summaryTable);
    }

    private void addSummaryRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, BOLD_FONT));
        labelCell.setBackgroundColor(new BaseColor(240, 240, 240));
        labelCell.setPadding(5f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, NORMAL_FONT));
        valueCell.setPadding(5f);
        table.addCell(valueCell);
    }

    private void addTransactionsTable(Document document, List<Transaction> transactions, String accountNumber) throws DocumentException {
        Paragraph heading = new Paragraph("Transaction History", BOLD_FONT);
        heading.setSpacingAfter(8f);
        document.add(heading);

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.2f, 1.8f, 1.8f, 0.9f, 1f, 1f});

        String[] headers = {"Date", "From Account", "To Account", "Type", "Amount", "Status"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, HEADER_FONT));
            cell.setBackgroundColor(new BaseColor(0, 51, 102));
            cell.setPadding(6f);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        boolean alternate = false;
        for (Transaction t : transactions) {
            BaseColor rowColor = alternate ? new BaseColor(245, 245, 245) : BaseColor.WHITE;
            boolean isCredit = t.getToAccount().getAccountNumber().equals(accountNumber);

            addTxCell(table, t.getCreatedAt() != null ? t.getCreatedAt().format(FORMATTER) : "-", rowColor, Element.ALIGN_LEFT);
            addTxCell(table, t.getFromAccount().getAccountNumber(), rowColor, Element.ALIGN_CENTER);
            addTxCell(table, t.getToAccount().getAccountNumber(), rowColor, Element.ALIGN_CENTER);
            addTxCell(table, t.getType().name(), rowColor, Element.ALIGN_CENTER);

            PdfPCell amountCell = new PdfPCell(new Phrase(
                (isCredit ? "+ " : "- ") + t.getAmount().toPlainString(),
                new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD,
                    isCredit ? new BaseColor(0, 128, 0) : new BaseColor(200, 0, 0))));
            amountCell.setBackgroundColor(rowColor);
            amountCell.setPadding(5f);
            amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(amountCell);

            addTxCell(table, t.getStatus().name(), rowColor, Element.ALIGN_CENTER);
            alternate = !alternate;
        }

        if (transactions.isEmpty()) {
            PdfPCell noDataCell = new PdfPCell(new Phrase("No transactions found", NORMAL_FONT));
            noDataCell.setColspan(6);
            noDataCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            noDataCell.setPadding(10f);
            table.addCell(noDataCell);
        }

        document.add(table);
    }

    private void addTxCell(PdfPTable table, String text, BaseColor bgColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(5f);
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }

    private void addFooter(Document document) throws DocumentException {
        document.add(Chunk.NEWLINE);
        LineSeparator separator = new LineSeparator();
        separator.setLineColor(new BaseColor(0, 51, 102));
        document.add(new Chunk(separator));
        Paragraph footer = new Paragraph(
            "This is a computer-generated statement and does not require a signature. " +
            "For queries, contact support@securebank.com",
            new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY));
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(5f);
        document.add(footer);
    }
}
