/**
 * AdminTransactions Module - Handles loading and displaying order transaction history for admin dashboard.
 * All transactions are Orders in the system.
 */
const AdminTransactions = (function() {
    'use strict';

    const config = {
        apiBaseUrl: '/api/admin'
    };

    const state = {
        keyword: null,
        status: null,
        from: null,
        to: null,
        page: 0,
        pageSize: 20,
        totalElements: 0,
        totalPages: 0,
        loading: false,
        transactions: []
    };

    // CSRF helpers
    function getCsrfToken() {
        const meta = document.querySelector('meta[name="_csrf"]');
        return meta ? meta.getAttribute('content') : '';
    }

    function getCsrfHeader() {
        const meta = document.querySelector('meta[name="_csrf_header"]');
        return meta ? meta.getAttribute('content') : 'X-CSRF-TOKEN';
    }

    function getHeaders() {
        const headers = {
            'Accept': 'application/json'
        };
        headers[getCsrfHeader()] = getCsrfToken();
        return headers;
    }

    // Show/hide loading states
    function showLoading() {
        document.getElementById('table-loading').classList.remove('hidden');
        document.getElementById('table-content').classList.add('hidden');
        document.getElementById('table-error').classList.add('hidden');
    }

    function hideLoading() {
        document.getElementById('table-loading').classList.add('hidden');
    }

    function showContent() {
        hideLoading();
        document.getElementById('table-content').classList.remove('hidden');
        document.getElementById('table-error').classList.add('hidden');
    }

    function showError(message) {
        hideLoading();
        document.getElementById('table-content').classList.add('hidden');
        document.getElementById('table-error').classList.remove('hidden');
        document.getElementById('error-message').textContent = message || 'Có lỗi xảy ra khi tải dữ liệu';
    }

    // Format helpers
    function formatCurrency(amount) {
        if (amount === null || amount === undefined) return '0₫';
        return new Intl.NumberFormat('vi-VN', {
            style: 'currency',
            currency: 'VND'
        }).format(amount);
    }

    function formatNumber(num) {
        if (num === null || num === undefined) return '0';
        return new Intl.NumberFormat('vi-VN').format(num);
    }

    function formatDateTime(dateString) {
        if (!dateString) return '--';
        const date = new Date(dateString);
        const hours = date.getHours().toString().padStart(2, '0');
        const minutes = date.getMinutes().toString().padStart(2, '0');
        const day = date.getDate().toString().padStart(2, '0');
        const month = (date.getMonth() + 1).toString().padStart(2, '0');
        const year = date.getFullYear();
        return `${hours}:${minutes} ${day}/${month}/${year}`;
    }

    function formatDateShort(dateString) {
        if (!dateString) return '--';
        const date = new Date(dateString);
        const day = date.getDate().toString().padStart(2, '0');
        const month = (date.getMonth() + 1).toString().padStart(2, '0');
        const year = date.getFullYear();
        return `${day}/${month}/${year}`;
    }

    // Status configurations for Order statuses
    function getStatusConfig(status) {
        const configs = {
            'PENDING': { bgColor: 'bg-yellow-100', textColor: 'text-yellow-700', icon: 'ph-clock', text: 'Chờ xử lý' },
            'AWAITING_PAYMENT': { bgColor: 'bg-yellow-100', textColor: 'text-yellow-700', icon: 'ph-clock', text: 'Chờ thanh toán' },
            'PAID': { bgColor: 'bg-blue-100', textColor: 'text-blue-700', icon: 'ph-check-circle', text: 'Đã thanh toán' },
            'PROCESSING': { bgColor: 'bg-blue-100', textColor: 'text-blue-700', icon: 'ph-arrows-clockwise', text: 'Đang xử lý' },
            'CREDENTIAL_ASSIGNED': { bgColor: 'bg-blue-100', textColor: 'text-blue-700', icon: 'ph-key', text: 'Đã giao tài khoản' },
            'AWAITING_BUYER_CONFIRMATION': { bgColor: 'bg-yellow-100', textColor: 'text-yellow-700', icon: 'ph-hourglass-medium', text: 'Chờ xác nhận' },
            'COMPLETED': { bgColor: 'bg-green-100', textColor: 'text-green-700', icon: 'ph-check-circle', text: 'Hoàn thành' },
            'CANCELLED': { bgColor: 'bg-gray-100', textColor: 'text-gray-600', icon: 'ph-x', text: 'Đã hủy' },
            'REFUNDED': { bgColor: 'bg-orange-100', textColor: 'text-orange-700', icon: 'ph-arrow-counter-clockwise', text: 'Hoàn tiền' },
            'DISPUTED': { bgColor: 'bg-red-100', textColor: 'text-red-700', icon: 'ph-warning', text: 'Khiếu nại' },
            'PAYMENT_FAILED': { bgColor: 'bg-red-100', textColor: 'text-red-700', icon: 'ph-x-circle', text: 'Thanh toán thất bại' },
            'PAYMENT_EXPIRED': { bgColor: 'bg-gray-100', textColor: 'text-gray-600', icon: 'ph-clock', text: 'Hết hạn thanh toán' }
        };
        return configs[status] || { bgColor: 'bg-gray-100', textColor: 'text-gray-600', icon: 'ph-circle', text: status || 'N/A' };
    }

    // API Functions
    async function fetchTransactions() {
        showLoading();
        state.loading = true;

        try {
            const params = new URLSearchParams();
            if (state.keyword) params.append('keyword', state.keyword);
            if (state.status) params.append('status', state.status);
            if (state.from) params.append('from', state.from);
            if (state.to) params.append('to', state.to);
            params.append('page', state.page.toString());
            params.append('size', state.pageSize.toString());

            const response = await fetch(`${config.apiBaseUrl}/transactions?${params.toString()}`, {
                method: 'GET',
                headers: getHeaders(),
                credentials: 'same-origin'
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const contentType = response.headers.get('Content-Type') || '';
            if (!contentType.includes('application/json')) {
                const errorText = await response.text();
                console.error('Server returned non-JSON response. Content-Type:', contentType);
                throw new Error('Server trả về phản hồi không hợp lệ. Vui lòng thử lại.');
            }

            const data = await response.json();
            state.transactions = data.content || [];
            state.totalElements = data.totalElements || 0;
            state.totalPages = data.totalPages || 0;

            renderTable(state.transactions);
            renderPagination();
            showContent();

            // Show/hide empty state
            const emptyState = document.getElementById('empty-state');
            if (state.transactions.length === 0) {
                emptyState.classList.remove('hidden');
            } else {
                emptyState.classList.add('hidden');
            }

        } catch (error) {
            console.error('Error fetching transactions:', error);
            showError(error.message);
        } finally {
            state.loading = false;
        }
    }

    async function loadStats() {
        try {
            const response = await fetch(`${config.apiBaseUrl}/transactions/stats`, {
                method: 'GET',
                headers: getHeaders(),
                credentials: 'same-origin'
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const contentType = response.headers.get('Content-Type') || '';
            if (!contentType.includes('application/json')) {
                console.error('Server returned non-JSON response for stats');
                return;
            }

            const stats = await response.json();
            renderStats(stats);

        } catch (error) {
            console.error('Error loading stats:', error);
        }
    }

    // Render Functions
    function renderStats(stats) {
        document.getElementById('stat-total').textContent = formatNumber(stats.totalTransactions || 0);
        document.getElementById('stat-success').textContent = formatNumber(stats.successfulTransactions || 0);
        document.getElementById('stat-pending').textContent = formatNumber(stats.pendingTransactions || 0);
        document.getElementById('stat-frozen').textContent = formatNumber(stats.frozenTransactions || 0);
        document.getElementById('stat-refunded').textContent = formatNumber(stats.refundedTransactions || 0);
    }

    function buildRow(tx) {
        const statusConfig = getStatusConfig(tx.status);

        const row = document.createElement('tr');
        row.className = 'border-b border-gray-100 hover:bg-gray-50 transition-colors';

        // Order number
        const orderCell = document.createElement('td');
        orderCell.className = 'py-3 px-4';
        const orderNum = tx.orderInfo ? tx.orderInfo.orderNumber : tx.transactionId;
        orderCell.innerHTML = `<span class="font-mono text-sm font-bold text-primary cursor-pointer hover:underline" onclick="AdminTransactions.viewTransaction('${tx.transactionId}')">#${orderNum || '--'}</span>`;
        row.appendChild(orderCell);

        // Status
        const statusCell = document.createElement('td');
        statusCell.className = 'py-3 px-4';
        statusCell.innerHTML = `
            <span class="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-bold ${statusConfig.bgColor} ${statusConfig.textColor}">
                <i class="ph-fill ${statusConfig.icon}"></i>
                ${statusConfig.text}
            </span>
        `;
        row.appendChild(statusCell);

        // Amount
        const amountCell = document.createElement('td');
        amountCell.className = 'py-3 px-4 font-bold text-gray-900';
        amountCell.textContent = formatCurrency(tx.amount);
        row.appendChild(amountCell);

        // Buyer
        const buyerCell = document.createElement('td');
        buyerCell.className = 'py-3 px-4';
        buyerCell.innerHTML = tx.buyerInfo ? `
            <div class="flex items-center gap-2">
                <div class="w-8 h-8 rounded-full bg-blue-100 text-primary flex items-center justify-center font-bold text-xs">
                    ${(tx.buyerInfo.username || 'N/A').substring(0, 2).toUpperCase()}
                </div>
                <span class="text-sm text-gray-700">${tx.buyerInfo.username || 'N/A'}</span>
            </div>
        ` : '<span class="text-gray-400">--</span>';
        row.appendChild(buyerCell);

        // Seller(s) - handle multiple sellers
        const sellerCell = document.createElement('td');
        sellerCell.className = 'py-3 px-4';
        if (tx.sellerInfo) {
            // Show comma-separated sellers if multiple (sellerInfo is UserInfo with username field)
            const sellers = [];
            if (tx.sellerInfo.username) sellers.push(tx.sellerInfo.username);
            // Also check if there's an additionalSellers array (not in current DTO but future-proof)
            if (tx.additionalSellers) {
                tx.additionalSellers.forEach(s => sellers.push(s.username || s));
            }
            if (sellers.length > 0) {
                sellerCell.innerHTML = `
                    <div class="flex items-center gap-2">
                        <div class="w-8 h-8 rounded-full bg-green-100 text-success flex items-center justify-center font-bold text-xs">
                            ${(sellers[0] || 'N/A').substring(0, 2).toUpperCase()}
                        </div>
                        <span class="text-sm text-gray-700" title="${sellers.join(', ')}">${sellers.length > 1 ? sellers[0] + ' +' + (sellers.length - 1) : sellers[0]}</span>
                    </div>
                `;
            } else {
                sellerCell.innerHTML = '<span class="text-gray-400">--</span>';
            }
        } else {
            sellerCell.innerHTML = '<span class="text-gray-400">--</span>';
        }
        row.appendChild(sellerCell);

        // Created date
        const dateCell = document.createElement('td');
        dateCell.className = 'py-3 px-4 text-sm text-gray-600 whitespace-nowrap';
        dateCell.textContent = formatDateTime(tx.createdAt);
        row.appendChild(dateCell);

        // Actions
        const actionsCell = document.createElement('td');
        actionsCell.className = 'py-3 px-4';
        actionsCell.innerHTML = `
            <button onclick="AdminTransactions.viewTransaction('${tx.transactionId}')"
                class="text-primary hover:text-blue-700 font-bold text-sm flex items-center gap-1">
                <i class="ph ph-eye"></i> Xem
            </button>
        `;
        row.appendChild(actionsCell);

        return row;
    }

    function renderTable(transactions) {
        const tbody = document.getElementById('transactions-table-body');
        tbody.innerHTML = '';

        if (!transactions || transactions.length === 0) {
            return;
        }

        transactions.forEach(tx => {
            tbody.appendChild(buildRow(tx));
        });
    }

    function renderPagination() {
        const paginationInfo = document.getElementById('pagination-info');
        const paginationControls = document.getElementById('pagination-controls');

        // Update info text
        const start = state.page * state.pageSize + 1;
        const end = Math.min((state.page + 1) * state.pageSize, state.totalElements);
        paginationInfo.textContent = `Hiển thị ${start}-${end} của ${state.totalElements} đơn hàng`;

        // Clear and rebuild controls
        paginationControls.innerHTML = '';

        // Previous button
        const prevBtn = document.createElement('button');
        prevBtn.className = `px-3 py-1 rounded-lg text-sm font-bold ${state.page === 0 ? 'text-gray-300 cursor-not-allowed' : 'text-gray-600 hover:bg-gray-100'}`;
        prevBtn.disabled = state.page === 0;
        prevBtn.innerHTML = '<i class="ph ph-caret-left"></i>';
        prevBtn.onclick = () => changePage(state.page - 1);
        paginationControls.appendChild(prevBtn);

        // Page numbers
        const maxVisiblePages = 5;
        let startPage = Math.max(0, state.page - Math.floor(maxVisiblePages / 2));
        let endPage = Math.min(state.totalPages - 1, startPage + maxVisiblePages - 1);

        if (endPage - startPage + 1 < maxVisiblePages) {
            startPage = Math.max(0, endPage - maxVisiblePages + 1);
        }

        for (let i = startPage; i <= endPage; i++) {
            const pageBtn = document.createElement('button');
            pageBtn.className = `px-3 py-1 rounded-lg text-sm font-bold ${i === state.page ? 'bg-primary text-white' : 'text-gray-600 hover:bg-gray-100'}`;
            pageBtn.textContent = (i + 1).toString();
            pageBtn.onclick = () => changePage(i);
            paginationControls.appendChild(pageBtn);
        }

        // Next button
        const nextBtn = document.createElement('button');
        nextBtn.className = `px-3 py-1 rounded-lg text-sm font-bold ${state.page >= state.totalPages - 1 ? 'text-gray-300 cursor-not-allowed' : 'text-gray-600 hover:bg-gray-100'}`;
        nextBtn.disabled = state.page >= state.totalPages - 1;
        nextBtn.innerHTML = '<i class="ph ph-caret-right"></i>';
        nextBtn.onclick = () => changePage(state.page + 1);
        paginationControls.appendChild(nextBtn);
    }

    function changePage(newPage) {
        if (newPage < 0 || newPage >= state.totalPages || state.loading) return;
        state.page = newPage;
        fetchTransactions();
    }

    // View transaction detail
    async function viewTransaction(transactionId) {
        const modal = document.getElementById('transaction-modal');
        const modalContent = document.getElementById('modal-content');
        const modalTitle = document.getElementById('modal-title');

        modalTitle.textContent = `Chi tiết đơn hàng #${transactionId.replace('TXN-ORDER-', '')}`;
        modalContent.innerHTML = `
            <div class="text-center py-8">
                <i class="ph ph-spinner ph-spin text-3xl text-gray-400"></i>
                <p class="text-gray-500 mt-2">Đang tải...</p>
            </div>
        `;
        modal.classList.remove('hidden');

        try {
            const response = await fetch(`${config.apiBaseUrl}/transactions/${transactionId}`, {
                method: 'GET',
                headers: getHeaders(),
                credentials: 'same-origin'
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const contentType = response.headers.get('Content-Type') || '';
            if (!contentType.includes('application/json')) {
                throw new Error('Server trả về phản hồi không hợp lệ.');
            }

            const tx = await response.json();
            renderTransactionDetail(tx);

        } catch (error) {
            console.error('Error loading transaction detail:', error);
            modalContent.innerHTML = `
                <div class="text-center py-8">
                    <i class="ph ph-warning-circle text-3xl text-error"></i>
                    <p class="text-gray-500 mt-2">${error.message}</p>
                </div>
            `;
        }
    }

    function renderTransactionDetail(tx) {
        const modalContent = document.getElementById('modal-content');
        const statusConfig = getStatusConfig(tx.status);

        modalContent.innerHTML = `
            <div class="space-y-4">
                <!-- Status Banner -->
                <div class="flex items-center justify-between p-4 rounded-lg ${statusConfig.bgColor}">
                    <div class="flex items-center gap-3">
                        <i class="ph-fill ${statusConfig.icon} text-2xl ${statusConfig.textColor}"></i>
                        <div>
                            <div class="font-bold ${statusConfig.textColor}">${statusConfig.text}</div>
                            <div class="text-sm opacity-80">Đơn hàng</div>
                        </div>
                    </div>
                    <div class="text-right">
                        <div class="text-2xl font-bold ${statusConfig.textColor}">${formatCurrency(tx.amount)}</div>
                    </div>
                </div>

                <!-- Transaction Info -->
                <div class="grid grid-cols-2 gap-4">
                    <div>
                        <div class="text-sm text-gray-500">Mã giao dịch</div>
                        <div class="font-mono font-bold">#${tx.transactionId || '--'}</div>
                    </div>
                    <div>
                        <div class="text-sm text-gray-500">Ngày tạo</div>
                        <div class="font-bold">${formatDateTime(tx.createdAt)}</div>
                    </div>
                </div>

                <!-- Order Info -->
                ${tx.orderInfo ? `
                <div class="border-t border-gray-200 pt-4">
                    <h3 class="font-bold text-gray-900 mb-3 flex items-center gap-2">
                        <i class="ph ph-receipt text-primary"></i> Thông tin đơn hàng
                    </h3>
                    <div class="grid grid-cols-2 gap-4">
                        <div>
                            <div class="text-sm text-gray-500">Mã đơn hàng</div>
                            <div class="font-mono font-bold">#${tx.orderInfo.orderNumber || '--'}</div>
                        </div>
                        <div>
                            <div class="text-sm text-gray-500">Số lượng</div>
                            <div class="font-bold">${tx.orderInfo.quantity || 0}</div>
                        </div>
                    </div>
                </div>
                ` : ''}

                <!-- Buyer Info -->
                ${tx.buyerInfo ? `
                <div class="border-t border-gray-200 pt-4">
                    <h3 class="font-bold text-gray-900 mb-3 flex items-center gap-2">
                        <i class="ph ph-shopping-cart text-primary"></i> Người mua
                    </h3>
                    <div class="flex items-center gap-3">
                        <div class="w-10 h-10 rounded-full bg-blue-100 text-primary flex items-center justify-center font-bold">
                            ${(tx.buyerInfo.username || 'N/A').substring(0, 2).toUpperCase()}
                        </div>
                        <div>
                            <div class="font-bold">${tx.buyerInfo.username || 'N/A'}</div>
                            <div class="text-sm text-gray-500">${tx.buyerInfo.email || ''}</div>
                        </div>
                    </div>
                </div>
                ` : ''}

                <!-- Seller Info -->
                ${tx.sellerInfo ? `
                <div class="border-t border-gray-200 pt-4">
                    <h3 class="font-bold text-gray-900 mb-3 flex items-center gap-2">
                        <i class="ph ph-store text-success"></i> Người bán
                    </h3>
                    <div class="flex items-center gap-3">
                        <div class="w-10 h-10 rounded-full bg-green-100 text-success flex items-center justify-center font-bold">
                            ${(tx.sellerInfo.username || 'N/A').substring(0, 2).toUpperCase()}
                        </div>
                        <div>
                            <div class="font-bold">${tx.sellerInfo.username || 'N/A'}</div>
                            <div class="text-sm text-gray-500">${tx.sellerInfo.email || ''}</div>
                        </div>
                    </div>
                </div>
                ` : ''}

                <!-- Description -->
                ${tx.description ? `
                <div class="border-t border-gray-200 pt-4">
                    <h3 class="font-bold text-gray-900 mb-2">Mô tả</h3>
                    <p class="text-gray-700 bg-gray-50 p-3 rounded-lg">${tx.description}</p>
                </div>
                ` : ''}

                <!-- Close button -->
                <div class="border-t border-gray-200 pt-4">
                    <button onclick="AdminTransactions.closeModal()" class="btn-outline w-full">
                        Đóng
                    </button>
                </div>
            </div>
        `;
    }

    function closeModal() {
        document.getElementById('transaction-modal').classList.add('hidden');
    }

    // Filter functions
    function applyFilters() {
        state.keyword = document.getElementById('filter-keyword').value.trim() || null;
        state.status = document.getElementById('filter-status').value || null;
        state.from = document.getElementById('filter-from').value || null;
        state.to = document.getElementById('filter-to').value || null;
        state.page = 0; // Reset to first page
        fetchTransactions();
    }

    function clearFilters() {
        document.getElementById('filter-keyword').value = '';
        document.getElementById('filter-status').value = '';
        document.getElementById('filter-from').value = '';
        document.getElementById('filter-to').value = '';
        state.keyword = null;
        state.status = null;
        state.from = null;
        state.to = null;
        state.page = 0;
        fetchTransactions();
    }

    // Export function
    function exportToCSV() {
        const params = new URLSearchParams();
        if (state.keyword) params.append('keyword', state.keyword);
        if (state.status) params.append('status', state.status);
        if (state.from) params.append('from', state.from);
        if (state.to) params.append('to', state.to);
        params.append('export', 'true');

        window.location.href = `${config.apiBaseUrl}/transactions/export?${params.toString()}`;
    }

    // Refresh function
    function refresh() {
        fetchTransactions();
        loadStats();
    }

    // Initialize
    function init() {
        fetchTransactions();
        loadStats();

        // Keyword search on Enter key
        const keywordInput = document.getElementById('filter-keyword');
        if (keywordInput) {
            keywordInput.addEventListener('keydown', function(e) {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    applyFilters();
                }
            });
        }

        // Close modal on escape key
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape') {
                closeModal();
            }
        });

        // Close modal on backdrop click
        document.getElementById('transaction-modal').addEventListener('click', function(e) {
            if (e.target === this) {
                closeModal();
            }
        });
    }

    // Public API
    return {
        init: init,
        refresh: refresh,
        applyFilters: applyFilters,
        clearFilters: clearFilters,
        viewTransaction: viewTransaction,
        closeModal: closeModal,
        exportToCSV: exportToCSV
    };
})();
