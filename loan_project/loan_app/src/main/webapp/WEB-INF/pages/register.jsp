<%@ include file="header.jsp" %>

<div class="card">
    <h2 style="text-align:center; margin-top:0; color:#2c3e50;">Customer Registration</h2>
    <p style="text-align:center; color:#718096; font-size:13px; margin-top:-10px; margin-bottom:20px;">
        Register a new borrower account to apply and track loans.
    </p>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-error">${errorMessage}</div>
    </c:if>

    <form action="/register" method="post">
        <!-- CSRF Token -->
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

        <label style="font-weight:600; font-size:13px; color:#4a5568;">Full Name</label>
        <input type="text" name="name" placeholder="John Doe" required>

        <label style="font-weight:600; font-size:13px; color:#4a5568;">Email Address</label>
        <input type="email" name="email" placeholder="customer@example.com" required>

        <label style="font-weight:600; font-size:13px; color:#4a5568;">Password</label>
        <input type="password" name="password" id="password" placeholder="Choose a strong password" required autocomplete="new-password">

        <div style="background:#f7fafc; border-left:3px solid #e67e22; padding:10px 12px; margin-bottom:15px; font-size:12px; color:#4a5568; border-radius: 4px;">
            <strong>Password Requirements:</strong>
            <ul style="margin:5px 0 0 15px; padding:0; line-height: 1.6;">
                <li>8 to 64 characters long</li>
                <li>At least one uppercase and one lowercase letter</li>
                <li>At least one number (0-9)</li>
                <li>At least one special character (@$!%*?&#^()_+-=...)</li>
            </ul>
        </div>

        <button type="submit" class="btn" style="margin-top:10px;">Create Customer Account</button>
    </form>

    <div style="text-align:center; margin-top:20px; font-size:13px; color:#718096;">
        Already registered? <a href="/login" style="color:#e67e22; text-decoration:none; font-weight:600;">Sign In</a>
    </div>
</div>