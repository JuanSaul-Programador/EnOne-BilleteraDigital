 const f = document.getElementById('f');
    const email = document.getElementById('email');
    const phone = document.getElementById('phone');
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

    f.addEventListener('submit', async (e) => {
      e.preventDefault();
      hideError();

      if (!isEmail(email.value)) {
        return showError('Correo electrónico inválido');
      }
      if (!isPhoneE164(phone.value)) {
        return showError('Teléfono inválido. Usa formato +E164');
      }
      if (!isPasswordOk(pass.value)) {
        return showError('La contraseña debe tener al menos 8 caracteres');
      }

      setLoading(true);

      try {
        const r = await api('/api/onboarding/start', {
          method: 'POST',
          body: {
            email: email.value.trim(),
            phone: phone.value.trim(),
            password: pass.value
          }
        });
        
        if (!r.data || !r.data.id) {
          throw new Error("No se recibió un ID de sesión válido del servidor");
        }
        
        // Guardar sessionId en múltiples lugares para asegurar que no se pierda
        setSID(r.data.id);
        sessionStorage.setItem('onboardingSessionId', r.data.id);
        
        // Redirigir con sessionId en la URL como backup
        location.href = 'verify-phone.html?sessionId=' + r.data.id;
      } catch (err) {
        showError(err.message || 'Error al crear la cuenta');
      } finally {
        setLoading(false);
      }
    });