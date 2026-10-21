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
        <div class="card" style="max-width: 540px; width: 92%;">
            <h2 style="text-align:center; margin-top:0; color:#24446B;">Apply for a Loan</h2>
            <p style="text-align:center; color:#64748B; font-size:13px; margin-top:-10px; margin-bottom:20px;">
                Calculate your EMI and submit your loan application.
            </p>

            <form id="loanApplyForm" action="/applyLoan" method="post">
                <!-- CSRF Token -->
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

                <label for="loanAmountInput" style="font-weight:600; font-size:13px; color:#24446B;">
                    Loan Amount (₹) <span style="color:#DC2626;">*</span>
                </label>
                <input type="number"
                       id="loanAmountInput"
                       name="amount"
                       step="any"
                       min="${minLoanAmount != null ? minLoanAmount : 100}"
                       max="${maxLoanAmount != null ? maxLoanAmount : 10000000}"
                       placeholder="e.g. 50000"
                       required
                       style="margin-bottom:6px;">

                <!-- Range validation message alert box -->
                <div id="amountValidationMsg" style="display:none; font-size:12px; margin-top:2px; margin-bottom:12px; padding:8px 12px; border-radius:6px; font-weight:500;"></div>

                <div id="permittedRangeHint" style="font-size:11px; color:#64748B; margin-top:-4px; margin-bottom:14px;">
                    Permitted range: ₹<fmt:formatNumber value="${minLoanAmount != null ? minLoanAmount : 100}" type="number"/> – ₹<fmt:formatNumber value="${maxLoanAmount != null ? maxLoanAmount : 10000000}" type="number"/>
                </div>

                <!-- Dynamically Determined Loan Policy Terms (Configurable Bank Policy Slabs) -->
                <div style="background:#F8FAFC; border:1px solid #E2E8F0; border-radius:8px; padding:14px; margin-bottom:16px;">
                    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:10px; flex-wrap:wrap; gap:6px;">
                        <span style="font-size:12px; font-weight:700; color:#475569; text-transform:uppercase; letter-spacing:0.5px;">
                            🏛️ Dynamic Loan Terms (Bank Policy Slabs)
                        </span>
                        <span id="activeSlabBadge" style="font-size:11px; font-weight:600; padding:2px 8px; border-radius:12px; background:#E2E8F0; color:#475569;">
                            Enter amount
                        </span>
                    </div>

                    <div style="display:grid; grid-template-columns: 1fr 1fr; gap:10px;">
                        <div style="background:#FFFFFF; border:1px solid #CBD5E1; border-radius:6px; padding:10px;">
                            <div style="font-size:11px; color:#64748B; font-weight:600;">Annual Interest Rate</div>
                            <div id="displayInterestRate" style="font-size:17px; font-weight:700; color:#24446B; margin-top:2px;">
                                --% <span style="font-size:11px; font-weight:500; color:#059669;">(Fixed)</span>
                            </div>
                        </div>
                        <div style="background:#FFFFFF; border:1px solid #CBD5E1; border-radius:6px; padding:10px;">
                            <div style="font-size:11px; color:#64748B; font-weight:600;">Repayment Tenure</div>
                            <div id="displayTenureMonths" style="font-size:17px; font-weight:700; color:#24446B; margin-top:2px;">
                                -- Months
                            </div>
                        </div>
                    </div>

                    <!-- Reducing-Balance Monthly EMI Breakdown -->
                    <div style="margin-top:10px; background:#FFFFFF; border:1px solid #CBD5E1; border-radius:6px; padding:10px;">
                        <div style="display:flex; justify-content:space-between; align-items:center;">
                            <div>
                                <div style="font-size:11px; color:#64748B; font-weight:600;">Estimated Monthly EMI</div>
                                <div id="displayEmi" style="font-size:19px; font-weight:800; color:#ED7D16; margin-top:2px;">
                                    ₹ --
                                </div>
                            </div>
                            <div style="text-align:right;">
                                <div style="font-size:11px; color:#64748B; font-weight:600;">Total Repayment</div>
                                <div id="displayTotalRepayment" style="font-size:13px; font-weight:700; color:#24446B; margin-top:2px;">
                                    ₹ --
                                </div>
                            </div>
                        </div>
                    </div>

                    <div style="font-size:11px; color:#64748B; margin-top:8px; line-height:1.4;">
                        ℹ️ Interest rate and tenure are dynamically determined by bank policy slabs based on the loan amount entered. Reducing-balance EMI is calculated automatically.
                    </div>
                </div>

                <!-- Collapsible Bank Loan Amount Slabs Overview -->
                <c:if test="${not empty loanSlabs}">
                    <details style="margin-bottom:16px; font-size:12px; color:#475569; background:#F1F5F9; border-radius:6px; padding:8px 12px; border:1px solid #E2E8F0;">
                        <summary style="cursor:pointer; font-weight:600; color:#24446B;">
                            📋 View Bank Policy Rate & Tenure Slabs
                        </summary>
                        <div style="margin-top:8px; overflow-x:auto;">
                            <table style="width:100%; border-collapse:collapse; font-size:11px; text-align:left;">
                                <thead>
                                    <tr style="border-bottom:1px solid #CBD5E1; color:#475569;">
                                        <th style="padding:4px 6px;">Slab Range</th>
                                        <th style="padding:4px 6px;">Annual Rate</th>
                                        <th style="padding:4px 6px;">Tenure</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="slab" items="${loanSlabs}">
                                        <tr style="border-bottom:1px solid #E2E8F0;">
                                            <td style="padding:4px 6px;">
                                                ₹<fmt:formatNumber value="${slab.minAmount}" type="number"/> – ₹<fmt:formatNumber value="${slab.maxAmount}" type="number"/>
                                            </td>
                                            <td style="padding:4px 6px; font-weight:600; color:#059669;">
                                                <fmt:formatNumber value="${slab.interestRate}" type="number" minFractionDigits="1" maxFractionDigits="2"/>% p.a.
                                            </td>
                                            <td style="padding:4px 6px;">
                                                ${slab.tenureMonths} Months
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </details>
                </c:if>

                <button type="submit" id="submitLoanBtn" class="btn" style="margin-top:10px; background-color:#ED7D16;">
                    Submit Loan Application
                </button>
            </form>

            <div style="text-align:center; margin-top:20px;">
                <a href="/dashboard" style="font-size:13px; color:#64748B; text-decoration:none;">
                    Cancel & Back to Dashboard
                </a>
            </div>
        </div>

        <!-- Dynamic Loan Policy & EMI Calculation Script -->
        <script>
            (function() {
                // Bank policy loan slabs injected dynamically from server backend
                const loanSlabs = [
                    <c:forEach var="slab" items="${loanSlabs}" varStatus="status">
                    {
                        name: "${slab.slabName != null ? slab.slabName : ''}",
                        minAmount: ${slab.minAmount},
                        maxAmount: ${slab.maxAmount},
                        interestRate: ${slab.interestRate},
                        tenureMonths: ${slab.tenureMonths}
                    }${!status.last ? ',' : ''}
                    </c:forEach>
                ];

                const minLimit = ${minLoanAmount != null ? minLoanAmount : 100.0};
                const maxLimit = ${maxLoanAmount != null ? maxLoanAmount : 10000000.0};

                const amountInput = document.getElementById('loanAmountInput');
                const validationMsg = document.getElementById('amountValidationMsg');
                const displayRate = document.getElementById('displayInterestRate');
                const displayTenure = document.getElementById('displayTenureMonths');
                const displayEmi = document.getElementById('displayEmi');
                const displayTotal = document.getElementById('displayTotalRepayment');
                const activeSlabBadge = document.getElementById('activeSlabBadge');
                const submitBtn = document.getElementById('submitLoanBtn');
                const form = document.getElementById('loanApplyForm');

                function formatRupees(num) {
                    if (isNaN(num)) return '₹ --';
                    return '₹ ' + Math.round(num).toLocaleString('en-IN');
                }

                function formatCurrencyDecimal(num) {
                    return '₹' + Number(num).toLocaleString('en-IN', { maximumFractionDigits: 2 });
                }

                function showValidation(msg, isError) {
                    if (!validationMsg) return;
                    if (!msg) {
                        validationMsg.style.display = 'none';
                        return;
                    }
                    validationMsg.style.display = 'block';
                    validationMsg.innerHTML = msg;
                    if (isError) {
                        validationMsg.style.background = '#FEE2E2';
                        validationMsg.style.color = '#B91C1C';
                        validationMsg.style.border = '1px solid #FCA5A5';
                        if (amountInput) amountInput.style.borderColor = '#DC2626';
                    } else {
                        validationMsg.style.background = '#ECFDF5';
                        validationMsg.style.color = '#047857';
                        validationMsg.style.border = '1px solid #A7F3D0';
                        if (amountInput) amountInput.style.borderColor = '#059669';
                    }
                }

                function resetCalculatedTerms() {
                    displayRate.innerHTML = '--% <span style="font-size:11px; font-weight:500; color:#059669;">(Fixed)</span>';
                    displayTenure.textContent = '-- Months';
                    displayEmi.textContent = '₹ --';
                    displayTotal.textContent = '₹ --';
                    activeSlabBadge.textContent = 'Enter amount';
                    activeSlabBadge.style.background = '#E2E8F0';
                    activeSlabBadge.style.color = '#475569';
                }

                function findSlabForAmount(amount) {
                    // Search in sorted slabs
                    for (let i = 0; i < loanSlabs.length; i++) {
                        const s = loanSlabs[i];
                        if (amount >= (s.minAmount - 0.001) && amount <= (s.maxAmount + 0.001)) {
                            return s;
                        }
                    }
                    // Continuity check
                    for (let i = 0; i < loanSlabs.length; i++) {
                        const s = loanSlabs[i];
                        if (amount <= s.maxAmount) {
                            return s;
                        }
                    }
                    if (loanSlabs.length > 0) {
                        return loanSlabs[loanSlabs.length - 1];
                    }
                    return null;
                }

                function updateDynamicLoanTerms() {
                    if (!amountInput) return;
                    const rawVal = amountInput.value.trim();

                    if (rawVal === '') {
                        showValidation('', false);
                        resetCalculatedTerms();
                        if (amountInput) amountInput.style.borderColor = '';
                        submitBtn.disabled = false;
                        return;
                    }

                    const amount = parseFloat(rawVal);

                    if (isNaN(amount)) {
                        showValidation('⚠️ Please enter a valid numerical loan amount.', true);
                        resetCalculatedTerms();
                        submitBtn.disabled = true;
                        return;
                    }

                    if (amount < minLimit) {
                        showValidation('⚠️ Loan amount is below permitted minimum of ' + formatCurrencyDecimal(minLimit) + '.', true);
                        resetCalculatedTerms();
                        submitBtn.disabled = true;
                        return;
                    }

                    if (amount > maxLimit) {
                        showValidation('⚠️ Loan amount exceeds maximum permitted limit of ' + formatCurrencyDecimal(maxLimit) + ' (₹1 Crore).', true);
                        resetCalculatedTerms();
                        submitBtn.disabled = true;
                        return;
                    }

                    // Valid amount within permitted range!
                    const matchedSlab = findSlabForAmount(amount);
                    if (!matchedSlab) {
                        showValidation('⚠️ No applicable policy slab found for this amount.', true);
                        resetCalculatedTerms();
                        submitBtn.disabled = true;
                        return;
                    }

                    // Enable submit and show positive feedback
                    submitBtn.disabled = false;
                    showValidation('✓ Applicable Tier: <strong>' + matchedSlab.interestRate + '% p.a.</strong> for <strong>' + matchedSlab.tenureMonths + ' Months</strong>', false);

                    // Update Interest Rate and Tenure displays
                    displayRate.innerHTML = matchedSlab.interestRate + '% <span style="font-size:11px; font-weight:500; color:#059669;">(Fixed)</span>';
                    displayTenure.textContent = matchedSlab.tenureMonths + ' Months';

                    // Update Slab Badge
                    const slabLabel = matchedSlab.name ? matchedSlab.name : ('Slab ₹' + Math.round(matchedSlab.minAmount) + ' - ₹' + Math.round(matchedSlab.maxAmount));
                    activeSlabBadge.textContent = slabLabel;
                    activeSlabBadge.style.background = '#DCFCE7';
                    activeSlabBadge.style.color = '#15803D';

                    // Calculate Reducing-Balance EMI:
                    // r = monthly rate = (annualRate / 100) / 12
                    // n = months
                    // EMI = [P * r * (1 + r)^n] / [(1 + r)^n - 1]
                    const r = (matchedSlab.interestRate / 100) / 12;
                    const n = matchedSlab.tenureMonths;
                    let emi = 0;
                    if (r > 0 && n > 0) {
                        emi = (amount * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
                    }
                    const totalRepayment = emi * n;

                    displayEmi.textContent = formatRupees(emi);
                    displayTotal.textContent = formatRupees(totalRepayment);
                }

                // Attach reactive input listeners for instant without-refresh updates
                if (amountInput) {
                    amountInput.addEventListener('input', updateDynamicLoanTerms);
                    amountInput.addEventListener('keyup', updateDynamicLoanTerms);
                    amountInput.addEventListener('change', updateDynamicLoanTerms);

                    // Trigger immediately if input already has an initial value
                    if (amountInput.value) {
                        updateDynamicLoanTerms();
                    }
                }

                // Client-side form submit validation guard
                if (form) {
                    form.addEventListener('submit', function(e) {
                        const amount = parseFloat(amountInput.value);
                        if (isNaN(amount) || amount < minLimit || amount > maxLimit) {
                            e.preventDefault();
                            showValidation('⚠️ Please enter a valid loan amount between ' + formatCurrencyDecimal(minLimit) + ' and ' + formatCurrencyDecimal(maxLimit) + '.', true);
                            amountInput.focus();
                        }
                    });
                }
            })();
        </script>
    </c:otherwise>
</c:choose>