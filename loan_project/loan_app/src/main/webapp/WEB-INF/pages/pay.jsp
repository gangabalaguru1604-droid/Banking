<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="header.jsp" %>

<div class="card">
    <h2 style="text-align:center; margin-top:0; color:#2c3e50;">Loan Repayment</h2>
    <p style="text-align:center; color:#718096; font-size:13px; margin-top:-10px; margin-bottom:20px;">
        Process an EMI or principal repayment against the loan.
    </p>

    <c:choose>
        <c:when test="${loan != null}">
            <div style="background:#f7fafc; border:1px solid #e2e8f0; border-radius:6px; padding:15px; margin-bottom:20px; font-size:13px;">
                <div style="display:flex; justify-content:space-between; margin-bottom:6px;">
                    <span style="color:#718096;">Loan Account:</span>
                    <strong>#${loan.loanId}</strong>
                </div>
                <div style="display:flex; justify-content:space-between; margin-bottom:6px;">
                    <span style="color:#718096;">Borrower:</span>
                    <strong>${loan.user.name}</strong>
                </div>
                <div style="display:flex; justify-content:space-between; margin-bottom:6px;">
                    <span style="color:#718096;">Monthly EMI:</span>
                    <strong style="color:#2b6cb0;">₹<fmt:formatNumber value="${loan.emi}" pattern="#,##0.00"/></strong>
                </div>
                <div style="display:flex; justify-content:space-between;">
                    <span style="color:#718096;">Outstanding Balance:</span>
                    <strong style="color:#e53e3e;">₹<fmt:formatNumber value="${loan.remaining}" pattern="#,##0.00"/></strong>
                </div>
            </div>

            <form action="/pay" method="post">
                <!-- CSRF Token -->
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                <!-- Hidden Loan ID and Idempotency Reference -->
                <input type="hidden" name="loanId" value="${loan.loanId}">
                <input type="hidden" name="transactionReference" value="${transactionReference}">
                <input type="hidden" name="paymentMode" value="UPI">

                <label style="font-weight:600; font-size:13px; color:#4a5568;">Repayment Amount (₹)</label>
                <input type="number" step="any" name="amount" min="1" max="${loan.remaining}"
                       placeholder="Enter amount to pay" required>

                <button type="submit" class="btn" style="margin-top:10px;">Confirm & Pay</button>
            </form>
        </c:when>
        <c:otherwise>
            <div class="alert alert-error">No active loan record found to process payment.</div>
        </c:otherwise>
    </c:choose>

    <div style="text-align:center; margin-top:20px;">
        <a href="/dashboard" style="font-size:13px; color:#718096; text-decoration:none;">Cancel & Back to Dashboard</a>
    </div>
</div>