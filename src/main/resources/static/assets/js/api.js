const BASE = location.origin + '/enone';


function setCookie(name, value, days = 7) {
	const expires = new Date();
	expires.setTime(expires.getTime() + (days * 24 * 60 * 60 * 1000));
	document.cookie = `${name}=${value};expires=${expires.toUTCString()};path=/;SameSite=Lax`;
}

function getCookie(name) {
	const nameEQ = name + "=";
	const ca = document.cookie.split(';');
	for (let i = 0; i < ca.length; i++) {
		let c = ca[i];
		while (c.charAt(0) === ' ') c = c.substring(1, c.length);
		if (c.indexOf(nameEQ) === 0) return c.substring(nameEQ.length, c.length);
	}
	return null;
}

function deleteCookie(name) {
	document.cookie = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 UTC;path=/;`;
}

function getSID() { return localStorage.getItem('sid'); }
function setSID(v) { localStorage.setItem('sid', v); }
function clearSID() { localStorage.removeItem('sid'); }

function setToken(t) {
	localStorage.setItem('token', t);
	const saved = localStorage.getItem('token');
}

function getToken() {
	return localStorage.getItem('token');
}

function clearToken() {
	localStorage.removeItem('token');
}

async function api(path, { method = 'GET', body = null, auth = false } = {}) {
	const headers = { 'Content-Type': 'application/json' };
	if (auth) {
		const t = getToken();
		if (t) {
			headers['Authorization'] = 'Bearer ' + t;
		} else {
			throw new Error("Token no encontrado");
		}
	}

	try {
		const res = await fetch(BASE + path, {
			method, headers, body: body ? JSON.stringify(body) : null
		});

		const isLogin = path.includes('/auth/login');
		if ((res.status === 401 || res.status === 403) && !isLogin) {
			clearToken();
			window.location.href = 'login.html';
			return;
		}

		const txt = await res.text();
		let data = {};
		try {
			data = txt ? JSON.parse(txt) : {};
		} catch (e) {
			console.error("JSON Parse Error:", txt);
			if (!res.ok) throw new Error(txt || `HTTP ${res.status}`);
			data = { raw: txt };
		}

		if (!res.ok) {

			const errorMsg = data.error || data.message || `HTTP ${res.status}: ${res.statusText}`;
			throw new Error(errorMsg);
		}

		return data;
	} catch (error) {
		console.error("API Error:", method, path, error);

		if (error.message && (error.message.includes('401') || error.message.includes('403')) && !path.includes('/auth/login')) {
			clearToken();
			setTimeout(() => window.location.href = 'login.html', 1000);
			return;
		}

		throw error;
	}
}
