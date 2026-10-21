<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Indian Bank - Loan Management System</title>
    <!-- CSRF Tokens for AJAX -->
    <meta name="_csrf" content="${_csrf.token}"/>
    <meta name="_csrf_header" content="${_csrf.headerName}"/>

    <style>
        body {
            margin: 0;
            font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, Helvetica, Arial, sans-serif;
            background-color: #F7F9FC;
            color: #172B4D;
        }

        /* NAVBAR */
        .navbar {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 14px 36px;
            background: #FFFFFF;
            border-bottom: 1px solid #E2E8F0;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
        }

        .logo a {
            font-size: 20px;
            font-weight: 800;
            color: #24446B;
            text-decoration: none;
            display: flex;
            align-items: center;
            gap: 10px;
            letter-spacing: -0.3px;
        }

        .logo a span.brand-orange {
            color: #ED7D16;
        }

        .nav {
            display: flex;
            align-items: center;
            gap: 20px;
        }

        .nav a {
            text-decoration: none;
            color: #315B91;
            font-weight: 600;
            font-size: 14px;
            transition: color 0.15s ease-in-out;
            padding: 6px 10px;
            border-radius: 6px;
        }

        .nav a:hover {
            color: #ED7D16;
            background: #F8FAFC;
        }

        .user-badge {
            background: #F1F5F9;
            padding: 6px 14px;
            border-radius: 20px;
            font-size: 13px;
            font-weight: 600;
            color: #24446B;
            display: inline-flex;
            align-items: center;
            gap: 8px;
            border: 1px solid #E2E8F0;
        }

        .role-pill {
            background: #ED7D16;
            color: #FFFFFF;
            padding: 3px 9px;
            border-radius: 10px;
            font-size: 11px;
            font-weight: 700;
            letter-spacing: 0.3px;
            text-transform: uppercase;
        }

        /* CARD */
        .card {
            width: 380px;
            margin: 50px auto;
            padding: 30px;
            background: #ffffff;
            border-radius: 12px;
            box-shadow: 0 5px 25px rgba(0,0,0,0.08);
            animation: slideUp 0.6s ease-out;
            box-sizing: border-box;
        }

        .wide-card {
            width: 95%;
            max-width: 1400px;
            margin: 30px auto;
            padding: 32px;
            background: #ffffff;
            border-radius: 12px;
            box-shadow: 0 5px 25px rgba(0,0,0,0.08);
            box-sizing: border-box;
        }

        input, select {
            width: 100%;
            padding: 12px;
            margin-top: 8px;
            margin-bottom: 14px;
            border: 1px solid #cbd5e0;
            border-radius: 6px;
            box-sizing: border-box;
            font-size: 14px;
        }

        input:focus, select:focus {
            outline: none;
            border-color: #e67e22;
            box-shadow: 0 0 0 3px rgba(230, 126, 34, 0.2);
        }

        /* BUTTONS */
        .btn {
            width: 100%;
            padding: 12px;
            background: #e67e22;
            border: none;
            color: white;
            cursor: pointer;
            border-radius: 6px;
            font-size: 15px;
            font-weight: 600;
            transition: background 0.2s, transform 0.1s;
        }

        .btn:hover {
            background: #ca6f1e;
        }

        .btn-secondary {
            background: #4a5568;
        }

        .btn-secondary:hover {
            background: #2d3748;
        }

        .btn-danger {
            background: #e53e3e;
        }

        .btn-danger:hover {
            background: #c53030;
        }

        .logout-form {
            display: inline;
            margin: 0;
            padding: 0;
        }

        .logout-btn {
            background: #FEE2E2;
            border: 1px solid #FECACA;
            color: #DC2626;
            font-weight: 600;
            cursor: pointer;
            font-size: 13px;
            padding: 6px 14px;
            border-radius: 6px;
            transition: all 0.15s ease-in-out;
            display: inline-flex;
            align-items: center;
        }

        .logout-btn:hover {
            background: #DC2626;
            color: #FFFFFF;
            border-color: #DC2626;
        }

        /* ALERTS */
        .alert {
            padding: 12px 16px;
            border-radius: 6px;
            margin-bottom: 20px;
            font-size: 14px;
        }

        .alert-error {
            background-color: #fff5f5;
            color: #c53030;
            border: 1px solid #fed7d7;
        }

        .alert-success {
            background-color: #f0fff4;
            color: #276749;
            border: 1px solid #c6f6d5;
        }

        .alert-info {
            background-color: #ebf8ff;
            color: #2b6cb0;
            border: 1px solid #bee3f8;
        }

        /* TABLES */
        .table-responsive {
            width: 100%;
            overflow-x: auto;
            -webkit-overflow-scrolling: touch;
            margin-top: 20px;
            border: 1px solid #e2e8f0;
            border-radius: 8px;
            background: #ffffff;
        }

        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 0;
            white-space: nowrap;
        }

        table th, table td {
            padding: 14px 18px;
            text-align: left;
            border-bottom: 1px solid #edf2f7;
            font-size: 14px;
            vertical-align: middle;
        }

        table th {
            background-color: #f8fafc;
            color: #4a5568;
            font-weight: 600;
            font-size: 12px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        table tr:last-child td {
            border-bottom: none;
        }

        table tr:hover {
            background-color: #f8fafc;
        }

        /* RESPONSIVE MEDIA QUERIES */
        @media (max-width: 1200px) {
            .wide-card {
                width: 96%;
                padding: 24px;
                margin: 20px auto;
            }
        }

        @media (max-width: 768px) {
            .wide-card {
                width: 98%;
                padding: 18px 14px;
                margin: 12px auto;
                border-radius: 8px;
            }
            .navbar {
                padding: 12px 18px;
                flex-direction: column;
                align-items: flex-start;
                gap: 12px;
            }
            .nav {
                width: 100%;
                flex-wrap: wrap;
                gap: 10px;
                justify-content: flex-start;
            }
            .user-badge {
                font-size: 12px;
                padding: 4px 10px;
            }
        }

        /* SESSION TIMEOUT MODAL */
        #sessionModal {
            display: none;
            position: fixed;
            z-index: 9999;
            left: 0;
            top: 0;
            width: 100%;
            height: 100%;
            background-color: rgba(0, 0, 0, 0.6);
            backdrop-filter: blur(3px);
            align-items: center;
            justify-content: center;
        }

        .modal-content {
            background-color: #ffffff;
            padding: 30px;
            border-radius: 12px;
            width: 400px;
            text-align: center;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
            animation: slideUp 0.3s ease-out;
        }

        .countdown-timer {
            font-size: 36px;
            font-weight: 700;
            color: #e53e3e;
            margin: 15px 0;
        }

        @keyframes slideUp {
            from { opacity: 0; transform: translateY(20px); }
            to { opacity: 1; transform: translateY(0); }
        }

        @keyframes float {
            0% { transform: translateY(0); }
            50% { transform: translateY(-10px); }
            100% { transform: translateY(0); }
        }
    </style>
</head>
<body>

<c:set var="userObj" value="${not empty sessionScope.user ? sessionScope.user : currentUser}" />

<div class="navbar">
    <div class="logo">
        <a href="/">🏦 Indian Bank</a>
    </div>

    <div class="nav">
        <c:choose>
            <c:when test="${userObj != null}">
                <span class="user-badge">
                    👤 <c:out value="${userObj.name}"/>
                    <span class="role-pill">
                        <c:choose>
                            <c:when test="${userObj.role == 'ROLE_CUSTOMER'}">Customer</c:when>
                            <c:when test="${userObj.role == 'ROLE_LOAN_OFFICER'}">Loan Officer</c:when>
                            <c:when test="${userObj.role == 'ROLE_COLLECTIONS_AGENT'}">Collections Agent</c:when>
                            <c:when test="${userObj.role == 'ROLE_COLLECTIONS_MANAGER'}">Collections Manager</c:when>
                            <c:when test="${userObj.role == 'ROLE_ADMIN'}">Admin</c:when>
                            <c:otherwise><c:out value="${userObj.role}"/></c:otherwise>
                        </c:choose>
                    </span>
                </span>

                <a href="/dashboard">Dashboard</a>

                <c:if test="${userObj.role == 'ROLE_LOAN_OFFICER' || userObj.role == 'ROLE_COLLECTIONS_AGENT' || userObj.role == 'ROLE_COLLECTIONS_MANAGER' || userObj.role == 'ROLE_ADMIN'}">
                    <a href="/loan-search">Loan Search</a>
                </c:if>

                <c:if test="${userObj.role == 'ROLE_ADMIN'}">
                    <a href="/admin/staff">Staff Management</a>
                    <a href="/admin/audit-logs">Audit Trail</a>
                    <a href="/admin/reminders/demo" style="color:#d97706; font-weight:700;">🔔 DPD Reminders Demo</a>
                </c:if>

                <c:if test="${userObj.role == 'ROLE_CUSTOMER'}">
                    <a href="/applyLoan">Apply Loan</a>
                </c:if>

                <form class="logout-form" action="/logout" method="post">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                    <button type="submit" class="logout-btn">Log Out</button>
                </form>
            </c:when>
            <c:otherwise>
                <a href="/">Home</a>
                <a href="/login">Login</a>
                <a href="/register">Sign Up</a>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<!-- Session Timeout Warning Modal (Active only when user is logged in) -->
<c:if test="${userObj != null}">
<div id="sessionModal">
    <div class="modal-content">
        <h3 style="margin-top:0; color:#2d3748;">Session Inactivity Warning</h3>
        <p style="color:#718096; font-size:14px;">Your session is about to expire due to inactivity.</p>
        <div class="countdown-timer" id="sessionCountdown">60</div>
        <p style="color:#718096; font-size:13px;">Seconds remaining before automatic logout</p>
        <div style="display:flex; gap:10px; margin-top:20px;">
            <button class="btn" onclick="stayLoggedIn()">Stay Logged In</button>
            <button class="btn btn-secondary" onclick="logoutNow()">Log Out Now</button>
        </div>
    </div>
</div>

<script>
    // Server-side session timeout is exactly 10 minutes (600 seconds)
    // Warning displayed 60 seconds before expiration (at 540 seconds)
    const TOTAL_TIMEOUT_SEC = 600;
    const WARNING_TIME_SEC = 60;
    const INACTIVITY_LIMIT_SEC = TOTAL_TIMEOUT_SEC - WARNING_TIME_SEC; // 540 seconds

    let inactivityTimer = 0;
    let warningCountdown = WARNING_TIME_SEC;
    let countdownInterval = null;
    let modalShown = false;

    function resetInactivity() {
        if (!modalShown) {
            inactivityTimer = 0;
        }
    }

    // Reset inactivity counter on user interactions (mouse, keyboard)
    window.addEventListener('mousemove', resetInactivity);
    window.addEventListener('keydown', resetInactivity);
    window.addEventListener('click', resetInactivity);

    setInterval(function() {
        if (!modalShown) {
            inactivityTimer++;
            if (inactivityTimer >= INACTIVITY_LIMIT_SEC) {
                showWarningModal();
            }
        }
    }, 1000);

    function showWarningModal() {
        modalShown = true;
        warningCountdown = WARNING_TIME_SEC;
        document.getElementById('sessionCountdown').innerText = warningCountdown;
        document.getElementById('sessionModal').style.display = 'flex';

        countdownInterval = setInterval(function() {
            warningCountdown--;
            document.getElementById('sessionCountdown').innerText = warningCountdown;
            if (warningCountdown <= 0) {
                clearInterval(countdownInterval);
                // Inactivity limit reached: redirect to login with expired notice
                window.location.href = '/login?expired=true';
            }
        }, 1000);
    }

    function stayLoggedIn() {
        // Send keep-alive request to server with CSRF header
        const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

        fetch('/session/keep-alive', {
            method: 'POST',
            headers: {
                [csrfHeader]: csrfToken
            }
        }).then(response => {
            if (response.ok) {
                clearInterval(countdownInterval);
                modalShown = false;
                inactivityTimer = 0;
                document.getElementById('sessionModal').style.display = 'none';
            } else {
                window.location.href = '/login?expired=true';
            }
        }).catch(() => {
            window.location.href = '/login?expired=true';
        });
    }

    function logoutNow() {
        // Submit logout form
        const logoutForm = document.querySelector('.logout-form');
        if (logoutForm) {
            logoutForm.submit();
        } else {
            window.location.href = '/login?logout=true';
        }
    }
</script>
</c:if>