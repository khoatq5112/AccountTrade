/**
 * Admin Disputes Module
 */
const AdminDisputes = (function() {
    const config = {
        apiBaseUrl: '/api/admin',
        refreshInterval: 60000
    };

    const state = {
        disputes: [],
        filteredDisputes: [],
        stats: { total: 0, open: 0, investigating: 0, resolved: 0 },
        filters: { status: '' },
        loading: false,
        error: null
    };

    async function fetchDisputes() {
        const response = await fetch(config.apiBaseUrl + '/disputes?limit=100', {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin'
        });
        if (!response.ok) {
            throw new Error('Failed to fetch disputes: ' + response.statusText);
        }
        return response.json();
    }

    function showLoading() {
        state.loading = true;
        var loadingEl = document.getElementById('disputes-loading');
        var contentEl = document.getElementById('disputes-content');
        var errorEl = document.getElementById('disputes-error');
        if (loadingEl) loadingEl.classList.remove('hidden');
        if (contentEl) contentEl.classList.add('hidden');
        if (errorEl) errorEl.classList.add('hidden');
    }

    function hideLoading() {
        state.loading = false;
        var loadingEl = document.getElementById('disputes-loading');
        var contentEl = document.getElementById('disputes-content');
        if (loadingEl) loadingEl.classList.add('hidden');
        if (contentEl) contentEl.classList.remove('hidden');
    }

    function showError(message) {
        state.error = message;
        var loadingEl = document.getElementById('disputes-loading');
        var contentEl = document.getElementById('disputes-content');
        var errorEl = document.getElementById('disputes-error');
        if (loadingEl) loadingEl.classList.add('hidden');
        if (contentEl) contentEl.classList.add('hidden');
        if (errorEl) {
            errorEl.innerHTML = '<div class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg"><i class="ph-fill ph-warning-circle"></i> ' + message + '</div>';
            errorEl.classList.remove('hidden');
        }
    }

    function formatCurrency(amount) {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);
    }

    function formatRelativeTime(dateString) {
        var date = new Date(dateString);
        var now = new Date();
        var diffMs = now - date;
        var diffMins = Math.floor(diffMs / 60000);
        var diffHours = Math.floor(diffMs / 3600000);
        var diffDays = Math.floor(diffMs / 86400000);
        if (diffMins < 1) return 'Vừa xong';
        if (diffMins < 60) return diffMins + ' phút trước';
        if (diffHours < 24) return diffHours + ' giờ trước';
        if (diffDays < 7) return diffDays + ' ngày trước';
        return date.toLocaleDateString('vi-VN');
    }

    function updateStats(disputes) {
        state.stats.total = disputes.length;
        state.stats.open = disputes.filter(function(d) { return d.status === 'OPENED'; }).length;
        state.stats.investigating = disputes.filter(function(d) { return d.status === 'UNDER_REVIEW'; }).length;
        state.stats.resolved = disputes.filter(function(d) { return d.status === 'RESOLVED'; }).length;

        var totalEl = document.getElementById('stat-total-disputes');
        var openEl = document.getElementById('stat-open-disputes');
        var investigatingEl = document.getElementById('stat-investigating');
        var resolvedEl = document.getElementById('stat-resolved');

        if (totalEl) totalEl.textContent = state.stats.total;
        if (openEl) openEl.textContent = state.stats.open;
        if (investigatingEl) investigatingEl.textContent = state.stats.investigating;
        if (resolvedEl) resolvedEl.textContent = state.stats.resolved;
    }

    function renderDisputes(disputes) {
        var container = document.getElementById('disputes-table-body');
        if (!container) return;

        if (disputes.length === 0) {
            container.innerHTML = '<tr><td colspan="5" class="py-8 text-center text-gray-500"><i class="ph ph-check-circle text-4xl text-success mb-2"></i><p>Không có khiếu nại nào</p></td></tr>';
            return;
        }

        var html = '';
        for (var i = 0; i < disputes.length; i++) {
            var dispute = disputes[i];
            html += '<tr class="hover:bg-red-50/50 transition-colors group">';
            html += '<td class="py-4 px-4">';
            html += '<div class="font-mono text-sm font-bold text-gray-900">#' + (dispute.orderNumber || 'N/A') + '</div>';
            html += '<div class="text-sm font-bold text-primary">' + formatCurrency(dispute.orderAmount) + '</div>';
            html += '</td>';
            html += '<td class="py-4 px-4">';
            html += '<div class="font-bold text-gray-900 text-sm">' + (dispute.reason || 'Không có lý do') + '</div>';
            html += '</td>';
            html += '<td class="py-4 px-4">';
            html += '<div class="flex items-center gap-2 text-sm">';
            html += '<span class="font-bold text-gray-900">' + (dispute.buyerName || 'N/A') + '</span>';
            html += '<i class="ph-bold ph-arrow-right text-gray-400"></i>';
            html += '<span class="font-bold text-gray-900">' + (dispute.sellerName || 'N/A') + '</span>';
            html += '</div>';
            html += '</td>';
            html += '<td class="py-4 px-4">';
            html += '<span class="text-xs font-bold text-error"><i class="ph-bold ph-clock"></i> ' + formatRelativeTime(dispute.openedAt) + '</span>';
            html += '</td>';
            html += '<td class="py-4 px-4 text-right">';
            html += '<button class="text-xs font-bold bg-error text-white px-3 py-2 rounded shadow-sm hover:bg-red-600">Can thiệp</button>';
            html += '</td>';
            html += '</tr>';
        }
        container.innerHTML = html;
    }

    function applyFilters() {
        var filtered = state.disputes.slice();
        if (state.filters.status) {
            filtered = filtered.filter(function(d) { return d.status === state.filters.status; });
        }
        state.filteredDisputes = filtered;
        renderDisputes(filtered);
    }

    async function loadDisputes() {
        showLoading();
        try {
            var disputes = await fetchDisputes();
            state.disputes = disputes;
            updateStats(disputes);
            applyFilters();
            hideLoading();
        } catch (error) {
            console.error('Error loading disputes:', error);
            showError(error.message);
        }
    }

    return {
        init: function() {
            console.log('Initializing Admin Disputes...');
            loadDisputes();
            setInterval(loadDisputes, config.refreshInterval);
        },
        refresh: function() {
            loadDisputes();
        },
        filterByStatus: function(status) {
            state.filters.status = status;
            applyFilters();
        }
    };
})();
