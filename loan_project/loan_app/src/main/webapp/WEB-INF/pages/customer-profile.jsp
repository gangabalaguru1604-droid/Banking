<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ include file="header.jsp" %>

<div class="wide-card">
    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; border-bottom:2px solid #edf2f7; padding-bottom:15px;">
        <div>
            <div style="display:flex; align-items:center; gap:10px;">
                <h2 style="margin:0; color:#2c3e50;">👤 Customer Profile & Loan Portfolio</h2>
                <span style="background:#ebf8ff; color:#2b6cb0; padding:4px 10px; border-radius:12px; font-size:12px; font-weight:700; font-family:monospace;">
                    <c:out value="${profile.customerId}"/>
                </span>
            </div>
            <p style="color:#718096; font-size:14px; margin:4px 0 0 0;">
                Comprehensive verified KYC and active loan accounts for customer.
            </p>
        </div>
        <div style="display:flex; align-items:center; gap:10px;">
            <span style="background:#edf2f7; color:#2d3748; padding:6px 14px; border-radius:20px; font-size:13px; font-weight:600; border:1px solid #cbd5e0;">
                🏢 Branch: <strong><c:out value="${profile.branch}"/></strong>
            </span>
            <a href="/loan-details?loanId=${profile.primaryLoanId}" class="btn btn-primary" style="padding:8px 16px; font-size:13px; width:auto; text-decoration:none; background:#4f46e5; color:#ffffff; font-weight:600; border-radius:6px;">
                🎯 360° Account View
            </a>
            <a href="/loan-search" class="btn btn-secondary" style="padding:8px 16px; font-size:13px; width:auto; text-decoration:none;">
                &larr; Back to Search
            </a>
        </div>
    </div>

    <!-- Security & Masking Notice -->
    <div class="alert alert-info" style="display:flex; align-items:center; justify-content:space-between;">
        <div>
            <strong>🛡️ Data Privacy Notice:</strong> In compliance with banking regulations, sensitive personal data (Aadhaar, PAN, Mobile, Email) is masked by default. 
            All reveal requests require documented business justification and are recorded in the immutable audit log.
        </div>
        <span style="background:#bee3f8; color:#2b6cb0; padding:3px 8px; border-radius:10px; font-size:11px; font-weight:700;">
            STRICT AUDIT
        </span>
    </div>

    <!-- Contact Detail Update Required Notice -->
    <c:if test="${profile.contactDetailUpdateRequired}">
        <div style="background:#fffbeb; border:1px solid #fde68a; color:#92400e; padding:12px 18px; border-radius:8px; margin-bottom:20px; font-size:14px; display:flex; align-items:center; gap:10px;">
            <span style="font-size:18px;">⚠️</span>
            <div>
                <strong>Contact Detail Update Required:</strong> Customer communication delivery failed or email bounced. Please verify and update borrower email and mobile number during outreach.
            </div>
        </div>
    </c:if>

    <!-- Customer Details & KYC Card -->
    <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap:20px; margin-bottom:30px;">
        
        <!-- Primary Demographics -->
        <div style="background:#ffffff; border:1px solid #e2e8f0; border-radius:8px; padding:18px; box-shadow:0 2px 5px rgba(0,0,0,0.03);">
            <div style="font-size:12px; font-weight:700; color:#a0aec0; text-transform:uppercase; letter-spacing:0.5px;">Customer Name</div>
            <div style="font-size:18px; font-weight:700; color:#2d3748; margin-top:4px;">
                <c:out value="${profile.customerName}"/>
            </div>
            <div style="margin-top:14px; font-size:12px; font-weight:700; color:#a0aec0; text-transform:uppercase;">Customer ID</div>
            <div style="font-size:15px; font-weight:600; color:#4a5568; font-family:monospace; margin-top:2px;">
                <c:out value="${profile.customerId}"/>
            </div>
            <div style="margin-top:14px; font-size:12px; font-weight:700; color:#a0aec0; text-transform:uppercase;">Primary Account Number</div>
            <div style="font-size:15px; font-weight:700; color:#2b6cb0; font-family:monospace; margin-top:2px;">
                <c:out value="${profile.primaryAccountNumber}"/>
            </div>
        </div>

        <!-- Phone & Email (Masked with Reveal) -->
        <div style="background:#ffffff; border:1px solid #e2e8f0; border-radius:8px; padding:18px; box-shadow:0 2px 5px rgba(0,0,0,0.03);">
            <!-- Mobile -->
            <div style="display:flex; justify-content:space-between; align-items:center;">
                <div style="font-size:12px; font-weight:700; color:#a0aec0; text-transform:uppercase;">Registered Mobile</div>
                <button type="button" class="btn" onclick="openRevealModal('PHONE', 'Registered Mobile Number')" 
                        style="padding:3px 8px; width:auto; font-size:11px; background:#4a5568; margin:0;" id="revealBtn-PHONE">
                    👁️ Reveal
                </button>
            </div>
            <div id="fieldVal-PHONE" style="font-size:15px; font-weight:600; color:#2d3748; font-family:monospace; margin-top:4px;">
                <c:out value="${profile.maskedPhone}"/>
            </div>

            <!-- Email -->
            <div style="margin-top:16px; display:flex; justify-content:space-between; align-items:center;">
                <div style="font-size:12px; font-weight:700; color:#a0aec0; text-transform:uppercase;">Email Address</div>
                <button type="button" class="btn" onclick="openRevealModal('EMAIL', 'Customer Email Address')" 
                        style="padding:3px 8px; width:auto; font-size:11px; background:#4a5568; margin:0;" id="revealBtn-EMAIL">
                    👁️ Reveal
                </button>
            </div>
            <div id="fieldVal-EMAIL" style="font-size:15px; font-weight:600; color:#2d3748; font-family:monospace; margin-top:4px;">
                <c:out value="${profile.maskedEmail}"/>
            </div>
        </div>

        <!-- PAN & Aadhaar (Masked with Reveal) -->
        <div style="background:#ffffff; border:1px solid #e2e8f0; border-radius:8px; padding:18px; box-shadow:0 2px 5px rgba(0,0,0,0.03);">
            <!-- PAN -->
            <div style="display:flex; justify-content:space-between; align-items:center;">
                <div style="font-size:12px; font-weight:700; color:#a0aec0; text-transform:uppercase;">PAN Number</div>
                <button type="button" class="btn" onclick="openRevealModal('PAN', 'Customer PAN Number')" 
                        style="padding:3px 8px; width:auto; font-size:11px; background:#4a5568; margin:0;" id="revealBtn-PAN">
                    👁️ Reveal
                </button>
            </div>
            <div id="fieldVal-PAN" style="font-size:15px; font-weight:600; color:#2d3748; font-family:monospace; margin-top:4px;">
                <c:out value="${profile.maskedPan}"/>
            </div>

            <!-- Aadhaar -->
            <div style="margin-top:16px; display:flex; justify-content:space-between; align-items:center;">
                <div style="font-size:12px; font-weight:700; color:#a0aec0; text-transform:uppercase;">Aadhaar Number</div>
                <button type="button" class="btn" onclick="openRevealModal('AADHAAR', 'Customer Aadhaar Number')" 
                        style="padding:3px 8px; width:auto; font-size:11px; background:#4a5568; margin:0;" id="revealBtn-AADHAAR">
                    👁️ Reveal
                </button>
            </div>
            <div id="fieldVal-AADHAAR" style="font-size:15px; font-weight:600; color:#2d3748; font-family:monospace; margin-top:4px;">
                <c:out value="${profile.maskedAadhaar}"/>
            </div>
        </div>

    </div>

    <!-- Active & Historical Loan Portfolio Table -->
    <h3 style="color:#2d3748; margin-top:20px; border-bottom:2px solid #edf2f7; padding-bottom:10px;">
        Authorized Customer Loan Accounts
    </h3>

    <table>
        <thead>
            <tr>
                <th>Loan Account No</th>
                <th>Principal Amount</th>
                <th>Interest Rate</th>
                <th>Tenure</th>
                <th>Monthly EMI</th>
                <th>Outstanding Balance</th>
                <th>Status</th>
                <th>Branch</th>
                <th>Origination Date</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="loan" items="${profile.loans}">
                <tr>
                    <td>
                        <strong style="color:#2b6cb0; font-family:monospace; font-size:14px;">
                            <c:out value="${loan.accountNumber}"/>
                        </strong>
                    </td>
                    <td>₹<c:out value="${loan.amount}"/></td>
                    <td><c:out value="${loan.interest}"/>%</td>
                    <td><c:out value="${loan.months}"/> Months</td>
                    <td>₹<c:out value="${loan.emi}"/></td>
                    <td>
                        <strong style="color:${loan.remaining > 0 ? '#c53030' : '#276749'};">
                            ₹<c:out value="${loan.remaining}"/>
                        </strong>
                    </td>
                    <td>
                        <c:choose>
                            <c:when test="${loan.status == 'ACTIVE'}">
                                <span style="color:#276749; background:#c6f6d5; padding:3px 8px; border-radius:10px; font-size:12px; font-weight:600;">ACTIVE</span>
                            </c:when>
                            <c:when test="${loan.status == 'CLOSED'}">
                                <span style="color:#4a5568; background:#e2e8f0; padding:3px 8px; border-radius:10px; font-size:12px; font-weight:600;">CLOSED</span>
                            </c:when>
                            <c:otherwise>
                                <span style="color:#b7791f; background:#feebc8; padding:3px 8px; border-radius:10px; font-size:12px; font-weight:600;"><c:out value="${loan.status}"/></span>
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td>
                        <span style="background:#edf2f7; color:#4a5568; padding:3px 8px; border-radius:12px; font-size:12px; font-weight:600;">
                            <c:out value="${loan.branch}"/>
                        </span>
                    </td>
                    <td style="font-size:13px; color:#718096;"><c:out value="${loan.createdAt}"/></td>
                </tr>
            </c:forEach>
        </tbody>
    </table>

</div>

<!-- Sensitive Data Reveal Modal -->
<div id="revealModal" style="display:none; position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(0,0,0,0.5); z-index:9999; justify-content:center; align-items:center;">
    <div style="background:#ffffff; border-radius:10px; width:460px; max-width:90%; padding:25px; box-shadow:0 10px 30px rgba(0,0,0,0.2);">
        <h3 style="margin-top:0; color:#2c3e50; display:flex; align-items:center; gap:8px;">
            🔒 Authorize Sensitive Data Reveal
        </h3>
        <p style="color:#718096; font-size:13px; margin-top:-5px;">
            Requesting unmasked access to <strong id="modalFieldLabel" style="color:#2d3748;"></strong>.
        </p>

        <div id="modalAlert" style="display:none;" class="alert alert-error"></div>

        <form id="revealForm" onsubmit="submitRevealRequest(event)">
            <input type="hidden" id="revealTargetField" value=""/>
            <input type="hidden" id="revealLoanId" value="${profile.primaryLoanId}"/>

            <label style="font-weight:600; font-size:13px; color:#4a5568;">
                Business Justification / Purpose of Access <span style="color:#e53e3e;">*</span>
            </label>
            <textarea id="revealReason" rows="3" required minlength="8" maxlength="255"
                      placeholder="e.g. Identity verification for loan disbursal or court notice compliance."
                      style="width:100%; padding:10px; border:1px solid #cbd5e0; border-radius:6px; font-size:13px; box-sizing:border-box; margin-top:6px; margin-bottom:12px;"></textarea>
            
            <div style="font-size:11px; color:#a0aec0; margin-bottom:15px;">
                Note: Minimum 8 characters. Your Officer ID, timestamp, and IP address will be recorded in the audit trail.
            </div>

            <div style="display:flex; justify-content:flex-end; gap:10px;">
                <button type="button" class="btn btn-secondary" onclick="closeRevealModal()" style="width:auto; padding:8px 18px; font-size:13px;">
                    Cancel
                </button>
                <button type="submit" class="btn" id="confirmRevealBtn" style="width:auto; padding:8px 18px; font-size:13px; background:#e67e22;">
                    Confirm & Reveal
                </button>
            </div>
        </form>
    </div>
</div>

<script>
    let activeField = null;

    function openRevealModal(field, label) {
        activeField = field;
        document.getElementById('revealTargetField').value = field;
        document.getElementById('modalFieldLabel').innerText = label;
        document.getElementById('revealReason').value = '';
        document.getElementById('modalAlert').style.display = 'none';
        document.getElementById('revealModal').style.display = 'flex';
        document.getElementById('revealReason').focus();
    }

    function closeRevealModal() {
        document.getElementById('revealModal').style.display = 'none';
        document.getElementById('modalAlert').style.display = 'none';
        activeField = null;
    }

    function submitRevealRequest(event) {
        event.preventDefault();
        const field = document.getElementById('revealTargetField').value;
        const loanId = document.getElementById('revealLoanId').value;
        const reason = document.getElementById('revealReason').value.trim();
        const modalAlert = document.getElementById('modalAlert');
        const confirmBtn = document.getElementById('confirmRevealBtn');

        if (reason.length < 8) {
            modalAlert.innerText = 'Justification must be at least 8 characters long.';
            modalAlert.style.display = 'block';
            return;
        }

        const csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
        const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
        const csrfToken = csrfTokenMeta ? csrfTokenMeta.getAttribute('content') : '';
        const csrfHeader = csrfHeaderMeta ? csrfHeaderMeta.getAttribute('content') : 'X-CSRF-TOKEN';

        confirmBtn.disabled = true;
        confirmBtn.innerText = 'Verifying...';

        fetch('/loan-search/reveal', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeader]: csrfToken
            },
            body: JSON.stringify({
                loanId: parseInt(loanId),
                field: field,
                reason: reason
            })
        })
        .then(response => response.json().then(data => ({ status: response.status, body: data })))
        .then(result => {
            confirmBtn.disabled = false;
            confirmBtn.innerText = 'Confirm & Reveal';

            if (result.status === 200 && result.body.success) {
                // Update field value in DOM
                const targetEl = document.getElementById('fieldVal-' + field);
                if (targetEl) {
                    targetEl.innerHTML = '<span style="color:#276749; font-weight:700;">' + result.body.unmaskedValue + '</span> ' +
                        '<span style="font-size:11px; background:#c6f6d5; color:#22543d; padding:2px 6px; border-radius:8px; margin-left:6px;">REVEALED</span>';
                }
                // Disable button
                const btnEl = document.getElementById('revealBtn-' + field);
                if (btnEl) {
                    btnEl.disabled = true;
                    btnEl.innerText = 'Unmasked';
                    btnEl.style.background = '#a0aec0';
                    btnEl.style.cursor = 'default';
                }
                closeRevealModal();
            } else {
                modalAlert.innerText = result.body.message || 'Authorization failed or loan is outside your permitted branch.';
                modalAlert.style.display = 'block';
            }
        })
        .catch(err => {
            confirmBtn.disabled = false;
            confirmBtn.innerText = 'Confirm & Reveal';
            modalAlert.innerText = 'Error communicating with security service: ' + err.message;
            modalAlert.style.display = 'block';
        });
    }
</script>
</body>
</html>
