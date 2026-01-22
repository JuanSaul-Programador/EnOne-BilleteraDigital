
function isTokenExpired() {
    const token = getToken();
    if (!token) return true;
    try {
        const parts = token.split('.');
        if (parts.length !== 3) return true;

        // Decodificar con padding correcto
        const base64Url = parts[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const paddedBase64 = base64 + '='.repeat((4 - base64.length % 4) % 4);

        const p = JSON.parse(atob(paddedBase64));
        const now = Math.floor(Date.now() / 1000);
        return now >= p.exp;
    } catch (e) {
        console.error("Error validando token:", e);
        return true;
    }
}

const f = document.getElementById('f');
const email = document.getElementById('email');
const pass = document.getElementById('pass');
const btn = document.getElementById('btn');
const btnText = document.getElementById('btnText');
const btnLoader = document.getElementById('btnLoader');
const errorMsg = document.getElementById('errorMsg');


function showError(msg) {
    errorMsg.textContent = msg;
    errorMsg.classList.remove('hidden');
}

function hideError() {
    errorMsg.classList.add('hidden');
}


function setLoading(loading) {
    btn.disabled = loading;
    btnText.classList.toggle('hidden', loading);
    btnLoader.classList.toggle('hidden', !loading);
}


function getRolesFromToken(token) {
    try {
        if (!token || typeof token !== 'string') {
            return [];
        }

        const parts = token.split('.');
        if (parts.length !== 3) {
            return [];
        }

        // Decodificar el payload (segunda parte del JWT)
        const base64Url = parts[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');

        // Agregar padding si es necesario
        const paddedBase64 = base64 + '='.repeat((4 - base64.length % 4) % 4);

        const jsonPayload = decodeURIComponent(
            atob(paddedBase64)
                .split('')
                .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                .join('')
        );

        const payload = JSON.parse(jsonPayload);

        return payload.roles || payload.authorities || [];
    } catch (e) {

        return [];
    }
}


function isAdmin(roles) {
    if (!Array.isArray(roles)) {
        return false;
    }


    return roles.some(role =>
        role === 'ADMIN' ||
        role === 'ROLE_ADMIN' ||
        role === 'admin'
    );
}


function redirectByRole(token) {
    const roles = getRolesFromToken(token);

    if (isAdmin(roles)) {

        window.location.replace('dashboardAdmin.html');
    } else {
        window.location.replace('wallet.html');
    }
}


f.addEventListener('submit', async (e) => {
    e.preventDefault();
    hideError();

    const username = email.value.trim();
    const password = pass.value;


    if (!username || !password) {
        return showError('Por favor completa todos los campos');
    }

    if (!isEmail(username)) {
        return showError('Correo electrónico inválido');
    }

    setLoading(true);

    try {

        const response = await api('/api/auth/login', {
            method: 'POST',
            body: { username, password }
        });

        if (!response) {
            throw new Error('Usuario o contraseña incorrectos (Sin respuesta)');
        }

        const token = response.data?.token;

        setToken(token);
        const savedToken = getToken();

        redirectByRole(token);

    } catch (err) {

        let errorMessage = 'Error al iniciar sesión';

        if (err.message) {
            if (err.message.includes('401') || err.message.includes('Unauthorized')) {
                errorMessage = 'Usuario o contraseña incorrectos';
            } else if (err.message.includes('403') || err.message.includes('Forbidden')) {
                errorMessage = 'Acceso denegado. Verifica tus credenciales';
            } else if (err.message.includes('404')) {
                errorMessage = 'Servicio no disponible. Contacta al administrador';
            } else if (err.message.includes('Network') || err.message.includes('conexión')) {
                errorMessage = 'Error de conexión. Verifica tu internet';
            } else {
                errorMessage = err.message;
            }
        }

        showError(errorMessage);

    } finally {
        setLoading(false);
    }
});


email.addEventListener('input', hideError);
pass.addEventListener('input', hideError);


(function checkExistingSession() {
    const token = getToken();

    if (token && !isTokenExpired()) {
        redirectByRole(token);
    }
})();