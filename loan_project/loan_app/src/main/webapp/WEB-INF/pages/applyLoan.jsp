<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="header.jsp" %>

<c:set var="actingUser" value="${not empty currentUser ? currentUser : sessionScope.user}" />
<c:set var="isStaffUser" value="${actingUser.role == 'ROLE_LOAN_OFFICER' || actingUser.role == 'ROLE_ADMIN'}" />

<div class="card" style="max-width: 520px; width: 90%;">
    <c:choose>
        <c:when test="${isStaffUser}">
            <h2 style="text-align:center; margin-top:0; color:#24446B;">Originate New Loan</h2>
            <p style="text-align:center; color:#64748B; font-size:13px; margin-top:-10px; margin-bottom:20px;">
                Originate a sanctioned loan on behalf of a registered customer account.
            </p>
        </c:when>
        <c:otherwise>
            <h2 style="text-align:center; margin-top:0; color:#24446B;">Apply for a Loan</h2>
            <p style="text-align:center; color:#64748B; font-size:13px; margin-top:-10px; margin-bottom:20px;">
                Calculate your EMI and submit your loan application.
            </p>
        </c:otherwise>
    </c:choose>

    <form action="/applyLoan" method="post">
        <!-- CSRF Token -->
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

        <c:if test="${isStaffUser}">
            <label style="font-weight:600; font-size:13px; color:#24446B;">Borrower / Customer Email <span style="color:#DC2626;">*</span></label>
            <input type="email" name="customerEmail" placeholder="customer@bank.com" required>
        </c:if>

        <label style="font-weight:600; font-size:13px; color:#24446B;">Loan Amount (₹)</label>
        <input type="number" step="any" name="amount" min="100" placeholder="e.g. 50000" required>

        <label style="font-weight:600; font-size:13px; color:#24446B;">Annual Interest Rate (%)</label>
        <input type="number" step="any" name="interest" min="0.1" max="100" placeholder="e.g. 8.5" required>

        <label style="font-weight:600; font-size:13px; color:#24446B;">Tenure (Months)</label>
        <input type="number" name="months" min="1" max="360" placeholder="e.g. 24" required>

        <c:choose>
            <c:when test="${isStaffUser}">
                <button type="submit" class="btn" style="margin-top:10px; background-color:#24446B;">Originate Loan for Customer</button>
            </c:when>
            <c:otherwise>
                <button type="submit" class="btn" style="margin-top:10px; background-color:#ED7D16;">Submit Loan Application</button>
            </c:otherwise>
        </c:choose>
    </form>

    <div style="text-align:center; margin-top:20px;">
        <a href="/dashboard" style="font-size:13px; color:#64748B; text-decoration:none;">Cancel & Back to Dashboard</a>
    </div>
</div>