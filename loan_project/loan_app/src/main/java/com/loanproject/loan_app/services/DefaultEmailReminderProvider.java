                if (root.has(fieldName)) {
                    return root.get(fieldName).asText();
                }
            }
        } catch (Exception ignored) {
        }
        // Regex fallback
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"");
        java.util.regex.Matcher m = p.matcher(body);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    public String previewHtml(ReminderMessageRequest request) {
        return buildHtmlBody(request);
    }

    public String previewSubject(ReminderMessageRequest request) {
        return buildSubject(request);
    }

    private String buildSubject(ReminderMessageRequest request) {
        String account = request.getMaskedAccountNumber();
        switch (request.getDpdDay()) {
            case 1:
                return "Friendly Reminder: EMI Payment Due for Loan " + account;
            case 2:
                return "Second Notice: Overdue EMI Payment for Loan " + account;
            case 3:
                return "Final Notice: Overdue EMI Payment Prior to IVR Escalation for Loan " + account;
            default:
                return "Repayment Reminder for Loan " + account;
        }
    }

    private String buildHtmlBody(ReminderMessageRequest request) {
        String dueDateStr = request.getDueDate() != null ? request.getDueDate().format(DATE_FORMATTER) : "N/A";
        BigDecimal overdue = request.getOverdueAmount() != null ? request.getOverdueAmount() : BigDecimal.ZERO;
        BigDecimal penalty = request.getPenaltyAmount();
        BigDecimal total = overdue.add(penalty);
        String paymentUrl = request.getPaymentUrl() != null ? request.getPaymentUrl() : "#";
        String helpdesk = request.getHelpdeskNumber() != null ? request.getHelpdeskNumber() : "1800-200-8899";

        String headerTitle;
        String introMessage;
        String headerBg;

        if (request.getDpdDay() == 1) {
            headerTitle = "Friendly Repayment Reminder";
            headerBg = "#1e3a8a"; // Classic blue
            introMessage = "We hope you are doing well. This is a gentle reminder that your loan installment was due on <strong>"
                    + dueDateStr + "</strong> and remains unpaid. Please clear this amount to maintain an unblemished credit record.";
        } else if (request.getDpdDay() == 2) {
            headerTitle = "Second Notice - Overdue Repayment";
            headerBg = "#d97706"; // Amber / warning
            introMessage = "This is our second notice regarding your overdue loan installment which was due on <strong>"
                    + dueDateStr + "</strong>. Late payment penalties may apply. Please settle your dues immediately.";
        } else {
            headerTitle = "Final Notice Before Escalation";
            headerBg = "#b91c1c"; // Urgent red
            introMessage = "<strong>CRITICAL NOTICE:</strong> Your loan installment is now 3 days overdue. This is the final email reminder before your account is escalated to automated IVR outreach and field collections.";
        }

        return "<!DOCTYPE html>"
                + "<html><head><meta charset='UTF-8'><style>"
                + "body { font-family: 'Segoe UI', Tahoma, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 20px; color: #1e293b; }"
                + ".card { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; border: 1px solid #e2e8f0; overflow: hidden; }"
                + ".header { background-color: " + headerBg + "; color: #ffffff; padding: 24px; text-align: center; }"
                + ".header h2 { margin: 0; font-size: 20px; font-weight: 600; }"
                + ".content { padding: 24px; font-size: 14px; line-height: 1.6; }"
                + ".summary-table { width: 100%; border-collapse: collapse; margin: 20px 0; background: #f1f5f9; border-radius: 6px; overflow: hidden; }"
                + ".summary-table td { padding: 10px 16px; border-bottom: 1px solid #e2e8f0; }"
                + ".summary-table td.label { font-weight: 600; color: #475569; width: 45%; }"
                + ".summary-table td.value { font-weight: 700; color: #0f172a; text-align: right; }"
                + ".btn-container { text-align: center; margin: 24px 0; }"
                + ".btn { display: inline-block; background-color: #2563eb; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 15px; }"
                + ".footer { padding: 16px 24px; background: #f8fafc; border-top: 1px solid #e2e8f0; font-size: 12px; color: #64748b; text-align: center; }"
                + "</style></head><body>"
                + "<div class='card'>"
                + "<div class='header'><h2>" + headerTitle + "</h2></div>"
                + "<div class='content'>"
                + "<p>Dear " + escapeHtml(request.getCustomerName()) + ",</p>"
                + "<p>" + introMessage + "</p>"
                + "<table class='summary-table'>"
                + "<tr><td class='label'>Loan Account:</td><td class='value'>" + escapeHtml(request.getMaskedAccountNumber()) + "</td></tr>"
                + "<tr><td class='label'>Due Date:</td><td class='value'>" + dueDateStr + "</td></tr>"
                + "<tr><td class='label'>Overdue Principal/Interest:</td><td class='value'>&#8377; " + overdue.toPlainString() + "</td></tr>"
                + "<tr><td class='label'>Late Penalty:</td><td class='value'>&#8377; " + penalty.toPlainString() + "</td></tr>"
                + "<tr><td class='label' style='font-size:16px;'>Total Amount Due:</td><td class='value' style='font-size:16px; color:#b91c1c;'>&#8377; " + total.toPlainString() + "</td></tr>"
                + "</table>"
                + "<div class='btn-container'><a href='" + escapeHtml(paymentUrl) + "' class='btn'>Pay Now Securely</a></div>"
                + "<p>If you have already made this payment in the past few hours, please ignore this notice as reconciliation may be in progress.</p>"
                + "<p>For any queries or assistance, contact our Customer Helpdesk at <strong>" + escapeHtml(helpdesk) + "</strong> or visit your nearest branch.</p>"
                + "<p>Sincerely,<br><strong>Indian Bank Collections & Accounts Team</strong></p>"
                + "</div>"
                + "<div class='footer'>This is an automated communication from Indian Bank Loan Management System. Please do not reply directly to this email.</div>"
                + "</div></body></html>";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
