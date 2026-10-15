<%@ include file="header.jsp" %>

<div class="card">
    <h2 style="text-align: center; margin-top: 0; color: #2c3e50;">Set New Password</h2>
    <p style="text-align: center; color: #718096; font-size: 13px; margin-top: -10px; margin-bottom: 25px;">
        Choose a strong, unique password for your account.
    </p>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-error">${errorMessage}</div>
    </c:if>

    <form action="/reset-password" method="post">
        <!-- CSRF Token -->
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <input type="hidden" name="token" value="${token}"/>

        <label for="password" style="font-weight: 600; font-size: 13px; color: #4a5568;">New Password</label>
        <input type="password" id="password" name="password" placeholder="Enter new password" required autocomplete="new-password">

        <label for="confirmPassword" style="font-weight: 600; font-size: 13px; color: #4a5568;">Confirm New Password</label>
        <input type="password" id="confirmPassword" name="confirmPassword" placeholder="Confirm new password" required autocomplete="new-password">

        <div style="background:#f7fafc; border-left:3px solid #e67e22; padding:10px 12px; margin-bottom:15px; font-size:12px; color:#4a5568;">
            <strong>Password Requirements:</strong>
            <ul style="margin:5px 0 0 15px; padding:0;">
                <li>8 to 64 characters long</li>
                <li>At least one uppercase and one lowercase letter</li>
                <li>At least one number (0-9)</li>
                <li>At least one special character (@$!%*?&#^()_+-=...)</li>
            </ul>
        </div>

        <button type="submit" class="btn">Update Password</button>
    </form>

    <div style="text-align: center; margin-top: 20px;">
        <a href="/login" style="font-size: 13px; color: #718096; text-decoration: none;">Cancel & Return to Login</a>
    </div>
</div>
