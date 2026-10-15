<%@ include file="header.jsp" %>

<div class="card">
    <h2 style="text-align: center; margin-top: 0; color: #2c3e50;">Account Recovery</h2>
    <p style="text-align: center; color: #718096; font-size: 13px; margin-top: -10px; margin-bottom: 25px;">
        Enter your registered email address to receive password reset instructions.
    </p>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-error">${errorMessage}</div>
    </c:if>

    <c:if test="${not empty infoMessage}">
        <div class="alert alert-info">${infoMessage}</div>
    </c:if>

    <form action="/forgot-password" method="post">
        <!-- CSRF Token -->
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

        <label for="email" style="font-weight: 600; font-size: 13px; color: #4a5568;">Registered Email Address</label>
        <input type="email" id="email" name="email" placeholder="e.g. user@bank.com" required autocomplete="email">

        <button type="submit" class="btn" style="margin-top: 10px;">Send Recovery Instructions</button>
    </form>

    <div style="text-align: center; margin-top: 20px;">
        <a href="/login" style="font-size: 13px; color: #e67e22; text-decoration: none;">Back to Login</a>
    </div>
</div>
