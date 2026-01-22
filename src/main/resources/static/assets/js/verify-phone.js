
// Función para obtener sessionId de múltiples fuentes
function getSessionId() {
    // 1. Intentar desde localStorage
    let sessionId = localStorage.getItem('sid');

    // 2. Si no está, intentar desde URL
    if (!sessionId || sessionId === 'null' || sessionId === 'undefined') {
        const urlParams = new URLSearchParams(window.location.search);
        sessionId = urlParams.get('sessionId');
        if (sessionId) {
            localStorage.setItem('sid', sessionId);
        }
    }

    // 3. Si aún no está, intentar desde sessionStorage
    if (!sessionId || sessionId === 'null' || sessionId === 'undefined') {
        sessionId = sessionStorage.getItem('onboardingSessionId');
    }

    return sessionId;
}

let sid = getSessionId();

// Eliminado bloque de código hardcoded
if (!sid || sid === 'null' || sid === 'undefined') {
    sessionId = sessionStorage.getItem('onboardingSessionId');
    if (sessionId) sid = sessionId;
}

if (!sid || sid === 'null' || sid === 'undefined') {
    alert("Sesión no encontrada. Serás redirigido al registro.");
    location.href = 'register.html';
}


const formEmail = document.getElementById('formEmail');
const emailCodeInput = document.getElementById('emailCode');
const emailErrorMsg = document.getElementById('emailErrorMsg');
const emailSuccessMsg = document.getElementById('emailSuccessMsg');
const verifyEmailBtn = document.getElementById('verifyEmailBtn');
const verifyEmailText = document.getElementById('verifyEmailText');
const verifyEmailLoader = document.getElementById('verifyEmailLoader');
const resendEmailBtn = document.getElementById('resendEmail');


const formPhone = document.getElementById('formPhone');
const phoneCodeInput = document.getElementById('code');
const phoneErrorMsg = document.getElementById('msg');
const phoneSuccessMsg = document.getElementById('successMsg');
const verifyPhoneBtn = document.getElementById('verifyBtn');
const verifyPhoneText = document.getElementById('verifyText');
const verifyPhoneLoader = document.getElementById('verifyLoader');
const resendPhoneBtn = document.getElementById('resendPhone');


let isEmailVerified = false;
let isPhoneVerified = false;


function showMsg(type, section, message) {
    const errorEl = (section === 'email') ? emailErrorMsg : phoneErrorMsg;
    const successEl = (section === 'email') ? emailSuccessMsg : phoneSuccessMsg;

    errorEl.classList.add('hidden');
    successEl.classList.add('hidden');

    if (type === 'error') {
        errorEl.textContent = message;
        errorEl.classList.remove('hidden');
    } else if (type === 'success') {
        successEl.textContent = message;
        successEl.classList.remove('hidden');
    }
}

function setLoading(section, loading) {
    const btn = (section === 'email') ? verifyEmailBtn : verifyPhoneBtn;
    const text = (section === 'email') ? verifyEmailText : verifyPhoneText;
    const loader = (section === 'email') ? verifyEmailLoader : verifyPhoneLoader;

    btn.disabled = loading;
    text.classList.toggle('hidden', loading);
    loader.classList.toggle('hidden', !loading);
}


function checkAndRedirect() {

    if (isEmailVerified && isPhoneVerified) {


        Swal.fire({
            icon: 'success',
            title: '¡Verificación Completa!',
            text: 'Has verificado tu email y tu teléfono. Serás redirigido.',
            background: '#1e40af',
            color: '#f3f4f6',
            confirmButtonColor: '#10b981',
            confirmButtonText: 'Continuar',
            timer: 2000,
            timerProgressBar: true
        }).then(() => {
            location.href = 'kyc.html';
        });
    }
}


resendEmailBtn.addEventListener('click', async () => {
    showMsg(null, 'email'); // Oculta mensajes
    resendEmailBtn.disabled = true;

    try {
        await api('/api/onboarding/resend-email', {
            method: 'POST',
            body: { sessionId: sid }
        });
        showMsg('success', 'email', '✓ Correo reenviado. Revisa tu bandeja (y spam).');
        startCooldown(resendEmailBtn, 60); // Asume que startCooldown está en util.js
    } catch (e) {
        resendEmailBtn.disabled = false;
        showMsg('error', 'email', e.message || 'Error al reenviar el correo');
    }
});


resendPhoneBtn.addEventListener('click', async () => {
    showMsg(null, 'phone');
    resendPhoneBtn.disabled = true;

    try {
        await api('/api/onboarding/resend-phone', {
            method: 'POST',
            body: { sessionId: sid }
        });
        showMsg('success', 'phone', '✓ Código reenviado correctamente');
        startCooldown(resendPhoneBtn, 60);
    } catch (e) {
        resendPhoneBtn.disabled = false;
        showMsg('error', 'phone', e.message || 'Error al reenviar el código');
    }
});


formEmail.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    showMsg(null, 'email');


    const c_raw = (emailCodeInput.value || '').trim();
    const c_cleaned = c_raw.replace(/[^0-9]/g, '');

    if (!/^\d{6}$/.test(c_cleaned)) {
        return showMsg('error', 'email', 'El código debe tener 6 dígitos');
    }

    setLoading('email', true);

    try {
        // Usar sessionId dinámico
        const requestBody = { sessionId: sid, code: c_cleaned };

        await api('/api/onboarding/verify-email-code', {
            method: 'POST',
            body: requestBody
        });

        showMsg('success', 'email', '✓ Email verificado correctamente.');


        verifyEmailBtn.disabled = true;
        emailCodeInput.disabled = true;
        resendEmailBtn.disabled = true;
        verifyEmailText.textContent = "Verificado";


        isEmailVerified = true;
        checkAndRedirect();


    } catch (e) {
        showMsg('error', 'email', e.message || 'Código incorrecto');
    } finally {
        setLoading('email', false);
    }
});



formPhone.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    showMsg(null, 'phone');


    const c_raw = (phoneCodeInput.value || '').trim();
    const c_cleaned = c_raw.replace(/[^0-9]/g, '');

    if (!/^\d{6}$/.test(c_cleaned)) {
        return showMsg('error', 'phone', 'El código debe tener 6 dígitos');
    }

    setLoading('phone', true);

    try {
        // Usar sessionId dinámico
        const requestBody = { sessionId: sid, code: c_cleaned };

        await api('/api/onboarding/verify-phone', {
            method: 'POST',
            body: requestBody
        });

        showMsg('success', 'phone', '✓ Teléfono verificado correctamente');


        verifyPhoneBtn.disabled = true;
        phoneCodeInput.disabled = true;
        resendPhoneBtn.disabled = true;
        verifyPhoneText.textContent = "Verificado";

        isPhoneVerified = true;

        checkAndRedirect();


    } catch (e) {
        showMsg('error', 'phone', e.message || 'Código incorrecto');
    } finally {
        setLoading('phone', false);
    }
});