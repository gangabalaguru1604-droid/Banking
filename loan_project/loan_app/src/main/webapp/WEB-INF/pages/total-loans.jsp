<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="header.jsp" %>

<div class="dashboard-container" style="max-width: 1380px; margin: 24px auto; padding: 0 16px;">

    <!-- Breadcrumb / Header Navigation -->
    <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px; margin-bottom:20px;">
        <div>
            <div style="font-size:12px; color:#64748B; margin-bottom:4px;">
                <a href="/dashboard" style="color:#24446B; text-decoration:none; font-weight:600;">Operational Dashboard</a> &rsaquo; All Loans
            </div>
            <h1 style="margin:0; font-size:24px; color:#24446B; display:flex; align-items:center; gap:8px;">
                <span>🏛️ All Loans Portfolio</span>
                <span class="status-badge status-active" style="font-size:13px; font-weight:700;">
                    <c:out value="${totalLoansCount != null ? totalLoansCount : 0}"/> Accounts
                </span>
            </h1>
            <p style="margin:4px 0 0 0; color:#64748B; font-size:13px;">
                Comprehensive loan records across authorized branches &bull; Branch: <strong><c:out value="${officerBranch}"/></strong>
            </p>
        </div>
        <div style="display:flex; gap:10px;">
            <a href="/dashboard" class="btn btn-secondary" style="width:auto; padding:8px 16px; font-size:13px; text-decoration:none; display:inline-flex; align-items:center; gap:6px;">
                <span>&larr; Back to Dashboard</span>
            </a>
            <a href="/active-borrowers" class="btn" style="width:auto; padding:8px 16px; font-size:13px; text-decoration:none; background:#059669; color:#fff; display:inline-flex; align-items:center; gap:6px;">
                <span>👥 Active Borrowers Directory</span>
            </a>
        </div>
    </div>

    <!-- Filter and Search Toolbar -->
    <div class="staff-panel" style="margin-bottom:20px; padding:16px;">
        <div style="display:grid; grid-template-columns: 2fr 1fr 1fr auto auto; gap:12px; align-items:flex-end;">
            <div>
                <label for="loansSearchInput" style="font-size:12px; font-weight:600; color:#475569; display:block; margin-bottom:4px;">Search Loans</label>
                <input type="text" id="loansSearchInput" placeholder="Search borrower name, customer ID, account no, application ID, or branch..."
                       style="margin-bottom:0; font-size:13px; padding:8px 12px; width:100%; box-sizing:border-box;" oninput="applyLoanFilters()">
            </div>
            <div>
                <label for="branchFilterSelect" style="font-size:12px; font-weight:600; color:#475569; display:block; margin-bottom:4px;">Filter Branch</label>
                <select id="branchFilterSelect" style="margin-bottom:0; font-size:13px; padding:8px 12px; width:100%; box-sizing:border-box;" onchange="applyLoanFilters()">
                    <option value="">All Branches (${allBranches != null ? allBranches.size() : 0})</option>
                    <c:forEach var="br" items="${allBranches}">
                        <option value="${br}" ${selectedBranch == br ? 'selected' : ''}>${br}</option>
                    </c:forEach>
                </select>
            </div>
            <div>
                <label for="statusFilterSelect" style="font-size:12px; font-weight:600; color:#475569; display:block; margin-bottom:4px;">Filter Status</label>
                <select id="statusFilterSelect" style="margin-bottom:0; font-size:13px; padding:8px 12px; width:100%; box-sizing:border-box;" onchange="applyLoanFilters()">
                    <option value="">All Statuses</option>
                    <c:forEach var="st" items="${allStatuses}">
                        <option value="${st}" ${selectedStatus == st ? 'selected' : ''}>${st}</option>
                    </c:forEach>
                </select>
            </div>
            <div>
                <label for="pageSizeSelect" style="font-size:12px; font-weight:600; color:#475569; display:block; margin-bottom:4px;">Page Size</label>
                <select id="pageSizeSelect" style="margin-bottom:0; font-size:13px; padding:8px 12px; width:100px; box-sizing:border-box;" onchange="changeLoanPageSize()">
                    <option value="10">10 / page</option>
                    <option value="25" selected>25 / page</option>
                    <option value="50">50 / page</option>
                    <option value="100">100 / page</option>
                    <option value="ALL">All records</option>
                </select>
            </div>
            <div>
                <button type="button" class="btn btn-secondary" onclick="resetLoanFilters()" style="width:auto; padding:8px 14px; font-size:13px; height:38px;">
                    Reset
                </button>
            </div>
        </div>
    </div>

    <!-- Loans Table Panel -->
    <div class="staff-panel">
        <div class="staff-panel-header" style="display:flex; justify-content:space-between; align-items:center;">
            <h2 class="staff-panel-title" style="margin:0;">
                <span>📋 Loan Records</span>
                <span id="filteredCountBadge" style="font-size:12px; font-weight:normal; color:#64748B;">
                    (Showing <span id="showingCount">0</span> of <c:out value="${totalLoansCount != null ? totalLoansCount : 0}"/> accounts)
                </span>
            </h2>
        </div>

        <div class="table-responsive">
            <table id="totalLoansTable">
                <thead>
                    <tr>
                        <th style="min-width:130px;">Account Number</th>
                        <th style="min-width:90px;">App ID</th>
                        <th style="min-width:140px;">Borrower Name</th>
                        <th style="min-width:110px;">Customer ID</th>
                        <th style="min-width:110px;">Loan Type</th>
                        <th style="min-width:120px;">Sanctioned (₹)</th>
                        <th style="min-width:90px;">Rate</th>
                        <th style="min-width:80px;">Tenure</th>
                        <th style="min-width:110px;">Monthly EMI</th>
                        <th style="min-width:120px;">Outstanding Balance</th>
                        <th style="min-width:80px;">Branch</th>
                        <th style="min-width:100px;">Status</th>
                        <th style="min-width:110px;">Start Date</th>
                        <th style="min-width:80px; text-align:center;">Action</th>
                    </tr>
                </thead>
                <tbody id="loansTableBody">
                    <c:forEach var="l" items="${loans}">
                        <tr class="loan-record-row"
                            data-account="<c:out value="${l.accountNumber}"/>"
                            data-appid="<c:out value="${l.loanId}"/>"
                            data-borrower="<c:out value="${l.borrowerName}"/>"
                            data-customer="<c:out value="${l.customerId}"/>"
                            data-type="<c:out value="${l.productType}"/>"
                            data-branch="<c:out value="${l.branch}"/>"
                            data-status="<c:out value="${l.status}"/>"
                            data-repayment="<c:out value="${l.repaymentStatus}"/>">
                            <td>
                                <strong style="color:#2B6CB0; font-family:monospace; font-size:13px;">
                                    <c:out value="${l.accountNumber}"/>
                                </strong>
                            </td>
                            <td>
                                <span style="font-family:monospace; font-size:12px; color:#64748B;">
                                    #<c:out value="${l.loanId}"/>
                                </span>
                            </td>
                            <td>
                                <strong><c:out value="${l.borrowerName}"/></strong>
                            </td>
                            <td>
                                <span style="font-family:monospace; font-size:12px; color:#475569;">
                                    <c:out value="${l.customerId}"/>
                                </span>
                            </td>
                            <td>
                                <span style="font-size:12px; color:#475569;">
                                    <c:out value="${l.productType != null ? l.productType : 'Personal Loan'}"/>
                                </span>
                            </td>
                            <td>
                                <strong>₹<fmt:formatNumber value="${l.amount}" pattern="#,##0.00"/></strong>
                            </td>
                            <td>
                                <span style="color:#059669; font-weight:600;"><c:out value="${l.interest}"/>%</span>
                            </td>
                            <td>
                                <c:out value="${l.months}"/> mos
                            </td>
                            <td style="color:#2B6CB0; font-weight:600;">
                                ₹<fmt:formatNumber value="${l.emi}" pattern="#,##0.00"/>
                            </td>
                            <td>
                                <strong style="color:${l.remaining > 0 ? '#DC2626' : '#15803D'};">
                                    ₹<fmt:formatNumber value="${l.remaining}" pattern="#,##0.00"/>
                                </strong>
                            </td>
                            <td>
                                <span style="background:#F1F5F9; color:#334155; padding:2px 6px; border-radius:4px; font-size:11px; font-weight:600;">
                                    <c:out value="${l.branch}"/>
                                </span>
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
                            <td>
                                <span style="font-size:12px; color:#64748B;">
                                    <c:out value="${l.startDate != null ? l.startDate : l.createdAt}"/>
                                </span>
                            </td>
                            <td style="text-align:center;">
                                <a href="/loan-details?loanId=${l.loanId}" class="btn-profile" title="Open 360° Comprehensive Profile">
                                    Profile
                                </a>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty loans}">
                        <tr id="noLoansRow">
                            <td colspan="14" style="text-align:center; padding:48px 20px; color:#64748B;">
                                <div style="font-size:36px; margin-bottom:8px;">📋</div>
                                <div style="font-size:16px; font-weight:600; color:#1E293B;">No Loan Records Found</div>
                                <div style="font-size:13px; color:#94A3B8;">There are currently no loans on record for your authorized branch portfolio.</div>
                            </td>
                        </tr>
                    </c:if>
                    <tr id="emptyFilterRow" style="display:none;">
                        <td colspan="14" style="text-align:center; padding:40px 20px; color:#64748B;">
                            <div style="font-size:32px; margin-bottom:8px;">🔍</div>
                            <div style="font-size:15px; font-weight:600; color:#1E293B;">No Matching Loans Found</div>
                            <div style="font-size:13px; color:#94A3B8;">No records match your search or filter criteria. Try adjusting the query or clearing filters.</div>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <!-- Pagination Bar -->
        <div id="loansPaginationControls" style="display:flex; justify-content:space-between; align-items:center; padding:12px 16px; border-top:1px solid #E2E8F0; flex-wrap:wrap; gap:8px;">
            <div id="paginationInfo" style="font-size:13px; color:#64748B;">
                Page <span id="currentPageNum">1</span> of <span id="totalPageNum">1</span>
            </div>
            <div id="paginationButtons" style="display:flex; gap:6px;">
                <button type="button" id="prevPageBtn" class="btn btn-secondary" style="padding:6px 12px; font-size:12px;" onclick="goToLoanPage(currentLoanPage - 1)">Previous</button>
                <div id="pageNumberPills" style="display:flex; gap:4px;"></div>
                <button type="button" id="nextPageBtn" class="btn btn-secondary" style="padding:6px 12px; font-size:12px;" onclick="goToLoanPage(currentLoanPage + 1)">Next</button>
            </div>
        </div>
    </div>
</div>

<script>
    let currentLoanPage = 1;
    let loanPageSize = 25;
    let visibleLoanRows = [];

    function initLoanTable() {
        const rows = Array.from(document.querySelectorAll('#loansTableBody tr.loan-record-row'));
        visibleLoanRows = rows;
        applyLoanFilters();
    }

    function applyLoanFilters() {
        const searchInput = (document.getElementById('loansSearchInput')?.value || '').toLowerCase().trim();
        const branchFilter = document.getElementById('branchFilterSelect')?.value || '';
        const statusFilter = (document.getElementById('statusFilterSelect')?.value || '').toLowerCase();
        const allRows = Array.from(document.querySelectorAll('#loansTableBody tr.loan-record-row'));

        visibleLoanRows = allRows.filter(row => {
            const acc = (row.dataset.account || '').toLowerCase();
            const appid = (row.dataset.appid || '').toLowerCase();
            const borrower = (row.dataset.borrower || '').toLowerCase();
            const cust = (row.dataset.customer || '').toLowerCase();
            const type = (row.dataset.type || '').toLowerCase();
            const branch = row.dataset.branch || '';
            const status = (row.dataset.status || '').toLowerCase();

            const matchesSearch = !searchInput ||
                acc.includes(searchInput) ||
                appid.includes(searchInput) ||
                borrower.includes(searchInput) ||
                cust.includes(searchInput) ||
                type.includes(searchInput) ||
                branch.toLowerCase().includes(searchInput);

            const matchesBranch = !branchFilter || branch === branchFilter;
            const matchesStatus = !statusFilter || status === statusFilter;

            return matchesSearch && matchesBranch && matchesStatus;
        });

        currentLoanPage = 1;
        renderLoanPagination();
    }

    function changeLoanPageSize() {
        const val = document.getElementById('pageSizeSelect')?.value;
        loanPageSize = val === 'ALL' ? 999999 : parseInt(val, 10);
        currentLoanPage = 1;
        renderLoanPagination();
    }

    function resetLoanFilters() {
        if (document.getElementById('loansSearchInput')) document.getElementById('loansSearchInput').value = '';
        if (document.getElementById('branchFilterSelect')) document.getElementById('branchFilterSelect').value = '';
        if (document.getElementById('statusFilterSelect')) document.getElementById('statusFilterSelect').value = '';
        applyLoanFilters();
    }

    function goToLoanPage(page) {
        const totalPages = Math.max(1, Math.ceil(visibleLoanRows.length / loanPageSize));
        if (page < 1 || page > totalPages) return;
        currentLoanPage = page;
        renderLoanPagination();
    }

    function renderLoanPagination() {
        const total = visibleLoanRows.length;
        const totalPages = Math.max(1, Math.ceil(total / loanPageSize));
        if (currentLoanPage > totalPages) currentLoanPage = totalPages;

        const startIdx = (currentLoanPage - 1) * loanPageSize;
        const endIdx = Math.min(startIdx + loanPageSize, total);

        const allRows = Array.from(document.querySelectorAll('#loansTableBody tr.loan-record-row'));
        allRows.forEach(row => row.style.display = 'none');

        visibleLoanRows.forEach((row, idx) => {
            if (idx >= startIdx && idx < endIdx) {
                row.style.display = '';
            } else {
                row.style.display = 'none';
            }
        });

        const emptyRow = document.getElementById('emptyFilterRow');
        if (emptyRow) {
            emptyRow.style.display = (total === 0 && allRows.length > 0) ? '' : 'none';
        }

        document.getElementById('showingCount').textContent = total;
        document.getElementById('currentPageNum').textContent = currentLoanPage;
        document.getElementById('totalPageNum').textContent = totalPages;

        const prevBtn = document.getElementById('prevPageBtn');
        const nextBtn = document.getElementById('nextPageBtn');
        if (prevBtn) prevBtn.disabled = (currentLoanPage <= 1);
        if (nextBtn) nextBtn.disabled = (currentLoanPage >= totalPages);

        const pills = document.getElementById('pageNumberPills');
        if (pills) {
            pills.innerHTML = '';
            for (let p = 1; p <= totalPages; p++) {
                if (totalPages > 7 && Math.abs(p - currentLoanPage) > 2 && p !== 1 && p !== totalPages) {
                    continue;
                }
                const btn = document.createElement('button');
                btn.type = 'button';
                btn.className = (p === currentLoanPage) ? 'btn' : 'btn btn-secondary';
                btn.style.padding = '4px 8px';
                btn.style.fontSize = '11px';
                btn.style.minWidth = '28px';
                btn.textContent = p;
                btn.onclick = () => goToLoanPage(p);
                pills.appendChild(btn);
            }
        }
    }

    document.addEventListener('DOMContentLoaded', initLoanTable);
</script>

<style>
    .btn-profile {
        background-color: #24446B;
        color: #FFFFFF !important;
        padding: 5px 12px;
        font-size: 12px;
        font-weight: 600;
        border-radius: 4px;
        text-decoration: none;
        display: inline-block;
        transition: background-color 0.15s ease;
    }
    .btn-profile:hover {
        background-color: #1A365D;
    }
</style>
