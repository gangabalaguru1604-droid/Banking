<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="header.jsp" %>

<div class="dashboard-container" style="max-width: 1380px; margin: 24px auto; padding: 0 16px;">

    <!-- Breadcrumb / Header Navigation -->
    <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px; margin-bottom:20px;">
        <div>
            <div style="font-size:12px; color:#64748B; margin-bottom:4px;">
                <a href="/dashboard" style="color:#24446B; text-decoration:none; font-weight:600;">Operational Dashboard</a> &rsaquo; Active Borrowers Directory
            </div>
            <h1 style="margin:0; font-size:24px; color:#24446B; display:flex; align-items:center; gap:8px;">
                <span>👥 Active Borrowers Directory</span>
                <span class="status-badge status-active" style="font-size:13px; font-weight:700;">
                    <c:out value="${activeBorrowersCount != null ? activeBorrowersCount : 0}"/> Distinct Borrowers
                </span>
            </h1>
            <p style="margin:4px 0 0 0; color:#64748B; font-size:13px;">
                Borrowers currently servicing active loans across authorized branches &bull; Branch: <strong><c:out value="${officerBranch}"/></strong>
            </p>
        </div>
        <div style="display:flex; gap:10px;">
            <a href="/dashboard" class="btn btn-secondary" style="width:auto; padding:8px 16px; font-size:13px; text-decoration:none; display:inline-flex; align-items:center; gap:6px;">
                <span>&larr; Back to Dashboard</span>
            </a>
            <a href="/all-loans" class="btn" style="width:auto; padding:8px 16px; font-size:13px; text-decoration:none; background:#24446B; color:#fff; display:inline-flex; align-items:center; gap:6px;">
                <span>🏛️ All Loans Portfolio</span>
            </a>
        </div>
    </div>

    <!-- Filter and Search Toolbar -->
    <div class="staff-panel" style="margin-bottom:20px; padding:16px;">
        <div style="display:grid; grid-template-columns: 2fr 1fr auto auto; gap:12px; align-items:flex-end;">
            <div>
                <label for="borrowersSearchInput" style="font-size:12px; font-weight:600; color:#475569; display:block; margin-bottom:4px;">Search Borrowers</label>
                <input type="text" id="borrowersSearchInput" placeholder="Search customer ID, borrower name, or loan account number..."
                       style="margin-bottom:0; font-size:13px; padding:8px 12px; width:100%; box-sizing:border-box;" oninput="applyBorrowerFilters()">
            </div>
            <div>
                <label for="borrowerBranchSelect" style="font-size:12px; font-weight:600; color:#475569; display:block; margin-bottom:4px;">Filter Branch</label>
                <select id="borrowerBranchSelect" style="margin-bottom:0; font-size:13px; padding:8px 12px; width:100%; box-sizing:border-box;" onchange="applyBorrowerFilters()">
                    <option value="">All Branches (${allBranches != null ? allBranches.size() : 0})</option>
                    <c:forEach var="br" items="${allBranches}">
                        <option value="${br}" ${selectedBranch == br ? 'selected' : ''}>${br}</option>
                    </c:forEach>
                </select>
            </div>
            <div>
                <label for="borrowerPageSize" style="font-size:12px; font-weight:600; color:#475569; display:block; margin-bottom:4px;">Page Size</label>
                <select id="borrowerPageSize" style="margin-bottom:0; font-size:13px; padding:8px 12px; width:100px; box-sizing:border-box;" onchange="changeBorrowerPageSize()">
                    <option value="10">10 / page</option>
                    <option value="25" selected>25 / page</option>
                    <option value="50">50 / page</option>
                    <option value="ALL">All records</option>
                </select>
            </div>
            <div>
                <button type="button" class="btn btn-secondary" onclick="resetBorrowerFilters()" style="width:auto; padding:8px 14px; font-size:13px; height:38px;">
                    Reset
                </button>
            </div>
        </div>
    </div>

    <!-- Active Borrowers Table Panel -->
    <div class="staff-panel">
        <div class="staff-panel-header" style="display:flex; justify-content:space-between; align-items:center;">
            <h2 class="staff-panel-title" style="margin:0;">
                <span>👥 Distinct Active Borrowers</span>
                <span id="filteredBorrowersBadge" style="font-size:12px; font-weight:normal; color:#64748B;">
                    (Showing <span id="showingBorrowersCount">0</span> of <c:out value="${activeBorrowersCount != null ? activeBorrowersCount : 0}"/> borrowers)
                </span>
            </h2>
        </div>

        <div class="table-responsive">
            <table id="activeBorrowersTable">
                <thead>
                    <tr>
                        <th style="min-width:110px;">Customer ID</th>
                        <th style="min-width:150px;">Borrower Name</th>
                        <th style="min-width:160px;">Active Loan Account(s)</th>
                        <th style="min-width:90px; text-align:center;">Active Loans</th>
                        <th style="min-width:130px;">Total Sanctioned (₹)</th>
                        <th style="min-width:130px;">Total Outstanding</th>
                        <th style="min-width:80px;">Branch</th>
                        <th style="min-width:180px;">Contact Information</th>
                        <th style="min-width:90px;">Loan Status</th>
                        <th style="min-width:80px; text-align:center;">Action</th>
                    </tr>
                </thead>
                <tbody id="borrowersTableBody">
                    <c:forEach var="b" items="${activeBorrowers}">
                        <tr class="borrower-record-row"
                            data-customer="<c:out value="${b.customerId}"/>"
                            data-borrower="<c:out value="${b.borrowerName}"/>"
                            data-accounts="<c:forEach var="acc" items="${b.accountNumbers}">${acc} </c:forEach>"
                            data-branch="<c:out value="${b.branch}"/>">
                            <td>
                                <strong style="font-family:monospace; font-size:13px; color:#24446B;">
                                    <c:out value="${b.customerId}"/>
                                </strong>
                            </td>
                            <td>
                                <strong><c:out value="${b.borrowerName}"/></strong>
                            </td>
                            <td>
                                <div style="display:flex; flex-wrap:wrap; gap:4px;">
                                    <c:forEach var="acc" items="${b.accountNumbers}">
                                        <a href="/loan-details?loanId=${b.primaryLoanId}" style="text-decoration:none;">
                                            <span style="font-family:monospace; font-size:11px; background:#EFF6FF; color:#1D4ED8; border:1px solid #BFDBFE; padding:2px 6px; border-radius:4px; font-weight:600;">
                                                <c:out value="${acc}"/>
                                            </span>
                                        </a>
                                    </c:forEach>
                                </div>
                            </td>
                            <td style="text-align:center;">
                                <span style="background:#ECFDF5; color:#065F46; border:1px solid #A7F3D0; font-weight:700; font-size:12px; padding:2px 8px; border-radius:12px;">
                                    <c:out value="${b.activeLoanCount}"/>
                                </span>
                            </td>
                            <td>
                                <strong>₹<fmt:formatNumber value="${b.totalSanctionedAmount}" pattern="#,##0.00"/></strong>
                            </td>
                            <td>
                                <strong style="color:${b.totalOutstandingBalance > 0 ? '#DC2626' : '#15803D'};">
                                    ₹<fmt:formatNumber value="${b.totalOutstandingBalance}" pattern="#,##0.00"/>
                                </strong>
                            </td>
                            <td>
                                <span style="background:#F1F5F9; color:#334155; padding:2px 6px; border-radius:4px; font-size:11px; font-weight:600;">
                                    <c:out value="${b.branch}"/>
                                </span>
                            </td>
                            <td>
                                <div style="font-size:12px; color:#334155;">
                                    <c:out value="${b.contactInfo}"/>
                                </div>
                            </td>
                            <td>
                                <span class="status-badge status-active">Active</span>
                            </td>
                            <td style="text-align:center;">
                                <a href="/loan-details?loanId=${b.primaryLoanId}" class="btn-profile" title="Open 360° Comprehensive Profile">
                                    Profile
                                </a>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty activeBorrowers}">
                        <tr id="noBorrowersRow">
                            <td colspan="10" style="text-align:center; padding:48px 20px; color:#64748B;">
                                <div style="font-size:36px; margin-bottom:8px;">👥</div>
                                <div style="font-size:16px; font-weight:600; color:#1E293B;">No Active Borrowers Found</div>
                                <div style="font-size:13px; color:#94A3B8;">There are currently no borrowers servicing active loans in your authorized branch portfolio.</div>
                            </td>
                        </tr>
                    </c:if>
                    <tr id="emptyBorrowersFilterRow" style="display:none;">
                        <td colspan="10" style="text-align:center; padding:40px 20px; color:#64748B;">
                            <div style="font-size:32px; margin-bottom:8px;">🔍</div>
                            <div style="font-size:15px; font-weight:600; color:#1E293B;">No Matching Borrowers Found</div>
                            <div style="font-size:13px; color:#94A3B8;">No records match your search or branch filter criteria.</div>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <!-- Pagination Bar -->
        <div id="borrowersPaginationControls" style="display:flex; justify-content:space-between; align-items:center; padding:12px 16px; border-top:1px solid #E2E8F0; flex-wrap:wrap; gap:8px;">
            <div style="font-size:13px; color:#64748B;">
                Page <span id="currentBorrowerPageNum">1</span> of <span id="totalBorrowerPageNum">1</span>
            </div>
            <div style="display:flex; gap:6px;">
                <button type="button" id="prevBorrowerBtn" class="btn btn-secondary" style="padding:6px 12px; font-size:12px;" onclick="goToBorrowerPage(currentBorrowerPage - 1)">Previous</button>
                <div id="borrowerPagePills" style="display:flex; gap:4px;"></div>
                <button type="button" id="nextBorrowerBtn" class="btn btn-secondary" style="padding:6px 12px; font-size:12px;" onclick="goToBorrowerPage(currentBorrowerPage + 1)">Next</button>
            </div>
        </div>
    </div>
</div>

<script>
    let currentBorrowerPage = 1;
    let borrowerPageSize = 25;
    let visibleBorrowerRows = [];

    function initBorrowersTable() {
        const rows = Array.from(document.querySelectorAll('#borrowersTableBody tr.borrower-record-row'));
        visibleBorrowerRows = rows;
        applyBorrowerFilters();
    }

    function applyBorrowerFilters() {
        const searchInput = (document.getElementById('borrowersSearchInput')?.value || '').toLowerCase().trim();
        const branchFilter = document.getElementById('borrowerBranchSelect')?.value || '';
        const allRows = Array.from(document.querySelectorAll('#borrowersTableBody tr.borrower-record-row'));

        visibleBorrowerRows = allRows.filter(row => {
            const cust = (row.dataset.customer || '').toLowerCase();
            const borrower = (row.dataset.borrower || '').toLowerCase();
            const accounts = (row.dataset.accounts || '').toLowerCase();
            const branch = row.dataset.branch || '';

            const matchesSearch = !searchInput ||
                cust.includes(searchInput) ||
                borrower.includes(searchInput) ||
                accounts.includes(searchInput);

            const matchesBranch = !branchFilter || branch === branchFilter;

            return matchesSearch && matchesBranch;
        });

        currentBorrowerPage = 1;
        renderBorrowerPagination();
    }

    function changeBorrowerPageSize() {
        const val = document.getElementById('borrowerPageSize')?.value;
        borrowerPageSize = val === 'ALL' ? 999999 : parseInt(val, 10);
        currentBorrowerPage = 1;
        renderBorrowerPagination();
    }

    function resetBorrowerFilters() {
        if (document.getElementById('borrowersSearchInput')) document.getElementById('borrowersSearchInput').value = '';
        if (document.getElementById('borrowerBranchSelect')) document.getElementById('borrowerBranchSelect').value = '';
        applyBorrowerFilters();
    }

    function goToBorrowerPage(page) {
        const totalPages = Math.max(1, Math.ceil(visibleBorrowerRows.length / borrowerPageSize));
        if (page < 1 || page > totalPages) return;
        currentBorrowerPage = page;
        renderBorrowerPagination();
    }

    function renderBorrowerPagination() {
        const total = visibleBorrowerRows.length;
        const totalPages = Math.max(1, Math.ceil(total / borrowerPageSize));
        if (currentBorrowerPage > totalPages) currentBorrowerPage = totalPages;

        const startIdx = (currentBorrowerPage - 1) * borrowerPageSize;
        const endIdx = Math.min(startIdx + borrowerPageSize, total);

        const allRows = Array.from(document.querySelectorAll('#borrowersTableBody tr.borrower-record-row'));
        allRows.forEach(row => row.style.display = 'none');

        visibleBorrowerRows.forEach((row, idx) => {
            if (idx >= startIdx && idx < endIdx) {
                row.style.display = '';
            } else {
                row.style.display = 'none';
            }
        });

        const emptyRow = document.getElementById('emptyBorrowersFilterRow');
        if (emptyRow) {
            emptyRow.style.display = (total === 0 && allRows.length > 0) ? '' : 'none';
        }

        document.getElementById('showingBorrowersCount').textContent = total;
        document.getElementById('currentBorrowerPageNum').textContent = currentBorrowerPage;
        document.getElementById('totalBorrowerPageNum').textContent = totalPages;

        const prevBtn = document.getElementById('prevBorrowerBtn');
        const nextBtn = document.getElementById('nextBorrowerBtn');
        if (prevBtn) prevBtn.disabled = (currentBorrowerPage <= 1);
        if (nextBtn) nextBtn.disabled = (currentBorrowerPage >= totalPages);

        const pills = document.getElementById('borrowerPagePills');
        if (pills) {
            pills.innerHTML = '';
            for (let p = 1; p <= totalPages; p++) {
                if (totalPages > 7 && Math.abs(p - currentBorrowerPage) > 2 && p !== 1 && p !== totalPages) {
                    continue;
                }
                const btn = document.createElement('button');
                btn.type = 'button';
                btn.className = (p === currentBorrowerPage) ? 'btn' : 'btn btn-secondary';
                btn.style.padding = '4px 8px';
                btn.style.fontSize = '11px';
                btn.style.minWidth = '28px';
                btn.textContent = p;
                btn.onclick = () => goToBorrowerPage(p);
                pills.appendChild(btn);
            }
        }
    }

    document.addEventListener('DOMContentLoaded', initBorrowersTable);
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
