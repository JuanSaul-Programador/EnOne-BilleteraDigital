/**
 * EnOne Chatbot Widget - Versión Profesional
 * Integración con Backend Watson Controller
 */
(function () {
    // 1. Check Auth (Hide if not logged in)
    if (typeof getToken !== 'function' || !getToken()) {
        console.log('Chatbot: No token found, widget will not load.');
        return;
    }

    // 2. Inject CSS
    const style = document.createElement('style');
    style.textContent = `
        #watson-widget-container {
            position: fixed;
            bottom: 24px;
            right: 24px;
            z-index: 9999;
            font-family: 'Inter', sans-serif;
        }

        #watson-toggle-btn {
            width: 64px;
            height: 64px;
            border-radius: 32px;
            background: linear-gradient(135deg, #2563eb, #1d4ed8);
            box-shadow: 0 4px 20px rgba(37, 99, 235, 0.5);
            border: none;
            cursor: pointer;
            transition: all 0.3s ease;
            display: flex;
            align-items: center;
            justify-content: center;
            position: relative;
        }
        
        #watson-toggle-btn:hover {
            transform: scale(1.1);
            box-shadow: 0 6px 28px rgba(37, 99, 235, 0.7);
        }

        #watson-toggle-btn i {
            font-size: 32px;
            color: white;
        }

        #watson-toggle-btn::before {
            content: '';
            position: absolute;
            width: 100%;
            height: 100%;
            border-radius: 50%;
            background: linear-gradient(135deg, #2563eb, #1d4ed8);
            animation: pulse 2s infinite;
            z-index: -1;
        }

        @keyframes pulse {
            0%, 100% { transform: scale(1); opacity: 0.5; }
            50% { transform: scale(1.15); opacity: 0; }
        }

        #watson-chat-window {
            position: absolute;
            bottom: 80px;
            right: 0;
            width: 380px;
            height: 550px;
            background: white;
            border-radius: 24px;
            box-shadow: 0 12px 48px rgba(0,0,0,0.2);
            display: none;
            flex-direction: column;
            overflow: hidden;
            border: 1px solid #e5e7eb;
            animation: slideIn 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        }

        @keyframes slideIn {
            from { opacity: 0; transform: translateY(20px) scale(0.95); }
            to { opacity: 1; transform: translateY(0) scale(1); }
        }

        .watson-header {
            background: linear-gradient(135deg, #2563eb, #1d4ed8);
            padding: 20px;
            color: white;
            display: flex;
            justify-content: space-between;
            align-items: center;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        }
        
        .watson-header-content {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .watson-header-icon {
            width: 40px;
            height: 40px;
            background: rgba(255,255,255,0.2);
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 20px;
        }

        .watson-header h3 {
            margin: 0;
            font-size: 18px;
            font-weight: 700;
        }

        .watson-header-subtitle {
            font-size: 12px;
            opacity: 0.9;
            margin-top: 2px;
        }

        .watson-close {
            background: rgba(255,255,255,0.2);
            border: none;
            color: white;
            cursor: pointer;
            font-size: 24px;
            padding: 8px;
            border-radius: 8px;
            transition: background 0.2s;
            width: 36px;
            height: 36px;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .watson-close:hover { background: rgba(255,255,255,0.3); }

        .watson-quick-actions {
            padding: 16px;
            background: #f8fafc;
            border-bottom: 1px solid #e5e7eb;
            display: flex;
            gap: 8px;
            overflow-x: auto;
        }

        .quick-action-btn {
            background: white;
            border: 1px solid #e5e7eb;
            border-radius: 20px;
            padding: 8px 16px;
            font-size: 13px;
            color: #1f2937;
            cursor: pointer;
            white-space: nowrap;
            transition: all 0.2s;
            display: flex;
            align-items: center;
            gap: 6px;
        }

        .quick-action-btn:hover {
            background: #2563eb;
            color: white;
            border-color: #2563eb;
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
        }

        .watson-messages {
            flex: 1;
            padding: 20px;
            overflow-y: auto;
            background: #f8fafc;
            display: flex;
            flex-direction: column;
            gap: 16px;
        }

        .watson-messages::-webkit-scrollbar {
            width: 6px;
        }

        .watson-messages::-webkit-scrollbar-thumb {
            background: #cbd5e1;
            border-radius: 3px;
        }

        .msg {
            max-width: 80%;
            padding: 12px 16px;
            border-radius: 16px;
            font-size: 14px;
            line-height: 1.5;
            word-wrap: break-word;
            animation: messageIn 0.3s ease-out;
        }

        @keyframes messageIn {
            from { opacity: 0; transform: translateY(10px); }
            to { opacity: 1; transform: translateY(0); }
        }

        .msg-bot {
            align-self: flex-start;
            background: white;
            color: #1f2937;
            border-bottom-left-radius: 4px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.08);
            border: 1px solid #e5e7eb;
        }

        .msg-user {
            align-self: flex-end;
            background: linear-gradient(135deg, #2563eb, #1d4ed8);
            color: white;
            border-bottom-right-radius: 4px;
            box-shadow: 0 2px 8px rgba(37, 99, 235, 0.3);
        }

        .watson-input-area {
            padding: 16px;
            background: white;
            border-top: 1px solid #e5e7eb;
            display: flex;
            gap: 12px;
        }

        #watson-input {
            flex: 1;
            border: 2px solid #e5e7eb;
            border-radius: 24px;
            padding: 12px 20px;
            outline: none;
            font-size: 14px;
            transition: border-color 0.2s;
        }
        #watson-input:focus { 
            border-color: #2563eb;
            box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
        }

        #watson-send {
            background: linear-gradient(135deg, #2563eb, #1d4ed8);
            color: white;
            border: none;
            border-radius: 50%;
            width: 48px;
            height: 48px;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: all 0.2s;
            box-shadow: 0 2px 8px rgba(37, 99, 235, 0.3);
        }
        #watson-send:hover { 
            transform: scale(1.1);
            box-shadow: 0 4px 12px rgba(37, 99, 235, 0.5);
        }
        
        .typing-indicator {
            display: flex;
            gap: 4px;
            padding: 12px 16px;
        }

        .typing-indicator span {
            display: inline-block;
            width: 8px;
            height: 8px;
            background-color: #94a3b8;
            border-radius: 50%;
            animation: typing 1.4s infinite ease-in-out both;
        }
        .typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
        .typing-indicator span:nth-child(2) { animation-delay: -0.16s; }
        @keyframes typing {
            0%, 80%, 100% { transform: scale(0.8); opacity: 0.5; }
            40% { transform: scale(1.2); opacity: 1; }
        }
    `;
    document.head.appendChild(style);

    // 3. Inject HTML
    const container = document.createElement('div');
    container.id = 'watson-widget-container';
    container.innerHTML = `
        <div id="watson-chat-window">
            <div class="watson-header">
                <div class="watson-header-content">
                    <div class="watson-header-icon">
                        <i class="bi bi-chat-dots-fill"></i>
                    </div>
                    <div>
                        <h3>Asistente EnOne</h3>
                        <div class="watson-header-subtitle">Siempre listo para ayudarte</div>
                    </div>
                </div>
                <button class="watson-close"><i class="bi bi-x-lg"></i></button>
            </div>
            <div class="watson-quick-actions">
                <button class="quick-action-btn" data-query="¿Cuál es mi saldo?">
                    <i class="bi bi-wallet2"></i> Mi saldo
                </button>
                <button class="quick-action-btn" data-query="Ver mi último movimiento">
                    <i class="bi bi-clock-history"></i> Movimientos
                </button>
                <button class="quick-action-btn" data-query="¿Cómo funciona EnOne?">
                    <i class="bi bi-question-circle"></i> Ayuda
                </button>
            </div>
            <div class="watson-messages" id="watson-messages">
                <div class="msg msg-bot">
                    👋 Hola, soy tu asistente virtual de EnOne. ¿En qué puedo ayudarte hoy?
                </div>
            </div>
            <div class="watson-input-area">
                <input type="text" id="watson-input" placeholder="Escribe tu consulta..." autocomplete="off">
                <button id="watson-send"><i class="bi bi-send-fill"></i></button>
            </div>
        </div>
        <button id="watson-toggle-btn">
            <i class="bi bi-chat-dots-fill"></i>
        </button>
    `;
    document.body.appendChild(container);

    // 4. Logic
    const toggleBtn = document.getElementById('watson-toggle-btn');
    const chatWindow = document.getElementById('watson-chat-window');
    const closeBtn = container.querySelector('.watson-close');
    const input = document.getElementById('watson-input');
    const sendBtn = document.getElementById('watson-send');
    const msgsContainer = document.getElementById('watson-messages');
    const quickActionBtns = container.querySelectorAll('.quick-action-btn');

    let sessionId = null;

    function toggleChat() {
        if (chatWindow.style.display === 'flex') {
            chatWindow.style.display = 'none';
            toggleBtn.style.display = 'flex';
        } else {
            chatWindow.style.display = 'flex';
            toggleBtn.style.display = 'none';
            input.focus();
        }
    }

    toggleBtn.addEventListener('click', toggleChat);
    closeBtn.addEventListener('click', toggleChat);

    // Quick actions
    quickActionBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const query = btn.getAttribute('data-query');
            input.value = query;
            sendMessage();
        });
    });

    function addMessage(text, isUser) {
        const div = document.createElement('div');
        div.className = `msg ${isUser ? 'msg-user' : 'msg-bot'}`;

        let formatted = text
            .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
            .replace(/\n/g, '<br>');

        div.innerHTML = formatted;
        msgsContainer.appendChild(div);
        msgsContainer.scrollTop = msgsContainer.scrollHeight;
    }

    function showTyping() {
        const div = document.createElement('div');
        div.className = 'msg msg-bot typing-indicator';
        div.id = 'typing';
        div.innerHTML = '<span></span><span></span><span></span>';
        msgsContainer.appendChild(div);
        msgsContainer.scrollTop = msgsContainer.scrollHeight;
    }

    function removeTyping() {
        const typing = document.getElementById('typing');
        if (typing) typing.remove();
    }

    async function sendMessage() {
        const text = input.value.trim();
        if (!text) return;

        addMessage(text, true);
        input.value = '';
        showTyping();

        try {
            const response = await api('/api/chat/send', {
                method: 'POST',
                auth: true,
                body: {
                    message: text,
                    sessionId: sessionId
                }
            });

            removeTyping();

            if (response.success && response.data) {
                addMessage(response.data.message, false);
                sessionId = response.data.sessionId;

                if (response.data.intentDetected === 'navegacion_perfil') {
                    setTimeout(() => window.location.href = 'profile.html', 1500);
                }
            } else {
                addMessage("Lo siento, hubo un error de comunicación.", false);
            }

        } catch (error) {
            removeTyping();
            console.error(error);
            addMessage("Error enviando mensaje. Verifica tu conexión.", false);
        }
    }

    sendBtn.addEventListener('click', sendMessage);
    input.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') sendMessage();
    });

})();
