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
// Intentar recuperar desde sessionStorage si no está en localStorage
if (!sid || sid === 'null' || sid === 'undefined') {
  sessionId = sessionStorage.getItem('onboardingSessionId');
  if (sessionId) sid = sessionId;
}

if (!sid || sid === 'null' || sid === 'undefined') {
  location.href = 'index.html';
}

const f = document.getElementById('f');
const btn = document.getElementById('btn');
const btnText = document.getElementById('btnText');
const btnLoader = document.getElementById('btnLoader');

const docNumberInput = document.getElementById('docNumber');
docNumberInput.addEventListener('input', (e) => {
  e.target.value = e.target.value.replace(/[^0-9]/g, '').substring(0, 8);
});

function setLoading(loading) {
  btn.disabled = loading;
  btnText.classList.toggle('hidden', loading);
  btnLoader.classList.toggle('hidden', !loading);
}

f.addEventListener('submit', async (e) => {
  e.preventDefault();

  const docNumber = document.getElementById('docNumber').value.trim();
  const firstName = document.getElementById('firstName').value.trim();
  const lastName = document.getElementById('lastName').value.trim();
  const gender = document.getElementById('gender').value || null;


  if (!docNumber || docNumber.length !== 8) {
    Swal.fire({
      icon: 'warning',
      title: 'DNI inválido',
      text: 'El DNI debe tener exactamente 8 dígitos',
      confirmButtonColor: '#6366f1'
    });
    return;
  }

  if (!firstName || !lastName) {
    Swal.fire({
      icon: 'warning',
      title: 'Campos incompletos',
      text: 'Por favor completa todos los campos obligatorios',
      confirmButtonColor: '#6366f1'
    });
    return;
  }

  const payload = {
    sessionId: sid, // Usar sessionId dinámico
    documentType: 'DNI',
    documentNumber: docNumber,
    firstName: firstName,
    lastName: lastName,
    gender: gender
  };

  setLoading(true);

  try {
    await api('/api/onboarding/complete', {
      method: 'POST',
      body: payload
    });


    await Swal.fire({
      icon: 'success',
      title: '¡Registro completado!',
      text: 'Tu cuenta ha sido creada exitosamente. Ahora puedes iniciar sesión.',
      confirmButtonColor: '#10b981',
      confirmButtonText: 'Ir al login'
    });

    clearSID();
    location.href = 'login.html';

  } catch (err) {
    console.error('Error:', err);

    let errorTitle = 'Error en el registro';
    let errorMessage = err.message || 'Ocurrió un error al completar el registro';
    let icon = 'error';


    if (errorMessage.includes('DNI no encontrado') || errorMessage.includes('RENIEC')) {
      errorTitle = 'DNI no encontrado';
      errorMessage = 'Los datos ingresados no coinciden con los registros de RENIEC. Verifica que tu DNI, nombres y apellidos sean correctos.';
      icon = 'warning';
    } else if (errorMessage.includes('no coinciden')) {
      errorTitle = 'Datos incorrectos';
      errorMessage = 'Los nombres o apellidos no coinciden con los datos registrados en RENIEC. Verifica que estén escritos exactamente como aparecen en tu DNI.';
      icon = 'warning';
    } else if (errorMessage.includes('18 años') || errorMessage.includes('menor')) {
      errorTitle = 'Edad no permitida';
      errorMessage = 'Debes ser mayor de 18 años para registrarte en la plataforma.';
      icon = 'info';
    } else if (errorMessage.includes('ya está registrado') || errorMessage.includes('ya usado')) {
      errorTitle = 'DNI ya registrado';
      errorMessage = 'Este DNI ya está registrado en el sistema. Si ya tienes una cuenta, intenta iniciar sesión.';
      icon = 'info';
    } else if (errorMessage.includes('Sesión expirada')) {
      errorTitle = 'Sesión expirada';
      errorMessage = 'Tu sesión ha expirado. Por favor, inicia el proceso de registro nuevamente.';
      icon = 'warning';
    }

    Swal.fire({
      icon: icon,
      title: errorTitle,
      text: errorMessage,
      confirmButtonColor: '#6366f1',
      confirmButtonText: 'Entendido'
    });

  } finally {
    setLoading(false);
  }
});