<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ include file="header.jsp" %>

<div class="wide-card">
    <h2 style="margin-top:0; color:#2c3e50;">🛡️ Bank Staff Management</h2>
    <p style="color:#718096; font-size:14px; margin-top:-10px;">
        Authorized Administrator Portal: Provision staff accounts, assign roles, unlock accounts, and reset MFA.
    </p>

    <c:if test="${param.success == 'provisioned'}">
        <div class="alert alert-success">Staff member successfully provisioned with mandatory MFA enrollment.</div>
    </c:if>
    <c:if test="${param.success == 'unlocked'}">
        <div class="alert alert-success">Staff account has been unlocked. Failed attempt counter reset.</div>
    </c:if>
    <c:if test="${param.success == 'role_updated'}">
        <div class="alert alert-success">Staff role successfully updated.</div>
    </c:if>
    <c:if test="${param.success == 'mfa_reset'}">
        <div class="alert alert-success">MFA secret has been reset for the staff member.</div>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-error">${errorMessage}</div>
    </c:if>

    <h3 style="color:#2d3748; margin-top:30px; border-bottom:2px solid #edf2f7; padding-bottom:10px;">Existing Staff Directory</h3>
    <table>
        <thead>
            <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Email</th>
                <th>Role</th>
                <th>Status</th>
                <th>MFA</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="staff" items="${staffList}">
                <tr>
                    <td>${staff.id}</td>
                    <td><strong>${staff.name}</strong></td>
                    <td>${staff.email}</td>
                    <td>
                        <form action="/admin/staff/role" method="post" style="display:flex; gap:6px; margin:0;">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                            <input type="hidden" name="staffId" value="${staff.id}"/>
                            <select name="newRole" style="padding:4px 8px; margin:0; width:auto; font-size:12px;">
                                <c:forEach var="r" items="${roles}">
                                    <option value="${r}" ${staff.role == r ? 'selected' : ''}>${r.displayName}</option>
                                </c:forEach>
                            </select>
                            <button type="submit" class="btn" style="padding:4px 8px; width:auto; font-size:12px;">Change</button>
                        </form>
                    </td>
                    <td>
                        <c:choose>
                            <c:when test="${staff.accountNonLocked}">
                                <span style="color:#276749; background:#c6f6d5; padding:3px 8px; border-radius:10px; font-size:12px; font-weight:600;">ACTIVE</span>
                            </c:when>
                            <c:otherwise>
                                <span style="color:#c53030; background:#fed7d7; padding:3px 8px; border-radius:10px; font-size:12px; font-weight:600;">LOCKED (5 failures)</span>
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td>
                        <span style="color:#2b6cb0; background:#bee3f8; padding:3px 8px; border-radius:10px; font-size:12px; font-weight:600;">
                            ${staff.mfaType} (Enrolled)
                        </span>
                    </td>
                    <td>
                        <div style="display:flex; gap:6px;">
                            <c:if test="${!staff.accountNonLocked}">
                                <form action="/admin/staff/unlock" method="post" style="margin:0;">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                    <input type="hidden" name="email" value="${staff.email}"/>
                                    <button type="submit" class="btn btn-secondary" style="padding:4px 10px; width:auto; font-size:12px;">Unlock</button>
                                </form>
                            </c:if>
                            <form action="/admin/staff/reset-mfa" method="post" style="margin:0;">
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                <input type="hidden" name="staffId" value="${staff.id}"/>
                                <button type="submit" class="btn btn-danger" style="padding:4px 10px; width:auto; font-size:12px;">Reset MFA</button>
                            </form>
                        </div>
                    </td>
                </tr>
            </c:forEach>
        </tbody>
    </table>

    <h3 style="color:#2d3748; margin-top:40px; border-bottom:2px solid #edf2f7; padding-bottom:10px;">➕ Provision New Staff Account</h3>
    <form action="/admin/staff/provision" method="post" style="max-width:600px; margin-top:20px;">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

        <label style="font-weight:600; font-size:13px; color:#4a5568;">Full Name</label>
        <input type="text" name="name" placeholder="Staff Member Name" required>

        <label style="font-weight:600; font-size:13px; color:#4a5568;">Corporate Email Address</label>
        <input type="email" name="email" placeholder="staff@bank.com" required>

        <label style="font-weight:600; font-size:13px; color:#4a5568;">Temporary Password</label>
        <input type="password" name="password" placeholder="Min 8 characters with upper, lower, number, special" required>

        <label style="font-weight:600; font-size:13px; color:#4a5568;">Assigned Role</label>
        <select name="role" required>
            <option value="ROLE_LOAN_OFFICER">Loan Officer (Origination & Processing)</option>
            <option value="ROLE_COLLECTIONS_AGENT">Collections Agent (Repayment Management)</option>
            <option value="ROLE_COLLECTIONS_MANAGER">Collections Manager (Supervision & Repayments)</option>
            <option value="ROLE_ADMIN">Administrator (Staff Provisioning & Security Auditing)</option>
        </select>

        <button type="submit" class="btn" style="margin-top:10px; width:auto; padding:12px 30px;">Provision Staff Member</button>
    </form>
</div>
