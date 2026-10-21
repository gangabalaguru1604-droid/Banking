<%@ include file="header.jsp" %>

<div style="display:flex; justify-content:space-around; align-items:center; padding:80px 40px; min-height:70vh; background:#FFFFFF;">

    <div style="max-width:520px; animation: slideUp 1s;">
        <h1 style="font-size:48px; font-weight:800; color:#172B4D; margin:0 0 16px; letter-spacing:-0.5px;">Smart Loan Management</h1>

        <p style="color:#555555; font-size:17px; line-height:1.6; margin:0 0 28px;">
            Apply loans, calculate EMI, and track payments easily.
        </p>

        <a href="/register" style="text-decoration:none;">
            <button class="btn" style="width:200px; padding:14px 20px; background-color:#ED7D16; color:#FFFFFF; font-size:16px; font-weight:700; border:none; border-radius:6px; cursor:pointer; box-shadow:0 4px 12px rgba(237,125,22,0.3);">
                Get Started
            </button>
        </a>
    </div>

    <div style="animation: float 3s infinite; max-width:440px; width:100%;">
        <svg viewBox="0 0 340 230" fill="none" xmlns="http://www.w3.org/2000/svg" style="width:100%; height:auto; display:block; filter: drop-shadow(0 15px 35px rgba(0,80,164,0.12));" aria-label="Digital Banking & Loan Management" role="img">
            <defs>
                <linearGradient id="jspPhoneGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stop-color="#0050A4" />
                    <stop offset="100%" stop-color="#002B49" />
                </linearGradient>
                <linearGradient id="jspScreenGrad" x1="0%" y1="0%" x2="0%" y2="100%">
                    <stop offset="0%" stop-color="#FFFFFF" />
                    <stop offset="100%" stop-color="#F8FAFC" />
                </linearGradient>
                <linearGradient id="jspOrangeGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stop-color="#F97316" />
                    <stop offset="100%" stop-color="#ED7D16" />
                </linearGradient>
                <linearGradient id="jspCoinGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stop-color="#FBBF24" />
                    <stop offset="100%" stop-color="#F59E0B" />
                </linearGradient>
            </defs>
            <circle cx="170" cy="115" r="95" fill="#EFF6FF" opacity="0.7"/>
            <circle cx="70" cy="70" r="45" fill="#FFF7ED" opacity="0.6"/>
            <!-- Left: Approved Document Card -->
            <g transform="translate(28, 45)">
                <rect width="112" height="135" rx="10" fill="#FFFFFF" stroke="#E2E8F0" stroke-width="1.5"/>
                <rect x="12" y="14" width="45" height="6" rx="3" fill="#0050A4"/>
                <rect x="12" y="26" width="72" height="4" rx="2" fill="#E2E8F0"/>
                <rect x="12" y="36" width="56" height="4" rx="2" fill="#E2E8F0"/>
                <rect x="12" y="46" width="66" height="4" rx="2" fill="#E2E8F0"/>
                <line x1="12" y1="58" x2="100" y2="58" stroke="#F1F5F9" stroke-width="1.5"/>
                <rect x="12" y="68" width="36" height="4" rx="2" fill="#94A3B8"/>
                <text x="12" y="87" fill="#002B49" font-size="13" font-weight="bold" font-family="system-ui, sans-serif">₹5,00,000</text>
                <rect x="12" y="97" width="70" height="20" rx="10" fill="#ECFDF5" stroke="#A7F3D0" stroke-width="1"/>
                <circle cx="22" cy="107" r="5" fill="#10B981"/>
                <path d="M19.5 107 L21.5 109 L24.5 105" stroke="#FFFFFF" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
                <text x="32" y="111" fill="#065F46" font-size="9" font-weight="bold" font-family="system-ui, sans-serif">APPROVED</text>
            </g>
            <!-- Center-Right: Smartphone with Loan Dashboard -->
            <g transform="translate(155, 15)">
                <rect width="135" height="195" rx="20" fill="url(#jspPhoneGrad)"/>
                <rect x="6" y="6" width="123" height="183" rx="15" fill="url(#jspScreenGrad)"/>
                <rect x="52" y="10" width="31" height="4" rx="2" fill="#CBD5E1"/>
                <rect x="14" y="24" width="22" height="4" rx="2" fill="#0050A4"/>
                <circle cx="115" cy="26" r="3" fill="#ED7D16"/>
                <rect x="14" y="36" width="107" height="45" rx="8" fill="#0050A4"/>
                <text x="22" y="50" fill="#93C5FD" font-size="8" font-family="system-ui, sans-serif">Active Loan Sanction</text>
                <text x="22" y="67" fill="#FFFFFF" font-size="14" font-weight="bold" font-family="system-ui, sans-serif">₹10,00,000</text>
                <rect x="91" y="44" width="23" height="12" rx="4" fill="#ED7D16"/>
                <text x="94" y="53" fill="#FFFFFF" font-size="7" font-weight="bold" font-family="system-ui, sans-serif">10.5%</text>
                <rect x="14" y="90" width="107" height="48" rx="8" fill="#FFFFFF" stroke="#E2E8F0" stroke-width="1"/>
                <text x="22" y="103" fill="#64748B" font-size="8" font-family="system-ui, sans-serif">Repayment Progress</text>
                <text x="94" y="103" fill="#0050A4" font-size="8" font-weight="bold" font-family="system-ui, sans-serif">65%</text>
                <rect x="22" y="110" width="91" height="6" rx="3" fill="#E2E8F0"/>
                <rect x="22" y="110" width="59" height="6" rx="3" fill="url(#jspOrangeGrad)"/>
                <text x="22" y="128" fill="#10B981" font-size="8" font-weight="600" font-family="system-ui, sans-serif">● On Schedule (0 DPD)</text>
                <rect x="14" y="146" width="107" height="26" rx="6" fill="#ED7D16"/>
                <text x="41" y="163" fill="#FFFFFF" font-size="10" font-weight="bold" font-family="system-ui, sans-serif">Pay EMI Online</text>
                <path d="M102 159 L106 159 M104 156 L107 159 L104 162" stroke="#FFFFFF" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
            </g>
            <!-- Stacked Coins -->
            <g transform="translate(125, 148)">
                <ellipse cx="22" cy="38" rx="16" ry="6" fill="#D97706"/>
                <ellipse cx="22" cy="35" rx="16" ry="6" fill="url(#jspCoinGrad)"/>
                <ellipse cx="22" cy="30" rx="16" ry="6" fill="#D97706"/>
                <ellipse cx="22" cy="27" rx="16" ry="6" fill="url(#jspCoinGrad)"/>
                <ellipse cx="22" cy="22" rx="16" ry="6" fill="#D97706"/>
                <ellipse cx="22" cy="19" rx="16" ry="6" fill="url(#jspCoinGrad)"/>
                <text x="18" y="22" fill="#78350F" font-size="10" font-weight="bold" font-family="system-ui, sans-serif">₹</text>
                <ellipse cx="48" cy="40" rx="13" ry="5" fill="#D97706"/>
                <ellipse cx="48" cy="37" rx="13" ry="5" fill="url(#jspCoinGrad)"/>
                <ellipse cx="48" cy="32" rx="13" ry="5" fill="#D97706"/>
                <ellipse cx="48" cy="29" rx="13" ry="5" fill="url(#jspCoinGrad)"/>
                <text x="45" y="32" fill="#78350F" font-size="8" font-weight="bold" font-family="system-ui, sans-serif">₹</text>
            </g>
            <!-- Shield Badge -->
            <g transform="translate(265, 80)">
                <circle cx="20" cy="20" r="18" fill="#FFFFFF" stroke="#0050A4" stroke-width="1.5"/>
                <path d="M20 10 L27 13.5 V19 C27 23.5 24 27.5 20 29 C16 27.5 13 23.5 13 19 V13.5 L20 10 Z" fill="#0050A4"/>
                <path d="M17.5 19 L19.5 21 L23 17" stroke="#FFFFFF" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
            </g>
        </svg>
    </div>

</div>