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
        const response = await fetch(config.apiBaseUrl + '/disputes/all?page=0&size=100', {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin'
        });
        if (!response.ok) {
            throw new Error('Failed to fetch disputes: ' + response.statusText);
        }
        const data = await response.json();
        return data.content || [];
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
        state.stats.resolved = disputes.filter(function(d) { return d.status === 'RESOLVED' || d.status === 'CANCELLED'; }).length;

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
            container.innerHTML = '<tr><td colspan="6" class="py-8 text-center text-gray-500"><i class="ph ph-check-circle text-4xl text-success mb-2"></i><p>Không có khiếu nại nào</p></td></tr>';
            return;
        }

        var html = '';
        for (var i = 0; i < disputes.length; i++) {
            var dispute = disputes[i];

            var statusLabel = dispute.status || 'N/A';
            var statusClass = 'bg-gray-100 text-gray-600';
            var statusIcon = '<i class="ph-fill ph-question text-sm"></i>';
            if (statusLabel === 'OPENED') {
                statusClass = 'bg-red-100 text-error';
                statusIcon = '<i class="ph-fill ph-warning-circle text-sm"></i>';
            } else if (statusLabel === 'UNDER_REVIEW') {
                statusClass = 'bg-orange-100 text-warning';
                statusIcon = '<i class="ph-fill ph-magnifying-glass text-sm"></i>';
            } else if (statusLabel === 'RESOLVED') {
                statusClass = 'bg-green-100 text-success';
                statusIcon = '<i class="ph-fill ph-check-circle text-sm"></i>';
            } else if (statusLabel === 'CANCELLED') {
                statusClass = 'bg-gray-100 text-gray-500';
                statusIcon = '<i class="ph-fill ph-x-circle text-sm"></i>';
            }

            html += '<tr class="hover:bg-gray-50 transition-colors group border-b border-gray-50">';
            html += '<td class="py-4 px-4">';
            html += '<div class="font-mono text-sm font-bold text-gray-900">' + (dispute.disputeNumber || 'N/A') + '</div>';
            html += '<div class="text-sm font-bold text-primary">' + formatCurrency(dispute.orderAmount) + '</div>';
            html += '</td>';
            html += '<td class="py-4 px-4">';
            html += '<div class="font-bold text-gray-900 text-sm">' + (dispute.reason || 'Không có lý do') + '</div>';
            html += '<div class="text-xs text-gray-400 mt-0.5"><i class="ph ph-tag"></i> ' + (dispute.disputeType || 'N/A') + '</div>';
            html += '</td>';
            html += '<td class="py-4 px-4">';
            html += '<div class="flex items-center gap-2 text-sm">';
            html += '<span class="font-bold text-blue-600">' + (dispute.buyerName || 'N/A') + '</span>';
            html += '<i class="ph-bold ph-arrow-right text-gray-400 text-xs"></i>';
            html += '<span class="font-bold text-purple-600">' + (dispute.sellerName || 'N/A') + '</span>';
            html += '</div>';
            html += '</td>';
            html += '<td class="py-4 px-4">';
            html += '<span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold ' + statusClass + '">';
            html += statusIcon;
            html += statusLabel.replace('_', ' ');
            html += '</span>';
            html += '</td>';
            html += '<td class="py-4 px-4">';
            html += '<span class="text-xs font-medium text-gray-500"><i class="ph-bold ph-clock"></i> ' + formatRelativeTime(dispute.openedAt) + '</span>';
            html += '</td>';
            html += '<td class="py-4 px-4 text-right">';
            if (dispute.status === 'OPENED' || dispute.status === 'UNDER_REVIEW') {
                html += '<button onclick="AdminDisputes.intervene(' + dispute.disputeId + ')" class="text-xs font-bold bg-error text-white px-3 py-2 rounded-lg shadow-sm hover:bg-red-600 transition-colors">Can thiệp</button>';
            } else {
                html += '<button onclick="AdminDisputes.intervene(' + dispute.disputeId + ')" class="text-xs font-bold btn-outline px-3 py-2 rounded-lg transition-colors">Xem chi tiết</button>';
            }
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

    /**
     * Navigate to dispute detail page for intervention
     * @param {number} disputeId - The dispute ID to intervene
     */
    function intervene(disputeId) {
        window.location.href = '/admin/disputes/' + disputeId;
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
        },
        intervene: intervene
    };
})();
