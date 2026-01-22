/* global BASE, getToken, clearToken, api, Swal */
// BASE, getToken, clearToken, and api are imported from api.js

let current2FASecret = null;
let codeInterval = null;

let userHasActiveCard = false;
let userActiveCardMasked = '';
let userActiveCardHolder = '';

let userEmailCache = '';
let userPhoneCache = '';
let currentUserLimit = 0.00;
let tempNewLimit = 0.00;

function isTokenExpired() {
    const token = getToken();
    if (!token) return true;
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return Date.now() >= payload.exp * 1000;
    } catch (e) { return true; }
}

if (isTokenExpired()) {
    clearToken();
    location.href = 'login.html';
}

function showAlert(icon, title, text) {
    Swal.fire({
        icon: icon, title: title, text: text,
        toast: true, position: 'top-end', showConfirmButton: false,
        timer: 5000, timerProgressBar: true,
        background: '#1e40af', color: '#f3f4f6',
    });
}

function showError(msg) { showAlert('error', 'Error', msg); }
function showSuccess(msg) { showAlert('success', 'Éxito', msg); }
function showInfo(msg) { showAlert('info', 'Información', msg); }

function openModal(id) {
    const modal = document.getElementById(id);
    if (!modal) return;
    modal.classList.add('active');
    modal.classList.remove('opacity-0', 'pointer-events-none');
    const mc = modal.querySelector('.modal-content');
    if (mc) mc.classList.remove('scale-95');
}

function closeModal(id) {
    const modal = document.getElementById(id);
    if (!modal) return;
    const mc = modal.querySelector('.modal-content');
    if (mc) {
        mc.classList.add('scale-95');
        setTimeout(() => {
            modal.classList.add('opacity-0', 'pointer-events-none');
            modal.classList.remove('active');
        }, 150);
    }
}

function setButtonLoading(btn, textEl, loaderEl, loading) {
    if (!btn || !textEl || !loaderEl) {
        console.error('setButtonLoading: elementos inválidos', { btn, textEl, loaderEl });
        return;
    }
    btn.disabled = loading;
    if (loading) {
        textEl.classList.add('hidden');
        loaderEl.classList.remove('hidden');
    } else {
        textEl.classList.remove('hidden');
        loaderEl.classList.add('hidden');
    }
}

document.getElementById('logout').addEventListener('click', () => {
    clearToken();
    location.href = 'login.html';
});

// ========== CARD MANAGEMENT ==========
async function loadCardStatus() {
    try {
        const response = await api('/api/wallet/activar-tarjeta/status', { auth: true });

        // Robust handling: data might be in response.data or response directly
        const cardData = response.data || response;

        userHasActiveCard = cardData.hasActiveCard || false;
        userActiveCardMasked = cardData.maskedNumber || '**** **** **** ----';
        userActiveCardHolder = cardData.holderName || 'NOMBRE NO DISPONIBLE';

        updateCardView();
    } catch (err) {
        userHasActiveCard = false;
        updateCardView();
    }
}

function updateCardView() {
    const cardEnabledSection = document.getElementById('cardEnabled');
    const cardDisabledSection = document.getElementById('cardDisabled');
    if (userHasActiveCard) {
        cardEnabledSection.classList.remove('hidden');
        cardDisabledSection.classList.add('hidden');
        document.getElementById('cardMaskedNumber').textContent = userActiveCardMasked;
        document.getElementById('cardHolderName').textContent = userActiveCardHolder;
    } else {
        cardEnabledSection.classList.add('hidden');
        cardDisabledSection.classList.remove('hidden');
    }
}

document.getElementById('btnDeactivateCard').addEventListener('click', () => {
    Swal.fire({
        title: '¿Desactivar tarjeta?',
        text: 'Tu tarjeta se desvinculará y deberás activar una nueva para depositar. ¿Deseas continuar?',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: 'Sí, desactivar',
        cancelButtonText: 'Cancelar',
        background: '#1e40af',
        color: '#f3f4f6',
        confirmButtonColor: '#dc2626',
        cancelButtonColor: '#4b5563'
    }).then(async (result) => {
        if (result.isConfirmed) {
            showInfo('Desactivando tarjeta...');
            try {
                await api('/api/wallet/desactivar-tarjeta', { method: 'POST', auth: true });
                showSuccess('Tarjeta desactivada exitosamente.');
                userHasActiveCard = false;
                updateCardView();
            } catch (err) {
                showError(`Error al desactivar: ${err.message}`);
            }
        }
    });
});

// ========== 2FA MANAGEMENT ==========
async function load2FAStatus() {
    try {
        const response = await api('/api/auth/2fa/status', { auth: true });
        const status = response.data;
        if (status.enabled) {
            document.getElementById('2faEnabled').classList.remove('hidden');
            document.getElementById('2faDisabled').classList.add('hidden');
            document.getElementById('2faStatusBadge').className = 'badge-enabled px-4 py-2 rounded-full font-semibold text-white flex items-center gap-2';
            document.getElementById('2faStatusBadge').innerHTML = '<i class="bi bi-check-circle"></i> Activado';
            document.getElementById('securityLevel').textContent = 'Alta';
        } else {
            document.getElementById('2faEnabled').classList.add('hidden');
            document.getElementById('2faDisabled').classList.remove('hidden');
            document.getElementById('2faStatusBadge').className = 'badge-disabled px-4 py-2 rounded-full font-semibold text-white flex items-center gap-2';
            document.getElementById('2faStatusBadge').innerHTML = '<i class="bi bi-x-circle"></i> Desactivado';
            document.getElementById('securityLevel').textContent = 'Básica';
        }
    } catch (err) {
        console.error('Error al cargar estado 2FA:', err);
    }
}

document.getElementById('btnEnable2FA').addEventListener('click', async () => {
    openModal('modalSetup2FA');
    document.getElementById('setupCodeContainer').innerHTML = '<div class="spinner-large"></div>';
    try {
        const response = await api('/api/auth/2fa/generate', { method: 'POST', auth: true });
        const setup = response.data;
        current2FASecret = setup.secret;
        document.getElementById('setupCodeContainer').innerHTML = `
            <div class="text-center">
                <div class="bg-gradient-to-br from-blue-500 to-blue-700 p-8 rounded-2xl shadow-2xl inline-block">
                    <div class="text-sm text-blue-200 mb-2 uppercase tracking-wide">Tu código de activación</div>
                    <div class="text-6xl font-bold text-white tracking-widest font-mono select-all" style="letter-spacing: 0.3em;">
                        ${setup.secret}
                    </div>
                    <div class="text-xs text-blue-200 mt-3 flex items-center justify-center gap-1">
                        <i class="bi bi-clock"></i> Válido por 5 minutos
                    </div>
                </div>
                <button class="mt-4 bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg text-sm transition flex items-center gap-2 mx-auto" onclick="navigator.clipboard.writeText('${setup.secret}'); showSuccess('Código copiado al portapapeles');">
                    <i class="bi bi-clipboard"></i> Copiar código
                </button>
            </div>
        `;
    } catch (err) {
        showError('Error al generar código 2FA: ' + err.message);
        closeModal('modalSetup2FA');
    }
});

document.getElementById('btnNext2FA').addEventListener('click', () => {
    closeModal('modalSetup2FA');
    openModal('modalVerify2FA');
    document.getElementById('verify2FACode').value = '';
});

document.getElementById('formVerify2FA').addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = document.getElementById('btnVerify2FASubmit');
    const icon = btn.querySelector('i');

    btn.disabled = true;
    if (icon) icon.className = 'spinner';

    try {
        const code = document.getElementById('verify2FACode').value;
        const result = await api('/api/auth/2fa/verify', {
            method: 'POST', auth: true, body: { code }
        });
        if (result.success) {
            showSuccess('¡2FA activado correctamente!');
            closeModal('modalVerify2FA');
            await load2FAStatus();
        } else {
            showError('Código incorrecto. Por favor, inténtalo de nuevo.');
        }
    } catch (err) {
        showError('Error al verificar código: ' + err.message);
    } finally {
        btn.disabled = false;
        if (icon) icon.className = 'bi bi-check-circle';
    }
});

document.getElementById('btnDisable2FA').addEventListener('click', () => {
    openModal('modalDisable2FA');
    document.getElementById('disable2FACode').value = '';
});

document.getElementById('formDisable2FA').addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = document.getElementById('btnDisable2FASubmit');
    const icon = btn.querySelector('i');

    btn.disabled = true;
    if (icon) icon.className = 'spinner';

    try {
        const code = document.getElementById('disable2FACode').value;
        const result = await api('/api/auth/2fa/disable', {
            method: 'POST', auth: true, body: { code }
        });
        if (result.success) {
            showSuccess('2FA desactivado correctamente');
            closeModal('modalDisable2FA');
            await load2FAStatus();
            if (codeInterval) { clearInterval(codeInterval); codeInterval = null; }
        } else {
            showError('Código incorrecto');
        }
    } catch (err) {
        showError('Error al desactivar 2FA: ' + err.message);
    } finally {
        btn.disabled = false;
        if (icon) icon.className = 'bi bi-shield-x';
    }
});

document.getElementById('btnShow2FACode').addEventListener('click', async function () {
    const btn = this;
    const codeDisplay = document.getElementById('current2FACode');
    if (codeDisplay.textContent !== '••••••') {
        codeDisplay.textContent = '••••••';
        btn.innerHTML = '<i class="bi bi-eye"></i> Mostrar código';
        btn.classList.remove('bg-gray-600', 'hover:bg-gray-700');
        btn.classList.add('bg-purple-600', 'hover:bg-purple-700');
        if (codeInterval) { clearInterval(codeInterval); codeInterval = null; }
        return;
    }
    try {
        const response = await api('/api/auth/2fa/current-code', { auth: true });
        const data = response.data;
        codeDisplay.textContent = data.code;
        btn.innerHTML = '<i class="bi bi-eye-slash"></i> Ocultar código';
        btn.classList.remove('bg-purple-600', 'hover:bg-purple-700');
        btn.classList.add('bg-gray-600', 'hover:bg-gray-700');
        if (codeInterval) clearInterval(codeInterval);
        codeInterval = setInterval(async () => {
            try {
                const newResponse = await api('/api/auth/2fa/current-code', { auth: true });
                const newData = newResponse.data;
                codeDisplay.textContent = newData.code;
                codeDisplay.classList.add('animate-pulse');
                setTimeout(() => { codeDisplay.classList.remove('animate-pulse'); }, 1000);
            } catch (err) {
                console.error('Error al actualizar código:', err);
                clearInterval(codeInterval);
                codeInterval = null;
            }
        }, 300000); // 5 minutos
    } catch (err) {
        showError('Error al obtener código: ' + err.message);
    }
});

document.getElementById('verify2FACode').addEventListener('input', (e) => { e.target.value = e.target.value.replace(/\D/g, ''); });
document.getElementById('disable2FACode').addEventListener('input', (e) => { e.target.value = e.target.value.replace(/\D/g, ''); });
document.getElementById('changeLimitSmsCode').addEventListener('input', (e) => { e.target.value = e.target.value.replace(/\D/g, ''); });

// ========== DELETE ACCOUNT ==========
const modalDeleteAccount = document.getElementById('modalDeleteAccount');
const deleteStep1 = document.getElementById('deleteStep1');
const deleteStep2 = document.getElementById('deleteStep2');
const deleteConfirmText = document.getElementById('deleteConfirmText');
const btnDeleteGoToStep2 = document.getElementById('btnDeleteGoToStep2');
const formDeleteAccount = document.getElementById('formDeleteAccount');
const deletePassword = document.getElementById('deletePassword');
const deleteSmsCode = document.getElementById('deleteSmsCode');
const btnRequestSmsCode = document.getElementById('btnRequestSmsCode');
const btnSmsText = document.getElementById('btnSmsText');
const btnSmsLoader = document.getElementById('btnSmsLoader');
const btnDeleteConfirmFinal = document.getElementById('btnDeleteConfirmFinal');
const btnDeleteText = document.getElementById('btnDeleteText');
const btnDeleteLoader = document.getElementById('btnDeleteLoader');

deleteConfirmText.addEventListener('input', () => {
    if (deleteConfirmText.value === 'ELIMINAR') {
        btnDeleteGoToStep2.disabled = false;
        btnDeleteGoToStep2.classList.remove('opacity-50', 'cursor-not-allowed');
    } else {
        btnDeleteGoToStep2.disabled = true;
        btnDeleteGoToStep2.classList.add('opacity-50', 'cursor-not-allowed');
    }
});

document.getElementById('btnShowDeleteModal').addEventListener('click', () => {
    deleteStep1.classList.remove('hidden');
    deleteStep2.classList.add('hidden');
    deleteConfirmText.value = '';
    formDeleteAccount.reset();
    btnDeleteGoToStep2.disabled = true;
    btnDeleteGoToStep2.classList.add('opacity-50', 'cursor-not-allowed');
    deleteSmsCode.disabled = true;
    btnDeleteConfirmFinal.disabled = true;
    btnDeleteConfirmFinal.classList.add('opacity-50', 'cursor-not-allowed');
    setButtonLoading(btnRequestSmsCode, btnSmsText, btnSmsLoader, false);
    setButtonLoading(btnDeleteConfirmFinal, btnDeleteText, btnDeleteLoader, false);
    openModal('modalDeleteAccount');
});

btnDeleteGoToStep2.addEventListener('click', () => {
    deleteStep1.classList.add('hidden');
    deleteStep2.classList.remove('hidden');
});

btnRequestSmsCode.addEventListener('click', async () => {
    const password = deletePassword.value;
    if (!password) { showError("Por favor, ingresa tu contraseña actual."); return; }
    setButtonLoading(btnRequestSmsCode, btnSmsText, btnSmsLoader, true);
    try {
        await api('/api/auth/request-deletion-code', { method: 'POST', auth: true, body: { password: password } });
        showSuccess("Código SMS enviado a tu teléfono.");
        deleteSmsCode.disabled = false;
        btnDeleteConfirmFinal.disabled = false;
        btnDeleteConfirmFinal.classList.remove('opacity-50', 'cursor-not-allowed');
    } catch (err) {
        showError(err.message || "Error al enviar el código");
        setButtonLoading(btnRequestSmsCode, btnSmsText, btnSmsLoader, false);
    }
});

formDeleteAccount.addEventListener('submit', async (e) => {
    e.preventDefault();
    const password = deletePassword.value;
    const code = deleteSmsCode.value;
    if (!password || !code || !/^\d{6}$/.test(code)) { showError("Contraseña y código SMS (6 dígitos) son requeridos."); return; }
    setButtonLoading(btnDeleteConfirmFinal, btnDeleteText, btnDeleteLoader, true);
    try {
        await api('/api/auth/delete-account', { method: 'POST', auth: true, body: { password: password, code: code } });
        await Swal.fire({
            icon: 'success', title: 'Cuenta Eliminada',
            text: 'Tu cuenta ha sido eliminada permanentemente. Serás redirigido.',
            background: '#1e40af', color: '#f3f4f6', confirmButtonColor: '#10b981'
        });
        clearToken();
        location.href = 'login.html';
    } catch (err) {
        showError(err.message || "Error al eliminar la cuenta");
        setButtonLoading(btnDeleteConfirmFinal, btnDeleteText, btnDeleteLoader, false);
    }
});

// ========== CHANGE EMAIL ==========
const btnShowChangeEmail = document.getElementById('btnShowChangeEmail');
const modalChangeEmail = document.getElementById('modalChangeEmail');

const changeEmailStep1 = document.getElementById('changeEmailStep1');
const formEmailPass = document.getElementById('formEmailPass');
const changeEmailPassword = document.getElementById('changeEmailPassword');
const btnEmailPassVerify = document.getElementById('btnEmailPassVerify');
const btnEmailPassText = document.getElementById('btnEmailPassText');
const btnEmailPassLoader = document.getElementById('btnEmailPassLoader');

const changeEmailStep2 = document.getElementById('changeEmailStep2');
const changeEmailPhoneMask = document.getElementById('changeEmailPhoneMask');
const formEmailSms = document.getElementById('formEmailSms');
const changeEmailSmsCode = document.getElementById('changeEmailSmsCode');
const btnEmailSmsVerify = document.getElementById('btnEmailSmsVerify');
const btnEmailSmsText = document.getElementById('btnEmailSmsText');
const btnEmailSmsLoader = document.getElementById('btnEmailSmsLoader');

const changeEmailStep3 = document.getElementById('changeEmailStep3');
const formEmailNew = document.getElementById('formEmailNew');
const changeEmailNewEmail = document.getElementById('changeEmailNewEmail');
const btnEmailNewVerify = document.getElementById('btnEmailNewVerify');
const btnEmailNewText = document.getElementById('btnEmailNewText');
const btnEmailNewLoader = document.getElementById('btnEmailNewLoader');

const changeEmailStep4 = document.getElementById('changeEmailStep4');
const changeEmailNewEmailMask = document.getElementById('changeEmailNewEmailMask');
const formEmailConfirm = document.getElementById('formEmailConfirm');
const changeEmailFinalCode = document.getElementById('changeEmailFinalCode');
const btnEmailFinalVerify = document.getElementById('btnEmailFinalVerify');
const btnEmailFinalText = document.getElementById('btnEmailFinalText');
const btnEmailFinalLoader = document.getElementById('btnEmailFinalLoader');

function resetChangeEmailModal() {
    formEmailPass.reset(); formEmailSms.reset(); formEmailNew.reset(); formEmailConfirm.reset();
    changeEmailStep1.classList.remove('hidden');
    changeEmailStep2.classList.add('hidden');
    changeEmailStep3.classList.add('hidden');
    changeEmailStep4.classList.add('hidden');
    setButtonLoading(btnEmailPassVerify, btnEmailPassText, btnEmailPassLoader, false);
    setButtonLoading(btnEmailSmsVerify, btnEmailSmsText, btnEmailSmsLoader, false);
    setButtonLoading(btnEmailNewVerify, btnEmailNewText, btnEmailNewLoader, false);
    setButtonLoading(btnEmailFinalVerify, btnEmailFinalText, btnEmailFinalLoader, false);
}

btnShowChangeEmail.addEventListener('click', () => {
    resetChangeEmailModal();
    openModal('modalChangeEmail');
});

formEmailPass.addEventListener('submit', async (e) => {
    e.preventDefault();
    setButtonLoading(btnEmailPassVerify, btnEmailPassText, btnEmailPassLoader, true);
    try {
        await api('/api/auth/change-email/request', {
            method: 'POST', auth: true, body: { password: changeEmailPassword.value }
        });
        changeEmailPhoneMask.textContent = userPhoneCache.replace(/(\+\d{2})\d+(\d{3})/, '$1...$2');
        changeEmailStep1.classList.add('hidden');
        changeEmailStep2.classList.remove('hidden');
    } catch (err) {
        showError(err.message || "Error al verificar contraseña");
    } finally {
        setButtonLoading(btnEmailPassVerify, btnEmailPassText, btnEmailPassLoader, false);
    }
});

formEmailSms.addEventListener('submit', async (e) => {
    e.preventDefault();
    setButtonLoading(btnEmailSmsVerify, btnEmailSmsText, btnEmailSmsLoader, true);
    try {
        await api('/api/auth/change-email/verify-phone', {
            method: 'POST', auth: true, body: { code: changeEmailSmsCode.value }
        });
        changeEmailStep2.classList.add('hidden');
        changeEmailStep3.classList.remove('hidden');
    } catch (err) {
        showError(err.message || "Código SMS incorrecto");
    } finally {
        setButtonLoading(btnEmailSmsVerify, btnEmailSmsText, btnEmailSmsLoader, false);
    }
});

formEmailNew.addEventListener('submit', async (e) => {
    e.preventDefault();
    setButtonLoading(btnEmailNewVerify, btnEmailNewText, btnEmailNewLoader, true);
    try {
        const newEmail = changeEmailNewEmail.value;
        await api('/api/auth/change-email/send-new-email', {
            method: 'POST', auth: true, body: { newEmail: newEmail }
        });
        changeEmailNewEmailMask.textContent = newEmail;
        changeEmailStep3.classList.add('hidden');
        changeEmailStep4.classList.remove('hidden');
    } catch (err) {
        showError(err.message || "Error al enviar código al nuevo email");
    } finally {
        setButtonLoading(btnEmailNewVerify, btnEmailNewText, btnEmailNewLoader, false);
    }
});

formEmailConfirm.addEventListener('submit', async (e) => {
    e.preventDefault();
    setButtonLoading(btnEmailFinalVerify, btnEmailFinalText, btnEmailFinalLoader, true);
    try {
        await api('/api/auth/change-email/confirm-new-email', {
            method: 'POST', auth: true, body: { code: changeEmailFinalCode.value }
        });
        showSuccess("¡Email actualizado exitosamente!");
        closeModal('modalChangeEmail');
        userEmailCache = changeEmailNewEmail.value;
        document.getElementById('userEmail').textContent = userEmailCache;
    } catch (err) {
        showError(err.message || "Código final incorrecto");
    } finally {
        setButtonLoading(btnEmailFinalVerify, btnEmailFinalText, btnEmailFinalLoader, false);
    }
});

// ========== CHANGE PHONE ==========
const btnShowChangePhone = document.getElementById('btnShowChangePhone');
const modalChangePhone = document.getElementById('modalChangePhone');

const changePhoneStep1 = document.getElementById('changePhoneStep1');
const formPhonePass = document.getElementById('formPhonePass');
const changePhonePassword = document.getElementById('changePhonePassword');
const btnPhonePassVerify = document.getElementById('btnPhonePassVerify');
const btnPhonePassText = document.getElementById('btnPhonePassText');
const btnPhonePassLoader = document.getElementById('btnPhonePassLoader');

const changePhoneStep2 = document.getElementById('changePhoneStep2');
const changePhoneEmailMask = document.getElementById('changePhoneEmailMask');
const formPhoneEmail = document.getElementById('formPhoneEmail');
const changePhoneEmailCode = document.getElementById('changePhoneEmailCode');
const btnPhoneEmailVerify = document.getElementById('btnPhoneEmailVerify');
const btnPhoneEmailText = document.getElementById('btnPhoneEmailText');
const btnPhoneEmailLoader = document.getElementById('btnPhoneEmailLoader');

const changePhoneStep3 = document.getElementById('changePhoneStep3');
const formPhoneNew = document.getElementById('formPhoneNew');
const changePhoneNewPhone = document.getElementById('changePhoneNewPhone');
const btnPhoneNewVerify = document.getElementById('btnPhoneNewVerify');
const btnPhoneNewText = document.getElementById('btnPhoneNewText');
const btnPhoneNewLoader = document.getElementById('btnPhoneNewLoader');

const changePhoneStep4 = document.getElementById('changePhoneStep4');
const changePhoneNewPhoneMask = document.getElementById('changePhoneNewPhoneMask');
const formPhoneConfirm = document.getElementById('formPhoneConfirm');
const changePhoneFinalCode = document.getElementById('changePhoneFinalCode');
const btnPhoneFinalVerify = document.getElementById('btnPhoneFinalVerify');
const btnPhoneFinalText = document.getElementById('btnPhoneFinalText');
const btnPhoneFinalLoader = document.getElementById('btnPhoneFinalLoader');

function resetChangePhoneModal() {
    formPhonePass.reset(); formPhoneEmail.reset(); formPhoneNew.reset(); formPhoneConfirm.reset();
    changePhoneStep1.classList.remove('hidden');
    changePhoneStep2.classList.add('hidden');
    changePhoneStep3.classList.add('hidden');
    changePhoneStep4.classList.add('hidden');
    setButtonLoading(btnPhonePassVerify, btnPhonePassText, btnPhonePassLoader, false);
    setButtonLoading(btnPhoneEmailVerify, btnPhoneEmailText, btnPhoneEmailLoader, false);
    setButtonLoading(btnPhoneNewVerify, btnPhoneNewText, btnPhoneNewLoader, false);
    setButtonLoading(btnPhoneFinalVerify, btnPhoneFinalText, btnPhoneFinalLoader, false);
}

btnShowChangePhone.addEventListener('click', () => {
    resetChangePhoneModal();
    openModal('modalChangePhone');
});

formPhonePass.addEventListener('submit', async (e) => {
    e.preventDefault();
    setButtonLoading(btnPhonePassVerify, btnPhonePassText, btnPhonePassLoader, true);
    try {
        await api('/api/auth/change-phone/request', {
            method: 'POST', auth: true, body: { password: changePhonePassword.value }
        });
        changePhoneEmailMask.textContent = userEmailCache.replace(/(.{2}).*(@.*)/, '$1***$2');
        changePhoneStep1.classList.add('hidden');
        changePhoneStep2.classList.remove('hidden');
    } catch (err) {
        showError(err.message || "Error al verificar contraseña");
    } finally {
        setButtonLoading(btnPhonePassVerify, btnPhonePassText, btnPhonePassLoader, false);
    }
});

formPhoneEmail.addEventListener('submit', async (e) => {
    e.preventDefault();
    setButtonLoading(btnPhoneEmailVerify, btnPhoneEmailText, btnPhoneEmailLoader, true);
    try {
        await api('/api/auth/change-phone/verify-email', {
            method: 'POST', auth: true, body: { code: changePhoneEmailCode.value }
        });
        changePhoneStep2.classList.add('hidden');
        changePhoneStep3.classList.remove('hidden');
    } catch (err) {
        showError(err.message || "Código de email incorrecto");
    } finally {
        setButtonLoading(btnPhoneEmailVerify, btnPhoneEmailText, btnPhoneEmailLoader, false);
    }
});

formPhoneNew.addEventListener('submit', async (e) => {
    e.preventDefault();
    setButtonLoading(btnPhoneNewVerify, btnPhoneNewText, btnPhoneNewLoader, true);
    try {
        const newPhone = changePhoneNewPhone.value;
        await api('/api/auth/change-phone/send-new-phone', {
            method: 'POST', auth: true, body: { newPhone: newPhone }
        });
        changePhoneNewPhoneMask.textContent = newPhone.replace(/(\+\d{2})\d+(\d{3})/, '$1...$2');
        changePhoneStep3.classList.add('hidden');
        changePhoneStep4.classList.remove('hidden');
    } catch (err) {
        showError(err.message || "Error al enviar código al nuevo teléfono");
    } finally {
        setButtonLoading(btnPhoneNewVerify, btnPhoneNewText, btnPhoneNewLoader, false);
    }
});

formPhoneConfirm.addEventListener('submit', async (e) => {
    e.preventDefault();
    setButtonLoading(btnPhoneFinalVerify, btnPhoneFinalText, btnPhoneFinalLoader, true);
    try {
        await api('/api/auth/change-phone/confirm-new-phone', {
            method: 'POST', auth: true, body: { code: changePhoneFinalCode.value }
        });
        showSuccess("¡Teléfono actualizado exitosamente!");
        closeModal('modalChangePhone');
        userPhoneCache = changePhoneNewPhone.value;
        document.getElementById('userPhone').textContent = userPhoneCache;
    } catch (err) {
        showError(err.message || "Código final incorrecto");
    } finally {
        setButtonLoading(btnPhoneFinalVerify, btnPhoneFinalText, btnPhoneFinalLoader, false);
    }
});

// ========== CHANGE LIMIT ==========
const btnShowChangeLimit = document.getElementById('btnShowChangeLimit');
const modalChangeLimit = document.getElementById('modalChangeLimit');

const changeLimitStep1 = document.getElementById('changeLimitStep1');
const formLimitRequest = document.getElementById('formLimitRequest');
const modalCurrentLimitDisplay = document.getElementById('modalCurrentLimitDisplay');
const changeLimitNewAmount = document.getElementById('changeLimitNewAmount');
const btnLimitRequestSms = document.getElementById('btnLimitRequestSms');
const btnLimitRequestText = document.getElementById('btnLimitRequestText');
const btnLimitRequestLoader = document.getElementById('btnLimitRequestLoader');

const changeLimitStep2 = document.getElementById('changeLimitStep2');
const formLimitConfirm = document.getElementById('formLimitConfirm');
const changeLimitPhoneMask = document.getElementById('changeLimitPhoneMask');
const changeLimitNewAmountDisplay = document.getElementById('changeLimitNewAmountDisplay');
const changeLimitSmsCode = document.getElementById('changeLimitSmsCode');
const btnLimitConfirm = document.getElementById('btnLimitConfirm');
const btnLimitConfirmText = document.getElementById('btnLimitConfirmText');
const btnLimitConfirmLoader = document.getElementById('btnLimitConfirmLoader');

function resetChangeLimitModal() {
    formLimitRequest.reset();
    formLimitConfirm.reset();
    changeLimitStep1.classList.remove('hidden');
    changeLimitStep2.classList.add('hidden');
    setButtonLoading(btnLimitRequestSms, btnLimitRequestText, btnLimitRequestLoader, false);
    setButtonLoading(btnLimitConfirm, btnLimitConfirmText, btnLimitConfirmLoader, false);
    modalCurrentLimitDisplay.textContent = `S/ ${parseFloat(currentUserLimit).toFixed(2)}`;
}

btnShowChangeLimit.addEventListener('click', () => {
    resetChangeLimitModal();
    openModal('modalChangeLimit');
});

formLimitRequest.addEventListener('submit', async (e) => {
    e.preventDefault();
    const newLimitRaw = changeLimitNewAmount.value;
    const newLimit = parseFloat(newLimitRaw);

    if (isNaN(newLimit) || newLimit <= 0) {
        showError("Ingresa un monto válido para el límite.");
        return;
    }

    if (newLimit < 500) {
        showError("El límite mínimo es de S/ 500.00");
        return;
    }

    if (newLimit > 2000) {
        showError("El límite máximo es de S/ 2,000.00");
        return;
    }

    tempNewLimit = newLimit;

    setButtonLoading(btnLimitRequestSms, btnLimitRequestText, btnLimitRequestLoader, true);
    try {
        await api('/api/auth/change-limit/request', {
            method: 'POST', auth: true, body: { newLimit: tempNewLimit }
        });

        changeLimitPhoneMask.textContent = userPhoneCache.replace(/(\+\d{2})\d+(\d{3})/, '$1...$2');
        changeLimitNewAmountDisplay.textContent = `S/ ${tempNewLimit.toFixed(2)}`;

        changeLimitStep1.classList.add('hidden');
        changeLimitStep2.classList.remove('hidden');

    } catch (err) {
        if (err.message.includes('24 horas')) {
            Swal.fire({
                icon: 'warning',
                title: 'Debes esperar 24 horas',
                html: err.message.replace('Debes esperar 24 horas entre cambios de límite. ', ''),
                background: '#1e40af',
                color: '#f3f4f6',
                confirmButtonColor: '#3b82f6'
            });
        } else {
            showError(err.message || "Error al solicitar el cambio de límite");
        }
    } finally {
        setButtonLoading(btnLimitRequestSms, btnLimitRequestText, btnLimitRequestLoader, false);
    }
});

formLimitConfirm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const code = changeLimitSmsCode.value;
    if (!/^\d{6}$/.test(code)) {
        showError("Ingresa un código SMS de 6 dígitos.");
        return;
    }

    setButtonLoading(btnLimitConfirm, btnLimitConfirmText, btnLimitConfirmLoader, true);
    try {
        await api('/api/auth/change-limit/confirm', {
            method: 'POST', auth: true, body: { code: code }
        });

        closeModal('modalChangeLimit');

        // Reload user data to get updated limit
        showInfo('Actualizando información...');
        const updatedResponse = await api('/api/auth/me', { auth: true });
        const updatedUser = updatedResponse.data;

        updateLimitDisplay(updatedUser);

        showSuccess(`¡Límite actualizado a S/ ${currentUserLimit.toFixed(2)}!`);
        console.log('[Profile] Límite actualizado:', currentUserLimit);

    } catch (err) {
        showError(err.message || "Código SMS incorrecto");
    } finally {
        setButtonLoading(btnLimitConfirm, btnLimitConfirmText, btnLimitConfirmLoader, false);
    }
});

// ========== UPDATE LIMIT DISPLAY ==========
function updateLimitDisplay(user) {
    const limit = user.dailyTransactionLimit || 1000.00;
    const volume = user.totalDailyVolumeInPen || 0.00;
    const remaining = Math.max(0, limit - volume);
    // Calcular porcentaje limitado a 100%
    const percentage = limit > 0 ? Math.min(100, (volume / limit) * 100) : 0;

    currentUserLimit = limit;

    // Actualizar Textos
    const limitEl = document.getElementById('currentLimitDisplay');
    const volumeEl = document.getElementById('dailyVolumeDisplay');
    const remainingEl = document.getElementById('remainingLimitText');
    const percentEl = document.getElementById('limitPercentage');
    const progressBar = document.getElementById('limitProgressBar');

    if (limitEl) limitEl.textContent = `S/ ${parseFloat(limit).toFixed(2)}`;
    if (volumeEl) volumeEl.textContent = `Uso: S/ ${parseFloat(volume).toFixed(2)}`;
    if (remainingEl) remainingEl.textContent = `Disponible: S/ ${parseFloat(remaining).toFixed(2)}`;
    if (percentEl) percentEl.textContent = `${percentage.toFixed(1)}%`;

    // Actualizar Barra
    if (progressBar) {
        progressBar.style.width = `${percentage}%`;

        // Reset classes
        progressBar.className = 'h-2 rounded-full transition-all duration-500';

        // Color logic (Solid colors for cleaner look)
        if (percentage >= 90) {
            progressBar.classList.add('bg-red-600');
        } else if (percentage >= 70) {
            progressBar.classList.add('bg-yellow-500');
        } else {
            progressBar.classList.add('bg-blue-600');
        }
    }
    console.log('[Profile] UI de límites actualizada:', { limit, volume, percentage });
}

// ========== INITIALIZE PAGE ==========
(async () => {
    try {
        // Load user data - IMPORTANT: Extract from response.data for Spring Boot
        const response = await api('/api/auth/me', { auth: true });
        const user = response.data;

        if (user.firstName && user.lastName) {
            document.getElementById('headerName').textContent = `${user.firstName} ${user.lastName}`;
        }
        document.getElementById('userName').textContent = user.firstName || '-';
        document.getElementById('userLastName').textContent = user.lastName || '-';

        userEmailCache = user.email || '-';
        userPhoneCache = user.phone || 'No registrado';
        document.getElementById('userEmail').textContent = userEmailCache;
        document.getElementById('userPhone').textContent = userPhoneCache;

        updateLimitDisplay(user);

        if (user.firstName) {
            const initial = user.firstName.charAt(0).toUpperCase();
            document.getElementById('userAvatar').innerHTML = initial;
        }

        if (user.createdAt) {
            try {
                let date;

                if (Array.isArray(user.createdAt)) {
                    const [year, month, day] = user.createdAt;
                    date = new Date(year, month - 1, day);
                } else if (typeof user.createdAt === 'string') {
                    const isoString = user.createdAt.replace(' ', 'T');
                    date = new Date(isoString);
                } else if (typeof user.createdAt === 'number') {
                    date = new Date(user.createdAt);
                } else if (typeof user.createdAt === 'object' && user.createdAt.year) {
                    date = new Date(user.createdAt.year, user.createdAt.monthValue - 1, user.createdAt.dayOfMonth);
                } else {
                    console.error('Formato de createdAt no reconocido:', user.createdAt);
                    document.getElementById('accountCreated').textContent = 'N/A';
                    throw new Error('Formato no reconocido');
                }

                if (!isNaN(date.getTime())) {
                    const meses = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun',
                        'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
                    const mes = meses[date.getMonth()];
                    const año = date.getFullYear();

                    document.getElementById('accountCreated').textContent = `${mes} ${año}`;
                } else {
                    console.error('Fecha inválida después de conversión');
                    document.getElementById('accountCreated').textContent = 'N/A';
                }
            } catch (error) {
                console.error('Error al procesar fecha:', error);
                document.getElementById('accountCreated').textContent = 'N/A';
            }
        } else {
            console.warn('user.createdAt no existe');
            document.getElementById('accountCreated').textContent = 'N/A';
        }

        await loadCardStatus();
        await load2FAStatus();

    } catch (err) {
        console.error('Error al cargar perfil:', err);
        showError('Error al cargar el perfil. Redirigiendo...');
        setTimeout(() => {
            clearToken();
            location.href = 'login.html';
        }, 2000);
    }
})();
