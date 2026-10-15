<%@ include file="header.jsp" %>

<div class="card" style="width: 500px; max-width: 92%;">
    <div style="text-align: center; margin-bottom: 20px;">
        <div style="display: inline-flex; align-items: center; justify-content: center; width: 54px; height: 54px; background: #eef2ff; border-radius: 50%; color: #4338ca; font-size: 26px; margin-bottom: 12px;">
            🔐
        </div>
        <h2 style="margin: 0; color: #1e293b; font-size: 22px; font-weight: 700;">Two-Factor Authentication Setup</h2>
        <p style="color: #64748b; font-size: 13px; margin: 6px 0 0 0;">
            Secure your account with Google Authenticator or any standard TOTP app.
        </p>
    </div>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-error" style="margin-bottom: 18px;">
            ${errorMessage}
        </div>
    </c:if>

    <!-- Expiry Timer Badge -->
    <div style="background: #f1f5f9; padding: 10px 14px; border-radius: 8px; margin-bottom: 20px; font-size: 13px; text-align: center; color: #475569; display: flex; align-items: center; justify-content: center; gap: 8px;">
        <span>⏳ Enrollment session expires in:</span>
        <span id="enrollCountdown" style="font-weight: 700; color: #dc2626; font-family: monospace; font-size: 15px;">05:00</span>
    </div>

    <!-- Step 1: Scan QR Code -->
    <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px; padding: 18px; margin-bottom: 20px; text-align: center;">
        <div style="font-weight: 600; font-size: 14px; color: #334155; margin-bottom: 12px; display: flex; align-items: center; justify-content: center; gap: 6px;">
            <span style="background: #e67e22; color: #fff; border-radius: 50%; width: 20px; height: 20px; display: inline-flex; align-items: center; justify-content: center; font-size: 11px;">1</span>
            Scan this QR code with your Authenticator app
        </div>

        <div style="display: inline-block; padding: 10px; background: #ffffff; border-radius: 10px; box-shadow: 0 4px 12px rgba(0,0,0,0.06); border: 1px solid #e2e8f0;">
            <img src="${qrCodeDataUri}" alt="MFA Provisioning QR Code" width="220" height="220" style="display: block; border-radius: 4px;" id="qrCodeImage" />
        </div>

        <!-- Manual Key Display -->
        <div style="margin-top: 14px; font-size: 12px; color: #64748b;">
            <span>Cannot scan QR code? Enter this key manually:</span>
            <div style="margin-top: 6px; display: flex; align-items: center; justify-content: center; gap: 8px;">
                <code id="secretKeyText" style="background: #ffffff; border: 1px solid #cbd5e1; padding: 6px 12px; border-radius: 6px; font-family: monospace; font-size: 13px; font-weight: 700; letter-spacing: 2px; color: #0f172a;">${secretKey}</code>
                <button type="button" onclick="copySecretKey()" style="background: #f1f5f9; border: 1px solid #cbd5e1; padding: 6px 10px; border-radius: 6px; cursor: pointer; font-size: 12px; color: #334155; font-weight: 600;" id="copyBtn">
                    📋 Copy
                </button>
            </div>
            <div style="font-size: 11px; color: #94a3b8; margin-top: 4px;">
                Account: <strong>${accountEmail}</strong> | Issuer: <strong>${issuer}</strong>
            </div>
        </div>
    </div>

    <!-- Step 2: Verification Form -->
    <div style="margin-bottom: 10px;">
        <div style="font-weight: 600; font-size: 14px; color: #334155; margin-bottom: 12px; display: flex; align-items: center; justify-content: center; gap: 6px;">
            <span style="background: #e67e22; color: #fff; border-radius: 50%; width: 20px; height: 20px; display: inline-flex; align-items: center; justify-content: center; font-size: 11px;">2</span>
            Enter the 6-digit code from your app
        </div>

        <form action="/mfa-enroll" method="post" id="enrollForm">
            <!-- CSRF Token -->
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

            <input type="text" id="code" name="code" maxlength="6" pattern="[0-9]{6}"
                   placeholder="000000" required autofocus autocomplete="one-time-code"
                   inputmode="numeric"
                   style="text-align: center; font-size: 26px; font-weight: 700; letter-spacing: 8px; font-family: monospace; padding: 12px; color: #1e293b;" />

            <button type="submit" class="btn" style="margin-top: 10px; padding: 14px; font-size: 15px;">
                Verify Code & Complete Enrollment
            </button>
        </form>
    </div>

    <div style="text-align: center; margin-top: 18px;">
        <a href="/login" style="font-size: 13px; color: #64748b; text-decoration: none;">Cancel & Return to Login</a>
    </div>
</div>

<script>
    // 5-minute countdown timer
    let enrollSecondsRemaining = 300;
    const countdownEl = document.getElementById('enrollCountdown');

    const enrollInterval = setInterval(function() {
        enrollSecondsRemaining--;
        if (enrollSecondsRemaining <= 0) {
            clearInterval(enrollInterval);
            countdownEl.innerText = "00:00 (Expired)";
            alert("Your 5-minute enrollment session has expired. Please log in again to generate a new setup code.");
            window.location.href = '/login?expired=true';
            return;
        }

        const mins = Math.floor(enrollSecondsRemaining / 60);
        const secs = enrollSecondsRemaining % 60;
        countdownEl.innerText = (mins < 10 ? '0' : '') + mins + ':' + (secs < 10 ? '0' : '') + secs;
    }, 1000);

    function copySecretKey() {
        const rawKey = "${rawSecret}";
        navigator.clipboard.writeText(rawKey).then(function() {
            const copyBtn = document.getElementById('copyBtn');
            copyBtn.innerText = "✓ Copied!";
            copyBtn.style.background = "#dcfce7";
            copyBtn.style.borderColor = "#86efac";
            setTimeout(function() {
                copyBtn.innerText = "📋 Copy";
                copyBtn.style.background = "#f1f5f9";
                copyBtn.style.borderColor = "#cbd5e1";
            }, 2500);
        }).catch(function() {
            alert("Secret key: " + rawKey);
        });
    }
</script>
