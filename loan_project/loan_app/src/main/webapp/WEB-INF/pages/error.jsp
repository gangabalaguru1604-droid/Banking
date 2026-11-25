<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ include file="header.jsp" %>

<style>
    .error-container {
        display: flex;
        justify-content: center;
        align-items: center;
        min-height: calc(100vh - 180px);
        padding: 30px 20px;
        box-sizing: border-box;
    }

    .error-card {
        max-width: 600px;
        width: 100%;
        background: #FFFFFF;
        border-radius: 16px;
        padding: 40px 36px;
        box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
        border: 1px solid #E2E8F0;
        text-align: center;
        animation: slideUp 0.4s ease-out;
    }

    .error-icon-wrapper {
        width: 80px;
        height: 80px;
        margin: 0 auto 20px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 28px;
        font-weight: 800;
        letter-spacing: -0.5px;
    }

    .badge-403, .badge-401 {
        background: #FEF3C7;
        color: #D97706;
        border: 2px solid #FCD34D;
    }

    .badge-404 {
        background: #EFF6FF;
        color: #2563EB;
        border: 2px solid #BFDBFE;
    }

    .badge-500, .badge-503, .badge-405, .badge-default {
        background: #FEE2E2;
        color: #DC2626;
        border: 2px solid #FECACA;
    }

    .error-title {
        font-size: 24px;
        font-weight: 700;
        color: #1E293B;
        margin-top: 0;
        margin-bottom: 12px;
    }

    .error-message {
        font-size: 15px;
        color: #475569;
        line-height: 1.6;
        margin-bottom: 18px;
    }

    .error-suggestion {
        font-size: 14px;
        color: #64748B;
        background: #F8FAFC;
        padding: 12px 18px;
        border-radius: 8px;
        border-left: 4px solid #ED7D16;
        margin-bottom: 24px;
        text-align: left;
    }

    .error-path {
        font-size: 12px;
        color: #94A3B8;
        margin-bottom: 24px;
        word-break: break-all;
    }

    .error-path code {
        background: #F1F5F9;
        padding: 3px 8px;
        border-radius: 4px;
        color: #334155;
    }

    .error-actions {
        display: flex;
        gap: 12px;
        justify-content: center;
        flex-wrap: wrap;
    }

    .btn-action {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        padding: 12px 22px;
        border-radius: 8px;
        font-size: 14px;
        font-weight: 600;
        text-decoration: none;
        cursor: pointer;
        transition: all 0.2s ease;
        border: none;
        min-width: 130px;
    }

    .btn-primary-action {
        background: #0050A4;
        color: #FFFFFF;
    }

    .btn-primary-action:hover {
        background: #003F82;
        color: #FFFFFF;
    }

    .btn-orange-action {
        background: #ED7D16;
        color: #FFFFFF;
    }

    .btn-orange-action:hover {
        background: #D96C0B;
        color: #FFFFFF;
    }

    .btn-outline-action {
        background: #FFFFFF;
        color: #475569;
        border: 1px solid #CBD5E1;
    }

    .btn-outline-action:hover {
        background: #F1F5F9;
        color: #1E293B;
    }

    @keyframes slideUp {
        from {
            opacity: 0;
            transform: translateY(20px);
        }
        to {
            opacity: 1;
            transform: translateY(0);
        }
    }
</style>

<div class="error-container">
    <div class="error-card">
        <c:set var="badgeClass" value="badge-default" />
        <c:if test="${statusCode == 401 || statusCode == 403}">
            <c:set var="badgeClass" value="badge-403" />
        </c:if>
        <c:if test="${statusCode == 404}">
            <c:set var="badgeClass" value="badge-404" />
        </c:if>
        <c:if test="${statusCode >= 500}">
            <c:set var="badgeClass" value="badge-500" />
        </c:if>

        <div class="error-icon-wrapper ${badgeClass}">
            <c:out value="${statusCode != null ? statusCode : '!'}" />
        </div>

        <h1 class="error-title">
            <c:out value="${errorTitle != null ? errorTitle : 'Unexpected Error'}" />
        </h1>

        <p class="error-message">
            <c:out value="${errorMessage != null ? errorMessage : 'An unexpected error occurred while processing your request.'}" />
        </p>

        <c:if test="${not empty errorSuggestion}">
            <div class="error-suggestion">
                <strong>Tip:</strong> <c:out value="${errorSuggestion}" />
            </div>
        </c:if>

        <c:if test="${not empty requestPath}">
            <div class="error-path">
                Requested URL: <code><c:out value="${requestPath}" /></code>
            </div>
        </c:if>

        <div class="error-actions">
            <a href="/dashboard" class="btn-action btn-primary-action">
                Dashboard
            </a>
            <a href="/login" class="btn-action btn-orange-action">
                Log In
            </a>
            <button type="button" onclick="window.history.back()" class="btn-action btn-outline-action">
                ← Go Back
            </button>
        </div>
    </div>
</div>

</body>
</html>
