<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="header.jsp" %>

<div class="wide-card" style="max-width: 1400px; margin: 0 auto; padding: 25px;">

    <!-- Top Action / Breadcrumb Bar -->
    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; border-bottom:2px solid #edf2f7; padding-bottom:15px; flex-wrap:wrap; gap:15px;">
        <div>
            <div style="display:flex; align-items:center; gap:12px;">
                <h2 style="margin:0; color:#1e293b; font-size:24px; font-weight:800; letter-spacing:-0.5px;">
                    🎯 360° Loan Account Profile
                </h2>
                <span style="background:#e0e7ff; color:#4338ca; padding:5px 12px; border-radius:14px; font-size:13px; font-weight:700; font-family:monospace;">
                    <c:out value="${loanDetails.accountNumber}"/>
                </span>
                <span style="background:#f1f5f9; color:#475569; padding:5px 12px; border-radius:14px; font-size:12px; font-weight:600;">
                    🏢 Branch: <c:out value="${loanDetails.branch}"/>
                </span>
            </div>
            <p style="color:#64748b; font-size:14px; margin:6px 0 0 0;">
                Unified real-time banking intelligence dashboard for authorized loan officers and collections specialists.
            </p>
        </div>

        <!-- Action Controls -->
        <div style="display:flex; align-items:center; gap:12px; flex-wrap:wrap;">
            <div style="background:#f8fafc; border:1px solid #cbd5e1; padding:6px 14px; border-radius:8px; font-size:13px; color:#334155;">
                <span style="color:#64748b;">⏱️ Last refreshed:</span>
                <strong style="color:#0f172a;"><c:out value="${loanDetails.lastRefreshedFormatted}"/></strong>
            </div>

            <!-- Safe Refresh Button -->
            <a href="/loan-details?loanId=${loanDetails.loanId}&refresh=true"
               class="btn btn-outline-primary"
               style="display:inline-flex; align-items:center; gap:6px; padding:7px 14px; font-size:13px; font-weight:600; text-decoration:none; border:1px solid #6366f1; color:#6366f1; border-radius:6px; background:#ffffff;">
                🔄 Refresh Data
            </a>

            <!-- Download PDF Statement -->
            <a href="/loan-details/${loanDetails.loanId}/statement.pdf"
               id="btnDownloadStatement"
               onclick="handleStatementDownload(event, ${loanDetails.loanId})"
               target="_blank"
               class="btn btn-primary"
               style="display:inline-flex; align-items:center; gap:6px; padding:7px 16px; font-size:13px; font-weight:600; text-decoration:none; background:#24446B; color:#ffffff; border-radius:6px; border:none; box-shadow:0 2px 4px rgba(36,68,107,0.2);">
                📄 Download Statement (PDF)
            </a>

            <a href="/loan-search"
               class="btn btn-secondary"
               style="padding:7px 14px; font-size:13px; text-decoration:none; background:#e2e8f0; color:#334155; border-radius:6px; border:none;">
                &larr; Search
            </a>
        </div>
    </div>

    <!-- Refresh Notification Banner -->
    <c:if test="${isRefreshed}">
        <div class="alert alert-success" style="background:#ecfdf5; border:1px solid #a7f3d0; color:#065f46; padding:12px 18px; border-radius:8px; margin-bottom:20px; font-size:14px; display:flex; align-items:center; justify-content:space-between;">
            <div>
                <strong>✅ Data Refreshed Successfully:</strong> Authoritative loan and installment records have been synchronized as of <strong><c:out value="${loanDetails.lastRefreshedFormatted}"/>.</strong>
            </div>
            <span style="font-size:12px; font-weight:600; opacity:0.8;">Correlation ID: <c:out value="${loanDetails.correlationId}"/></span>
        </div>
    </c:if>

    <!-- SECTION 1: Top 2-Column Overview (Customer Profile & Loan Details) -->
    <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(420px, 1fr)); gap:20px; margin-bottom:25px;">

        <!-- SECTION A: Customer Profile -->
        <div style="background:#ffffff; border:1px solid #e2e8f0; border-radius:10px; padding:20px; box-shadow:0 2px 6px rgba(0,0,0,0.03);">
            <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #f1f5f9; padding-bottom:12px; margin-bottom:16px;">
                <h3 style="margin:0; font-size:16px; font-weight:700; color:#1e293b; display:flex; align-items:center; gap:8px;">
                    👤 Customer Profile
                </h3>
                <c:choose>
                    <c:when test="${loanDetails.customerProfile.status == 'SUCCESS'}">
                        <span style="background:#dcfce7; color:#15803d; padding:3px 8px; border-radius:10px; font-size:11px; font-weight:700;">
                            KYC: <c:out value="${loanDetails.customerProfile.data.kycStatus}"/>
                        </span>
                    </c:when>
                    <c:otherwise>
                        <span style="background:#fee2e2; color:#b91c1c; padding:3px 8px; border-radius:10px; font-size:11px; font-weight:700;">
                            <c:out value="${loanDetails.customerProfile.status}"/>
                        </span>
                    </c:otherwise>
                </c:choose>
            </div>

            <c:choose>
                <c:when test="${loanDetails.customerProfile.status == 'SUCCESS'}">
                    <c:set var="cust" value="${loanDetails.customerProfile.data}"/>
                    <c:if test="${cust.contactDetailUpdateRequired}">
                        <div style="background:#fffbeb; border:1px solid #fde68a; color:#92400e; padding:10px 14px; border-radius:6px; font-size:13px; margin-bottom:14px; display:flex; align-items:center; gap:8px;">
                            <span>⚠️</span>
                            <div>
                                <strong>Contact Detail Update Required:</strong> Customer communication bounced or email/mobile is invalid. Please request updated contact details during borrower outreach.
                            </div>
                        </div>
                    </c:if>
                    <div style="display:grid; grid-template-columns: 1fr 1fr; gap:14px; font-size:13px;">
                        <div>
                            <div style="color:#64748b; font-size:11px; font-weight:700; text-transform:uppercase;">Customer Name</div>
                            <div style="color:#0f172a; font-weight:700; font-size:15px; margin-top:2px;"><c:out value="${cust.fullName}"/></div>
                        </div>
                        <div>
                            <div style="color:#64748b; font-size:11px; font-weight:700; text-transform:uppercase;">Customer ID</div>
                            <div style="color:#4338ca; font-weight:600; font-family:monospace; margin-top:2px;"><c:out value="${cust.customerId}"/></div>
                        </div>
                        <div>
                            <div style="color:#64748b; font-size:11px; font-weight:700; text-transform:uppercase;">Date of Birth</div>
                            <div style="color:#334155; margin-top:2px;"><c:out value="${cust.dateOfBirth}"/></div>
                        </div>
                        <div>
                            <div style="color:#64748b; font-size:11px; font-weight:700; text-transform:uppercase;">Preferred Contact</div>
                            <div style="color:#334155; margin-top:2px;"><c:out value="${cust.preferredContactMethod}"/></div>
                        </div>
                        <div>
                            <div style="color:#64748b; font-size:11px; font-weight:700; text-transform:uppercase;">Mobile Number</div>
                            <div style="color:#334155; font-family:monospace; margin-top:2px;">
                                <c:out value="${cust.maskedPhone}"/> <span style="color:#94a3b8; font-size:11px;">(Masked)</span>
                            </div>
                        </div>
                        <div>
                            <div style="color:#64748b; font-size:11px; font-weight:700; text-transform:uppercase;">Email Address</div>
                            <div style="color:#334155; font-family:monospace; margin-top:2px;">
                                <c:out value="${cust.maskedEmail}"/> <span style="color:#94a3b8; font-size:11px;">(Masked)</span>
                            </div>
                        </div>
                        <div style="grid-column: span 2;">
                            <div style="color:#64748b; font-size:11px; font-weight:700; text-transform:uppercase;">Permanent Address</div>
                            <div style="color:#334155; margin-top:2px;"><c:out value="${cust.address}"/></div>
                        </div>
                    </div>
                </c:when>
                <c:otherwise>
                    <div style="background:#fef2f2; border:1px solid #fecaca; color:#991b1b; padding:12px; border-radius:6px; font-size:13px;">
                        ⚠️ <strong>Customer Profile Unavailable:</strong> <c:out value="${loanDetails.customerProfile.errorMessage}"/>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>

        <!-- SECTION B: Loan Details -->
        <div style="background:#ffffff; border:1px solid #e2e8f0; border-radius:10px; padding:20px; box-shadow:0 2px 6px rgba(0,0,0,0.03);">
            <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #f1f5f9; padding-bottom:12px; margin-bottom:16px;">
                <h3 style="margin:0; font-size:16px; font-weight:700; color:#1e293b; display:flex; align-items:center; gap:8px;">
                    💳 Loan Account Details
                </h3>
                <c:choose>
                    <c:when test="${loanDetails.loanDetails.data.loanStatus == 'APPROVED'}">
                        <span style="background:#dcfce7; color:#15803d; padding:3px 8px; border-radius:10px; font-size:11px; font-weight:700;">ACTIVE / APPROVED</span>
                    </c:when>
                    <c:when test="${loanDetails.loanDetails.data.loanStatus == 'CLOSED'}">
                        <span style="background:#f1f5f9; color:#475569; padding:3px 8px; border-radius:10px; font-size:11px; font-weight:700;">CLOSED</span>
                    </c:when>
                    <c:otherwise>
                        <span style="background:#fef3c7; color:#b45309; padding:3px 8px; border-radius:10px; font-size:11px; font-weight:700;">
                            <c:out value="${loanDetails.loanDetails.data.loanStatus}"/>
                        </span>
                    </c:otherwise>
                </c:choose>
            </div>

            <c:choose>
                <c:when test="${loanDetails.loanDetails.status == 'SUCCESS'}">
                    <c:set var="ld" value="${loanDetails.loanDetails.data}"/>
                    <div style="display:grid; grid-template-columns: 1fr 1fr; gap:14px; font-size:13px;">
                        <div>
                            <div style="color:#64748b; font-size:11px; font-weight:700; text-transform:uppercase;">Loan Account Number</div>
                            <div style="color:#0f172a; font-weight:700; font-family:monospace; font-size:14px; margin-top:2px;"><c:out value="${ld.loanAccountNumber}"/></div>
                        </div>
                        <div>
                            <div style="color:#64748b; font-size:11px; font-weight:700; text-transform:uppercase;">Loan Product Type</div>
                            <div style="color:#334155; font-weight:600; margin-top:2px;"><c:out value="${ld.productType}"/></div>
                        </div>
                        <div>
                            <div style="color:#64748b; font-size:11px; font-weight:700; text-transform:uppercase;">Sanctioned Amount</div>
                            <div style="color:#0f172a; font-weight:700; font-size:15px; margin-top:2px;">
                                ₹<fmt:formatNumber value="${ld.sanctionedAmount}" pattern="#,##0.00"/>
                            </div>
                        </div>
                        <div>
                            <div style="color:#64748b; font-size:11px; font-weight:700; text-transform:uppercase;">Disbursed Amount</div>
                            <div style="color:#0f172a; font-weight:700; font-size:15px; margin-top:2px;">
                                ₹<fmt:formatNumber value="${ld.disbursedAmount}" pattern="#,##0.00"/>
                            </div>
                        </div>
                        <div>
                            <div style="color:#64748b; font-size:11px; font-weight:700; text-transform:uppercase;">Disbursement Date</div>
                            <div style="color:#334155; margin-top:2px;"><c:out value="${ld.disbursementDate}"/></div>
                        </div>
                        <div>
                            <div style="color:#64748b; font-size:11px; font-weight:700; text-transform:uppercase;">Interest Rate & Type</div>
                            <div style="color:#334155; font-weight:600; margin-top:2px;">
                                <c:out value="${ld.interestRate}"/>% p.a. (<c:out value="${ld.interestType}"/>)
                            </div>
                        </div>
                        <div>
                            <div style="color:#64748b; font-size:11px; font-weight:700; text-transform:uppercase;">Tenure</div>
                            <div style="color:#334155; margin-top:2px;"><c:out value="${ld.tenureMonths}"/> Months</div>
                        </div>
                        <div>
                            <div style="color:#64748b; font-size:11px; font-weight:700; text-transform:uppercase;">Monthly EMI</div>
                            <div style="color:#4338ca; font-weight:800; font-size:15px; margin-top:2px;">
                                ₹<fmt:formatNumber value="${ld.monthlyEmi}" pattern="#,##0.00"/>
                            </div>
                        </div>
                    </div>
                </c:when>
                <c:otherwise>
                    <div style="background:#fef2f2; border:1px solid #fecaca; color:#991b1b; padding:12px; border-radius:6px; font-size:13px;">
                        ⚠️ <strong>Loan Details Unavailable:</strong> <c:out value="${loanDetails.loanDetails.errorMessage}"/>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <!-- SECTION 2: Outstanding Summary & Delinquency Information -->
    <div style="display:grid; grid-template-columns: 2fr 1fr; gap:20px; margin-bottom:25px;">

        <!-- SECTION C: Outstanding Summary -->
        <div style="background:#ffffff; border:1px solid #e2e8f0; border-radius:10px; padding:20px; box-shadow:0 2px 6px rgba(0,0,0,0.03);">
            <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #f1f5f9; padding-bottom:12px; margin-bottom:16px;">
                <h3 style="margin:0; font-size:16px; font-weight:700; color:#1e293b; display:flex; align-items:center; gap:8px;">
                    💰 Outstanding Balance Summary
                </h3>
                <span style="font-size:11px; color:#64748b;">Accounting Rule: Exact BigDecimal Half-Up</span>
            </div>

            <c:choose>
                <c:when test="${loanDetails.outstandingSummary.status == 'SUCCESS'}">
                    <c:set var="os" value="${loanDetails.outstandingSummary.data}"/>
                    <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap:15px;">
                        <div style="background:#f8fafc; border:1px solid #e2e8f0; border-radius:8px; padding:12px;">
                            <div style="color:#64748b; font-size:11px; font-weight:700; text-transform:uppercase;">Principal Outstanding</div>
                            <div style="color:#0f172a; font-size:18px; font-weight:800; margin-top:4px;">
                                ₹<fmt:formatNumber value="${os.principalOutstanding}" pattern="#,##0.00"/>
                            </div>
                        </div>
                        <div style="background:#f8fafc; border:1px solid #e2e8f0; border-radius:8px; padding:12px;">
                            <div style="color:#64748b; font-size:11px; font-weight:700; text-transform:uppercase;">Accrued Interest</div>
                            <div style="color:#0f172a; font-size:18px; font-weight:800; margin-top:4px;">
                                ₹<fmt:formatNumber value="${os.accruedInterest}" pattern="#,##0.00"/>
                            </div>
                        </div>
                        <div style="background:#f8fafc; border:1px solid #e2e8f0; border-radius:8px; padding:12px;">
                            <div style="color:#64748b; font-size:11px; font-weight:700; text-transform:uppercase;">Penalties & Late Fees</div>
                            <div style="color:#b91c1c; font-size:18px; font-weight:800; margin-top:4px;">
                                ₹<fmt:formatNumber value="${os.penaltiesAndLateFees}" pattern="#,##0.00"/>
                            </div>
                        </div>
                        <div style="background:#eef2ff; border:1px solid #c7d2fe; border-radius:8px; padding:12px;">
                            <div style="color:#4338ca; font-size:11px; font-weight:700; text-transform:uppercase;">Total Payable Amount</div>
                            <div style="color:#3730a3; font-size:20px; font-weight:800; margin-top:4px;">
                                ₹<fmt:formatNumber value="${os.totalPayable}" pattern="#,##0.00"/>
                            </div>
                        </div>
                    </div>

                    <!-- Foreclosure Amount Notice -->
                    <div style="margin-top:14px; background:#fafafa; border:1px dashed #d4d4d8; border-radius:6px; padding:10px 14px; font-size:12px; display:flex; justify-content:space-between; align-items:center;">
                        <div>
                            <strong>🏦 Indicative Foreclosure Amount:</strong>
                            <c:choose>
                                <c:when test="${os.foreclosureAmount != null}">
                                    <span style="font-size:14px; font-weight:800; color:#0f172a; margin-left:8px;">
                                        ₹<fmt:formatNumber value="${os.foreclosureAmount}" pattern="#,##0.00"/>
                                    </span>
                                </c:when>
                                <c:otherwise>
                                    <span style="color:#71717a; font-style:italic; margin-left:8px;"><c:out value="${os.foreclosureStatus}"/></span>
                                </c:otherwise>
                            </c:choose>
                        </div>
                        <span style="color:#a1a1aa; font-size:11px;">Subject to authoritative CBS verification at settlement date</span>
                    </div>
                </c:when>
                <c:otherwise>
                    <div style="background:#fef2f2; border:1px solid #fecaca; color:#991b1b; padding:12px; border-radius:6px; font-size:13px;">
                        ⚠️ <strong>Outstanding Balance Calculation Unavailable:</strong> <c:out value="${loanDetails.outstandingSummary.errorMessage}"/>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>

        <!-- SECTION F: Delinquency Information -->
        <div style="background:#ffffff; border:1px solid #e2e8f0; border-radius:10px; padding:20px; box-shadow:0 2px 6px rgba(0,0,0,0.03);">
            <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #f1f5f9; padding-bottom:12px; margin-bottom:16px;">
                <h3 style="margin:0; font-size:16px; font-weight:700; color:#1e293b; display:flex; align-items:center; gap:8px;">
                    ⚠️ Delinquency & DPD
                </h3>
            </div>

            <c:choose>
                <c:when test="${loanDetails.delinquency.status == 'SUCCESS'}">
                    <c:set var="del" value="${loanDetails.delinquency.data}"/>
                    <div style="display:flex; flex-direction:column; gap:12px;">
                        <div style="display:flex; justify-content:space-between; align-items:center; background:#f8fafc; padding:8px 12px; border-radius:6px;">
                            <span style="color:#64748b; font-size:12px; font-weight:600;">Days Past Due (DPD):</span>
                            <c:choose>
                                <c:when test="${del.dpd == 0}">
                                    <span style="background:#dcfce7; color:#15803d; padding:3px 10px; border-radius:12px; font-weight:800; font-size:13px;">0 Days (Regular)</span>
                                </c:when>
                                <c:when test="${del.dpd <= 30}">
                                    <span style="background:#fef3c7; color:#b45309; padding:3px 10px; border-radius:12px; font-weight:800; font-size:13px;"><c:out value="${del.dpd}"/> Days (SMA-0)</span>
                                </c:when>
                                <c:when test="${del.dpd <= 60}">
                                    <span style="background:#ffedd5; color:#c2410c; padding:3px 10px; border-radius:12px; font-weight:800; font-size:13px;"><c:out value="${del.dpd}"/> Days (SMA-1)</span>
                                </c:when>
                                <c:when test="${del.dpd <= 90}">
                                    <span style="background:#fee2e2; color:#b91c1c; padding:3px 10px; border-radius:12px; font-weight:800; font-size:13px;"><c:out value="${del.dpd}"/> Days (SMA-2)</span>
                                </c:when>
                                <c:otherwise>
                                    <span style="background:#991b1b; color:#ffffff; padding:3px 10px; border-radius:12px; font-weight:800; font-size:13px;"><c:out value="${del.dpd}"/> Days (NPA)</span>
                                </c:otherwise>
                            </c:choose>
                        </div>

                        <div style="display:flex; justify-content:space-between; align-items:center; padding:4px 6px; font-size:13px;">
                            <span style="color:#64748b;">Delinquency Bucket:</span>
                            <strong style="color:#0f172a;"><c:out value="${del.delinquencyBucket}"/></strong>
                        </div>

                        <div style="display:flex; justify-content:space-between; align-items:center; padding:4px 6px; font-size:13px;">
                            <span style="color:#64748b;">Overdue Amount:</span>
                            <strong style="color:${del.overdueAmount > 0 ? '#b91c1c' : '#15803d'}; font-size:14px;">
                                ₹<fmt:formatNumber value="${del.overdueAmount}" pattern="#,##0.00"/>
                            </strong>
                        </div>

                        <div style="display:flex; justify-content:space-between; align-items:center; padding:4px 6px; font-size:13px;">
                            <span style="color:#64748b;">Account Classification:</span>
                            <strong style="color:#0f172a;"><c:out value="${del.accountStatus}"/></strong>
                        </div>

                        <div style="display:flex; justify-content:space-between; align-items:center; padding:4px 6px; font-size:12px; border-top:1px solid #f1f5f9; padding-top:8px;">
                            <span style="color:#94a3b8;">Last Reminder Sent:</span>
                            <span style="color:#64748b;"><c:out value="${del.lastReminderSent}"/></span>
                        </div>
                    </div>
                </c:when>
                <c:otherwise>
                    <div style="background:#fef2f2; border:1px solid #fecaca; color:#991b1b; padding:12px; border-radius:6px; font-size:13px;">
                        ⚠️ <strong>Delinquency Data Unavailable:</strong> <c:out value="${loanDetails.delinquency.errorMessage}"/>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <!-- SECTION D: Repayment Schedule -->
    <div style="background:#ffffff; border:1px solid #e2e8f0; border-radius:10px; padding:20px; box-shadow:0 2px 6px rgba(0,0,0,0.03); margin-bottom:25px;">
        <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #f1f5f9; padding-bottom:12px; margin-bottom:16px;">
            <h3 style="margin:0; font-size:16px; font-weight:700; color:#1e293b; display:flex; align-items:center; gap:8px;">
                📅 Complete Repayment Schedule (Amortization)
            </h3>
            <span style="font-size:12px; color:#64748b;">Automated DPD Calculation strictly applied on unpaid installments past due</span>
        </div>

        <c:choose>
            <c:when test="${loanDetails.repaymentSchedule.status == 'SUCCESS'}">
                <c:set var="rs" value="${loanDetails.repaymentSchedule.data}"/>
                <div style="overflow-x:auto;">
                    <table style="width:100%; border-collapse:collapse; font-size:13px; text-align:left;">
                        <thead>
                            <tr style="background:#f8fafc; border-bottom:2px solid #e2e8f0; color:#475569; font-size:11px; text-transform:uppercase; letter-spacing:0.5px;">
                                <th style="padding:10px 12px;">Inst #</th>
                                <th style="padding:10px 12px;">Due Date</th>
                                <th style="padding:10px 12px; text-align:right;">EMI Amount</th>
                                <th style="padding:10px 12px; text-align:right;">Principal</th>
                                <th style="padding:10px 12px; text-align:right;">Interest</th>
                                <th style="padding:10px 12px; text-align:right;">Paid Amount</th>
                                <th style="padding:10px 12px; text-align:center;">Payment Status</th>
                                <th style="padding:10px 12px; text-align:center;">DPD</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="inst" items="${rs.installments}">
                                <tr style="border-bottom:1px solid #f1f5f9; background:${inst.status == 'OVERDUE' ? '#fff1f2' : ((inst.status == 'PAID' || inst.status == 'COMPLETED') ? '#f0fdf4' : (inst.status == 'PARTIALLY_PAID' ? '#fffbeb' : '#ffffff'))};">
                                    <td style="padding:10px 12px; font-weight:700; color:#334155;">#<c:out value="${inst.installmentNumber}"/></td>
                                    <td style="padding:10px 12px; color:#334155;"><c:out value="${inst.dueDate}"/></td>
                                    <td style="padding:10px 12px; text-align:right; font-weight:700; color:#0f172a;">
                                        ₹<fmt:formatNumber value="${inst.emiAmount}" pattern="#,##0.00"/>
                                    </td>
                                    <td style="padding:10px 12px; text-align:right; color:#64748b;">
                                        ₹<fmt:formatNumber value="${inst.principalComponent}" pattern="#,##0.00"/>
                                    </td>
                                    <td style="padding:10px 12px; text-align:right; color:#64748b;">
                                        ₹<fmt:formatNumber value="${inst.interestComponent}" pattern="#,##0.00"/>
                                    </td>
                                    <td style="padding:10px 12px; text-align:right; font-weight:600; color:${inst.paidAmount > 0 ? '#15803d' : '#94a3b8'};">
                                        ₹<fmt:formatNumber value="${inst.paidAmount}" pattern="#,##0.00"/>
                                    </td>
                                    <td style="padding:10px 12px; text-align:center;">
                                        <c:choose>
                                            <c:when test="${inst.status == 'PAID' || inst.status == 'COMPLETED'}">
                                                <span style="background:#dcfce7; color:#15803d; padding:3px 10px; border-radius:10px; font-size:11px; font-weight:700;">PAID</span>
                                            </c:when>
                                            <c:when test="${inst.status == 'PARTIALLY_PAID'}">
                                                <span style="background:#fef3c7; color:#b45309; border:1px solid #fde68a; padding:3px 10px; border-radius:10px; font-size:11px; font-weight:700;">PARTIALLY PAID</span>
                                            </c:when>
                                            <c:when test="${inst.status == 'OVERDUE'}">
                                                <span style="background:#fee2e2; color:#b91c1c; border:1px solid #fca5a5; padding:3px 10px; border-radius:10px; font-size:11px; font-weight:800; animation:pulse 2s infinite;">⚠️ OVERDUE</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span style="background:#e0f2fe; color:#0369a1; padding:3px 10px; border-radius:10px; font-size:11px; font-weight:700;">DUE</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td style="padding:10px 12px; text-align:center; font-weight:700; color:${inst.dpd > 0 ? '#b91c1c' : '#64748b'};">
                                        <c:out value="${inst.dpd}"/>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:when>
            <c:otherwise>
                <div style="background:#fef2f2; border:1px solid #fecaca; color:#991b1b; padding:12px; border-radius:6px; font-size:13px;">
                    ⚠️ <strong>Repayment Schedule Unavailable:</strong> <c:out value="${loanDetails.repaymentSchedule.errorMessage}"/>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

    <!-- SECTION E: Payment History -->
    <div style="background:#ffffff; border:1px solid #e2e8f0; border-radius:10px; padding:20px; box-shadow:0 2px 6px rgba(0,0,0,0.03); margin-bottom:25px;">
        <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #f1f5f9; padding-bottom:12px; margin-bottom:16px;">
            <h3 style="margin:0; font-size:16px; font-weight:700; color:#1e293b; display:flex; align-items:center; gap:8px;">
                📜 Verified Payment Transactions
            </h3>
            <span style="font-size:12px; color:#64748b;">Direct ledger transactions (No fabricated repayment data)</span>
        </div>

        <c:choose>
            <c:when test="${loanDetails.paymentHistory.status == 'SUCCESS'}">
                <c:set var="ph" value="${loanDetails.paymentHistory.data}"/>
                <c:choose>
                    <c:when test="${empty ph.transactions}">
                        <div style="text-align:center; padding:30px; color:#64748b; font-size:14px; background:#f8fafc; border-radius:8px; border:1px dashed #cbd5e1;">
                            ℹ️ No repayment transactions have been recorded for this loan account yet.
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div style="overflow-x:auto;">
                            <table style="width:100%; border-collapse:collapse; font-size:13px; text-align:left;">
                                <thead>
                                    <tr style="background:#f8fafc; border-bottom:2px solid #e2e8f0; color:#475569; font-size:11px; text-transform:uppercase;">
                                        <th style="padding:10px 12px;">Payment Date</th>
                                        <th style="padding:10px 12px; text-align:right;">Amount Paid</th>
                                        <th style="padding:10px 12px; text-align:center;">Mode</th>
                                        <th style="padding:10px 12px;">Transaction Reference</th>
                                        <th style="padding:10px 12px; text-align:center;">Status</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="tx" items="${ph.transactions}">
                                        <tr style="border-bottom:1px solid #f1f5f9;">
                                            <td style="padding:10px 12px; color:#334155;"><c:out value="${tx.paymentDate}"/></td>
                                            <td style="padding:10px 12px; text-align:right; font-weight:700; color:#15803d;">
                                                ₹<fmt:formatNumber value="${tx.amount}" pattern="#,##0.00"/>
                                            </td>
                                            <td style="padding:10px 12px; text-align:center;">
                                                <span style="background:#f1f5f9; color:#475569; padding:2px 8px; border-radius:6px; font-size:11px; font-weight:600;">
                                                    <c:out value="${tx.paymentMode}"/>
                                                </span>
                                            </td>
                                            <td style="padding:10px 12px; font-family:monospace; color:#4338ca;"><c:out value="${tx.transactionReference}"/></td>
                                            <td style="padding:10px 12px; text-align:center;">
                                                <span style="background:#dcfce7; color:#15803d; padding:2px 8px; border-radius:6px; font-size:11px; font-weight:700;">
                                                    <c:out value="${tx.status}"/>
                                                </span>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </c:when>
            <c:otherwise>
                <div style="background:#fef2f2; border:1px solid #fecaca; color:#991b1b; padding:12px; border-radius:6px; font-size:13px;">
                    ⚠️ <strong>Payment History Unavailable:</strong> <c:out value="${loanDetails.paymentHistory.errorMessage}"/>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

    <!-- SECTION 3: Bottom Grid (Collateral/Guarantor & Linked Accounts) -->
    <div style="display:grid; grid-template-columns: 1fr 1fr; gap:20px; margin-bottom:25px;">

        <!-- SECTION G: Collateral and Guarantor -->
        <div style="background:#ffffff; border:1px solid #e2e8f0; border-radius:10px; padding:20px; box-shadow:0 2px 6px rgba(0,0,0,0.03);">
            <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #f1f5f9; padding-bottom:12px; margin-bottom:16px;">
                <h3 style="margin:0; font-size:16px; font-weight:700; color:#1e293b; display:flex; align-items:center; gap:8px;">
                    🛡️ Collateral & Guarantor Security
                </h3>
            </div>

            <c:choose>
                <c:when test="${loanDetails.collateralGuarantor.status == 'SUCCESS'}">
                    <c:set var="cg" value="${loanDetails.collateralGuarantor.data}"/>

                    <!-- Collaterals List -->
                    <div style="margin-bottom:16px;">
                        <h4 style="margin:0 0 8px 0; font-size:13px; font-weight:700; color:#475569; text-transform:uppercase;">Pledged Collateral Assets</h4>
                        <c:choose>
                            <c:when test="${empty cg.collaterals}">
                                <div style="font-size:12px; color:#64748b; background:#f8fafc; padding:10px; border-radius:6px; border:1px dashed #cbd5e1;">
                                    No physical or liquid collateral pledged (Unsecured loan facility).
                                </div>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="c" items="${cg.collaterals}">
                                    <div style="background:#f8fafc; border:1px solid #e2e8f0; border-radius:6px; padding:10px; margin-bottom:8px; font-size:13px;">
                                        <div style="display:flex; justify-content:space-between;">
                                            <strong style="color:#0f172a;"><c:out value="${c.collateralType}"/></strong>
                                            <span style="font-weight:700; color:#15803d;">₹<fmt:formatNumber value="${c.estimatedValue}" pattern="#,##0.00"/></span>
                                        </div>
                                        <div style="color:#64748b; font-size:12px; margin-top:4px;"><c:out value="${c.description}"/></div>
                                        <div style="font-size:11px; color:#4338ca; margin-top:2px;">Lien Status: <c:out value="${c.lienStatus}"/></div>
                                    </div>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                    </div>

                    <!-- Guarantor List -->
                    <div>
                        <h4 style="margin:0 0 8px 0; font-size:13px; font-weight:700; color:#475569; text-transform:uppercase;">Guarantor Details</h4>
                        <c:choose>
                            <c:when test="${empty cg.guarantors}">
                                <div style="font-size:12px; color:#64748b; background:#f8fafc; padding:10px; border-radius:6px; border:1px dashed #cbd5e1;">
                                    No third-party guarantor assigned to this facility.
                                </div>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="g" items="${cg.guarantors}">
                                    <div style="background:#f8fafc; border:1px solid #e2e8f0; border-radius:6px; padding:10px; margin-bottom:8px; font-size:13px;">
                                        <div style="display:flex; justify-content:space-between;">
                                            <strong style="color:#0f172a;"><c:out value="${g.name}"/></strong>
                                            <span style="background:#dcfce7; color:#15803d; padding:2px 8px; border-radius:6px; font-size:11px; font-weight:700;">KYC: <c:out value="${g.kycStatus}"/></span>
                                        </div>
                                        <div style="color:#64748b; font-size:12px; margin-top:4px;">Relationship: <c:out value="${g.relationship}"/> | Phone: <c:out value="${g.phone}"/></div>
                                    </div>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </c:when>
                <c:otherwise>
                    <div style="background:#fef2f2; border:1px solid #fecaca; color:#991b1b; padding:12px; border-radius:6px; font-size:13px;">
                        ⚠️ <strong>Collateral & Guarantor Information Unavailable:</strong> <c:out value="${loanDetails.collateralGuarantor.errorMessage}"/>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>

        <!-- SECTION H: Linked Accounts -->
        <div style="background:#ffffff; border:1px solid #e2e8f0; border-radius:10px; padding:20px; box-shadow:0 2px 6px rgba(0,0,0,0.03);">
            <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #f1f5f9; padding-bottom:12px; margin-bottom:16px;">
                <h3 style="margin:0; font-size:16px; font-weight:700; color:#1e293b; display:flex; align-items:center; gap:8px;">
                    🏦 Linked Accounts & CBS Auto-Debit
                </h3>
            </div>

            <c:choose>
                <c:when test="${loanDetails.linkedAccount.status == 'SUCCESS'}">
                    <c:set var="la" value="${loanDetails.linkedAccount.data}"/>
                    <c:choose>
                        <c:when test="${empty la.accounts}">
                            <div style="font-size:12px; color:#64748b; background:#f8fafc; padding:15px; border-radius:6px; border:1px dashed #cbd5e1; text-align:center;">
                                ℹ️ No savings/current bank account is linked for auto-debit on this loan account.
                            </div>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="acc" items="${la.accounts}">
                                <div style="background:#f8fafc; border:1px solid #e2e8f0; border-radius:8px; padding:12px; margin-bottom:10px; font-size:13px;">
                                    <div style="display:flex; justify-content:space-between; align-items:center;">
                                        <strong style="color:#0f172a;"><c:out value="${acc.bankName}"/> (<c:out value="${acc.accountType}"/>)</strong>
                                        <c:if test="${acc.autoDebitEnabled}">
                                            <span style="background:#e0e7ff; color:#4338ca; padding:2px 8px; border-radius:6px; font-size:11px; font-weight:700;">AUTO-DEBIT ACTIVE</span>
                                        </c:if>
                                    </div>
                                    <div style="margin-top:6px; font-family:monospace; color:#334155; font-size:14px;">
                                        Account: <c:out value="${acc.maskedAccountNumber}"/>
                                    </div>
                                    <div style="margin-top:8px; padding-top:8px; border-top:1px dashed #cbd5e1; display:flex; justify-content:space-between; font-size:12px;">
                                        <span style="color:#64748b;">Live CBS Balance:</span>
                                        <c:choose>
                                            <c:when test="${acc.accountBalance != null}">
                                                <strong style="color:#15803d;">₹<fmt:formatNumber value="${acc.accountBalance}" pattern="#,##0.00"/></strong>
                                            </c:when>
                                            <c:otherwise>
                                                <span style="color:#94a3b8; font-style:italic;"><c:out value="${acc.coreBankingStatus}"/></span>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                </div>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </c:when>
                <c:when test="${loanDetails.linkedAccount.status == 'UNAVAILABLE'}">
                    <div style="background:#f8fafc; border:1px solid #cbd5e1; color:#475569; padding:16px; border-radius:8px; font-size:13px;">
                        <div style="font-weight:700; margin-bottom:6px; color:#1e293b;">
                            ℹ️ Core Banking Integration Notice:
                        </div>
                        <p style="margin:0; font-size:12px; line-height:1.5;">
                            <c:out value="${loanDetails.linkedAccount.errorMessage}"/>
                        </p>
                    </div>
                </c:when>
                <c:otherwise>
                    <div style="background:#fef2f2; border:1px solid #fecaca; color:#991b1b; padding:12px; border-radius:6px; font-size:13px;">
                        ⚠️ <strong>Linked Account Information Error:</strong> <c:out value="${loanDetails.linkedAccount.errorMessage}"/>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<script>
    // Visual pulse effect for overdue badges
    const style = document.createElement('style');
    style.innerHTML = `
        @keyframes pulse {
            0% { opacity: 1; }
            50% { opacity: 0.6; }
            100% { opacity: 1; }
        }
    `;
    document.head.appendChild(style);

    // Client-side Blob download handling with error feedback
    function handleStatementDownload(event, loanId) {
        if (!window.fetch || !window.Blob) {
            return; // Allow default link navigation
        }
        event.preventDefault();
        const btn = document.getElementById('btnDownloadStatement');
        const origHtml = btn ? btn.innerHTML : '📄 Download Statement (PDF)';
        if (btn) {
            btn.innerHTML = '⏳ Generating PDF...';
            btn.style.pointerEvents = 'none';
        }

        fetch('/loan-details/' + loanId + '/statement.pdf')
            .then(async (response) => {
                if (!response.ok) {
                    let errMessage = 'Download failed (HTTP ' + response.status + ')';
                    try {
                        const errData = await response.json();
                        if (errData && errData.message) errMessage = errData.message;
                    } catch (e) {}
                    throw new Error(errMessage);
                }
                const contentType = response.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    const errData = await response.json();
                    throw new Error(errData.message || 'Server returned an error.');
                }
                const blob = await response.blob();
                if (!blob || blob.size === 0) {
                    throw new Error('Downloaded statement file is empty.');
                }
                let filename = 'loan_statement_' + loanId + '.pdf';
                const disposition = response.headers.get('content-disposition');
                if (disposition && disposition.includes('filename=')) {
                    const match = disposition.match(/filename="?([^";]+)"?/);
                    if (match && match[1]) filename = match[1];
                }
                const blobUrl = window.URL.createObjectURL(blob);
                const tempLink = document.createElement('a');
                tempLink.href = blobUrl;
                tempLink.download = filename;
                document.body.appendChild(tempLink);
                tempLink.click();
                document.body.removeChild(tempLink);
                setTimeout(() => window.URL.revokeObjectURL(blobUrl), 1500);
            })
            .catch((err) => {
                alert('Loan Statement Download Notice:\n' + err.message);
            })
            .finally(() => {
                if (btn) {
                    btn.innerHTML = origHtml;
                    btn.style.pointerEvents = 'auto';
                }
            });
    }
</script>

</body>
</html>
