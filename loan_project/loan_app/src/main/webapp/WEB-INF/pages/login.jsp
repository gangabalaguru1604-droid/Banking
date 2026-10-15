<%@ include file="header.jsp" %>

<div class="card">
    <h2 style="text-align: center; margin-top: 0; color: #2c3e50;">Staff & Customer Login</h2>
    <p style="text-align: center; color: #718096; font-size: 13px; margin-top: -10px; margin-bottom: 25px;">
        Stage 1: Enter your Bank User ID / Email and Password
    </p>

    <!-- Error Messages (Generic to prevent account enumeration / state disclosure) -->
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-error">${errorMessage}</div>
    </c:if>

    <c:if test="${not empty logoutMessage}">
        <div class="alert alert-success">${logoutMessage}</div>
    </c:if>

    <c:if test="${not empty expiredMessage}">
        <div class="alert alert-info">${expiredMessage}</div>
    </c:if>

    <c:if test="${not empty resetSuccessMessage}">
        <div class="alert alert-success">${resetSuccessMessage}</div>
    </c:if>

    <form action="/login" method="post">
        <!-- CSRF Protection -->
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

        <label for="email" style="font-weight: 600; font-size: 13px; color: #4a5568;">User ID / Email</label>
        <input type="text" id="email" name="email" placeholder="e.g. staff@bank.com" required autocomplete="username">

        <label for="password" style="font-weight: 600; font-size: 13px; color: #4a5568;">Password</label>
        <input type="password" id="password" name="password" placeholder="Enter your password" required autocomplete="current-password">

        <div style="display: flex; justify-content: flex-end; margin-bottom: 15px;">
            <a href="/forgot-password" style="font-size: 13px; color: #e67e22; text-decoration: none;">Forgot Password?</a>
        </div>

        <button type="submit" class="btn">Proceed to Verification</button>
    </form>

    <div style="text-align: center; margin-top: 20px; font-size: 13px; color: #718096;">
        Customer without an account? <a href="/register" style="color: #e67e22; text-decoration: none; font-weight: 600;">Sign Up</a>
    </div>
</div>