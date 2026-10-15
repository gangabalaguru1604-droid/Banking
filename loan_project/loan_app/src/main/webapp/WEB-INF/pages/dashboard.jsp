<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="header.jsp" %>

<div style="padding: 10px 20px 40px 20px;">

<c:choose>
    
    <c:when test="${isStaff}">
        <c:set var="staffUser" value="${not empty currentUser ? currentUser : sessionScope.user}" />
        <div class="wide-card" style="margin-top: 15px;">
            <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:15px; border-bottom:1px solid #edf2f7; padding-bottom:20px;">
                <div>
                    <h2 style="margin:0; color:#2c3e50; font-size:24px; font-weight:700;">Staff Loan Portfolio</h2>
                    <p style="color:#718096; font-size:14px; margin:6px 0 0 0;">
                        Logged in as: <strong><c:out value="${staffUser.name}"/></strong> 
                        <span class="role-pill" style="margin-left:6px;"><c:out value="${staffUser.role}"/></span>
                        <c:if test="${not empty officerBranch}">
                            &bull; Branch: <strong><c:out value="${officerBranch}"/></strong>
                        </c:if>
                    </p>
                </div>
                <div style="display:flex; gap:10px; align-items:center;">
                    <a href="/loan-search" class="btn btn-secondary" style="width:auto; padding:10px 18px; font-size:14px; text-decoration:none; display:inline-flex; align-items:center; gap:6px;">
                        <span>🔍 Loan Search</span>
                    </a>
                </div>
            </div>

            <!-- Responsive Table Container -->
            <div class="table-responsive">
                <table>
                    <thead>
                        <tr>
                            <th>Account / ID</th>
                            <th>Borrower</th>
                            <th>Masked Email</th>
                            <th>Principal</th>
                            <th>Interest</th>
                            <th>Tenure</th>
                            <th>Monthly EMI</th>
                            <th>Outstanding Balance</th>
                            <th>Branch</th>
                            <th style="text-align:center;">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="l" items="${loans}">
                            <tr>
                                <td>
                                    <strong style="color:#2b6cb0; font-family:monospace; font-size:14px;">
                                        <c:choose>
                                            <c:when test="${not empty l.accountNumber}">
                                                <c:out value="${l.accountNumber}"/>
                                            </c:when>
                                            <c:otherwise>
                                                #<c:out value="${l.loanId}"/>
                                            </c:otherwise>
                                        </c:choose>
                                    </strong>
                                </td>
                                <td>
                                    <strong style="color:#2d3748;"><c:out value="${l.user.name}"/></strong>
                                </td>
                                <td>
                                    <span style="font-family:monospace; color:#4a5568; background:#edf2f7; padding:2px 6px; border-radius:4px; font-size:13px;">
                                        <c:out value="${l.user.email}"/>
                                    </span>
                                </td>
                                <td>₹<fmt:formatNumber value="${l.amount}" pattern="#,##0.00"/></td>
                                <td><c:out value="${l.interest}"/>%</td>
                                <td><c:out value="${l.months}"/> mos</td>
                                <td style="color:#2b6cb0; font-weight:600;">
                                    ₹<fmt:formatNumber value="${l.emi}" pattern="#,##0.00"/>
                                </td>
                                <td>
                                    <strong style="color:${l.remaining > 0 ? '#c53030' : '#276749'};">
                                        ₹<fmt:formatNumber value="${l.remaining}" pattern="#,##0.00"/>
                                    </strong>
                                </td>
                                <td>
                                    <span style="background:#edf2f7; color:#4a5568; padding:3px 8px; border-radius:12px; font-size:12px; font-weight:600;">
                                        <c:out value="${l.branch}"/>
                                    </span>
                                </td>
                                <td style="text-align:center;">
                                    <div style="display:inline-flex; gap:6px; align-items:center;">
                                        <a href="/loan-details?loanId=${l.loanId}" 
                                           class="btn" 
                                           style="padding:6px 14px; font-size:12px; width:auto; text-decoration:none; display:inline-block; background:#4f46e5; color:#ffffff; font-weight:600; border-radius:4px;">
                                            Profile
                                        </a>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty loans}">
                            <tr>
                                <td colspan="10" style="text-align:center; padding:50px 20px; color:#718096;">
                                    <div style="font-size:36px; margin-bottom:10px;">📋</div>
                                    <div style="font-size:16px; font-weight:600; color:#2d3748; margin-bottom:4px;">No Active Loans Available</div>
                                    <div style="font-size:13px; color:#a0aec0;">There are currently no active loan records assigned to your branch.</div>
                                </td>
                            </tr>
                        </c:if>
                    </tbody>
                </table>
            </div>
        </div>
    </c:when>

    <%-- Customer Dashboard View --%>
    <c:otherwise>
        <c:set var="userObj" value="${not empty currentUser ? currentUser : sessionScope.user}" />

        <style>
            .customer-dashboard {
                max-width: 1320px;
                margin: 20px auto 50px auto;
                padding: 0 16px;
            }

            /* WELCOME BANNER */
            .welcome-banner {
                display: flex;
                justify-content: space-between;
                align-items: center;
                flex-wrap: wrap;
                gap: 16px;
                margin-bottom: 28px;
                background: #FFFFFF;
                padding: 24px 28px;
                border-radius: 12px;
                border: 1px solid #E2E8F0;
                box-shadow: 0 1px 3px rgba(0,0,0,0.03);
            }

            .welcome-title {
                color: #172B4D;
                font-size: 26px;
                font-weight: 800;
                margin: 0 0 6px 0;
                letter-spacing: -0.3px;
            }

            .welcome-sub {
                color: #64748B;
                font-size: 15px;
                margin: 0;
            }

            .btn-orange-primary {
                background-color: #ED7D16;
                color: #FFFFFF !important;
                font-weight: 700;
                font-size: 14px;
                padding: 12px 24px;
                border-radius: 8px;
                text-decoration: none;
                display: inline-flex;
                align-items: center;
                gap: 8px;
                border: none;
                cursor: pointer;
                transition: background-color 0.15s ease, transform 0.1s ease;
                box-shadow: 0 2px 4px rgba(237, 125, 22, 0.2);
            }

            .btn-orange-primary:hover {
                background-color: #D96B0E;
                transform: translateY(-1px);
            }

            /* 4 SUMMARY CARDS */
            .summary-grid {
                display: grid;
                grid-template-columns: repeat(4, 1fr);
                gap: 20px;
                margin-bottom: 32px;
            }

            .summary-card {
                background: #FFFFFF;
                border: 1px solid #E2E8F0;
                border-radius: 12px;
                padding: 22px 24px;
                box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
                display: flex;
                flex-direction: column;
                justify-content: space-between;
                min-height: 140px;
                box-sizing: border-box;
                transition: box-shadow 0.2s ease, border-color 0.2s ease;
            }

            .summary-card:hover {
                box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
                border-color: #CBD5E1;
            }

            .card-top {
                display: flex;
                justify-content: space-between;
                align-items: center;
                margin-bottom: 12px;
            }

            .card-label {
                color: #24446B;
                font-size: 13px;
                font-weight: 700;
                text-transform: uppercase;
                letter-spacing: 0.5px;
            }

            .card-icon-wrap {
                width: 38px;
                height: 38px;
                border-radius: 10px;
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 18px;
            }

            .card-value {
                font-size: 30px;
                font-weight: 800;
                color: #172B4D;
                line-height: 1.1;
                margin-bottom: 6px;
            }

            .card-desc {
                font-size: 12px;
                color: #64748B;
                font-weight: 500;
            }

            /* CONTENT SECTION */
            .dashboard-section-card {
                background: #FFFFFF;
                border: 1px solid #E2E8F0;
                border-radius: 12px;
                padding: 26px 28px;
                box-shadow: 0 1px 3px rgba(0,0,0,0.03);
                margin-bottom: 32px;
            }

            .section-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                border-bottom: 1px solid #EDF2F7;
                padding-bottom: 16px;
                margin-bottom: 20px;
                flex-wrap: wrap;
                gap: 12px;
            }

            .section-title {
                color: #172B4D;
                font-size: 18px;
                font-weight: 700;
                margin: 0;
                letter-spacing: -0.2px;
            }

            .section-subtitle {
                color: #64748B;
                font-size: 13px;
                margin: 4px 0 0 0;
            }

            /* ACTIVITY LIST */
            .activity-list {
                display: flex;
                flex-direction: column;
            }

            .activity-row {
                display: flex;
                align-items: center;
                justify-content: space-between;
                padding: 16px 0;
                border-bottom: 1px solid #F1F5F9;
                gap: 16px;
            }

            .activity-row:last-child {
                border-bottom: none;
                padding-bottom: 0;
            }

            .activity-left {
                display: flex;
                align-items: center;
                gap: 14px;
            }

            .activity-icon {
                width: 42px;
                height: 42px;
                border-radius: 10px;
                background: #F1F5F9;
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 20px;
                flex-shrink: 0;
            }

            .activity-title {
                color: #172B4D;
                font-size: 14px;
                font-weight: 700;
                margin: 0 0 4px 0;
            }

            .activity-desc {
                color: #64748B;
                font-size: 13px;
                margin: 0;
            }

            .activity-right {
                text-align: right;
                flex-shrink: 0;
            }

            .activity-date {
                color: #94A3B8;
                font-size: 12px;
                margin-top: 4px;
            }

            .badge-pill {
                display: inline-block;
                padding: 4px 10px;
                border-radius: 12px;
                font-size: 11px;
                font-weight: 700;
                letter-spacing: 0.3px;
                text-transform: uppercase;
            }

            .badge-success {
                background: #E6F7F3;
                color: #0F9B79;
            }

            .badge-warning {
                background: #FEF3EB;
                color: #ED7D16;
            }

            .badge-secondary {
                background: #F1F5F9;
                color: #475569;
            }

            .badge-danger {
                background: #FEE2E2;
                color: #DC2626;
            }

            @media (max-width: 1024px) {
                .summary-grid {
                    grid-template-columns: repeat(2, 1fr);
                }
            }

            @media (max-width: 640px) {
                .summary-grid {
                    grid-template-columns: 1fr;
                }
                .welcome-banner {
                    flex-direction: column;
                    align-items: flex-start;
                }
                .btn-orange-primary {
                    width: 100%;
                    justify-content: center;
                }
                .activity-row {
                    flex-direction: column;
                    align-items: flex-start;
                }
                .activity-right {
                    text-align: left;
                }
            }
        </style>

        <div class="customer-dashboard">

            <%-- Welcome Section --%>
            <div class="welcome-banner">
                <div>
                    <h1 class="welcome-title">Welcome back, <c:out value="${userObj.name}"/>!</h1>
                    <p class="welcome-sub">Here's an overview of your loan account.</p>
                </div>
                <div>
                    <a href="/applyLoan" class="btn-orange-primary">
                        <span style="font-size: 16px; font-weight: 800;">+</span> Apply for Loan
                    </a>
                </div>
            </div>

            <%-- Loan Selection Notification Banner --%>
            <c:if test="${param.selectLoan}">
                <div class="alert alert-info" style="margin-bottom:24px; display:flex; align-items:center; gap:12px; border-left:4px solid #315B91;">
                    <span style="font-size:20px;">ℹ️</span>
                    <div>
                        <strong>Select Loan for Repayment:</strong> You have multiple active loan accounts. Please click the <strong>"Pay EMI"</strong> button next to the specific loan you wish to repay below.
                    </div>
                </div>
            </c:if>

            <%-- Four Summary Cards --%>
            <div class="summary-grid">
                <!-- 1. Total Loans -->
                <div class="summary-card">
                    <div class="card-top">
                        <span class="card-label">Total Loans</span>
                        <div class="card-icon-wrap" style="background:#F0F4F8; color:#24446B;">📋</div>
                    </div>
                    <div class="card-value">
                        <c:out value="${totalLoanCount != null ? totalLoanCount : 0}"/>
                    </div>
                    <div class="card-desc">All loan accounts on record</div>
                </div>

                <!-- 2. Active Loans -->
                <div class="summary-card">
                    <div class="card-top">
                        <span class="card-label">Active Loans</span>
                        <div class="card-icon-wrap" style="background:#E6F7F3; color:#0F9B79;">🟢</div>
                    </div>
                    <div class="card-value">
                        <c:out value="${activeLoanCount != null ? activeLoanCount : 0}"/>
                    </div>
                    <div class="card-desc">Currently servicing</div>
                </div>

                <!-- 3. Pending Applications -->
                <div class="summary-card">
                    <div class="card-top">
                        <span class="card-label">Pending Applications</span>
                        <div class="card-icon-wrap" style="background:#FEF3EB; color:#ED7D16;">⏳</div>
                    </div>
                    <div class="card-value">
                        <c:out value="${pendingLoanCount != null ? pendingLoanCount : 0}"/>
                    </div>
                    <div class="card-desc">Awaiting sanction / review</div>
                </div>

                <!-- 4. Outstanding Balance -->
                <div class="summary-card" style="border-left: 3px solid #ED7D16;">
                    <div class="card-top">
                        <span class="card-label">Outstanding Balance</span>
                        <div class="card-icon-wrap" style="background:#FEF3EB; color:#ED7D16;">💳</div>
                    </div>
                    <div class="card-value" style="color:#ED7D16;">
                        ₹<fmt:formatNumber value="${outstandingBalance != null ? outstandingBalance : (totalRemaining != null ? totalRemaining : 0.0)}" pattern="#,##0.00"/>
                    </div>
                    <div class="card-desc">Total remaining principal</div>
                </div>
            </div>

            <%-- Active Loans / Empty State Section --%>
            <c:choose>
                <c:when test="${not empty loans && activeLoanCount > 0}">
                    <div class="dashboard-section-card">
                        <div class="section-header">
                            <div>
                                <h2 class="section-title">Your Loan Portfolio</h2>
                                <p class="section-subtitle">Real-time balances and EMI installment commitments</p>
                            </div>
                            <span style="font-size:13px; color:#64748B; font-weight:600;">
                                Showing <c:out value="${loans.size()}"/> accounts
                            </span>
                        </div>

                        <div class="table-responsive" style="margin-top:0; border:none;">
                            <table>
                                <thead>
                                    <tr>
                                        <th>Loan Account</th>
                                        <th>Product Type</th>
                                        <th>Sanctioned Amount</th>
                                        <th>Interest Rate</th>
                                        <th>Tenure</th>
                                        <th>Monthly EMI</th>
                                        <th>Outstanding Balance</th>
                                        <th>Status</th>
                                        <th style="text-align:center;">Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="l" items="${loans}">
                                        <tr>
                                            <td>
                                                <strong style="color:#24446B; font-family:monospace; font-size:14px;">
                                                    <c:choose>
                                                        <c:when test="${not empty l.accountNumber}">
                                                             <c:out value="${l.accountNumber}"/>
                                                        </c:when>
                                                        <c:otherwise>
                                                             #<c:out value="${l.loanId}"/>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </strong>
                                            </td>
                                            <td>
                                                <c:out value="${not empty l.productType ? l.productType : 'Personal Loan'}"/>
                                            </td>
                                            <td>₹<fmt:formatNumber value="${l.amount}" pattern="#,##0.00"/></td>
                                            <td><c:out value="${l.interest}"/>% (<c:out value="${not empty l.interestType ? l.interestType : 'FIXED'}"/>)</td>
                                            <td><c:out value="${l.months}"/> mos</td>
                                            <td style="color:#315B91; font-weight:600;">
                                                ₹<fmt:formatNumber value="${l.emi}" pattern="#,##0.00"/>
                                            </td>
                                            <td>
                                                <strong style="color:${l.remaining > 0 ? '#ED7D16' : '#0F9B79'};">
                                                    ₹<fmt:formatNumber value="${l.remaining}" pattern="#,##0.00"/>
                                                </strong>
                                            </td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${l.status == 'ACTIVE' || l.status == 'APPROVED'}">
                                                        <span class="badge-pill badge-success">ACTIVE</span>
                                                    </c:when>
                                                    <c:when test="${l.status == 'CLOSED'}">
                                                        <span class="badge-pill badge-secondary">CLOSED</span>
                                                    </c:when>
                                                    <c:when test="${l.status == 'REJECTED'}">
                                                        <span class="badge-pill badge-danger">REJECTED</span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="badge-pill badge-warning"><c:out value="${l.status}"/></span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td style="text-align:center;">
                                                <c:choose>
                                                    <c:when test="${l.remaining > 0}">
                                                        <a href="/payPage?loanId=${l.loanId}" 
                                                           class="btn" 
                                                           style="padding:7px 16px; font-size:13px; width:auto; text-decoration:none; display:inline-block; background:#24446B; color:#FFFFFF; border-radius:6px; font-weight:600;">
                                                            Pay EMI
                                                        </a>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span style="color:#0F9B79; font-size:13px; font-weight:700;">Fully Paid</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </c:when>
                <c:otherwise>
                    <%-- Empty-loan state card --%>
                    <div style="background:#FFFFFF; border:1px solid #E2E8F0; border-radius:12px; padding:48px 32px; text-align:center; max-width:560px; margin:20px auto 32px auto; box-shadow:0 1px 3px rgba(0,0,0,0.04);">
                        <div style="font-size:52px; margin-bottom:16px;">🏦</div>
                        <h3 style="color:#172B4D; font-size:22px; font-weight:700; margin:0 0 10px 0;">No Active Loans</h3>
                        <p style="color:#64748B; font-size:14px; line-height:1.5; margin:0 0 24px 0;">
                            You don't have any active loans yet. Start a new application to get started.
                        </p>
                        <a href="/applyLoan" class="btn-orange-primary" style="margin:0 auto;">
                            Apply for a Loan
                        </a>
                    </div>
                </c:otherwise>
            </c:choose>

            <%-- Recent Activity Section --%>
            <div class="dashboard-section-card activity-section">
                <div class="section-header">
                    <div>
                        <h2 class="section-title">Recent Activity</h2>
                        <p class="section-subtitle">Your latest loan applications, approvals, and repayment records</p>
                    </div>
                </div>

                <c:choose>
                    <c:when test="${not empty recentActivities}">
                        <div class="activity-list">
                            <c:forEach var="act" items="${recentActivities}">
                                <div class="activity-row">
                                    <div class="activity-left">
                                        <div class="activity-icon">
                                            <c:out value="${act.icon}"/>
                                        </div>
                                        <div>
                                            <h4 class="activity-title"><c:out value="${act.title}"/></h4>
                                            <p class="activity-desc"><c:out value="${act.description}"/></p>
                                        </div>
                                    </div>
                                    <div class="activity-right">
                                        <span class="badge-pill <c:out value="${act.badgeClass}"/>">
                                            <c:out value="${act.status}"/>
                                        </span>
                                        <div class="activity-date"><c:out value="${act.formattedDate}"/></div>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div style="text-align:center; padding:36px 20px; color:#64748B;">
                            <div style="font-size:32px; margin-bottom:10px;">⏱️</div>
                            <p style="margin:0; font-size:14px; font-weight:500;">
                                Your recent applications and repayments will appear here.
                            </p>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

        </div>
    </c:otherwise>
</c:choose>

</div>
</body>
</html>