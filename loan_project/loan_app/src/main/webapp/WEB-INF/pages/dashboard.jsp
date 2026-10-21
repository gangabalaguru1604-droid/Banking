<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="header.jsp" %>

<div style="padding: 10px 20px 40px 20px;">

<c:choose>
    
    <c:when test="${isStaff}">
        <c:set var="staffUser" value="${not empty currentUser ? currentUser : sessionScope.user}" />

        <style>
            .staff-dashboard {
                max-width: 1400px;
                margin: 15px auto 40px auto;
                padding: 0 16px;
            }

            .staff-banner {
                background: #FFFFFF;
                border: 1px solid #E2E8F0;
                border-radius: 12px;
                padding: 24px 28px;
                margin-bottom: 24px;
                box-shadow: 0 1px 3px rgba(0,0,0,0.03);
                display: flex;
                justify-content: space-between;
                align-items: center;
                flex-wrap: wrap;
                gap: 16px;
            }

            .staff-banner-title {
                color: #24446B;
                font-size: 24px;
                font-weight: 800;
                margin: 0 0 6px 0;
                letter-spacing: -0.3px;
            }

            .staff-banner-sub {
                color: #64748B;
                font-size: 14px;
                margin: 0;
                display: flex;
                align-items: center;
                flex-wrap: wrap;
                gap: 8px;
            }

            /* METRICS GRID */
            .metrics-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
                gap: 18px;
                margin-bottom: 28px;
            }

            .metric-card {
                background: #FFFFFF;
                border: 1px solid #E2E8F0;
                border-radius: 10px;
                padding: 20px 22px;
                box-shadow: 0 1px 3px rgba(0,0,0,0.02);
                transition: transform 0.15s ease, box-shadow 0.15s ease;
            }

            .metric-card:hover {
                transform: translateY(-2px);
                box-shadow: 0 4px 12px rgba(0,0,0,0.05);
            }

            .clickable-card {
                cursor: pointer;
                position: relative;
                transition: transform 0.2s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.2s cubic-bezier(0.16, 1, 0.3, 1), border-color 0.2s ease;
                outline: none;
            }

            .clickable-card:hover {
                transform: translateY(-4px) scale(1.01);
                box-shadow: 0 10px 24px rgba(36, 68, 107, 0.12);
                border-color: #24446B;
            }

            .clickable-card:focus-visible {
                outline: 2px solid #ED7D16;
                outline-offset: 3px;
                box-shadow: 0 10px 24px rgba(237, 125, 22, 0.18);
            }

            .card-action-link {
                color: #24446B;
                font-weight: 700;
                font-size: 11px;
                transition: color 0.15s ease;
                display: inline-flex;
                align-items: center;
                gap: 4px;
            }

            .clickable-card:hover .card-action-link {
                color: #ED7D16;
                text-decoration: underline;
            }

            .metric-label {
                font-size: 12px;
                font-weight: 700;
                color: #64748B;
                text-transform: uppercase;
                letter-spacing: 0.5px;
                margin-bottom: 8px;
                display: flex;
                justify-content: space-between;
                align-items: center;
            }

            .metric-value {
                font-size: 26px;
                font-weight: 800;
                color: #172B4D;
                margin-bottom: 4px;
            }

            .metric-hint {
                font-size: 12px;
                color: #94A3B8;
            }

            /* DPD BUCKETS ROW */
            .dpd-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
                gap: 14px;
                margin-bottom: 28px;
            }

            .dpd-card {
                background: #FFFFFF;
                border: 1px solid #E2E8F0;
                border-radius: 8px;
                padding: 16px;
                border-left: 4px solid #CBD5E1;
            }

            .dpd-card.current { border-left-color: #10B981; }
            .dpd-card.sma0 { border-left-color: #F59E0B; }
            .dpd-card.sma1 { border-left-color: #F97316; }
            .dpd-card.sma2 { border-left-color: #EF4444; }
            .dpd-card.npa { border-left-color: #B91C1C; background: #FFF5F5; }

            .dpd-title {
                font-size: 12px;
                font-weight: 700;
                color: #475569;
                text-transform: uppercase;
                margin-bottom: 4px;
            }

            .dpd-count {
                font-size: 20px;
                font-weight: 800;
                color: #0F172A;
            }

            .dpd-amt {
                font-size: 12px;
                color: #64748B;
                margin-top: 2px;
            }

            /* CONTENT PANELS */
            .staff-panel {
                background: #FFFFFF;
                border: 1px solid #E2E8F0;
                border-radius: 12px;
                padding: 24px;
                margin-bottom: 28px;
                box-shadow: 0 1px 3px rgba(0,0,0,0.02);
            }

            .staff-panel-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                flex-wrap: wrap;
                gap: 12px;
                margin-bottom: 18px;
                padding-bottom: 14px;
                border-bottom: 1px solid #F1F5F9;
            }

            .staff-panel-title {
                font-size: 18px;
                font-weight: 700;
                color: #24446B;
                margin: 0;
                display: flex;
                align-items: center;
                gap: 8px;
            }

            /* ACTION BUTTONS */
            .action-btn-group {
                display: inline-flex;
                gap: 6px;
                align-items: center;
                justify-content: center;
            }

            .btn-profile {
                background: #24446B;
                color: #FFFFFF !important;
                padding: 6px 12px;
                border-radius: 4px;
                font-size: 12px;
                font-weight: 600;
                text-decoration: none;
                display: inline-flex;
                align-items: center;
                gap: 4px;
                transition: background 0.15s ease;
            }

            .btn-profile:hover {
                background: #172B4D;
            }

            .btn-approve {
                background: #059669;
                color: #FFFFFF;
                border: none;
                padding: 6px 12px;
                border-radius: 4px;
                font-size: 12px;
                font-weight: 600;
                cursor: pointer;
                transition: background 0.15s ease;
            }

            .btn-approve:hover {
                background: #047857;
            }

            .btn-reject {
                background: #DC2626;
                color: #FFFFFF;
                border: none;
                padding: 6px 12px;
                border-radius: 4px;
                font-size: 12px;
                font-weight: 600;
                cursor: pointer;
                transition: background 0.15s ease;
            }

            .btn-reject:hover {
                background: #B91C1C;
            }

            .btn-collect {
                background: #ED7D16;
                color: #FFFFFF;
                border: none;
                padding: 6px 12px;
                border-radius: 4px;
                font-size: 12px;
                font-weight: 600;
                cursor: pointer;
                transition: background 0.15s ease;
            }

            .btn-collect:hover {
                background: #D96F10;
            }

            /* STATUS BADGES */
            .status-badge {
                display: inline-block;
                padding: 3px 8px;
                border-radius: 10px;
                font-size: 11px;
                font-weight: 700;
                text-transform: uppercase;
                letter-spacing: 0.3px;
            }

            .status-active { background: #DCFCE7; color: #15803D; }
            .status-pending { background: #FEF3C7; color: #B45309; }
            .status-overdue { background: #FEE2E2; color: #B91C1C; }
            .status-closed { background: #F1F5F9; color: #475569; }
            .status-rejected { background: #FEE2E2; color: #991B1B; }

            .dpd-pill {
                padding: 2px 7px;
                border-radius: 8px;
                font-size: 11px;
                font-weight: 700;
            }
            .dpd-pill-zero { background: #DCFCE7; color: #166534; }
            .dpd-pill-low { background: #FEF3C7; color: #92400E; }
            .dpd-pill-high { background: #FEE2E2; color: #991B1B; }

            /* MODAL STYLES */
            .staff-modal-backdrop {
                display: none;
                position: fixed;
                z-index: 10000;
                left: 0;
                top: 0;
                width: 100%;
                height: 100%;
                background: rgba(15, 23, 42, 0.6);
                backdrop-filter: blur(2px);
                align-items: center;
                justify-content: center;
            }

            .staff-modal-card {
                background: #FFFFFF;
                border-radius: 12px;
                padding: 28px;
                width: 90%;
                max-width: 480px;
                box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1), 0 10px 10px -5px rgba(0,0,0,0.04);
                box-sizing: border-box;
                animation: slideUp 0.25s ease-out;
            }
        </style>

        <div class="staff-dashboard">

            <!-- Flash Notices -->
            <c:if test="${param.approved == 'true'}">
                <div class="alert alert-success" style="margin-bottom:20px;">
                    ✅ <strong>Application Approved:</strong> Loan application has been successfully sanctioned and activated. Amortization schedule has been generated.
                </div>
            </c:if>
            <c:if test="${param.rejected == 'true'}">
                <div class="alert alert-info" style="margin-bottom:20px;">
                    ℹ️ <strong>Application Rejected:</strong> Loan application status updated to REJECTED.
                </div>
            </c:if>
            <c:if test="${param.collectionRecorded == 'true'}">
                <div class="alert alert-success" style="margin-bottom:20px;">
                    ✅ <strong>Collection Recorded:</strong> Payment receipt generated, borrower balance updated, and installment schedule synchronized.
                </div>
            </c:if>

            <!-- Staff Banner -->
            <div class="staff-banner">
                <div>
                    <h1 class="staff-banner-title">
                        <c:choose>
                            <c:when test="${staffUser.role == 'ROLE_LOAN_OFFICER'}">🏛️ Loan Officer Operations Hub</c:when>
                            <c:when test="${staffUser.role == 'ROLE_COLLECTIONS_AGENT'}">💳 Collections Agent Operations Hub</c:when>
                            <c:when test="${staffUser.role == 'ROLE_COLLECTIONS_MANAGER'}">📊 Collections Management & Risk Portfolio</c:when>
                            <c:otherwise>🛡️ Central Staff Administration Portal</c:otherwise>
                        </c:choose>
                    </h1>
                    <div class="staff-banner-sub">
                        <span>Staff Member: <strong><c:out value="${staffUser.name}"/></strong></span>
                        <span class="role-pill"><c:out value="${staffUser.role}"/></span>
                        <c:if test="${not empty officerBranch}">
                            <span style="background:#EDF2F7; color:#2D3748; padding:3px 10px; border-radius:12px; font-weight:600; font-size:12px;">
                                🏢 Branch: <strong><c:out value="${officerBranch}"/></strong>
                            </span>
                        </c:if>
                    </div>
                </div>

                <!-- Quick Action Buttons -->
                <div style="display:flex; gap:10px; align-items:center; flex-wrap:wrap;">
                    <a href="/total-loans" class="btn btn-secondary" style="width:auto; padding:10px 18px; font-size:13px; text-decoration:none; display:inline-flex; align-items:center; gap:6px;" title="View all loans across authorized branches">
                        <span>🏛️ Total Loans (<c:out value="${totalLoansCount != null ? totalLoansCount : 0}"/>)</span>
                    </a>
                    <a href="/active-borrowers" class="btn btn-secondary" style="width:auto; padding:10px 18px; font-size:13px; text-decoration:none; display:inline-flex; align-items:center; gap:6px;" title="View distinct active borrowers directory">
                        <span>👥 Active Borrowers (<c:out value="${activeBorrowersCount != null ? activeBorrowersCount : 0}"/>)</span>
                    </a>
                    <a href="/loan-search" class="btn btn-secondary" style="width:auto; padding:10px 18px; font-size:13px; text-decoration:none; display:inline-flex; align-items:center; gap:6px;">
                        <span>🔍 Loan & Customer Search</span>
                    </a>
                </div>
            </div>

            <!-- ROLE-SPECIFIC SUMMARY CARDS -->
            <c:choose>

                <%-- 1. LOAN OFFICER SUMMARY CARDS --%>
                <c:when test="${staffUser.role == 'ROLE_LOAN_OFFICER'}">
                    <div class="metrics-grid">
                        <div class="metric-card clickable-card" id="cardOfficerActiveLoans"
                             onclick="window.location.href='/active-borrowers'"
                             onkeydown="if(event.key==='Enter'||event.key===' '){event.preventDefault();window.location.href='/active-borrowers';}"
                             role="button" tabindex="0" title="Click to view distinct active borrowers">
                            <div class="metric-label">
                                <span>Active Loans</span>
                                <span style="color:#059669;">📈</span>
                            </div>
                            <div class="metric-value"><c:out value="${activeLoansCount}"/></div>
                            <div class="metric-hint">Currently servicing in branch</div>
                        </div>

                        <div class="metric-card" style="border-top:3px solid #ED7D16;">
                            <div class="metric-label">
                                <span>Pending Applications</span>
                                <span style="color:#ED7D16;">📝</span>
                            </div>
                            <div class="metric-value" style="color:#ED7D16;"><c:out value="${pendingLoansCount}"/></div>
                            <div class="metric-hint">Require officer review & approval</div>
                        </div>

                        <div class="metric-card">
                            <div class="metric-label">
                                <span>Disbursed Volume</span>
                                <span style="color:#24446B;">🏛️</span>
                            </div>
                            <div class="metric-value">₹<fmt:formatNumber value="${totalPrincipal}" pattern="#,##0"/></div>
                            <div class="metric-hint">Cumulative sanctioned principal</div>
                        </div>

                        <div class="metric-card">
                            <div class="metric-label">
                                <span>Branch Outstanding</span>
                                <span style="color:#DC2626;">💼</span>
                            </div>
                            <div class="metric-value" style="color:#DC2626;">₹<fmt:formatNumber value="${totalRemaining}" pattern="#,##0"/></div>
                            <div class="metric-hint">Total principal receivable</div>
                        </div>
                    </div>
                </c:when>

                <%-- 2. COLLECTIONS AGENT SUMMARY CARDS --%>
                <c:when test="${staffUser.role == 'ROLE_COLLECTIONS_AGENT'}">
                    <div class="metrics-grid">
                        <div class="metric-card" style="border-top:3px solid #DC2626;">
                            <div class="metric-label">
                                <span>Overdue Accounts</span>
                                <span style="color:#DC2626;">⚠️</span>
                            </div>
                            <div class="metric-value" style="color:#DC2626;"><c:out value="${overdueLoansCount}"/></div>
                            <div class="metric-hint">Accounts requiring collection action</div>
                        </div>

                        <div class="metric-card" style="border-top:3px solid #B91C1C;">
                            <div class="metric-label">
                                <span>Delinquent Amount</span>
                                <span style="color:#B91C1C;">💸</span>
                            </div>
                            <div class="metric-value" style="color:#B91C1C;">₹<fmt:formatNumber value="${totalOverdueAmount}" pattern="#,##0.00"/></div>
                            <div class="metric-hint">Total overdue EMI balance</div>
                        </div>

                        <div class="metric-card">
                            <div class="metric-label">
                                <span>Critical DPD (>30 Days)</span>
                                <span style="color:#F59E0B;">🚨</span>
                            </div>
                            <div class="metric-value"><c:out value="${dpdSma1Count + dpdSma2Count + dpdNpaCount}"/></div>
                            <div class="metric-hint">Accounts at risk of NPA</div>
                        </div>

                        <div class="metric-card clickable-card" id="cardAgentTotalAccounts"
                             onclick="window.location.href='/total-loans'"
                             onkeydown="if(event.key==='Enter'||event.key===' '){event.preventDefault();window.location.href='/total-loans';}"
                             role="button" tabindex="0" title="Click to view all branch accounts">
                            <div class="metric-label">
                                <span>Total Branch Accounts</span>
                                <span style="color:#24446B;">🏢</span>
                            </div>
                            <div class="metric-value"><c:out value="${totalLoansCount}"/></div>
                            <div class="metric-hint">Assigned branch portfolio</div>
                        </div>
                    </div>
                </c:when>

                <%-- 3. COLLECTIONS MANAGER SUMMARY CARDS --%>
                <c:when test="${staffUser.role == 'ROLE_COLLECTIONS_MANAGER'}">
                    <div class="metrics-grid">
                        <div class="metric-card" style="border-top:3px solid #DC2626;">
                            <div class="metric-label">
                                <span>Portfolio At Risk (PAR)</span>
                                <span style="color:#DC2626;">📊</span>
                            </div>
                            <div class="metric-value" style="color:#DC2626;"><c:out value="${parRate}"/>%</div>
                            <div class="metric-hint">Overdue vs Total Outstanding</div>
                        </div>

                        <div class="metric-card">
                            <div class="metric-label">
                                <span>Total Delinquent Amount</span>
                                <span style="color:#B91C1C;">💸</span>
                            </div>
                            <div class="metric-value" style="color:#B91C1C;">₹<fmt:formatNumber value="${totalOverdueAmount}" pattern="#,##0.00"/></div>
                            <div class="metric-hint"><c:out value="${overdueLoansCount}"/> delinquent borrower accounts</div>
                        </div>

                        <div class="metric-card">
                            <div class="metric-label">
                                <span>Total Active Portfolio</span>
                                <span style="color:#059669;">💼</span>
                            </div>
                            <div class="metric-value">₹<fmt:formatNumber value="${totalRemaining}" pattern="#,##0"/></div>
                            <div class="metric-hint"><c:out value="${activeLoansCount}"/> servicing loans</div>
                        </div>

                        <div class="metric-card clickable-card" id="cardManagerTotalAccounts"
                             onclick="window.location.href='/total-loans'"
                             onkeydown="if(event.key==='Enter'||event.key===' '){event.preventDefault();window.location.href='/total-loans';}"
                             role="button" tabindex="0" title="Click to view all branch accounts">
                            <div class="metric-label">
                                <span>Total Accounts</span>
                                <span style="color:#24446B;">🏛️</span>
                            </div>
                            <div class="metric-value"><c:out value="${totalLoansCount}"/></div>
                            <div class="metric-hint">Total branch accounts monitored</div>
                        </div>
                    </div>

                    <!-- DPD Risk Classification Row -->
                    <div class="dpd-grid">
                        <div class="dpd-card current">
                            <div class="dpd-title">Standard (0 DPD)</div>
                            <div class="dpd-count"><c:out value="${dpdCurrentCount}"/></div>
                            <div class="dpd-amt">₹<fmt:formatNumber value="${dpdCurrentAmount}" pattern="#,##0"/></div>
                        </div>
                        <div class="dpd-card sma0">
                            <div class="dpd-title">SMA-0 (1-30 DPD)</div>
                            <div class="dpd-count"><c:out value="${dpdSma0Count}"/></div>
                            <div class="dpd-amt">₹<fmt:formatNumber value="${dpdSma0Amount}" pattern="#,##0"/></div>
                        </div>
                        <div class="dpd-card sma1">
                            <div class="dpd-title">SMA-1 (31-60 DPD)</div>
                            <div class="dpd-count"><c:out value="${dpdSma1Count}"/></div>
                            <div class="dpd-amt">₹<fmt:formatNumber value="${dpdSma1Amount}" pattern="#,##0"/></div>
                        </div>
                        <div class="dpd-card sma2">
                            <div class="dpd-title">SMA-2 (61-90 DPD)</div>
                            <div class="dpd-count"><c:out value="${dpdSma2Count}"/></div>
                            <div class="dpd-amt">₹<fmt:formatNumber value="${dpdSma2Amount}" pattern="#,##0"/></div>
                        </div>
                        <div class="dpd-card npa">
                            <div class="dpd-title" style="color:#991B1B;">NPA (90+ DPD)</div>
                            <div class="dpd-count" style="color:#991B1B;"><c:out value="${dpdNpaCount}"/></div>
                            <div class="dpd-amt" style="color:#991B1B;">₹<fmt:formatNumber value="${dpdNpaAmount}" pattern="#,##0"/></div>
                        </div>
                    </div>
                </c:when>

                <%-- 4. ADMIN SUMMARY CARDS --%>
                <c:otherwise>
                    <div class="metrics-grid">
                        <div class="metric-card clickable-card" id="cardTotalLoans"
                             onclick="window.location.href='/total-loans'"
                             onkeydown="if(event.key==='Enter'||event.key===' '){event.preventDefault();window.location.href='/total-loans';}"
                             role="button" tabindex="0" title="Click to view all loans">
                            <div class="metric-label">
                                <span>Total Loans</span>
                                <span style="color:#24446B;">🏛️</span>
                            </div>
                            <div class="metric-value"><c:out value="${totalLoansCount}"/></div>
                            <div class="metric-hint">Across authorized branches</div>
                        </div>
                        <div class="metric-card clickable-card" id="cardActiveBorrowers"
                             onclick="window.location.href='/active-borrowers'"
                             onkeydown="if(event.key==='Enter'||event.key===' '){event.preventDefault();window.location.href='/active-borrowers';}"
                             role="button" tabindex="0" title="Click to view distinct active borrowers directory">
                            <div class="metric-label">
                                <span>Active Borrowers</span>
                                <span style="color:#059669;">👥</span>
                            </div>
                            <div class="metric-value"><c:out value="${activeBorrowersCount != null ? activeBorrowersCount : activeLoansCount}"/></div>
                            <div class="metric-hint">Servicing loans</div>
                        </div>
                        <div class="metric-card" style="border-top:3px solid #ED7D16;">
                            <div class="metric-label">
                                <span>Pending Applications</span>
                                <span style="color:#ED7D16;">📝</span>
                            </div>
                            <div class="metric-value" style="color:#ED7D16;"><c:out value="${pendingLoansCount}"/></div>
                            <div class="metric-hint">Awaiting officer sanction</div>
                        </div>
                        <div class="metric-card">
                            <div class="metric-label">
                                <span>Total Outstanding</span>
                                <span style="color:#DC2626;">💼</span>
                            </div>
                            <div class="metric-value" style="color:#DC2626;">₹<fmt:formatNumber value="${totalRemaining}" pattern="#,##0"/></div>
                            <div class="metric-hint">Total portfolio receivable</div>
                        </div>
                    </div>
                </c:otherwise>

            </c:choose>

            <!-- ROLE SPECIFIC WORKFLOW SECTIONS -->

            <%-- SECTION A: PENDING LOAN APPLICATIONS (For Loan Officer & Admin) --%>
            <c:if test="${staffUser.role == 'ROLE_LOAN_OFFICER' || staffUser.role == 'ROLE_ADMIN'}">
                <div class="staff-panel">
                    <div class="staff-panel-header">
                        <h2 class="staff-panel-title">
                            <span>📝 Pending Loan Applications for Review</span>
                            <span class="status-badge status-pending"><c:out value="${pendingLoansCount}"/> Pending</span>
                        </h2>
                    </div>

                    <div class="table-responsive">
                        <table>
                            <thead>
                                <tr>
                                    <th>Application / Account</th>
                                    <th>Borrower Name</th>
                                    <th>Customer ID</th>
                                    <th>Sanction Amount</th>
                                    <th>Interest & Tenure</th>
                                    <th>Est. Monthly EMI</th>
                                    <th>Branch</th>
                                    <th>Status</th>
                                    <th style="text-align:center;">Decision Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="pl" items="${pendingLoans}">
                                    <tr>
                                        <td>
                                            <strong style="color:#2B6CB0; font-family:monospace; font-size:14px;">
                                                <c:out value="${pl.accountNumber}"/>
                                            </strong>
                                        </td>
                                        <td><strong><c:out value="${pl.user.name}"/></strong></td>
                                        <td><span style="font-family:monospace; color:#475569;"><c:out value="${pl.customerId}"/></span></td>
                                        <td><strong>₹<fmt:formatNumber value="${pl.amount}" pattern="#,##0.00"/></strong></td>
                                        <td><c:out value="${pl.interest}"/>% &bull; <c:out value="${pl.months}"/> mos</td>
                                        <td style="color:#2B6CB0; font-weight:600;">₹<fmt:formatNumber value="${pl.emi}" pattern="#,##0.00"/></td>
                                        <td>
                                            <span style="background:#EDF2F7; color:#475569; padding:2px 8px; border-radius:10px; font-size:11px; font-weight:600;">
                                                <c:out value="${pl.branch}"/>
                                            </span>
                                        </td>
                                        <td>
                                            <span class="status-badge status-pending"><c:out value="${pl.status}"/></span>
                                        </td>
                                        <td style="text-align:center;">
                                            <div class="action-btn-group">
                                                <!-- Single Profile Button -->
                                                <a href="/loan-details?loanId=${pl.loanId}" class="btn-profile" title="Open 360° Comprehensive Profile">
                                                    Profile
                                                </a>

                                                <!-- Approve Form -->
                                                <form action="/staff/loans/approve" method="post" style="display:inline; margin:0;">
                                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                                    <input type="hidden" name="loanId" value="${pl.loanId}"/>
                                                    <button type="submit" class="btn-approve" onclick="return confirm('Approve and sanction loan account ${pl.accountNumber}?')">
                                                        Approve
                                                    </button>
                                                </form>

                                                <!-- Reject Action -->
                                                <button type="button" class="btn-reject" onclick="openRejectModal('${pl.loanId}', '${pl.accountNumber}', '${pl.user.name}')">
                                                    Reject
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty pendingLoans}">
                                    <tr>
                                        <td colspan="9" style="text-align:center; padding:35px 20px; color:#64748B;">
                                            <div style="font-size:32px; margin-bottom:8px;">✅</div>
                                            <div style="font-size:15px; font-weight:600; color:#1E293B;">No Pending Loan Applications</div>
                                            <div style="font-size:13px; color:#94A3B8;">All applications submitted to your authorized branch have been reviewed.</div>
                                        </td>
                                    </tr>
                                </c:if>
                            </tbody>
                        </table>
                    </div>
                </div>
            </c:if>

            <%-- SECTION B: DELINQUENT & OVERDUE LOAN ACCOUNTS (For Collections Agent, Manager & Admin) --%>
            <c:if test="${staffUser.role == 'ROLE_COLLECTIONS_AGENT' || staffUser.role == 'ROLE_COLLECTIONS_MANAGER' || staffUser.role == 'ROLE_ADMIN'}">
                <div class="staff-panel">
                    <div class="staff-panel-header">
                        <h2 class="staff-panel-title">
                            <span>⚠️ Delinquent & Overdue Loan Accounts</span>
                            <span class="status-badge status-overdue"><c:out value="${overdueLoansCount}"/> Overdue</span>
                        </h2>
                    </div>

                    <div class="table-responsive">
                        <table>
                            <thead>
                                <tr>
                                    <th>Account No</th>
                                    <th>Borrower Name</th>
                                    <th>Mobile (Masked)</th>
                                    <th>Principal Sanctioned</th>
                                    <th>Outstanding Balance</th>
                                    <th>Overdue Amount</th>
                                    <th>DPD Bucket</th>
                                    <th>Branch</th>
                                    <th style="text-align:center;">Collection Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="dl" items="${delinquentLoans}">
                                    <tr>
                                        <td>
                                            <strong style="color:#2B6CB0; font-family:monospace; font-size:14px;">
                                                <c:out value="${dl.accountNumber}"/>
                                            </strong>
                                        </td>
                                        <td><strong><c:out value="${dl.user.name}"/></strong></td>
                                        <td><c:out value="${dl.maskedPhone}"/></td>
                                        <td>₹<fmt:formatNumber value="${dl.amount}" pattern="#,##0.00"/></td>
                                        <td>
                                            <strong style="color:#DC2626;">₹<fmt:formatNumber value="${dl.remaining}" pattern="#,##0.00"/></strong>
                                        </td>
                                        <td>
                                            <strong style="color:#991B1B; background:#FEE2E2; padding:3px 8px; border-radius:6px;">
                                                ₹<fmt:formatNumber value="${dl.overdueAmount > 0 ? dl.overdueAmount : dl.emi}" pattern="#,##0.00"/>
                                            </strong>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${dl.dpd > 90}">
                                                    <span class="dpd-pill dpd-pill-high"><c:out value="${dl.dpd}"/> DPD (NPA)</span>
                                                </c:when>
                                                <c:when test="${dl.dpd > 30}">
                                                    <span class="dpd-pill dpd-pill-high"><c:out value="${dl.dpd}"/> DPD</span>
                                                </c:when>
                                                <c:when test="${dl.dpd > 0}">
                                                    <span class="dpd-pill dpd-pill-low"><c:out value="${dl.dpd}"/> DPD</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="dpd-pill dpd-pill-zero">Current</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <span style="background:#EDF2F7; color:#475569; padding:2px 8px; border-radius:10px; font-size:11px; font-weight:600;">
                                                <c:out value="${dl.branch}"/>
                                            </span>
                                        </td>
                                        <td style="text-align:center;">
                                            <div class="action-btn-group">
                                                <!-- Single Profile Button -->
                                                <a href="/loan-details?loanId=${dl.loanId}" class="btn-profile" title="Open 360° Comprehensive Profile">
                                                    Profile
                                                </a>

                                                <!-- Record Collection Button (Collections Agent, Manager, Admin) -->
                                                <button type="button" class="btn-collect" 
                                                        onclick="openCollectionModal('${dl.loanId}', '${dl.accountNumber}', '${dl.user.name}', '${dl.remaining}', '${dl.overdueAmount > 0 ? dl.overdueAmount : dl.emi}')">
                                                    Record Collection
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty delinquentLoans}">
                                    <tr>
                                        <td colspan="9" style="text-align:center; padding:35px 20px; color:#64748B;">
                                            <div style="font-size:32px; margin-bottom:8px;">🎉</div>
                                            <div style="font-size:15px; font-weight:600; color:#1E293B;">Zero Delinquency</div>
                                            <div style="font-size:13px; color:#94A3B8;">All loan accounts in your authorized branch are currently up-to-date.</div>
                                        </td>
                                    </tr>
                                </c:if>
                            </tbody>
                        </table>
                    </div>
                </div>
            </c:if>

            <%-- SECTION C: RECENT BRANCH COLLECTIONS LOG (For Collections Agent, Manager & Admin) --%>
            <c:if test="${(staffUser.role == 'ROLE_COLLECTIONS_AGENT' || staffUser.role == 'ROLE_COLLECTIONS_MANAGER' || staffUser.role == 'ROLE_ADMIN') && not empty recentCollections}">
                <div class="staff-panel">
                    <div class="staff-panel-header">
                        <h2 class="staff-panel-title">
                            <span>💳 Recent Branch Collections Activity</span>
                        </h2>
                    </div>

                    <div class="table-responsive">
                        <table>
                            <thead>
                                <tr>
                                    <th>Transaction Ref</th>
                                    <th>Loan Account</th>
                                    <th>Payment Mode</th>
                                    <th>Amount Collected</th>
                                    <th>Date & Time</th>
                                    <th>Status</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="rc" items="${recentCollections}">
                                    <tr>
                                        <td><span style="font-family:monospace; color:#2B6CB0; font-weight:600;"><c:out value="${rc.transactionReference}"/></span></td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${not empty rc.loan}">
                                                    <a href="/loan-details?loanId=${rc.loan.loanId}" style="text-decoration:none; font-family:monospace; font-weight:700; color:#24446B;">
                                                        <c:out value="${rc.loan.accountNumber}"/>
                                                    </a>
                                                </c:when>
                                                <c:otherwise>
                                                    <span style="color:#94A3B8;">-</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <span style="background:#EDF2F7; padding:3px 8px; border-radius:6px; font-size:12px; font-weight:600;">
                                                <c:out value="${rc.paymentMode}"/>
                                            </span>
                                        </td>
                                        <td><strong style="color:#15803D;">₹<fmt:formatNumber value="${rc.amount}" pattern="#,##0.00"/></strong></td>
                                        <td><c:out value="${rc.paymentDate}"/></td>
                                        <td><span class="status-badge status-active"><c:out value="${rc.status}"/></span></td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </div>
            </c:if>

            <%-- SECTION D: AUTHORIZED BRANCH PORTFOLIO (Full View for All Staff Roles) --%>
            <div class="staff-panel">
                <div class="staff-panel-header">
                    <h2 class="staff-panel-title">
                        <span>🏛️ Authorized Branch Loan Portfolio</span>
                        <span style="font-size:13px; font-weight:normal; color:#64748B;">(<c:out value="${totalLoansCount}"/> accounts)</span>
                    </h2>
                </div>

                <div class="table-responsive">
                    <table>
                        <thead>
                            <tr>
                                <th>Account No</th>
                                <th>Borrower Name</th>
                                <th>Sanctioned (₹)</th>
                                <th>Interest & Tenure</th>
                                <th>Monthly EMI</th>
                                <th>Outstanding Balance</th>
                                <th>Repayment Status</th>
                                <th>DPD</th>
                                <th>Loan Status</th>
                                <th style="text-align:center;">Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="l" items="${loans}">
                                <tr>
                                    <td>
                                        <strong style="color:#2B6CB0; font-family:monospace; font-size:14px;">
                                            <c:out value="${l.accountNumber}"/>
                                        </strong>
                                    </td>
                                    <td><strong><c:out value="${l.user.name}"/></strong></td>
                                    <td>₹<fmt:formatNumber value="${l.amount}" pattern="#,##0.00"/></td>
                                    <td><c:out value="${l.interest}"/>% &bull; <c:out value="${l.months}"/> mos</td>
                                    <td style="color:#2B6CB0; font-weight:600;">₹<fmt:formatNumber value="${l.emi}" pattern="#,##0.00"/></td>
                                    <td>
                                        <strong style="color:${l.remaining > 0 ? '#DC2626' : '#15803D'};">
                                            ₹<fmt:formatNumber value="${l.remaining}" pattern="#,##0.00"/>
                                        </strong>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${l.repaymentStatus == 'OVERDUE'}">
                                                <span class="status-badge status-overdue">⚠️ Overdue</span>
                                            </c:when>
                                            <c:when test="${l.repaymentStatus == 'CURRENT'}">
                                                <span class="status-badge status-active">Current</span>
                                            </c:when>
                                            <c:when test="${l.repaymentStatus == 'CLOSED'}">
                                                <span class="status-badge status-closed">Closed</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="status-badge status-pending"><c:out value="${l.repaymentStatus}"/></span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${l.dpd > 0}">
                                                <span class="dpd-pill dpd-pill-high"><c:out value="${l.dpd}"/> DPD</span>
                                            </c:when>
                                            <c:when test="${l.status == 'CLOSED'}">
                                                <span style="color:#94A3B8; font-size:12px;">-</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="dpd-pill dpd-pill-zero">0 DPD</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${l.status == 'ACTIVE'}">
                                                <span class="status-badge status-active">Active</span>
                                            </c:when>
                                            <c:when test="${l.status == 'CLOSED'}">
                                                <span class="status-badge status-closed">Closed</span>
                                            </c:when>
                                            <c:when test="${l.status == 'REJECTED'}">
                                                <span class="status-badge status-rejected">Rejected</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="status-badge status-pending"><c:out value="${l.status}"/></span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td style="text-align:center;">
                                        <!-- Single Profile Button -->
                                        <a href="/loan-details?loanId=${l.loanId}" class="btn-profile" title="Open 360° Comprehensive Profile">
                                            Profile
                                        </a>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty loans}">
                                <tr>
                                    <td colspan="10" style="text-align:center; padding:40px 20px; color:#64748B;">
                                        <div style="font-size:32px; margin-bottom:8px;">📋</div>
                                        <div style="font-size:15px; font-weight:600; color:#1E293B;">No Loan Records Found</div>
                                        <div style="font-size:13px; color:#94A3B8;">There are currently no loan records assigned to your branch.</div>
                                    </td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </div>

        </div>

        <!-- REJECT LOAN MODAL -->
        <div id="rejectModal" class="staff-modal-backdrop">
            <div class="staff-modal-card">
                <h3 style="margin-top:0; color:#24446B;">Reject Loan Application</h3>
                <p style="color:#64748B; font-size:14px; margin-bottom:16px;">
                    Account: <strong id="rejectModalAccount"></strong> &bull; Borrower: <strong id="rejectModalBorrower"></strong>
                </p>
                <form action="/staff/loans/reject" method="post">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                    <input type="hidden" name="loanId" id="rejectModalLoanId"/>

                    <label style="font-weight:600; font-size:13px; color:#334155;">Rejection Reason <span style="color:#DC2626;">*</span></label>
                    <select name="reason" style="margin-top:6px; margin-bottom:16px;" required>
                        <option value="Credit score does not meet minimum bank threshold">Credit score does not meet minimum bank threshold</option>
                        <option value="Debt-to-income ratio exceeds permissible limits">Debt-to-income ratio exceeds permissible limits</option>
                        <option value="Incomplete or unverified borrower KYC documentation">Incomplete or unverified borrower KYC documentation</option>
                        <option value="Customer employment / income verification unconfirmed">Customer employment / income verification unconfirmed</option>
                        <option value="Application does not meet eligibility criteria">Other bank policy criteria not met</option>
                    </select>

                    <div style="display:flex; gap:10px; justify-content:flex-end;">
                        <button type="button" class="btn btn-secondary" style="width:auto; padding:8px 16px; font-size:13px;" onclick="closeRejectModal()">
                            Cancel
                        </button>
                        <button type="submit" class="btn btn-danger" style="width:auto; padding:8px 16px; font-size:13px;">
                            Confirm Rejection
                        </button>
                    </div>
                </form>
            </div>
        </div>

        <!-- RECORD COLLECTION MODAL -->
        <div id="collectionModal" class="staff-modal-backdrop">
            <div class="staff-modal-card">
                <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px;">
                    <h3 style="margin:0; color:#24446B;">Record Collection Payment</h3>
                    <button type="button" onclick="closeCollectionModal()" style="background:none; border:none; font-size:18px; cursor:pointer; color:#64748B;">&times;</button>
                </div>
                <p style="color:#64748B; font-size:13px; margin:0 0 16px 0;">
                    Account: <strong id="collectModalAccount" style="color:#2B6CB0;"></strong> &bull; 
                    Borrower: <strong id="collectModalBorrower"></strong>
                </p>

                <div style="background:#F8FAFC; border:1px solid #E2E8F0; border-radius:8px; padding:12px; margin-bottom:16px; display:flex; justify-content:space-between;">
                    <div>
                        <div style="font-size:11px; color:#64748B; text-transform:uppercase;">Outstanding Balance</div>
                        <div style="font-size:16px; font-weight:700; color:#DC2626;" id="collectModalBalance">₹0.00</div>
                    </div>
                    <div>
                        <div style="font-size:11px; color:#64748B; text-transform:uppercase;">Overdue EMI Due</div>
                        <div style="font-size:16px; font-weight:700; color:#991B1B;" id="collectModalOverdue">₹0.00</div>
                    </div>
                </div>

                <form action="/staff/collections/record" method="post">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                    <input type="hidden" name="loanId" id="collectModalLoanId"/>

                    <label style="font-weight:600; font-size:13px; color:#334155;">Collection Amount (₹) <span style="color:#DC2626;">*</span></label>
                    <input type="number" step="any" min="1" name="amount" id="collectModalAmount" required style="margin-top:6px; margin-bottom:12px;" />

                    <label style="font-weight:600; font-size:13px; color:#334155;">Payment Mode <span style="color:#DC2626;">*</span></label>
                    <select name="paymentMode" style="margin-top:6px; margin-bottom:12px;" required>
                        <option value="CASH">CASH (Physical Receipt Issued)</option>
                        <option value="UPI">UPI (QR / VPA Transfer)</option>
                        <option value="NEFT">NEFT / RTGS</option>
                        <option value="CHEQUE">Bank Cheque</option>
                        <option value="DD">Demand Draft</option>
                    </select>

                    <label style="font-weight:600; font-size:13px; color:#334155;">Receipt / Transaction Reference</label>
                    <input type="text" name="transactionReference" id="collectModalRef" placeholder="e.g. REC-2026-90812 or Bank Ref" maxlength="50" style="margin-top:6px; margin-bottom:18px;" />

                    <div style="display:flex; gap:10px; justify-content:flex-end;">
                        <button type="button" class="btn btn-secondary" style="width:auto; padding:8px 16px; font-size:13px;" onclick="closeCollectionModal()">
                            Cancel
                        </button>
                        <button type="submit" class="btn" style="width:auto; padding:8px 20px; font-size:13px; background-color:#ED7D16;">
                            Confirm & Record Collection
                        </button>
                    </div>
                </form>
            </div>
        </div>

        <script>
            function openRejectModal(loanId, accountNumber, borrowerName) {
                document.getElementById('rejectModalLoanId').value = loanId;
                document.getElementById('rejectModalAccount').innerText = accountNumber;
                document.getElementById('rejectModalBorrower').innerText = borrowerName;
                document.getElementById('rejectModal').style.display = 'flex';
            }

            function closeRejectModal() {
                document.getElementById('rejectModal').style.display = 'none';
            }

            function openCollectionModal(loanId, accountNumber, borrowerName, remaining, suggestedAmount) {
                document.getElementById('collectModalLoanId').value = loanId;
                document.getElementById('collectModalAccount').innerText = accountNumber;
                document.getElementById('collectModalBorrower').innerText = borrowerName;
                document.getElementById('collectModalBalance').innerText = '₹' + Number(remaining).toLocaleString('en-IN', {minimumFractionDigits: 2});
                document.getElementById('collectModalOverdue').innerText = '₹' + Number(suggestedAmount).toLocaleString('en-IN', {minimumFractionDigits: 2});
                document.getElementById('collectModalAmount').value = suggestedAmount > 0 ? suggestedAmount : remaining;
                document.getElementById('collectModalAmount').max = remaining;
                document.getElementById('collectModalRef').value = 'COL' + Date.now();
                document.getElementById('collectionModal').style.display = 'flex';
            }

            function closeCollectionModal() {
                document.getElementById('collectionModal').style.display = 'none';
            }

            // Close modals on clicking backdrop
            window.addEventListener('click', function(event) {
                const rejectModal = document.getElementById('rejectModal');
                const collectModal = document.getElementById('collectionModal');
                if (event.target === rejectModal) {
                    closeRejectModal();
                }
                if (event.target === collectModal) {
                    closeCollectionModal();
                }
            });
        </script>
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
                <div class="summary-card clickable-card"
                     onclick="var el = document.getElementById('customerLoanPortfolio'); if(el) el.scrollIntoView({behavior:'smooth'});"
                     onkeydown="if(event.key==='Enter'||event.key===' '){event.preventDefault(); var el = document.getElementById('customerLoanPortfolio'); if(el) el.scrollIntoView({behavior:'smooth'});}"
                     role="button" tabindex="0" title="Click to scroll to your loan portfolio">
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
                    <div class="dashboard-section-card" id="customerLoanPortfolio">
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