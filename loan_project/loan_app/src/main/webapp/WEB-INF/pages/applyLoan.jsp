<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="header.jsp" %>

<c:set var="actingUser" value="${not empty currentUser ? currentUser : sessionScope.user}" />
<c:set var="isStaffUser" value="${actingUser != null && actingUser.role != 'ROLE_CUSTOMER'}" />

<c:choose>
    <c:when test="${isStaffUser}">
        <div class="card" style="max-width: 520px; width: 90%; text-align: center;">
            <div style="font-size:40px; margin-bottom:10px;">🛡️</div>
            <h2 style="margin-top:0; color:#24446B;">Staff Operational Portal</h2>
            <div class="alert alert-info" style="text-align: left; margin: 20px 0; font-size:13px; line-height:1.5;">
                ℹ️ <strong>Staff Policy Restriction:</strong> Loan origination and customer loan applications cannot be submitted by staff accounts. Loan applications must be initiated directly by registered customers through the borrower self-service portal.
            </div>
            <a href="/dashboard" class="btn" style="text-decoration:none; display:inline-block; margin-top:10px; background-color:#24446B;">
                Return to Operational Dashboard
            </a>
        </div>
    </c:when>

    <c:otherwise>
        <div class="card" style="max-width: 520px; width: 90%;">
            <h2 style="text-align:center; margin-top:0; color:#24446B;">Apply for a Loan</h2>
            <p style="text-align:center; color:#64748B; font-size:13px; margin-top:-10px; margin-bottom:20px;">
                Calculate your EMI and submit your loan application.
            </p>

            <form action="/applyLoan" method="post">
                <!-- CSRF Token -->
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

                <label style="font-weight:600; font-size:13px; color:#24446B;">Loan Amount (₹) <span style="color:#DC2626;">*</span></label>
                <input type="number" step="any" name="amount" min="${minLoanAmount != null ? minLoanAmount : 100}" max="${maxLoanAmount != null ? maxLoanAmount : 10000000}" placeholder="e.g. 50000" required>
                <c:if test="${minLoanAmount != null}">
                    <div style="font-size:11px; color:#64748B; margin-top:-8px; margin-bottom:14px;">
                        Permitted range: ₹<fmt:formatNumber value="${minLoanAmount}" type="number"/> – ₹<fmt:formatNumber value="${maxLoanAmount}" type="number"/>
                    </div>
                </c:if>

                <!-- Predefined Loan Product Terms (Standard Bank Policy) -->
                <div style="background:#F8FAFC; border:1px solid #E2E8F0; border-radius:8px; padding:14px; margin-bottom:16px;">
                    <div style="font-size:12px; font-weight:700; color:#475569; text-transform:uppercase; letter-spacing:0.5px; margin-bottom:10px;">
                        🏛️ Predefined Loan Terms (Indian Bank Standard Policy)
                    </div>
                    <div style="display:grid; grid-template-columns: 1fr 1fr; gap:10px;">
                        <div style="background:#FFFFFF; border:1px solid #CBD5E1; border-radius:6px; padding:10px;">
                            <div style="font-size:11px; color:#64748B; font-weight:600;">Annual Interest Rate</div>
                            <div style="font-size:16px; font-weight:700; color:#24446B; margin-top:2px;">
                                <c:out value="${defaultInterestRate != null ? defaultInterestRate : 10.5}"/>% <span style="font-size:11px; font-weight:500; color:#059669;">(Fixed)</span>
                            </div>
                        </div>
                        <div style="background:#FFFFFF; border:1px solid #CBD5E1; border-radius:6px; padding:10px;">
                            <div style="font-size:11px; color:#64748B; font-weight:600;">Standard Tenure</div>
                            <div style="font-size:16px; font-weight:700; color:#24446B; margin-top:2px;">
                                <c:out value="${defaultTenureMonths != null ? defaultTenureMonths : 24}"/> Months
                            </div>
                        </div>
                    </div>
                    <div style="font-size:11px; color:#64748B; margin-top:8px;">
                        ℹ️ Rates and tenure are standardized by Indian Bank policy and calculated automatically upon submission.
                    </div>
                </div>

                <button type="submit" class="btn" style="margin-top:10px; background-color:#ED7D16;">Submit Loan Application</button>
            </form>

            <div style="text-align:center; margin-top:20px;">
                <a href="/dashboard" style="font-size:13px; color:#64748B; text-decoration:none;">Cancel & Back to Dashboard</a>
            </div>
        </div>
    </c:otherwise>
</c:choose>