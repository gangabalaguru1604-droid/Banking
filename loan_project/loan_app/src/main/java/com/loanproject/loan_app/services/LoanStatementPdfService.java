package com.loanproject.loan_app.services;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.loanproject.loan_app.dto.CustomerProfileSectionDTO;
import com.loanproject.loan_app.dto.LoanAccountDetailsDTO;
import com.loanproject.loan_app.dto.LoanDetailsSectionDTO;
import com.loanproject.loan_app.dto.OutstandingSummarySectionDTO;
import com.loanproject.loan_app.dto.PaymentHistorySectionDTO;
import com.loanproject.loan_app.dto.PaymentTransactionDTO;
import com.loanproject.loan_app.dto.RepaymentInstallmentDTO;
import com.loanproject.loan_app.dto.RepaymentScheduleSectionDTO;
import com.loanproject.loan_app.dto.SectionResult;
import com.loanproject.loan_app.dto.SectionStatus;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class LoanStatementPdfService {

    private static final Logger log = LoggerFactory.getLogger(LoanStatementPdfService.class);

    // Bank of India Institutional Color Palette
    private static final Color COLOR_BOI_BLUE = new Color(36, 68, 107);     // #24446B
    private static final Color COLOR_BOI_ORANGE = new Color(237, 125, 22);   // #ED7D16
    private static final Color COLOR_DARK = new Color(23, 43, 77);           // #172B4D
    private static final Color COLOR_MUTED = new Color(100, 116, 139);       // #64748B
    private static final Color COLOR_BG_PANEL = new Color(248, 250, 252);    // #F8FAFC
    private static final Color COLOR_BORDER = new Color(226, 232, 240);      // #E2E8F0

    public byte[] generateLoanStatementPdf(LoanAccountDetailsDTO details) {
        Document document = new Document(PageSize.A4, 32, 32, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Typography hierarchy
            Font bankTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, COLOR_BOI_BLUE);
            Font bankSubFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COLOR_BOI_ORANGE);
            Font docTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_BOI_BLUE);
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_MUTED);
            Font sectionTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COLOR_BOI_BLUE);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, COLOR_DARK);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_DARK);
            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Color.WHITE);
            Font tableBodyFont = FontFactory.getFont(FontFactory.HELVETICA, 7, COLOR_DARK);
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, COLOR_MUTED);

            // 1. BANK HEADER
            Paragraph bankName = new Paragraph("BANK OF INDIA", bankTitleFont);
            bankName.setAlignment(Element.ALIGN_CENTER);
            document.add(bankName);

            Paragraph bankTagline = new Paragraph("Relationship Beyond Banking • Retail & MSME Credit Operations Hub", bankSubFont);
            bankTagline.setAlignment(Element.ALIGN_CENTER);
            bankTagline.setSpacingAfter(4);
            document.add(bankTagline);

            Paragraph docTitle = new Paragraph("360° OFFICIAL LOAN ACCOUNT STATEMENT", docTitleFont);
            docTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(docTitle);

            String genTime = details.getLastRefreshedFormatted();
            if (genTime == null || genTime.isBlank() || "N/A".equals(genTime)) {
                genTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
            }
            Paragraph metaInfo = new Paragraph("Statement Generated: " + genTime + " | Correlation Ref: " + details.getCorrelationId(), metaFont);
            metaInfo.setAlignment(Element.ALIGN_CENTER);
            metaInfo.setSpacingAfter(12);
            document.add(metaInfo);

            // 2. TWO-COLUMN SUMMARY GRID (Customer Profile & Loan Details)
            PdfPTable summaryGrid = new PdfPTable(2);
            summaryGrid.setWidthPercentage(100);
            summaryGrid.setWidths(new float[]{50, 50});
            summaryGrid.setSpacingAfter(12);

            // Left Column: Customer Profile
            PdfPCell customerCell = new PdfPCell();
            customerCell.setPadding(8);
            customerCell.setBackgroundColor(COLOR_BG_PANEL);
            customerCell.setBorderColor(COLOR_BORDER);

            Paragraph cHead = new Paragraph("CUSTOMER PROFILE", sectionTitleFont);
            cHead.setSpacingAfter(6);
            customerCell.addElement(cHead);

            CustomerProfileSectionDTO cp = null;
            if (details.getCustomerProfile() != null && details.getCustomerProfile().getData() != null) {
                cp = details.getCustomerProfile().getData();
            }

            String customerName = cp != null && cp.getFullName() != null ? cp.getFullName() : "Valued Customer";
            String customerId = cp != null && cp.getCustomerId() != null ? cp.getCustomerId() : "CUST" + details.getLoanId();
            String maskedPhone = cp != null && cp.getMaskedPhone() != null ? cp.getMaskedPhone() : "N/A";
            String maskedEmail = cp != null && cp.getMaskedEmail() != null ? cp.getMaskedEmail() : "N/A";
            String address = cp != null && cp.getAddress() != null && !cp.getAddress().isBlank()
                    ? cp.getAddress()
                    : "Address verified on file with Branch";
            String branch = details.getBranch() != null ? details.getBranch() : "MAIN";

            customerCell.addElement(createFieldPhrase("Customer Name: ", customerName, labelFont, valueFont));
            customerCell.addElement(createFieldPhrase("Customer ID: ", customerId, labelFont, valueFont));
            customerCell.addElement(createFieldPhrase("Registered Mobile: ", maskedPhone, labelFont, valueFont));
            customerCell.addElement(createFieldPhrase("Registered Email: ", maskedEmail, labelFont, valueFont));
            customerCell.addElement(createFieldPhrase("Permanent Address: ", address, labelFont, valueFont));
            customerCell.addElement(createFieldPhrase("Branch: ", branch, labelFont, valueFont));

            summaryGrid.addCell(customerCell);

            // Right Column: Loan Account Summary
            PdfPCell loanCell = new PdfPCell();
            loanCell.setPadding(8);
            loanCell.setBackgroundColor(COLOR_BG_PANEL);
            loanCell.setBorderColor(COLOR_BORDER);

            Paragraph lHead = new Paragraph("LOAN ACCOUNT DETAILS", sectionTitleFont);
            lHead.setSpacingAfter(6);
            loanCell.addElement(lHead);

            LoanDetailsSectionDTO ld = null;
            if (details.getLoanDetails() != null && details.getLoanDetails().getData() != null) {
                ld = details.getLoanDetails().getData();
            }

            OutstandingSummarySectionDTO os = null;
            if (details.getOutstandingSummary() != null && details.getOutstandingSummary().getData() != null) {
                os = details.getOutstandingSummary().getData();
            }

            String accountNo = ld != null && ld.getLoanAccountNumber() != null
                    ? ld.getLoanAccountNumber()
                    : (details.getAccountNumber() != null ? details.getAccountNumber() : "LN" + String.format("%010d", details.getLoanId()));
            String productType = ld != null && ld.getProductType() != null ? ld.getProductType() : "Personal Loan";
            BigDecimal sanctionedAmount = ld != null && ld.getSanctionedAmount() != null ? ld.getSanctionedAmount() : BigDecimal.ZERO;
            BigDecimal disbursedAmount = ld != null && ld.getDisbursedAmount() != null ? ld.getDisbursedAmount() : sanctionedAmount;
            String disbursementDate = ld != null && ld.getDisbursementDate() != null && !"N/A".equals(ld.getDisbursementDate())
                    ? ld.getDisbursementDate()
                    : "Active";
            BigDecimal interestRate = ld != null && ld.getInterestRate() != null ? ld.getInterestRate() : BigDecimal.valueOf(10.5);
            String interestType = ld != null && ld.getInterestType() != null ? ld.getInterestType() : "FIXED";
            int tenure = ld != null ? ld.getTenureMonths() : 24;
            BigDecimal emi = ld != null && ld.getMonthlyEmi() != null ? ld.getMonthlyEmi() : BigDecimal.ZERO;
            String loanStatus = ld != null && ld.getLoanStatus() != null ? ld.getLoanStatus() : "ACTIVE";
            BigDecimal outstandingPrincipal = os != null && os.getPrincipalOutstanding() != null ? os.getPrincipalOutstanding() : sanctionedAmount;

            loanCell.addElement(createFieldPhrase("Loan Account Number: ", accountNo, labelFont, valueFont));
            loanCell.addElement(createFieldPhrase("Product Type: ", productType, labelFont, valueFont));
            loanCell.addElement(createFieldPhrase("Sanctioned Amount: ", formatCurrency(sanctionedAmount), labelFont, valueFont));
            loanCell.addElement(createFieldPhrase("Disbursed Amount: ", formatCurrency(disbursedAmount), labelFont, valueFont));
            loanCell.addElement(createFieldPhrase("Disbursement Date: ", disbursementDate, labelFont, valueFont));
            loanCell.addElement(createFieldPhrase("Annual Interest Rate: ", interestRate + "% (" + interestType + ")", labelFont, valueFont));
            loanCell.addElement(createFieldPhrase("Loan Tenure: ", tenure + " Months", labelFont, valueFont));
            loanCell.addElement(createFieldPhrase("Monthly EMI: ", formatCurrency(emi), labelFont, valueFont));
            loanCell.addElement(createFieldPhrase("Outstanding Principal: ", formatCurrency(outstandingPrincipal), labelFont, valueFont));
            loanCell.addElement(createFieldPhrase("Loan Status: ", loanStatus, labelFont, valueFont));

            summaryGrid.addCell(loanCell);
            document.add(summaryGrid);

            // 3. OUTSTANDING BALANCE SUMMARY SECTION
            Paragraph outHeading = new Paragraph("OUTSTANDING BALANCE & SETTLEMENT SUMMARY", sectionTitleFont);
            outHeading.setSpacingBefore(4);
            outHeading.setSpacingAfter(4);
            document.add(outHeading);

            PdfPTable osTable = new PdfPTable(5);
            osTable.setWidthPercentage(100);
            osTable.setWidths(new float[]{20, 20, 20, 20, 20});
            osTable.setSpacingAfter(10);

            addTableHeader(osTable, new String[]{"Principal Outstanding", "Accrued Interest", "Late Fees / Penalties", "Total Payable", "Foreclosure Amount"}, tableHeaderFont);

            BigDecimal accInterest = os != null && os.getAccruedInterest() != null ? os.getAccruedInterest() : BigDecimal.ZERO;
            BigDecimal lateFees = os != null && os.getPenaltiesAndLateFees() != null ? os.getPenaltiesAndLateFees() : BigDecimal.ZERO;
            BigDecimal totalPayable = os != null && os.getTotalPayable() != null ? os.getTotalPayable() : outstandingPrincipal.add(accInterest).add(lateFees);
            String foreclosureStr = os != null && os.getForeclosureAmount() != null ? formatCurrency(os.getForeclosureAmount()) : formatCurrency(totalPayable);

            addTableRow(osTable, new String[]{
                    formatCurrency(outstandingPrincipal),
                    formatCurrency(accInterest),
                    formatCurrency(lateFees),
                    formatCurrency(totalPayable),
                    foreclosureStr
            }, tableBodyFont);
            document.add(osTable);

            // 4. TRANSACTION & REPAYMENT HISTORY
            Paragraph rsHeading = new Paragraph("REPAYMENT SCHEDULE & TRANSACTION HISTORY", sectionTitleFont);
            rsHeading.setSpacingBefore(6);
            rsHeading.setSpacingAfter(4);
            document.add(rsHeading);

            RepaymentScheduleSectionDTO rs = null;
            if (details.getRepaymentSchedule() != null && details.getRepaymentSchedule().getData() != null) {
                rs = details.getRepaymentSchedule().getData();
            }

            if (rs != null && rs.getInstallments() != null && !rs.getInstallments().isEmpty()) {
                PdfPTable schTable = new PdfPTable(8);
                schTable.setWidthPercentage(100);
                schTable.setWidths(new float[]{8, 14, 14, 14, 14, 16, 12, 8});
                schTable.setSpacingAfter(10);

                addTableHeader(schTable, new String[]{
                        "Inst #", "Payment / Due Date", "EMI Amount", "Principal Paid", "Interest Paid", "Remaining Balance", "Status", "DPD"
                }, tableHeaderFont);

                BigDecimal runningBalance = sanctionedAmount;
                for (RepaymentInstallmentDTO inst : rs.getInstallments()) {
                    String dueDateStr = inst.getDueDate() != null ? inst.getDueDate() : "N/A";
                    BigDecimal princ = inst.getPrincipalComponent() != null ? inst.getPrincipalComponent() : BigDecimal.ZERO;
                    BigDecimal intr = inst.getInterestComponent() != null ? inst.getInterestComponent() : BigDecimal.ZERO;
                    BigDecimal emiVal = inst.getEmiAmount() != null ? inst.getEmiAmount() : princ.add(intr);

                    // Running balance calculation
                    runningBalance = runningBalance.subtract(princ);
                    if (runningBalance.compareTo(BigDecimal.ZERO) < 0) {
                        runningBalance = BigDecimal.ZERO;
                    }

                    schTable.addCell(createCell(String.valueOf(inst.getInstallmentNumber()), tableBodyFont, Element.ALIGN_CENTER));
                    schTable.addCell(createCell(dueDateStr, tableBodyFont, Element.ALIGN_CENTER));
                    schTable.addCell(createCell(formatCurrency(emiVal), tableBodyFont, Element.ALIGN_RIGHT));
                    schTable.addCell(createCell(formatCurrency(princ), tableBodyFont, Element.ALIGN_RIGHT));
                    schTable.addCell(createCell(formatCurrency(intr), tableBodyFont, Element.ALIGN_RIGHT));
                    schTable.addCell(createCell(formatCurrency(runningBalance), tableBodyFont, Element.ALIGN_RIGHT));

                    PdfPCell statusCell = createCell(inst.getStatus() != null ? inst.getStatus() : "DUE", tableBodyFont, Element.ALIGN_CENTER);
                    if ("OVERDUE".equalsIgnoreCase(inst.getStatus())) {
                        statusCell.setBackgroundColor(new Color(254, 226, 226));
                    } else if ("PAID".equalsIgnoreCase(inst.getStatus()) || "COMPLETED".equalsIgnoreCase(inst.getStatus())) {
                        statusCell.setBackgroundColor(new Color(220, 252, 231));
                    } else if ("PARTIALLY_PAID".equalsIgnoreCase(inst.getStatus())) {
                        statusCell.setBackgroundColor(new Color(254, 243, 199));
                    }
                    schTable.addCell(statusCell);
                    schTable.addCell(createCell(String.valueOf(inst.getDpd()), tableBodyFont, Element.ALIGN_CENTER));
                }
                document.add(schTable);
            } else {
                document.add(new Paragraph("No amortized repayment schedule records available at time of generation.", metaFont));
            }

            // 5. RECORDED PAYMENT TRANSACTIONS (If available)
            PaymentHistorySectionDTO ph = null;
            if (details.getPaymentHistory() != null && details.getPaymentHistory().getData() != null) {
                ph = details.getPaymentHistory().getData();
            }

            if (ph != null && ph.getTransactions() != null && !ph.getTransactions().isEmpty()) {
                Paragraph phHeading = new Paragraph("RECORDED PAYMENT RECEIPTS", sectionTitleFont);
                phHeading.setSpacingBefore(6);
                phHeading.setSpacingAfter(4);
                document.add(phHeading);

                PdfPTable payTable = new PdfPTable(5);
                payTable.setWidthPercentage(100);
                payTable.setWidths(new float[]{22, 18, 18, 26, 16});
                payTable.setSpacingAfter(10);

                addTableHeader(payTable, new String[]{"Payment Date", "Amount (₹)", "Mode", "Transaction Reference", "Status"}, tableHeaderFont);

                for (PaymentTransactionDTO tx : ph.getTransactions()) {
                    String txDate = tx.getPaymentDate() != null ? tx.getPaymentDate() : "N/A";
                    payTable.addCell(createCell(txDate, tableBodyFont, Element.ALIGN_CENTER));
                    payTable.addCell(createCell(formatCurrency(tx.getAmount()), tableBodyFont, Element.ALIGN_RIGHT));
                    payTable.addCell(createCell(tx.getPaymentMode() != null ? tx.getPaymentMode() : "CASH", tableBodyFont, Element.ALIGN_CENTER));
                    payTable.addCell(createCell(tx.getTransactionReference() != null ? tx.getTransactionReference() : "-", tableBodyFont, Element.ALIGN_LEFT));
                    payTable.addCell(createCell(tx.getStatus() != null ? tx.getStatus() : "SUCCESS", tableBodyFont, Element.ALIGN_CENTER));
                }
                document.add(payTable);
            }

            // 6. DISCLAIMER & INSTITUTIONAL FOOTER
            Paragraph footer = new Paragraph();
            footer.setSpacingBefore(12);
            footer.add(new Chunk("CONFIDENTIAL BANKING DOCUMENT — Bank of India Institutional Records.\n", footerFont));
            footer.add(new Chunk("This is a computer-generated statement and does not require a physical signature.", footerFont));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            // CRITICAL: Close document BEFORE calling out.toByteArray()
            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Error generating loan statement PDF for loanId: {}", details.getLoanId(), e);
            throw new RuntimeException("Failed to generate loan statement PDF", e);
        } finally {
            if (document != null && document.isOpen()) {
                document.close();
            }
        }
    }

    private Paragraph createFieldPhrase(String label, String value, Font labelFont, Font valueFont) {
        Paragraph p = new Paragraph();
        p.setLeading(11);
        p.add(new Chunk(label, labelFont));
        p.add(new Chunk(value != null ? value : "N/A", valueFont));
        return p;
    }

    private void addTableHeader(PdfPTable table, String[] headers, Font font) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(COLOR_BOI_BLUE);
            cell.setPadding(4);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell);
        }
    }

    private void addTableRow(PdfPTable table, String[] values, Font font) {
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(value != null ? value : "-", font));
            cell.setPadding(4);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell);
        }
    }

    private PdfPCell createCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "-", font));
        cell.setPadding(3);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "₹0.00";
        return "₹" + amount.setScale(2, RoundingMode.HALF_UP).toString();
    }
}
