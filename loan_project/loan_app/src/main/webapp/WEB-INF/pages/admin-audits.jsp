<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ include file="header.jsp" %>

<div class="wide-card">
    <h2 style="margin-top:0; color:#2c3e50;">🔒 Security & Authentication Audit Trail</h2>
    <p style="color:#718096; font-size:14px; margin-top:-10px;">
        Tamper-resistant audit log capturing Stage 1 logins, Stage 2 MFA attempts, lockouts, password resets, and administrative provisioning.
    </p>

    <table>
        <thead>
            <tr>
                <th>ID</th>
                <th>Timestamp (UTC)</th>
                <th>Username</th>
                <th>Event Type</th>
                <th>Outcome</th>
                <th>Failure Category</th>
                <th>Client IP</th>
                <th>Details</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="log" items="${auditLogs}">
                <tr>
                    <td>${log.id}</td>
                    <td style="font-family:monospace; font-size:12px;">${log.timestamp}</td>
                    <td><strong>${log.username}</strong></td>
                    <td>
                        <span style="background:#edf2f7; padding:2px 6px; border-radius:4px; font-size:11px; font-family:monospace;">
                            ${log.eventType}
                        </span>
                    </td>
                    <td>
                        <c:choose>
                            <c:when test="${log.outcome == 'SUCCESS'}">
                                <span style="color:#276749; background:#c6f6d5; padding:2px 8px; border-radius:10px; font-size:11px; font-weight:700;">
                                    SUCCESS
                                </span>
                            </c:when>
                            <c:otherwise>
                                <span style="color:#c53030; background:#fed7d7; padding:2px 8px; border-radius:10px; font-size:11px; font-weight:700;">
                                    FAILURE
                                </span>
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td>
                        <c:if test="${not empty log.failureCategory}">
                            <span style="color:#c53030; font-size:12px;">${log.failureCategory}</span>
                        </c:if>
                    </td>
                    <td style="font-family:monospace; font-size:12px;">${log.clientIp}</td>
                    <td style="font-size:12px; color:#4a5568;">${log.details}</td>
                </tr>
            </c:forEach>
        </tbody>
    </table>
</div>
