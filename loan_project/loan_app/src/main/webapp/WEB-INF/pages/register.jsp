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
        <input type="password" name="password" placeholder="Choose a password" required>

        <button type="submit" class="btn" style="margin-top:10px;">Create Customer Account</button>
    </form>

    <div style="text-align:center; margin-top:20px; font-size:13px; color:#718096;">
        Already registered? <a href="/login" style="color:#e67e22; text-decoration:none; font-weight:600;">Sign In</a>
    </div>
</div>