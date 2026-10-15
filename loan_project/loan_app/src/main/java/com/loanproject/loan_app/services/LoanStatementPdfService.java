package com.loanproject.loan_app.services;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

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
    private static final Locale IN_LOCALE = new Locale("en", "IN");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(IN_LOCALE);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    public byte[] generateLoanStatementPdf(LoanAccountDetailsDTO details) {
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(30, 41, 59));
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(100, 116, 139));
            Font sectionHeadingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(15, 23, 42));
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(71, 85, 105));
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(15, 23, 42));
            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
            Font tableBodyFont = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(30, 41, 59));
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, new Color(148, 163, 184));

            // Bank Header
            Paragraph bankName = new Paragraph("SECURE BANKING CORPORATION", titleFont);
            bankName.setAlignment(Element.ALIGN_CENTER);
            document.add(bankName);

            Paragraph docTitle = new Paragraph("OFFICIAL LOAN ACCOUNT STATEMENT", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(79, 70, 229)));
            docTitle.setAlignment(Element.ALIGN_CENTER);
            docTitle.setSpacingBefore(4);
            document.add(docTitle);

            Paragraph metaInfo = new Paragraph("Statement Generated: " + details.getLastRefreshedFormatted() + " | Ref ID: " + details.getCorrelationId(), subTitleFont);
            metaInfo.setAlignment(Element.ALIGN_CENTER);
            metaInfo.setSpacingAfter(15);
            document.add(metaInfo);

            // Divider
            document.add(new Paragraph(" "));

            // Customer & Loan Summary Grid
            PdfPTable summaryGrid = new PdfPTable(2);
            summaryGrid.setWidthPercentage(100);
            summaryGrid.setWidths(new float[]{50, 50});
            summaryGrid.setSpacingAfter(15);

            // Left Cell: Customer Details
            PdfPCell customerCell = new PdfPCell();
            customerCell.setPadding(8);
            customerCell.setBackgroundColor(new Color(248, 250, 252));
            customerCell.setBorderColor(new Color(226, 232, 240));

            Paragraph cHead = new Paragraph("CUSTOMER PROFILE", sectionHeadingFont);
            cHead.setSpacingAfter(6);
            customerCell.addElement(cHead);

            if (details.getCustomerProfile() != null && details.getCustomerProfile().getStatus() == SectionStatus.SUCCESS) {
                CustomerProfileSectionDTO cp = details.getCustomerProfile().getData();
                customerCell.addElement(new Phrase("Customer Name: " + cp.getFullName() + "\n", valueFont));
                customerCell.addElement(new Phrase("Customer ID: " + cp.getCustomerId() + "\n", valueFont));
                customerCell.addElement(new Phrase("KYC Status: " + cp.getKycStatus() + "\n", valueFont));
                customerCell.addElement(new Phrase("Contact: " + cp.getMaskedPhone() + " (" + cp.getMaskedEmail() + ")\n", valueFont));
                customerCell.addElement(new Phrase("Branch: " + details.getBranch() + "\n", valueFont));
            } else {
                customerCell.addElement(new Phrase("Customer profile details unavailable at time of generation.\n", valueFont));
            }
            summaryGrid.addCell(customerCell);

            // Right Cell: Loan Details
            PdfPCell loanCell = new PdfPCell();
            loanCell.setPadding(8);
            loanCell.setBackgroundColor(new Color(248, 250, 252));
            loanCell.setBorderColor(new Color(226, 232, 240));

            Paragraph lHead = new Paragraph("LOAN SUMMARY", sectionHeadingFont);
            lHead.setSpacingAfter(6);
            loanCell.addElement(lHead);

            if (details.getLoanDetails() != null && details.getLoanDetails().getStatus() == SectionStatus.SUCCESS) {
                LoanDetailsSectionDTO ld = details.getLoanDetails().getData();
                loanCell.addElement(new Phrase("Account Number: " + ld.getLoanAccountNumber() + "\n", valueFont));
                loanCell.addElement(new Phrase("Product Type: " + ld.getProductType() + "\n", valueFont));
                loanCell.addElement(new Phrase("Sanctioned Amount: " + formatCurrency(ld.getSanctionedAmount()) + "\n", valueFont));
                loanCell.addElement(new Phrase("Disbursed Amount: " + formatCurrency(ld.getDisbursedAmount()) + "\n", valueFont));
                loanCell.addElement(new Phrase("Interest Rate: " + ld.getInterestRate() + "% (" + ld.getInterestType() + ")\n", valueFont));
                loanCell.addElement(new Phrase("Tenure / Monthly EMI: " + ld.getTenureMonths() + " mos / " + formatCurrency(ld.getMonthlyEmi()) + "\n", valueFont));
                loanCell.addElement(new Phrase("Account Status: " + ld.getLoanStatus() + "\n", valueFont));
            } else {
                loanCell.addElement(new Phrase("Loan details unavailable.\n", valueFont));
            }
            summaryGrid.addCell(loanCell);

            document.add(summaryGrid);

            // Outstanding Summary Section
            Paragraph outHeading = new Paragraph("OUTSTANDING BALANCE SUMMARY", sectionHeadingFont);
            outHeading.setSpacingBefore(10);
            outHeading.setSpacingAfter(6);
            document.add(outHeading);

            if (details.getOutstandingSummary() != null && details.getOutstandingSummary().getStatus() == SectionStatus.SUCCESS) {
                OutstandingSummarySectionDTO os = details.getOutstandingSummary().getData();
                PdfPTable osTable = new PdfPTable(5);
                osTable.setWidthPercentage(100);
                osTable.setWidths(new float[]{20, 20, 20, 20, 20});

                addTableHeader(osTable, new String[]{"Principal Outstanding", "Accrued Interest", "Late Fees / Penalties", "Total Payable", "Foreclosure Amount"}, tableHeaderFont);
                addTableRow(osTable, new String[]{
                        formatCurrency(os.getPrincipalOutstanding()),
                        formatCurrency(os.getAccruedInterest()),
                        formatCurrency(os.getPenaltiesAndLateFees()),
                        formatCurrency(os.getTotalPayable()),
                        os.getForeclosureAmount() != null ? formatCurrency(os.getForeclosureAmount()) : "Unavailable"
                }, tableBodyFont);
                document.add(osTable);
            } else {
                document.add(new Paragraph("Outstanding balance calculations are currently unavailable.", subTitleFont));
            }

            // Repayment Schedule Section
            Paragraph rsHeading = new Paragraph("REPAYMENT SCHEDULE", sectionHeadingFont);
            rsHeading.setSpacingBefore(15);
            rsHeading.setSpacingAfter(6);
            document.add(rsHeading);

            if (details.getRepaymentSchedule() != null && details.getRepaymentSchedule().getStatus() == SectionStatus.SUCCESS) {
                RepaymentScheduleSectionDTO rs = details.getRepaymentSchedule().getData();
                if (rs.getInstallments() != null && !rs.getInstallments().isEmpty()) {
                    PdfPTable schTable = new PdfPTable(7);
                    schTable.setWidthPercentage(100);
                    schTable.setWidths(new float[]{10, 15, 15, 15, 15, 15, 15});

                    addTableHeader(schTable, new String[]{"Inst #", "Due Date", "EMI Amount", "Principal", "Interest", "Status", "DPD"}, tableHeaderFont);

                    for (RepaymentInstallmentDTO inst : rs.getInstallments()) {
                        String dueDateStr = inst.getDueDate() != null ? inst.getDueDate() : "N/A";
                        schTable.addCell(createCell(String.valueOf(inst.getInstallmentNumber()), tableBodyFont, Element.ALIGN_CENTER));
                        schTable.addCell(createCell(dueDateStr, tableBodyFont, Element.ALIGN_CENTER));
                        schTable.addCell(createCell(formatCurrency(inst.getEmiAmount()), tableBodyFont, Element.ALIGN_RIGHT));
                        schTable.addCell(createCell(formatCurrency(inst.getPrincipalComponent()), tableBodyFont, Element.ALIGN_RIGHT));
                        schTable.addCell(createCell(formatCurrency(inst.getInterestComponent()), tableBodyFont, Element.ALIGN_RIGHT));

                        PdfPCell statusCell = createCell(inst.getStatus(), tableBodyFont, Element.ALIGN_CENTER);
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
                    document.add(new Paragraph("No installment schedule records available.", subTitleFont));
                }
            } else {
                document.add(new Paragraph("Repayment schedule information unavailable.", subTitleFont));
            }

            // Payment History Section
            Paragraph phHeading = new Paragraph("PAYMENT HISTORY", sectionHeadingFont);
            phHeading.setSpacingBefore(15);
            phHeading.setSpacingAfter(6);
            document.add(phHeading);

            if (details.getPaymentHistory() != null && details.getPaymentHistory().getStatus() == SectionStatus.SUCCESS) {
                PaymentHistorySectionDTO ph = details.getPaymentHistory().getData();
                if (ph.getTransactions() != null && !ph.getTransactions().isEmpty()) {
                    PdfPTable payTable = new PdfPTable(5);
                    payTable.setWidthPercentage(100);
                    payTable.setWidths(new float[]{20, 20, 20, 25, 15});

                    addTableHeader(payTable, new String[]{"Payment Date", "Amount", "Mode", "Reference Number", "Status"}, tableHeaderFont);

                    for (PaymentTransactionDTO tx : ph.getTransactions()) {
                        String txDate = tx.getPaymentDate() != null ? tx.getPaymentDate() : "N/A";
                        payTable.addCell(createCell(txDate, tableBodyFont, Element.ALIGN_CENTER));
                        payTable.addCell(createCell(formatCurrency(tx.getAmount()), tableBodyFont, Element.ALIGN_RIGHT));
                        payTable.addCell(createCell(tx.getPaymentMode(), tableBodyFont, Element.ALIGN_CENTER));
                        payTable.addCell(createCell(tx.getTransactionReference(), tableBodyFont, Element.ALIGN_LEFT));
                        payTable.addCell(createCell(tx.getStatus(), tableBodyFont, Element.ALIGN_CENTER));
                    }
                    document.add(payTable);
                } else {
                    document.add(new Paragraph("No payment transactions recorded for this account.", subTitleFont));
                }
            } else {
                document.add(new Paragraph("Payment history unavailable.", subTitleFont));
            }

            // Footer / Disclaimer
            Paragraph footer = new Paragraph();
            footer.setSpacingBefore(20);
            footer.add(new Chunk("CONFIDENTIAL BANKING DOCUMENT - For authorized use only.\n", footerFont));
            footer.add(new Chunk("This is a computer-generated statement and does not require a physical signature.", footerFont));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return out.toByteArray();

        } catch (DocumentException e) {
            log.error("Error generating loan statement PDF for loanId: {}", details.getLoanId(), e);
            throw new RuntimeException("Failed to generate loan statement PDF", e);
        }
    }

    private void addTableHeader(PdfPTable table, String[] headers, Font font) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(new Color(79, 70, 229));
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell);
        }
    }

    private void addTableRow(PdfPTable table, String[] values, Font font) {
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(value != null ? value : "-", font));
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell);
        }
    }

    private PdfPCell createCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "-", font));
        cell.setPadding(4);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "₹0.00";
        return "₹" + amount.setScale(2, java.math.RoundingMode.HALF_UP).toString();
    }
}
