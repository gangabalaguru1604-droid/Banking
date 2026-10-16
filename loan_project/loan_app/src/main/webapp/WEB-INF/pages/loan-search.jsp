<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="header.jsp" %>

<div class="wide-card">
    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:15px;">
        <div>
            <h2 style="margin:0; color:#2c3e50;">🔍 Loan Account & Customer Search</h2>
            <p style="color:#718096; font-size:14px; margin:4px 0 0 0;">
                Authorized Bank Staff Portal — Search customer loan accounts and KYC profiles.
            </p>
        </div>
        <div>
            <span style="background:#edf2f7; color:#2d3748; padding:6px 14px; border-radius:20px; font-size:13px; font-weight:600; border:1px solid #cbd5e0;">
                🏢 Authorized Branch: <strong><c:out value="${officerBranch}"/></strong>
            </span>
        </div>
    </div>

    <!-- Error & Info Banners -->
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-error" id="searchErrorMessage">
            <strong>⚠️ Notice:</strong> <c:out value="${errorMessage}"/>
        </div>
    </c:if>

    <c:if test="${not empty infoMessage}">
        <div class="alert alert-info">
            ℹ️ <c:out value="${infoMessage}"/>
        </div>
    </c:if>

    <!-- Search Form Card -->
    <div style="background:#f8fafc; border:1px solid #e2e8f0; border-radius:8px; padding:20px; margin-bottom:25px;">
        <form action="/loan-search" method="get" id="searchForm">
            <div style="display:grid; grid-template-columns: 220px 1fr 140px; gap:15px; align-items:end;">
                <div>
                    <label style="font-weight:600; font-size:13px; color:#4a5568;">Search By Category</label>
                    <select name="searchType" id="searchTypeSelect" onchange="updateSearchPlaceholder()" style="margin-top:6px; margin-bottom:0;">
                        <option value="ACCOUNT_NUMBER" ${searchType == 'ACCOUNT_NUMBER' ? 'selected' : ''}>Loan Account No</option>
                        <option value="CUSTOMER_ID" ${searchType == 'CUSTOMER_ID' ? 'selected' : ''}>Customer ID</option>
                        <option value="PHONE" ${searchType == 'PHONE' ? 'selected' : ''}>Registered Mobile</option>
                        <option value="NAME" ${searchType == 'NAME' ? 'selected' : ''}>Customer Name</option>
                    </select>
                </div>

                <div>
                    <label style="font-weight:600; font-size:13px; color:#4a5568;" id="queryInputLabel">Search Query</label>
                    <input type="text" name="query" id="queryInput" value="<c:out value='${query}'/>" 
                           placeholder="Enter 12-character Account Number (e.g. LN0000000004)" 
                           required maxlength="50" style="margin-top:6px; margin-bottom:0;" />
                </div>

                <div>
                    <button type="submit" class="btn" style="height:44px; margin-bottom:0; display:flex; align-items:center; justify-content:center; gap:6px;">
                        <span>🔎 Search</span>
                    </button>
                </div>
            </div>

            <div style="margin-top:10px; font-size:12px; color:#718096;" id="queryFormatHint">
                💡 Exact Loan Account Number format: 2 uppercase letters followed by 10 digits (e.g., <strong>LN0000000004</strong>).
            </div>
        </form>
    </div>

    <!-- Results Section -->
    <c:if test="${hasSearched}">
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px;">
            <h3 style="margin:0; color:#2d3748;">
                Search Results 
                <c:if test="${results != null && results.totalElements > 0}">
                    <span style="font-size:14px; font-weight:normal; color:#718096;">
                        (Found ${results.totalElements} matching record${results.totalElements > 1 ? 's' : ''})
                    </span>
                </c:if>
            </h3>
        </div>

        <c:choose>
            <c:when test="${results != null && results.totalElements > 0}">
                <table>
                    <thead>
                        <tr>
                            <th>Account No</th>
                            <th>Customer ID</th>
                            <th>Customer Name</th>
                            <th>Mobile</th>
                            <th>Branch</th>
                            <th>Sanctioned</th>
                            <th>Outstanding</th>
                            <th>Repayment</th>
                            <th>DPD</th>
                            <th>Status</th>
                            <th style="text-align:center;">Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="row" items="${results.content}">
                            <tr>
                                <td>
                                    <strong style="color:#2b6cb0; font-family:monospace; font-size:14px;">
                                        <c:out value="${row.accountNumber}"/>
                                    </strong>
                                </td>
                                <td>
                                    <span style="font-family:monospace; color:#4a5568;">
                                        <c:out value="${row.customerId}"/>
                                    </span>
                                </td>
                                <td><strong><c:out value="${row.customerName}"/></strong></td>
                                <td><c:out value="${row.maskedPhone}"/></td>
                                <td>
                                    <span style="background:#edf2f7; color:#4a5568; padding:3px 8px; border-radius:12px; font-size:12px; font-weight:600;">
                                        <c:out value="${row.branch}"/>
                                    </span>
                                </td>
                                <td>₹<fmt:formatNumber value="${row.amount}" pattern="#,##0.00"/></td>
                                <td><strong style="color:${row.remaining > 0 ? '#c53030' : '#276749'};">₹<fmt:formatNumber value="${row.remaining}" pattern="#,##0.00"/></strong></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${row.repaymentStatus == 'OVERDUE'}">
                                            <span style="color:#9b2c2c; background:#fed7d7; padding:3px 8px; border-radius:10px; font-size:12px; font-weight:700;">
                                                ⚠️ OVERDUE
                                                <c:if test="${row.overdueAmount > 0}">
                                                    (₹<fmt:formatNumber value="${row.overdueAmount}" pattern="#,##0"/>)
                                                </c:if>
                                            </span>
                                        </c:when>
                                        <c:when test="${row.repaymentStatus == 'CURRENT'}">
                                            <span style="color:#22543d; background:#c6f6d5; padding:3px 8px; border-radius:10px; font-size:12px; font-weight:600;">CURRENT</span>
                                        </c:when>
                                        <c:when test="${row.repaymentStatus == 'CLOSED'}">
                                            <span style="color:#4a5568; background:#e2e8f0; padding:3px 8px; border-radius:10px; font-size:12px; font-weight:600;">CLOSED</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span style="color:#744210; background:#feebc8; padding:3px 8px; border-radius:10px; font-size:12px; font-weight:600;"><c:out value="${row.repaymentStatus}"/></span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${row.dpd > 0}">
                                            <span style="color:#742a2a; background:#fed7d7; padding:3px 8px; border-radius:10px; font-size:12px; font-weight:700;">
                                                <c:out value="${row.dpd}"/> DPD
                                            </span>
                                        </c:when>
                                        <c:when test="${row.status == 'CLOSED'}">
                                            <span style="color:#a0aec0; font-size:12px;">-</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span style="color:#22543d; background:#c6f6d5; padding:3px 8px; border-radius:10px; font-size:12px; font-weight:600;">0 DPD</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${row.status == 'ACTIVE'}">
                                            <span style="color:#276749; background:#c6f6d5; padding:3px 8px; border-radius:10px; font-size:12px; font-weight:600;">ACTIVE</span>
                                        </c:when>
                                        <c:when test="${row.status == 'CLOSED'}">
                                            <span style="color:#4a5568; background:#e2e8f0; padding:3px 8px; border-radius:10px; font-size:12px; font-weight:600;">CLOSED</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span style="color:#b7791f; background:#feebc8; padding:3px 8px; border-radius:10px; font-size:12px; font-weight:600;"><c:out value="${row.status}"/></span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td style="text-align:center;">
                                    <a href="/loan-details?loanId=${row.loanId}" 
                                       class="btn" 
                                       style="display:inline-block; text-decoration:none; text-align:center; padding:6px 14px; font-size:12px; width:auto; background:#24446B; color:#ffffff; font-weight:600; border-radius:4px;">
                                        Profile
                                    </a>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>

                <!-- Pagination -->
                <c:if test="${results.totalPages > 1}">
                    <div style="display:flex; justify-content:space-between; align-items:center; margin-top:20px; padding-top:15px; border-top:1px solid #edf2f7;">
                        <div style="font-size:13px; color:#718096;">
                            Page <strong>${results.number + 1}</strong> of <strong>${results.totalPages}</strong>
                        </div>
                        <div style="display:flex; gap:8px;">
                            <c:if test="${results.hasPrevious()}">
                                <a href="/loan-search?searchType=${searchType}&query=<c:out value='${query}'/>&page=${results.number - 1}&size=${size}" 
                                   class="btn btn-secondary" style="padding:6px 14px; width:auto; font-size:13px; text-decoration:none;">
                                    &laquo; Previous
                                </a>
                            </c:if>
                            <c:if test="${results.hasNext()}">
                                <a href="/loan-search?searchType=${searchType}&query=<c:out value='${query}'/>&page=${results.number + 1}&size=${size}" 
                                   class="btn btn-secondary" style="padding:6px 14px; width:auto; font-size:13px; text-decoration:none;">
                                    Next &raquo;
                                </a>
                            </c:if>
                        </div>
                    </div>
                </c:if>
            </c:when>
            <c:otherwise>
                <div style="background:#fffaf0; border:1px solid #feebc8; padding:25px; border-radius:8px; text-align:center; color:#744210; margin-top:10px;">
                    <p style="font-size:16px; font-weight:600; margin:0 0 6px 0;">No Loan Account Records Match Your Criteria</p>
                    <p style="font-size:13px; color:#975a16; margin:0;">
                        Please verify the input and ensure the account belongs to your authorized branch.
                    </p>
                </div>
            </c:otherwise>
        </c:choose>
    </c:if>
</div>

<script>
    function updateSearchPlaceholder() {
        const type = document.getElementById('searchTypeSelect').value;
        const input = document.getElementById('queryInput');
        const hint = document.getElementById('queryFormatHint');

        if (type === 'ACCOUNT_NUMBER') {
            input.placeholder = "Enter 12-character Account Number (e.g. LN0000000004)";
            input.maxLength = 12;
            hint.innerHTML = "💡 Exact Loan Account Number format: 2 uppercase letters followed by 10 digits (e.g., <strong>LN0000000004</strong>).";
        } else if (type === 'CUSTOMER_ID') {
            input.placeholder = "Enter Customer ID (e.g. CUST000001 or 1)";
            input.maxLength = 25;
            hint.innerHTML = "💡 Secondary search: Exact Customer ID match within your authorized branch.";
        } else if (type === 'PHONE') {
            input.placeholder = "Enter 10-digit mobile number (e.g. 9876543210)";
            input.maxLength = 15;
            hint.innerHTML = "💡 Secondary search: Registered phone number matching authorized customer accounts.";
        } else if (type === 'NAME') {
            input.placeholder = "Enter customer name (e.g. John Doe)";
            input.maxLength = 50;
            hint.innerHTML = "💡 Secondary search: Case-insensitive customer name match within your branch.";
        }
    }

    // Set initial placeholder on page load
    document.addEventListener('DOMContentLoaded', updateSearchPlaceholder);
</script>
</body>
</html>
