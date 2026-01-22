
function isEmail(v) { return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v); }
function isPasswordOk(v) { return typeof v === 'string' && v.length >= 8; }
function isPhoneE164(v) { return /^\+\d{8,15}$/.test(v); }

function startCooldown(btn, secs) {
  btn.disabled = true;
  const baseTxt = btn.dataset.base || btn.textContent;
  let left = secs;
  btn.textContent = baseTxt + ' (' + left + 's)';
  const id = setInterval(() => {
    left--;
    btn.textContent = baseTxt + ' (' + left + 's)';
    if (left <= 0) { clearInterval(id); btn.disabled = false; btn.textContent = baseTxt; }
  }, 1000);
}

function decodeJWT(token) {
    try {
        if (!token) return null;

        const parts = token.split('.');
        if (parts.length !== 3) return null;

        let payload = parts[1];

        while (payload.length % 4) {
            payload += '=';
        }

        payload = payload.replace(/-/g, '+').replace(/_/g, '/');

        const decoded = atob(payload);
        return JSON.parse(decoded);
    } catch (error) {
        console.error('Error decodificando JWT:', error);
        return null;
    }
}

function getCurrentUser() {
    const token = getToken();
    if (!token) return null;
    
    const payload = decodeJWT(token);
    if (!payload) return null;
    
    return {
        id: payload.sub || payload.userId,
        email: payload.email,
        firstName: payload.firstName,
        lastName: payload.lastName,
        fullName: payload.firstName && payload.lastName ? 
                 `${payload.firstName} ${payload.lastName}` : 
                 payload.email,
        roles: payload.roles || []
    };
}
