<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.fmt" prefix="fmt" %>
<%@ include file="header.jsp" %>

<div class="wide-card" style="max-width: 1400px; margin: 20px auto; padding: 25px;">

    <!-- Header -->
    <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:2px solid #edf2f7; padding-bottom:15px; margin-bottom:25px; flex-wrap:wrap; gap:15px;">
        <div>
            <h2 style="margin:0; color:#1e293b; font-size:24px; font-weight:800; display:flex; align-items:center; gap:10px;">
                🔔 DPD 1–3 Automated Loan Reminder Interactive Demo
            </h2>
            <p style="color:#64748b; font-size:14px; margin:6px 0 0 0;">
                Demonstrate automated reminder dispatches, DPD 1–3 email notices, webhook lifecycle, and SMS fallback for overdue borrowers.
            </p>
        </div>
        <div style="display:flex; gap:10px; align-items:center;">
            <span style="background:#e0f2fe; color:#0369a1; padding:6px 14px; border-radius:20px; font-size:13px; font-weight:600;">
                📅 System Business Date: <strong><c:out value="${today}"/></strong>
            </span>
            <span style="background:#fef3c7; color:#92400e; padding:6px 14px; border-radius:20px; font-size:13px; font-weight:600;">
                ⏰ Scheduler: <strong>Daily at 9:00 AM (Asia/Kolkata)</strong>
            </span>
        </div>
    </div>

    <!-- Notification Banners -->
    <c:if test="${param.success == 'loan_created_and_sent'}">
        <div class="alert alert-success" style="background:#ecfdf5; border:1px solid #10b981; color:#065f46; padding:14px 18px; border-radius:8px; margin-bottom:20px; box-shadow:0 2px 4px rgba(16,185,129,0.1);">
            <div style="font-weight:700; font-size:15px; margin-bottom:4px;">✉️ Overdue Loan Created & Reminder Email Sent!</div>
            <div>The delinquent installment was recorded and the DPD reminder email has been dispatched immediately to the recipient's email address. Check your inbox!</div>
        </div>
    </c:if>
    <c:if test="${param.success == 'loan_created'}">
        <div class="alert alert-success" style="background:#ecfdf5; border:1px solid #a7f3d0; color:#065f46; padding:12px 18px; border-radius:8px; margin-bottom:20px;">
            ✅ <strong>Demo Overdue Installment Created!</strong> The customer and delinquent installment are now primed for automated reminder processing.
        </div>
    </c:if>
    <c:if test="${param.success == 'scheduler_executed'}">
        <div class="alert alert-success" style="background:#ecfdf5; border:1px solid #a7f3d0; color:#065f46; padding:12px 18px; border-radius:8px; margin-bottom:20px;">
            ⚡ <strong>Reminder Engine Executed!</strong> Overdue installments have been processed, reminder notices dispatched, and audit logs recorded.
        </div>
    </c:if>
    <c:if test="${param.success != null && param.success.startsWith('webhook_')}">
        <div class="alert alert-info" style="background:#eff6ff; border:1px solid #bfdbfe; color:#1e40af; padding:12px 18px; border-radius:8px; margin-bottom:20px;">
            📡 <strong>Provider Webhook Simulated!</strong> Status transition successfully reconciled in database.
        </div>
    </c:if>

    <!-- Top Two Control Columns: Create Demo Loan & Run Scheduler -->
    <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(420px, 1fr)); gap:25px; margin-bottom:30px;">

        <!-- Card 1: Setup Overdue Borrower -->
        <div style="background:#ffffff; border:1px solid #e2e8f0; border-radius:10px; padding:22px; box-shadow:0 2px 6px rgba(0,0,0,0.03);">
            <h3 style="margin-top:0; color:#1e293b; font-size:17px; font-weight:700; border-bottom:1px solid #f1f5f9; padding-bottom:10px; display:flex; align-items:center; gap:8px;">
                🎯 Step 1: Create or Setup Overdue Borrower
            </h3>
            <p style="color:#64748b; font-size:13px; margin-bottom:18px;">
                Create a loan installment with a due date in the past (1, 2, or 3 days overdue) to trigger the workflow.
            </p>

            <form action="/admin/reminders/demo/create-overdue-loan" method="post">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

                <div style="margin-bottom:12px;">
                    <label style="display:block; font-size:12px; font-weight:700; color:#475569; margin-bottom:4px;">Borrower Name</label>
                    <input type="text" name="customerName" placeholder="Enter borrower name" required style="width:100%; padding:9px; border:1px solid #cbd5e1; border-radius:6px; font-size:13px;"/>
                </div>

                <div style="margin-bottom:12px;">
                    <label style="display:block; font-size:12px; font-weight:700; color:#475569; margin-bottom:4px;">
                        Recipient Email (Enter recipient email to receive reminder notice)
                    </label>
                    <input type="email" name="customerEmail" placeholder="e.g. customer@example.com" required style="width:100%; padding:9px; border:1px solid #cbd5e1; border-radius:6px; font-size:13px;"/>
                </div>

                <div style="display:grid; grid-template-columns: 1fr 1fr; gap:12px; margin-bottom:12px;">
                    <div>
                        <label style="display:block; font-size:12px; font-weight:700; color:#475569; margin-bottom:4px;">Mobile Number</label>
                        <input type="text" name="customerPhone" placeholder="Enter 10-digit mobile" required style="width:100%; padding:9px; border:1px solid #cbd5e1; border-radius:6px; font-size:13px;"/>
                    </div>
                    <div>
                        <label style="display:block; font-size:12px; font-weight:700; color:#475569; margin-bottom:4px;">Overdue EMI (&#8377;)</label>
                        <input type="number" step="0.01" name="emiAmount" placeholder="e.g. 4500.00" required style="width:100%; padding:9px; border:1px solid #cbd5e1; border-radius:6px; font-size:13px;"/>
                    </div>
                </div>

                <div style="margin-bottom:18px;">
                    <label style="display:block; font-size:12px; font-weight:700; color:#475569; margin-bottom:4px;">Delinquency Window (Days Past Due)</label>
                    <select name="dpdDays" style="width:100%; padding:9px; border:1px solid #cbd5e1; border-radius:6px; font-size:13px; background:#ffffff;">
                        <option value="1">DPD 1: 1 Day Overdue (Friendly Notice | &#8377;0 Penalty)</option>
                        <option value="2">DPD 2: 2 Days Overdue (Second Notice | &#8377;150 Penalty)</option>
                        <option value="3">DPD 3: 3 Days Overdue (Final Notice Before IVR | &#8377;300 Penalty)</option>
                    </select>
                </div>

                <div style="margin-bottom:18px; background:#eff6ff; border:1px solid #bfdbfe; border-radius:6px; padding:10px 14px;">
                    <label style="display:flex; align-items:center; gap:8px; font-size:13px; font-weight:700; color:#1e40af; cursor:pointer; margin:0;">
                        <input type="checkbox" name="sendImmediately" value="true" checked="checked" style="width:16px; height:16px; cursor:pointer;"/>
                        ⚡ Send Reminder Email Immediately (Demo Mode)
                    </label>
                    <div style="font-size:11px; color:#64748b; margin-top:4px; margin-left:24px;">
                        Sends the DPD notice to the recipient's inbox immediately upon submitting.
                    </div>
                </div>

                <button type="submit" class="btn btn-primary" style="background:#2563eb; color:#ffffff; font-weight:700; border:none; border-radius:6px; padding:11px 16px; cursor:pointer; display:flex; align-items:center; justify-content:center; gap:8px; width:100%;">
                    🚀 Create Overdue Loan & Send Email Now
                </button>
            </form>
        </div>

        <!-- Card 2: Trigger Reminder Engine -->
        <div style="background:#ffffff; border:1px solid #e2e8f0; border-radius:10px; padding:22px; box-shadow:0 2px 6px rgba(0,0,0,0.03); display:flex; flex-direction:column; justify-content:space-between;">
            <div>
                <h3 style="margin-top:0; color:#1e293b; font-size:17px; font-weight:700; border-bottom:1px solid #f1f5f9; padding-bottom:10px; display:flex; align-items:center; gap:8px;">
                    ⚡ Step 2: Trigger Reminder Workflow
                </h3>
                <p style="color:#64748b; font-size:13px; line-height:1.6;">
                    The DPD engine identifies unpaid installments where <code>dueDate</code> is 1, 2, or 3 calendar days ago.
                    It computes accurate penalties, enforces database unique claims to prevent duplicates, and dispatches the customized notice.
                </p>

                <div style="background:#f8fafc; border-left:4px solid #2563eb; padding:12px 16px; margin:15px 0; font-size:13px; color:#334155;">
                    <div style="font-weight:700; margin-bottom:4px;">📋 Automated Process Flow:</div>
                    1. Re-verifies payment status (skips paid loans)<br>
                    2. Checks channel eligibility (Email or SMS)<br>
                    3. Acquires atomic DB claim (no duplicates)<br>
                    4. Dispatches outside DB transaction with retries<br>
                    5. Records audit log with provider message ID
                </div>
            </div>

            <form action="/admin/reminders/demo/trigger-now" method="post" style="margin-top:15px;">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                <button type="submit" class="btn" style="background:#059669; color:#ffffff; font-size:15px; font-weight:700; border:none; border-radius:6px; padding:12px; cursor:pointer;">
                    ⚡ Trigger DPD 1–3 Reminder Engine Now
                </button>
            </form>
        </div>
    </div>

    <!-- Section 3: Eligible Overdue Installments -->
    <div style="background:#ffffff; border:1px solid #e2e8f0; border-radius:10px; padding:20px; margin-bottom:30px; box-shadow:0 2px 6px rgba(0,0,0,0.03);">
        <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #f1f5f9; padding-bottom:12px; margin-bottom:15px;">
            <h3 style="margin:0; font-size:17px; font-weight:700; color:#1e293b;">
                📋 Delinquent Installments Eligible for Reminders Today
            </h3>
            <span style="font-size:12px; color:#64748b;">
                Found: <strong><c:out value="${overdueInstallments != null ? overdueInstallments.size() : 0}"/></strong> records
            </span>
        </div>

        <c:choose>
            <c:when test="${empty overdueInstallments}">
                <div style="text-align:center; padding:30px; color:#64748b; font-size:14px; background:#f8fafc; border-radius:6px;">
                    ℹ️ No overdue installments found for today. Use the form above to create a demo delinquent loan!
                </div>
            </c:when>
            <c:otherwise>
                <div style="overflow-x:auto;">
                    <table style="width:100%; border-collapse:collapse; font-size:13px;">
                        <thead>
                            <tr style="background:#f1f5f9; text-align:left; color:#475569;">
                                <th style="padding:10px 14px;">Customer Name</th>
                                <th style="padding:10px 14px;">Recipient Email</th>
                                <th style="padding:10px 14px;">Loan Account</th>
                                <th style="padding:10px 14px;">Due Date</th>
                                <th style="padding:10px 14px;">Overdue Amount</th>
                                <th style="padding:10px 14px;">DPD Calculation</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="inst" items="${overdueInstallments}">
                                <tr style="border-bottom:1px solid #e2e8f0;">
                                    <td style="padding:12px 14px; font-weight:600;"><c:out value="${inst.loan.user.name}"/></td>
                                    <td style="padding:12px 14px; font-family:monospace;"><c:out value="${inst.loan.user.email}"/></td>
                                    <td style="padding:12px 14px; font-family:monospace; font-weight:700;"><c:out value="${inst.loan.accountNumber}"/></td>
                                    <td style="padding:12px 14px;"><c:out value="${inst.dueDate}"/></td>
                                    <td style="padding:12px 14px; font-weight:700; color:#b91c1c;">
                                        &#8377; <c:out value="${inst.emiAmount - inst.paidAmount}"/>
                                    </td>
                                    <td style="padding:12px 14px;">
                                        <c:choose>
                                            <c:when test="${today.toEpochDay() - inst.dueDate.toEpochDay() == 1}">
                                                <span style="background:#dbeafe; color:#1d4ed8; padding:3px 8px; border-radius:10px; font-size:11px; font-weight:700;">DPD 1 (Friendly)</span>
                                            </c:when>
                                            <c:when test="${today.toEpochDay() - inst.dueDate.toEpochDay() == 2}">
                                                <span style="background:#fef3c7; color:#b45309; padding:3px 8px; border-radius:10px; font-size:11px; font-weight:700;">DPD 2 (Notice)</span>
                                            </c:when>
                                            <c:when test="${today.toEpochDay() - inst.dueDate.toEpochDay() == 3}">
                                                <span style="background:#fee2e2; color:#b91c1c; padding:3px 8px; border-radius:10px; font-size:11px; font-weight:700;">DPD 3 (Final Notice)</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span style="background:#f1f5f9; color:#475569; padding:3px 8px; border-radius:10px; font-size:11px;">
                                                    <c:out value="${today.toEpochDay() - inst.dueDate.toEpochDay()}"/> Days
                                                </span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

</div>

