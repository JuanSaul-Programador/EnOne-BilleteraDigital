
let currentWallets = { PEN: null, USD: null };
let selectedCurrency = 'PEN';
let currentExchangeRate = null;
let transactionPage = 1;
let allTransactionsCache = [];
let recentTransactionsPage = 1;
let user2FAEnabled = false;
let currentTxForDownload = null;
let userHasActiveCard = false;
let userActiveCardMasked = '';


async function loadAllWallets() {
    try {
        const response = await api('/api/wallet/all', { auth: true });
        let wallets = response.data || response;


        if (!Array.isArray(wallets)) {
            if (wallets && wallets.data && Array.isArray(wallets.data)) {
                wallets = wallets.data;
            } else if (wallets && Array.isArray(wallets.wallets)) {
                wallets = wallets.wallets;
            } else {
                throw new Error("Formato de respuesta de wallets inválido");
            }
        }

        if (Array.isArray(wallets)) {
            wallets.forEach(w => {
                currentWallets[w.currency] = w;

                const balanceEl = document.getElementById('balance' + w.currency);
                const walletNumberEl = document.getElementById('walletNumber' + w.currency);
                const walletInfoEl = document.getElementById('walletInfo' + w.currency);

                const formattedBalance = w.balance.toFixed(2);
                const formattedCurrency = w.currency === 'PEN' ? 'S/' : '$';

                if (balanceEl) balanceEl.textContent = formattedBalance;
                if (walletNumberEl) walletNumberEl.textContent = w.walletNumber;

                if (walletInfoEl) {
                    walletInfoEl.textContent = `${formattedCurrency} ${formattedBalance}`;
                }
            });

            if (currentWallets.PEN) {
                switchWallet('PEN');
            } else if (currentWallets.USD) {
                switchWallet('USD');
            }
        }
    } catch (err) {
        showError('Error al cargar wallets');
    }
}

// Función para cargar transacciones
async function loadTransactions(pageNumber = 1) {
    if (pageNumber !== 1) return;

    const list = document.getElementById('transactionList');
    if (!list) return;

    list.innerHTML = `<p class="text-center py-4 text-blue-300">Cargando...</p>`;
    try {
        const response = await api(`/api/wallet/transactions?limit=5&page=1&currency=${selectedCurrency}`, { auth: true });

        let txs = response;
        if (response && response.data && Array.isArray(response.data)) {
            txs = response.data;
        } else if (response && Array.isArray(response.transactions)) {
            txs = response.transactions;
        } else if (!Array.isArray(response)) {
            txs = [];
        }

        if (!txs || txs.length === 0) {
            list.innerHTML = `<p class="text-center py-8 text-gray-400"><i class="bi bi-inbox text-4xl mb-2 block"></i> No hay movimientos recientes.</p>`;
        } else {
            list.innerHTML = txs.map(tx => generateTransactionHtml(tx, false)).join('');
            Array.from(list.children).forEach((element, index) => {
                if (element.classList.contains('transaction-item')) {
                    attachDataToElement(element, txs[index]);
                    element.addEventListener('click', handleTransactionClick);
                }
            });
        }
    } catch (err) {
        console.error('Tx Load Error:', err);
        showError('Error al cargar movimientos');
        if (list) {
            list.innerHTML = `<p class="text-center py-8 text-red-400"><i class="bi bi-exclamation-triangle"></i> Error al cargar.</p>`;
        }
    }
}

async function loadDetailedTransactions(page = null, reset = false) {
    const list = document.getElementById('detailedTransactionList');
    const btn = document.getElementById('btnHistoryLoadMore');
    const type = document.getElementById('historyFilterType').value;
    const curr = document.getElementById('historyFilterCurrency').value;
    const limit = 20;

    if (reset) {
        transactionPage = 1;
        list.innerHTML = `<p class="text-center py-8 text-blue-300"><i class="bi bi-hourglass-split"></i> Cargando...</p>`;
        btn.classList.add('hidden');
        allTransactionsCache = [];
    } else if (page) {
        transactionPage = page;
    } else {
        transactionPage++;
        btn.textContent = 'Cargando...';
        btn.disabled = true;
    }

    try {
        if (reset || allTransactionsCache.length === 0) {
            let allTxs = [];
            let currentP = 1;
            let hasMore = true;
            const fetchL = 50;

            while (hasMore) {
                const url = `/api/wallet/transactions?limit=${fetchL}&page=${currentP}`;
                const pageResponse = await api(url, { auth: true });

                let pageTxs = pageResponse;
                if (pageResponse && pageResponse.data && Array.isArray(pageResponse.data)) {
                    pageTxs = pageResponse.data;
                } else if (pageResponse && Array.isArray(pageResponse.transactions)) {
                    pageTxs = pageResponse.transactions;
                } else if (!Array.isArray(pageResponse)) {
                    pageTxs = [];
                }

                if (!pageTxs || pageTxs.length === 0) {
                    hasMore = false;
                } else {
                    allTxs = allTxs.concat(pageTxs);
                    if (pageTxs.length < fetchL) {
                        hasMore = false;
                    }
                    currentP++;
                }
            }
            allTransactionsCache = allTxs;
        }

        let filteredTxs = allTransactionsCache.filter(tx => {
            const typeMatch = !type || (type === 'DEPOSIT' && tx.type === 'DEPOSIT') || (type === 'TRANSFER' && tx.type.includes('TRANSFER')) || (type === 'CONVERT' && tx.type.includes('CONVERT'));
            const currMatch = !curr || tx.currency === curr;
            return typeMatch && currMatch;
        });

        const startIdx = (transactionPage - 1) * limit;
        const endIdx = startIdx + limit;
        const pageTxs = filteredTxs.slice(startIdx, endIdx);

        if (transactionPage === 1 && pageTxs.length === 0) {
            list.innerHTML = `<p class="text-center py-8 text-gray-400"><i class="bi bi-search text-4xl mb-2 block"></i> No hay resultados.</p>`;
            btn.classList.add('hidden');
            return;
        }
        if (transactionPage > 1 && pageTxs.length === 0) {
            showInfo('No hay más historial.');
            btn.classList.add('hidden');
            return;
        }

        const startingIndex = (transactionPage === 1) ? 0 : list.children.length;
        if (transactionPage === 1) list.innerHTML = '';

        list.insertAdjacentHTML('beforeend', pageTxs.map(tx => generateDetailedTransactionCard(tx)).join(''));

        Array.from(list.children).slice(startingIndex).forEach((element, index) => {
            if (element.classList.contains('transaction-item')) {
                attachDataToElement(element, pageTxs[index]);
                element.addEventListener('click', handleTransactionClick);
            }
        });

        btn.classList.toggle('hidden', endIdx >= filteredTxs.length);
    } catch (err) {
        console.error('History Error:', err);
        showError(`Error: ${err.message}`);
        if (reset || transactionPage === 1) {
            list.innerHTML = `<p class="text-center py-8 text-red-400"><i class="bi bi-exclamation-triangle"></i> Error al cargar.</p>`;
        }
        btn.classList.add('hidden');
    } finally {
        if (!btn.classList.contains('hidden')) {
            btn.textContent = 'Cargar Más...';
            btn.disabled = false;
        }
    }
}

async function loadRecentDetails() {
    const list = document.getElementById('recentDetailsList');
    const btn = document.getElementById('btnRecentLoadMore');
    const limit = 15;

    if (recentTransactionsPage === 1) {
        btn.classList.add('hidden');
    } else {
        btn.textContent = 'Cargando...';
        btn.disabled = true;
    }

    try {
        document.getElementById('recentDetailsCurrency').textContent = selectedCurrency;
        const response = await api(`/api/wallet/transactions?limit=${limit}&page=${recentTransactionsPage}&currency=${selectedCurrency}`, { auth: true });

        let txs = response;
        if (response && response.data && Array.isArray(response.data)) {
            txs = response.data;
        } else if (response && Array.isArray(response.transactions)) {
            txs = response.transactions;
        } else if (!Array.isArray(response)) {
            txs = [];
        }

        if (recentTransactionsPage === 1 && (!txs || txs.length === 0)) {
            list.innerHTML = `<p class="text-center py-8 text-gray-400"><i class="bi bi-inbox text-4xl mb-2 block"></i> No hay movimientos.</p>`;
            btn.classList.add('hidden');
            return;
        }

        if (recentTransactionsPage > 1 && (!txs || txs.length === 0)) {
            showInfo('No hay más transacciones.');
            btn.classList.add('hidden');
            return;
        }

        const startingIndex = (recentTransactionsPage === 1) ? 0 : list.children.length;
        if (recentTransactionsPage === 1) { list.innerHTML = ''; }
        list.insertAdjacentHTML('beforeend', txs.map(tx => generateDetailedTransactionCard(tx)).join(''));

        Array.from(list.children).slice(startingIndex).forEach((element, index) => {
            if (element.classList.contains('transaction-item')) {
                attachDataToElement(element, txs[index]);
            }
        });

        btn.classList.toggle('hidden', txs.length < limit);
    } catch (err) {
        console.error('Recent Details Error:', err);
        showError(`Error: ${err.message}`);
        if (recentTransactionsPage === 1) {
            list.innerHTML = `<p class="text-center py-8 text-red-400"><i class="bi bi-exclamation-triangle"></i> Error al cargar.</p>`;
        }
        btn.classList.add('hidden');
    } finally {
        if (!btn.classList.contains('hidden')) {
            btn.textContent = 'Cargar Más...';
            btn.disabled = false;
        }
    }
}

async function loadExchangeRate(from = 'PEN', to = 'USD') {
    try {
        const response = await api(`/api/wallet/exchange-rate?from=${from}&to=${to}`, { auth: true });
        const data = response.data || response;

        if (data && data.rate !== undefined) {
            currentExchangeRate = data.rate;
            document.getElementById('exchangeFrom').textContent = from;
            document.getElementById('exchangeTo').textContent = to;
            document.getElementById('exchangeRate').textContent = data.rate.toFixed(4);
        } else {
            throw new Error('Tipo de cambio no disponible');
        }
    } catch (err) {
        console.error('Rate Error:', err);
        showError('No se pudo cargar tipo de cambio');
        currentExchangeRate = null;
    }
}

function switchWallet(currency) {

    selectedCurrency = currency;
    const wallet = currentWallets[currency];
    if (!wallet) return;

    const currentCurrency = document.getElementById('currentCurrency');
    const currentBalance = document.getElementById('currentBalance');
    const currentWalletNumber = document.getElementById('currentWalletNumber');
    const currentCurrencyLabel = document.getElementById('currentCurrencyLabel');
    const currentCurrencyLabelShort = document.getElementById('currentCurrencyLabelShort');

    if (currentCurrency) currentCurrency.textContent = currency;
    if (currentBalance) currentBalance.textContent = wallet.balance.toFixed(2);
    if (currentWalletNumber) currentWalletNumber.innerHTML = `<i class="bi bi-credit-card-2-front"></i> ${wallet.walletNumber}`;
    if (currentCurrencyLabel) currentCurrencyLabel.textContent = currency === 'PEN' ? 'Soles' : 'Dólares';
    if (currentCurrencyLabelShort) currentCurrencyLabelShort.textContent = currency;

    document.querySelectorAll('.wallet-selector').forEach(btn => {
        if (btn.dataset.currency === currency) {
            btn.classList.add('wallet-selected');
        } else {
            btn.classList.remove('wallet-selected');
        }
    });

    const transactionList = document.getElementById('transactionList');
    if (transactionList) loadTransactions(1);
}

function isTokenExpired() {
    const token = getToken();
    if (!token) return true;
    try {
        const parts = token.split('.');
        if (parts.length !== 3) return true;
        const payload = JSON.parse(atob(parts[1]));
        const now = Math.floor(Date.now() / 1000);
        return now >= payload.exp;
    } catch (e) {
        return true;
    }
}

function validateTokenOnLoad() {
    const token = getToken();
    if (!token) {
        window.location.replace('login.html');
        return false;
    } else {
        if (isTokenExpired()) {
            clearToken();
            window.location.replace('login.html');
            return false;
        }
        return true;
    }
}

if (!validateTokenOnLoad()) {
    throw new Error("Token inválido, redirigiendo...");
}

// Utils
function showAlert(icon, title, text) { Swal.fire({ icon, title, text, toast: true, position: 'top-end', showConfirmButton: false, timer: 5000, timerProgressBar: true, background: '#1e40af', color: '#f3f4f6', customClass: { popup: 'swal2-popup' } }); }
function showError(msg) { showAlert('error', 'Error', msg || 'Error inesperado'); }
function showSuccess(msg) { showAlert('success', 'Éxito', msg || 'Operación realizada'); }
function showInfo(msg) { showAlert('info', 'Información', msg); }

function safeAddEventListener(id, event, handler) {
    try {
        const element = document.getElementById(id);
        if (element) element.addEventListener(event, handler);
    } catch (err) {
    }
}

function openModal(id) {
    const modal = document.getElementById(id); if (!modal) return;
    modal.classList.add('active');
    modal.classList.remove('opacity-0', 'pointer-events-none');
    const mc = modal.querySelector('.modal-content, .modal-content-light');
    if (mc) mc.classList.remove('scale-95');
}
function closeModal(id) {
    const modal = document.getElementById(id); if (!modal) return;
    const mc = modal.querySelector('.modal-content, .modal-content-light');
    if (mc) mc.classList.add('scale-95');
    setTimeout(() => { modal.classList.add('opacity-0', 'pointer-events-none'); modal.classList.remove('active'); }, 150);
}

function attachDataToElement(element, txData) {
    if (element) {
        element.__transactionData = txData;
    }
}

function generateVoucherHTML(tx, forDownload = false) {
    let formattedDate = 'Fecha inválida';
    const dateValue = tx.createdAt;
    let finalDate = null;
    if (typeof dateValue === 'number' && dateValue > 0) {
        finalDate = new Date(Math.floor(dateValue * 1000));
    } else if (typeof dateValue === 'string') {
        finalDate = new Date(dateValue);
    }
    if (finalDate && !isNaN(finalDate)) {
        const opts = {
            day: '2-digit', month: 'long', year: 'numeric',
            hour: '2-digit', minute: '2-digit', timeZone: 'America/Lima'
        };
        formattedDate = finalDate.toLocaleDateString('es-PE', opts).replace(/\./g, '');
    }

    const isPositive = ['DEPOSIT', 'TRANSFER_IN', 'CONVERT_IN'].includes(tx.type);
    let title = '¡Operación Exitosa!';

    if (tx.type === 'DEPOSIT') title = '¡Depósito Exitoso!';
    else if (tx.type === 'WITHDRAW') title = '¡Retiro Exitoso!';
    else if (tx.type === 'TRANSFER_IN') title = '¡Dinero Recibido!';
    else if (tx.type === 'TRANSFER_OUT') title = '¡Transferencia Enviada!';
    else if (tx.type.includes('CONVERT')) title = '¡Conversión Exitosa!';

    const sign = isPositive ? '+' : '';

    const containerStyle = forDownload ? `
        font-family: 'Inter', sans-serif;
        background: white; padding: 40px; max-width: 500px; margin: 0 auto;
        border: 2px solid #e5e7eb; border-radius: 16px;
    ` : `
        font-family: 'Inter', sans-serif;
        background: white; border-radius: 16px;
    `;

    return `
        <div style="${containerStyle}">
            <div style="text-align: center; margin-bottom: 30px;">
                <div style="width: 60px; height: 60px; background: #10b981; border-radius: 16px; display: inline-flex; align-items: center; justify-content: center; margin-bottom: 16px;">
                    <span style="color: white; font-size: 24px;">✓</span>
                </div>
                <h1 style="font-size: 28px; font-weight: 800; color: #1f2937; margin: 0 0 8px 0;">${title}</h1>
                <p style="color: #6b7280; font-size: 16px; margin: 0;">Comprobante de operación</p>
            </div>

            <div style="text-align: center; margin-bottom: 30px;">
                <p style="color: #6b7280; font-size: 14px; margin: 0 0 8px 0; font-weight: 500;">Monto total</p>
                <p style="font-size: 36px; font-weight: 900; color: #1e40af; margin: 0 0 12px 0;">${sign}${tx.currency} ${Math.abs(tx.amount || 0).toFixed(2)}</p>
                <span style="background: #1e40af; color: white; padding: 4px 12px; border-radius: 9999px; font-size: 12px; font-weight: 600; text-transform: uppercase;">
                    COMPLETADO
                </span>
            </div>

            ${tx.securityCode ? `
            <div style="background: #fffbeb; border: 1px dashed #fbbf24; border-radius: 12px; padding: 16px; text-align: center; margin-bottom: 30px;">
                <h3 style="color: #b45309; font-size: 14px; margin: 0 0 8px 0; font-weight: 700;">🛡️ Código de Seguridad</h3>
                <div style="font-family: 'Courier New', monospace; font-size: 24px; font-weight: 800; color: #92400e; background: white; padding: 12px; border-radius: 8px; letter-spacing: 4px;">${tx.securityCode}</div>
            </div>` : ''}

            <div style="background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 12px; padding: 24px; margin-bottom: 30px;">
                <h3 style="color: #374151; font-size: 16px; font-weight: 700; margin: 0 0 20px 0;">📋 Detalles de la Transacción</h3>
                
                ${tx.type === 'TRANSFER_IN' && tx.fromUser ? `
                <div style="display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #e5e7eb;">
                    <span style="color: #6b7280; font-weight: 500;">Recibido de:</span>
                    <span style="color: #1f2937; font-weight: 600;">${tx.fromUser}</span>
                </div>` : ''}
                
                ${tx.type === 'TRANSFER_OUT' && tx.toUser ? `
                <div style="display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #e5e7eb;">
                    <span style="color: #6b7280; font-weight: 500;">Enviado a:</span>
                    <span style="color: #1f2937; font-weight: 600;">${tx.toUser}</span>
                </div>` : ''}
                
                ${(tx.type === 'TRANSFER_IN' || tx.type === 'TRANSFER_OUT') && tx.id ? `
                <div style="display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #e5e7eb;">
                    <span style="color: #6b7280; font-weight: 500;">Nro. de operación:</span>
                    <span style="color: #1f2937; font-weight: 600; font-family: monospace;">${String(tx.id).padStart(8, '0')}</span>
                </div>` : ''}
                
                ${tx.transactionUid && !(tx.type === 'TRANSFER_IN' || tx.type === 'TRANSFER_OUT') ? `
                <div style="display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #e5e7eb;">
                    <span style="color: #6b7280; font-weight: 500;">ID Transacción:</span>
                    <span style="color: #1f2937; font-weight: 600; font-family: monospace; font-size: 12px;">${tx.transactionUid}</span>
                </div>` : ''}
                
                <div style="display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #e5e7eb;">
                    <span style="color: #6b7280; font-weight: 500;">Fecha y Hora:</span>
                    <span style="color: #1f2937; font-weight: 600;">${formattedDate}</span>
                </div>
                
                <div style="display: flex; justify-content: space-between; padding: 12px 0 0 0;">
                    <span style="color: #6b7280; font-weight: 500;">Descripción:</span>
                    <span style="color: #1f2937; font-weight: 600;">${tx.description || 'Sin descripción'}</span>
                </div>
            </div>

            <div style="text-align: center; padding-top: 20px; border-top: 1px solid #e5e7eb;">
                <p style="color: #9ca3af; font-size: 12px; font-weight: 600; margin: 0;">EnOne - Comprobante Digital Verificado</p>
                <p style="color: #d1d5db; font-size: 10px; margin: 4px 0 0 0;">Generado el ${new Date().toLocaleDateString('es-PE')}</p>
            </div>
        </div>
    `;
}

// Function to Show Transaction Detail
function showTransactionDetail(tx) {
    if (!tx) return;
    currentTxForDownload = tx;

    const modalContent = document.querySelector('#modalTransactionDetail .modal-content');
    if (modalContent) {
        const voucherHTML = generateVoucherHTML(tx, false);
        modalContent.innerHTML = `
            <div class="relative">
                <button class="absolute top-0 right-0 m-4 text-gray-400 hover:text-gray-600 z-10" onclick="closeModal('modalTransactionDetail')">
                    <i class="bi bi-x-circle text-2xl"></i>
                </button>
                <div class="p-6 md:p-8">
                    ${voucherHTML}
                    <div class="mt-8 flex gap-4 justify-center">
                        <button onclick="handleDownloadClick()" class="bg-blue-600 text-white px-6 py-3 rounded-xl hover:bg-blue-700 transition flex items-center gap-2 font-semibold shadow-lg shadow-blue-200">
                            <i class="bi bi-download"></i> Descargar Comprobante
                        </button>
                        <button onclick="closeModal('modalTransactionDetail')" class="bg-gray-100 text-gray-700 px-6 py-3 rounded-xl hover:bg-gray-200 transition font-medium">
                            Cerrar
                        </button>
                    </div>
                </div>
            </div>
        `;
    }
    openModal('modalTransactionDetail');
}

async function handleDownloadClick() {
    const tx = currentTxForDownload;
    if (!tx) { showError("No hay datos de transacción para descargar."); return; }

    const date = new Date().toISOString().split('T')[0];
    const type = (tx.type || 'TX').toLowerCase().replace('_', '-');
    const txId = tx.id || tx.transactionUid || 'unknown';
    const filename = `comprobante-enone-${type}-${txId}-${date}.png`;

    showInfo("Generando comprobante...");

    try {
        const voucherHTML = generateVoucherHTML(tx, true);
        const tempContainer = document.createElement('div');
        tempContainer.style.cssText = 'position: fixed; top: -9999px; left: -9999px; background: white; padding: 20px;';
        tempContainer.innerHTML = voucherHTML;
        document.body.appendChild(tempContainer);

        const canvas = await html2canvas(tempContainer, {
            useCORS: true, allowTaint: true, backgroundColor: '#ffffff', scale: 2, logging: false
        });

        document.body.removeChild(tempContainer);
        const imgData = canvas.toDataURL('image/png', 0.95);
        const link = document.createElement('a');
        link.href = imgData;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        showSuccess("¡Comprobante descargado exitosamente!");
    } catch (err) {
        console.error("Error al generar imagen:", err);
        showError("Error al generar la imagen del comprobante.");
    }
}

function handleTransactionClick(event) {
    const targetElement = event.target.closest('.transaction-item');
    if (!targetElement) return;
    const txData = targetElement.__transactionData;
    if (txData) {
        if (txData.id !== undefined && txData.transactionUid !== undefined) {
            showTransactionDetail(txData);
        } else {
            showError("Faltan datos clave (ID) en la transacción.");
        }
    } else {
        showError("No se pudo cargar el detalle (datos no adjuntos).");
    }
}

function generateTransactionHtml(tx, detailed = false) {
    let date; const dateValue = tx.createdAt; let finalDate = null; if (typeof dateValue === 'number' && dateValue > 0) { finalDate = new Date(Math.floor(dateValue * 1000)); } else if (typeof dateValue === 'string') { finalDate = new Date(dateValue); } if (finalDate && !isNaN(finalDate)) { const opts = { day: '2-digit', month: 'short', year: 'numeric', timeZone: 'America/Lima' }; if (detailed) { opts.hour = '2-digit'; opts.minute = '2-digit'; opts.second = '2-digit'; } date = finalDate.toLocaleDateString('es-PE', opts).replace(/\./g, ''); } else { date = 'Fecha inválida'; } const isPositive = ['DEPOSIT', 'TRANSFER_IN', 'CONVERT_IN'].includes(tx.type); let icon = 'bi-question-circle-fill'; let color = 'gray'; if (tx.type === 'DEPOSIT') { icon = 'bi-plus-circle-fill'; color = 'green'; } else if (tx.type === 'TRANSFER_IN') { icon = 'bi-arrow-down-circle-fill'; color = 'green'; } else if (tx.type === 'TRANSFER_OUT') { icon = 'bi-arrow-up-circle-fill'; color = 'red'; } else if (tx.type.includes('CONVERT')) { icon = 'bi-arrow-repeat'; color = 'purple'; } const sign = isPositive ? '+' : '-'; let typeText = (tx.type || 'N/A').replace(/_/g, ' '); typeText = typeText.charAt(0).toUpperCase() + typeText.slice(1).toLowerCase(); const safeDesc = tx.description || 'Sin descripción'; const colorClass = `text-${color}-600`;
    return `<div class='transaction-item cursor-pointer flex items-center gap-4 p-4 rounded-2xl transition-all'><div class="w-12 h-12 rounded-xl bg-${color}-100 flex items-center justify-center"><i class="bi ${icon} text-xl ${colorClass}"></i></div><div class="flex-grow min-w-0"><div class="font-semibold text-gray-800 truncate">${safeDesc}</div><div class="text-sm text-gray-500">${date} ${detailed ? `• ${typeText}` : ''}</div></div><div class="font-bold ${colorClass} flex-shrink-0 text-right"><div>${sign}${tx.currency} ${Math.abs(tx.amount || 0).toFixed(2)}</div></div></div>`;
}

function generateDetailedTransactionCard(tx) {
    let date; const dateValue = tx.createdAt; let finalDate = null; if (typeof dateValue === 'number' && dateValue > 0) { finalDate = new Date(Math.floor(dateValue * 1000)); } else if (typeof dateValue === 'string') { finalDate = new Date(dateValue); } if (finalDate && !isNaN(finalDate)) { const opts = { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit', timeZone: 'America/Lima' }; date = finalDate.toLocaleDateString('es-PE', opts).replace(/\./g, ''); } else { date = 'Fecha inválida'; } const isPositive = ['DEPOSIT', 'TRANSFER_IN', 'CONVERT_IN'].includes(tx.type); let icon = 'bi-question-circle-fill'; let bgColor = 'gray'; let borderColor = 'gray'; if (tx.type === 'DEPOSIT') { icon = 'bi-plus-circle-fill'; bgColor = 'green'; borderColor = 'green'; } else if (tx.type === 'TRANSFER_IN') { icon = 'bi-arrow-down-circle-fill'; bgColor = 'green'; borderColor = 'green'; } else if (tx.type === 'TRANSFER_OUT') { icon = 'bi-arrow-up-circle-fill'; bgColor = 'red'; borderColor = 'red'; } else if (tx.type.includes('CONVERT')) { icon = 'bi-arrow-repeat'; bgColor = 'purple'; borderColor = 'purple'; } const sign = isPositive ? '+' : '-'; let typeText = (tx.type || 'N/A').replace(/_/g, ' '); typeText = typeText.charAt(0).toUpperCase() + typeText.slice(1).toLowerCase(); const safeDesc = tx.description || 'Sin descripción'; let extraInfo = '';
    if (tx.type === 'TRANSFER_OUT' && tx.toUser) {
        extraInfo = `<div class="text-sm text-gray-600 mt-3 flex items-center gap-2 bg-gray-50 p-2 rounded-lg"><i class="bi bi-person-arrow-right text-${bgColor}-600"></i> Enviado a: <span class="font-medium text-${bgColor}-700">${tx.toUser}</span></div>`;
    } else if (tx.type === 'TRANSFER_IN' && tx.fromUser) {
        extraInfo = `<div class="text-sm text-gray-600 mt-3 flex items-center gap-2 bg-gray-50 p-2 rounded-lg"><i class="bi bi-person-arrow-left text-${bgColor}-600"></i> Recibido de: <span class="font-medium text-${bgColor}-700">${tx.fromUser}</span></div>`;
    } else if (tx.type.includes('CONVERT')) {
        const fromC = tx.type === 'CONVERT_OUT' ? tx.currency : (tx.currency === 'PEN' ? 'USD' : 'PEN');
        const toC = tx.type === 'CONVERT_IN' ? tx.currency : (tx.currency === 'PEN' ? 'USD' : 'PEN');
        extraInfo = `<div class="text-sm text-gray-600 mt-3 flex items-center gap-2 bg-gray-50 p-2 rounded-lg"><i class="bi bi-arrow-repeat text-${bgColor}-600"></i> Conversión: <span class="font-medium">${fromC} → ${toC}</span></div>`;
    }

    let idHtml = '';
    if (tx.type === 'TRANSFER_IN' || tx.type === 'TRANSFER_OUT') {
        idHtml = tx.id ? `<div class="text-xs text-gray-500 mt-2 font-mono flex items-center gap-1 bg-gray-50 p-2 rounded"><i class="bi bi-hash"></i> Nro: ${String(tx.id).padStart(8, '0')}</div>` : '';
    } else {
        idHtml = tx.transactionUid ? `<div class="text-xs text-gray-500 mt-2 font-mono flex items-center gap-1 bg-gray-50 p-2 rounded"><i class="bi bi-key-fill"></i> ID: ${tx.transactionUid.substring(0, 8)}...</div>` : '';
    }

    return `<div class='transaction-item cursor-pointer bg-${bgColor}-50 border-2 border-${borderColor}-200 rounded-2xl p-6 hover:bg-${bgColor}-100 hover:border-${borderColor}-300 transition-all hover:shadow-lg'><div class="flex items-start justify-between mb-3"><div class="flex items-center gap-4"><div class="w-14 h-14 rounded-2xl bg-${bgColor}-100 flex items-center justify-center"><i class="bi ${icon} text-2xl text-${bgColor}-600"></i></div><div><div class="font-bold text-gray-800 text-lg">${safeDesc}</div><div class="text-sm text-gray-500 mt-1">${date}</div></div></div><div class="text-right"><div class="font-bold text-xl text-${bgColor}-600">${sign}${tx.currency} ${Math.abs(tx.amount || 0).toFixed(2)}</div><div class="text-sm text-gray-500 mt-1">${typeText}</div></div></div>${extraInfo}${idHtml}</div>`;
}

async function updateUserHeader() {
    try {
        let user = getCurrentUser();

        try {
            const response = await api('/api/auth/me', { auth: true });
            const apiUser = response.data || response;
            if (apiUser) user = apiUser;
        } catch (e) {
            console.warn('API User Fetch failed, using token fallback', e);
        }

        if (user) {
            const headerNameEl = document.getElementById('headerName');
            const userAvatarEl = document.getElementById('userAvatar');

            if (headerNameEl) {
                const displayName = user.fullName || user.name || `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.email;
                headerNameEl.textContent = displayName;
                headerNameEl.classList.remove('hidden');
            }

            if (userAvatarEl) {
                const nameForInitials = user.firstName || user.name || user.email || 'U';
                const initials = nameForInitials.charAt(0).toUpperCase();
                userAvatarEl.innerHTML = `<span class="text-lg font-bold">${initials}</span>`;
            }
        }
    } catch (error) {

    }
}

async function checkActiveCard() {
    try {
        const response = await api('/api/wallet/activar-tarjeta/status', { auth: true });
        const cardData = response.data || response;
        userHasActiveCard = cardData.hasActiveCard || false;
        userActiveCardMasked = cardData.maskedNumber || '';
        updateDepositModalView();
    } catch (err) {
        userHasActiveCard = false;
        updateDepositModalView();
    }
}

function updateDepositModalView() {
    const noCardSection = document.getElementById('noCardSection');
    const hasCardSection = document.getElementById('hasCardSection');
    const activatedCardNumber = document.getElementById('activatedCardNumber');
    if (!noCardSection || !hasCardSection) return;

    if (userHasActiveCard) {
        noCardSection.style.display = 'none';
        hasCardSection.style.display = 'block';
        if (activatedCardNumber) activatedCardNumber.textContent = userActiveCardMasked;
    } else {
        noCardSection.style.display = 'block';
        hasCardSection.style.display = 'none';
    }
}

document.addEventListener('DOMContentLoaded', async function () {

    safeAddEventListener('logout', 'click', () => { showInfo("Cerrando sesión..."); clearToken(); setTimeout(() => { location.href = 'login.html'; }, 1000); });
    safeAddEventListener('walletPEN', 'click', () => switchWallet('PEN'));
    safeAddEventListener('walletUSD', 'click', () => switchWallet('USD'));
    safeAddEventListener('btnDeposit', 'click', async () => { await checkActiveCard(); openModal('modalDeposit'); });
    safeAddEventListener('btnTransferQuick', 'click', () => { window.location.href = 'transfer.html'; });

    safeAddEventListener('btnWithdraw', 'click', async () => {
        await checkActiveCard();
        if (userHasActiveCard) {
            if (document.getElementById('withdrawCardNumber')) document.getElementById('withdrawCardNumber').textContent = userActiveCardMasked;
            if (document.getElementById('formWithdraw')) document.getElementById('formWithdraw').reset();
            openModal('modalWithdraw');
        } else {
            showError("Necesitas tener una tarjeta activa para poder retirar dinero.");
        }
    });

    safeAddEventListener('btnConvert', 'click', async () => {
        openModal('modalConvert');
        if (document.getElementById('convertFrom')) document.getElementById('convertFrom').value = 'PEN';
        if (document.getElementById('convertTo')) document.getElementById('convertTo').value = 'USD';
        if (document.getElementById('convertAmount')) document.getElementById('convertAmount').value = '';
        if (document.getElementById('convertPreview')) document.getElementById('convertPreview').style.display = 'none';
        await loadExchangeRate();
    });

    safeAddEventListener('convertFrom', 'change', async (e) => {
        const fromCurrency = e.target.value;
        const toCurrency = (fromCurrency === 'PEN') ? 'USD' : 'PEN';
        if (document.getElementById('convertTo')) document.getElementById('convertTo').value = toCurrency;
        await loadExchangeRate(fromCurrency, toCurrency);
    });

    safeAddEventListener('convertAmount', 'input', (e) => {
        const amount = parseFloat(e.target.value) || 0;
        const preview = document.getElementById('convertPreview');
        if (amount > 0 && currentExchangeRate) {
            const result = amount * currentExchangeRate;
            const to = document.getElementById('convertTo')?.value;
            if (document.getElementById('convertResult')) document.getElementById('convertResult').textContent = result.toFixed(2) + ' ' + to;
            if (preview) preview.style.display = 'block';
        } else {
            if (preview) preview.style.display = 'none';
        }
    });

    safeAddEventListener('btnHistory', 'click', async () => {
        await loadDetailedTransactions(1, true);
        openModal('modalHistory');
    });

    safeAddEventListener('btnViewRecentDetails', 'click', async () => {
        recentTransactionsPage = 1;
        openModal('modalRecentDetails');
        await loadRecentDetails();
    });

    safeAddEventListener('btnOpenHistoryFromRecent', 'click', () => {
        closeModal('modalRecentDetails');
        const btnHistory = document.getElementById('btnHistory');
        if (btnHistory) setTimeout(() => btnHistory.click(), 200);
    });

    safeAddEventListener('btnActivateCard', 'click', () => {
        closeModal('modalDeposit');
        setTimeout(() => openModal('modalActivateCard'), 200);
    });

    safeAddEventListener('btnHistoryLoadMore', 'click', () => loadDetailedTransactions(null, false));
    safeAddEventListener('historyFilterType', 'change', () => loadDetailedTransactions(1, true));
    safeAddEventListener('historyFilterCurrency', 'change', () => loadDetailedTransactions(1, true));
    safeAddEventListener('btnRecentLoadMore', 'click', () => { recentTransactionsPage++; loadRecentDetails(); });

    safeAddEventListener('formActivateCard', 'submit', async (e) => {
        e.preventDefault();
        const btn = e.target.querySelector('button[type="submit"]');
        btn.disabled = true; btn.innerHTML = 'Validando...';
        try {
            const response = await api('/api/wallet/activar-tarjeta', {
                method: 'POST', auth: true,
                body: {
                    numeroTarjeta: document.getElementById('cardNumber').value.trim(),
                    cvv: document.getElementById('cardCVV').value.trim(),
                    fechaVencimiento: document.getElementById('cardExpiry').value.trim(),
                    nombreTitular: document.getElementById('cardHolder').value.trim().toUpperCase()
                }
            });
            if (response.success) {
                showSuccess('¡Tarjeta activada! ');
                userHasActiveCard = true;
                userActiveCardMasked = response.numeroTarjetaEnmascarado;
                closeModal('modalActivateCard');
                e.target.reset();
                setTimeout(() => { updateDepositModalView(); openModal('modalDeposit'); }, 500);
            } else { showError(response.mensaje || 'Error al activar'); }
        } catch (err) { showError(err.message); } finally { btn.disabled = false; btn.innerHTML = 'Activar Tarjeta'; }
    });

    safeAddEventListener('formDeposit', 'submit', async (e) => {
        e.preventDefault();
        try {
            const response = await api('/api/wallet/deposit', {
                method: 'POST', auth: true,
                body: {
                    amount: parseFloat(document.getElementById('depositAmount').value),
                    description: document.getElementById('depositDesc').value || 'Depósito'
                }
            });
            showSuccess('Depósito exitoso');
            closeModal('modalDeposit');
            e.target.reset();
            await loadAllWallets();
            await loadTransactions(1);
            showTransactionDetail(response.data || response);
        } catch (err) { showError(err.message); }
    });

    safeAddEventListener('formConvert', 'submit', async (e) => {
        e.preventDefault();
        try {
            const response = await api('/api/wallet/convert', {
                method: 'POST', auth: true,
                body: {
                    fromCurrency: document.getElementById('convertFrom').value,
                    toCurrency: document.getElementById('convertTo').value,
                    amount: parseFloat(document.getElementById('convertAmount').value),
                    description: document.getElementById('convertDesc').value || 'Conversión'
                }
            });
            showSuccess('Conversión exitosa');
            closeModal('modalConvert');
            await loadAllWallets();
            await loadTransactions(1);
            showTransactionDetail(response.data || response);
        } catch (err) { showError(err.message); }
    });

    safeAddEventListener('formWithdraw', 'submit', async (e) => {
        e.preventDefault();
        try {
            const response = await api('/api/wallet/withdraw', {
                method: 'POST', auth: true,
                body: {
                    amount: parseFloat(document.getElementById('withdrawAmount').value),
                    description: document.getElementById('withdrawDesc').value || 'Retiro'
                }
            });
            showSuccess('Retiro exitoso');
            closeModal('modalWithdraw');
            e.target.reset();
            await loadAllWallets();
            await loadTransactions(1);
            showTransactionDetail(response.data || response);
        } catch (err) { showError(err.message); }
    });

    try {
        await updateUserHeader();
        await loadAllWallets();
        loadTransactions(1);
        checkActiveCard();


        const loadingEl = document.getElementById('loading');
        const contentEl = document.getElementById('content');
        if (loadingEl) loadingEl.classList.add('hidden');
        if (contentEl) contentEl.classList.remove('hidden');

    } catch (err) {
        showError("Error al iniciar la aplicación");
        const loadingEl = document.getElementById('loading');
        if (loadingEl) loadingEl.classList.add('hidden');
    }
});


if (window.location.pathname.includes('transfer.html')) {
    let walletBalances = { PEN: 0, USD: 0 };

    async function loadTransferPageData() {
        try {

            const response = await api('/api/wallet/all', { auth: true });
            const wallets = response.data || response;

            wallets.forEach(w => {
                walletBalances[w.currency] = w.balance || 0;
            });

            updateTransferBalance('PEN');

            const loadingEl = document.getElementById('loading');
            const contentEl = document.getElementById('content');
            if (loadingEl) loadingEl.classList.add('hidden');
            if (contentEl) contentEl.classList.remove('hidden');

        } catch (err) {
            showError("Error al cargar información de la wallet");
            const loadingEl = document.getElementById('loading');
            if (loadingEl) loadingEl.classList.add('hidden');
        }
    }

    function updateTransferBalance(currency) {
        const balance = walletBalances[currency] || 0;
        const balanceEl = document.getElementById('transferAvailableBalance');
        const currencyLabelEl = document.getElementById('transferCurrencyLabel');
        const currencySymbolEl = document.getElementById('currencySymbol');

        if (balanceEl) {
            balanceEl.textContent = formatCurrency(balance, currency);
        }
        if (currencyLabelEl) {
            currencyLabelEl.textContent = currency;
        }
        if (currencySymbolEl) {
            currencySymbolEl.textContent = currency === 'PEN' ? 'S/' : '$';
        }
    }

    function formatCurrency(amount, currency) {
        return new Intl.NumberFormat('es-PE', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        }).format(amount);
    }

    // Event listener para cambio de moneda
    document.addEventListener('DOMContentLoaded', () => {
        const currencySelect = document.getElementById('transferCurrency');
        if (currencySelect) {
            currencySelect.addEventListener('change', (e) => {
                updateTransferBalance(e.target.value);
            });
        }

        // Validación de destinatario en tiempo real
        const transferToInput = document.getElementById('transferTo');
        let recipientData = null;
        let validationTimeout = null;

        if (transferToInput) {
            transferToInput.addEventListener('input', (e) => {
                const value = e.target.value.trim();

                // Limpiar timeout anterior
                if (validationTimeout) clearTimeout(validationTimeout);

                // Ocultar mensajes previos
                const validEl = document.getElementById('recipientValidation');
                const errorEl = document.getElementById('recipientError');
                if (validEl) validEl.classList.add('hidden');
                if (errorEl) errorEl.classList.add('hidden');
                recipientData = null;

                // Validar solo si hay suficiente texto
                if (value.length < 3) return;

                // Debounce: esperar 500ms después de que el usuario deje de escribir
                validationTimeout = setTimeout(async () => {
                    try {
                        const response = await api(`/api/wallet/validate-recipient?id=${encodeURIComponent(value)}`, { auth: true });
                        const data = response.data || response;

                        // Guardar datos del destinatario
                        recipientData = data;

                        // Mostrar validación exitosa
                        const nameEl = document.getElementById('recipientName');
                        if (nameEl) {
                            nameEl.textContent = `${data.firstName} ${data.lastName}`.trim() || data.email;
                        }
                        if (validEl) validEl.classList.remove('hidden');
                        if (errorEl) errorEl.classList.add('hidden');
                    } catch (err) {
                        // Mostrar error
                        recipientData = null;
                        const errorMsgEl = document.getElementById('recipientErrorMsg');
                        if (errorMsgEl) {
                            errorMsgEl.textContent = err.message || 'Usuario no encontrado';
                        }
                        if (errorEl) errorEl.classList.remove('hidden');
                        if (validEl) validEl.classList.add('hidden');
                    }
                }, 500);
            });
        }

        // Manejar submit del formulario
        const transferForm = document.getElementById('formTransferStep1');
        if (transferForm) {
            transferForm.addEventListener('submit', async (e) => {
                e.preventDefault();

                // Validar que el destinatario esté validado
                if (!recipientData) {
                    showError('Por favor, ingresa un destinatario válido');
                    return;
                }

                // Obtener datos del formulario
                const transferTo = document.getElementById('transferTo').value.trim();
                const currency = document.getElementById('transferCurrency').value;
                const amount = parseFloat(document.getElementById('transferAmount').value);
                const description = document.getElementById('transferDesc').value.trim();

                // Validaciones
                if (!transferTo || !currency || !amount || !description) {
                    showError('Por favor, completa todos los campos');
                    return;
                }

                if (amount <= 0) {
                    showError('El monto debe ser mayor a 0');
                    return;
                }

                const availableBalance = walletBalances[currency] || 0;
                if (amount > availableBalance) {
                    showError(`Saldo insuficiente. Disponible: ${currency} ${formatCurrency(availableBalance, currency)}`);
                    return;
                }

                // Guardar datos en sessionStorage para la página de confirmación
                const transferData = {
                    recipientId: recipientData.id,
                    recipientName: `${recipientData.firstName} ${recipientData.lastName}`.trim() || recipientData.email,
                    recipientEmail: recipientData.email,
                    recipientIdentifier: transferTo,
                    currency: currency,
                    amount: amount,
                    description: description,
                    timestamp: Date.now()
                };

                sessionStorage.setItem('pendingTransfer', JSON.stringify(transferData));

                // Redirigir a la página de confirmación
                window.location.href = 'confirm-transfer.html';
            });
        }

        // Cargar datos iniciales
        loadTransferPageData();
    });
}

// CONFIRM TRANSFER PAGE INITIALIZATION
if (window.location.pathname.includes('confirm-transfer.html')) {
    document.addEventListener('DOMContentLoaded', async () => {
        // Recuperar datos
        const transferDataStr = sessionStorage.getItem('pendingTransfer');
        if (!transferDataStr) {
            location.href = 'wallet.html';
            return;
        }

        const data = JSON.parse(transferDataStr);
        let is2FAEnabled = false;

        // Verificar estado de 2FA del usuario
        try {
            const userRes = await api('/api/auth/me', { auth: true });
            const userData = userRes.data || userRes;
            is2FAEnabled = userData.twoFactorEnabled || false;

            if (is2FAEnabled) {
                document.getElementById('2faSection')?.classList.remove('hidden');
            }
        } catch (err) {
            console.error("Error verificando 2FA:", err);
        }

        // Mostrar datos
        document.getElementById('summaryTransferAmount').textContent = formatCurrency(data.amount, data.currency);
        document.getElementById('summaryTransferTo').textContent = `${data.recipientName} (${data.recipientIdentifier})`;
        document.getElementById('summaryTransferFrom').textContent = `Wallet Principal (${data.currency})`;

        if (data.description) {
            document.getElementById('summaryTransferDesc').textContent = data.description;
            document.getElementById('summaryDescContainer').style.display = 'flex';
        }

        function formatCurrency(amount, currency) {
            const symbol = currency === 'PEN' ? 'S/' : '$';
            return `${symbol} ${new Intl.NumberFormat('es-PE', { minimumFractionDigits: 2 }).format(amount)}`;
        }

        // Manejar confirmación
        const form = document.getElementById('formConfirmTransfer');
        if (form) {
            form.addEventListener('submit', async (e) => {
                e.preventDefault();

                const tokenInput = document.getElementById('token2fa');
                const token2fa = tokenInput ? tokenInput.value.trim() : null;

                if (is2FAEnabled) {
                    if (!token2fa || token2fa.length !== 6) {
                        showError('Por favor, ingresa el código 2FA de 6 dígitos');
                        return;
                    }
                }

                const btn = document.getElementById('btnTransferSubmit');
                const originalText = btn.innerHTML;
                btn.disabled = true;
                btn.innerHTML = '<div class="spinner-border spinner-border-sm"></div> Procesando...';

                try {
                    const payload = {
                        toUsername: data.recipientIdentifier,
                        amount: data.amount,
                        description: data.description,
                        currency: data.currency,
                        token2fa: is2FAEnabled ? token2fa : null
                    };

                    const response = await api('/api/wallet/transfer', {
                        method: 'POST',
                        body: payload,
                        auth: true
                    });

                    // Éxito
                    const responseData = response.data || response;
                    sessionStorage.removeItem('pendingTransfer');
                    sessionStorage.setItem('voucherData', JSON.stringify(responseData));

                    showSuccess('¡Transferencia realizada!');
                    setTimeout(() => {
                        location.href = 'voucher.html';
                    }, 1000);

                } catch (err) {
                    console.error("Error en transferencia:", err);
                    showError(err.message || 'Error al procesar la transferencia');
                    btn.disabled = false;
                    btn.innerHTML = originalText;
                }
            });
        }
    });

    // Función global para el botón volver
    window.goBack = function () {
        history.back();
    };
}