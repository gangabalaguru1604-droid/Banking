<%@ include file="header.jsp" %>

<div class="card">
    <h2 style="text-align: center; margin-top: 0; color: #2c3e50;">Two-Factor Authentication</h2>
    <p style="text-align: center; color: #718096; font-size: 13px; margin-top: -10px; margin-bottom: 25px;">
        Stage 2: Enter the 6-digit code from your <strong>Authenticator App (TOTP)</strong>
    </p>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-error">
            ${errorMessage}
        </div>
    </c:if>

    <div style="background: #edf2f7; padding: 12px; border-radius: 6px; margin-bottom: 20px; font-size: 13px; text-align: center; color: #4a5568;">
        ⏳ Challenge expires in: <span id="mfaCountdown" style="font-weight: 700; color: #e53e3e;">05:00</span>
    </div>

    <form action="/mfa-verify" method="post">
        <!-- CSRF Token -->
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

        <label for="code" style="font-weight: 600; font-size: 13px; color: #4a5568;">6-Digit Security Code</label>
        <input type="text" id="code" name="code" maxlength="6" pattern="[0-9]{6}"
               placeholder="000000" required autofocus autocomplete="one-time-code"
               style="text-align: center; font-size: 24px; letter-spacing: 6px;">

        <button type="submit" class="btn" style="margin-top: 10px;">Verify & Complete Sign In</button>
    </form>

    <div style="text-align: center; margin-top: 20px;">
        <a href="/login" style="font-size: 13px; color: #718096; text-decoration: none;">Cancel & Return to Login</a>
    </div>
</div>

<script>
    // 5-minute validity countdown
    let mfaSecondsRemaining = 300;
    const countdownEl = document.getElementById('mfaCountdown');

    const mfaInterval = setInterval(function() {
        mfaSecondsRemaining--;
        if (mfaSecondsRemaining <= 0) {
            clearInterval(mfaInterval);
            countdownEl.innerText = "00:00 (Expired)";
            alert("Your 5-minute authentication challenge has expired. Please log in again.");
            window.location.href = '/login?error=true';
            return;
        }

        const mins = Math.floor(mfaSecondsRemaining / 60);
        const secs = mfaSecondsRemaining % 60;
        countdownEl.innerText = (mins < 10 ? '0' : '') + mins + ':' + (secs < 10 ? '0' : '') + secs;
    }, 1000);
</script>
