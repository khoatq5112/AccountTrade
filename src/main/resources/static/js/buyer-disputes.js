/**
 * Buyer Disputes Module
 * Handles dispute listing and detail viewing for buyer users
 */
const BuyerDisputes = {
    config: {
        apiBaseUrl: '/api/buyer/disputes',
        statusFilter: ''
    },

    state: {
        disputes: [],
        filteredDisputes: [],
        loading: false,
        error: null
    },

    /**
     * Initialize the module
     */
    init() {
        console.log('BuyerDisputes module initialized');
        this.loadDisputes();
    },

    /**
     * Load disputes from API
     */
    async loadDisputes() {
        this.showLoading();
        this.state.loading = true;
        this.state.error = null;

        
        try {
                const response = await fetch(this.config.apiBaseUrl, {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    credentials: 'include'
                });

                if (!response.ok) {
                    throw new Error('Failed to fetch disputes: ' + response.statusText);
                }

                const disputes = await response.json();
                this.state.disputes = disputes;
                this.state.filteredDisputes = disputes;
                this.updateStats(disputes);
                this.renderDisputes(disputes);
                this.hideLoading();
                this.state.loading = false;
            } catch (error) {
                console.error('Error loading disputes:', error);
                this.showError(error.message);
                this.state.loading = false;
                this.state.error = error.message;
            }
    },

    /**
     * Filter disputes by status
     */
    filterByStatus(status) {
        this.config.statusFilter = status;
        this.applyFilters();
    },

    /**
     * Apply filters to current disputes
     */
    applyFilters() {
        var filtered = this.state.disputes.slice();
        
        if (this.config.statusFilter) {
            filtered = filtered.filter(function(d) {
                return d.status === this.config.statusFilter;
            });
        }
        
        this.state.filteredDisputes = filtered;
        this.renderDisputes(filtered);
    },

    /**
     * Update statistics
     */
    updateStats(disputes) {
        var total = disputes.length;
        var opened = disputes.filter(function(d) { return d.status === 'OPENED'; }).length;
        var investigating = disputes.filter(function(d) { return d.status === 'UNDER_REVIEW'; }).length;
        var resolved = disputes.filter(function(d) { return d.status === 'RESOLVED'; }).length;

        var totalEl = document.getElementById('stat-total-disputes');
        var openEl = document.getElementById('stat-open-disputes');
        var investigatingEl = document.getElementById('stat-investigating');
        var resolvedEl = document.getElementById('stat-resolved');

        if (totalEl) totalEl.textContent = total;
        if (openEl) openEl.textContent = opened;
        if (investigatingEl) investigatingEl.textContent = investigating;
        if (resolvedEl) resolvedEl.textContent = resolved;
    },

    /**
     * Render disputes table
     */
    renderDisputes(disputes) {
        var container = document.getElementById('disputes-table-body');
        if (!container) return;

        if (disputes.length === 0) {
            container.innerHTML = '<tr><td colspan="5" class="py-8 text-center text-gray-500"><i class="ph ph-check-circle text-4xl text-success mb-2"></i><p>Không có khiếu nại nào nào p></td></tr>';
            return;
        }

        var html = '';
        for (var i = 0; i < disputes.length; i++) {
            var dispute = disputes[i];
            html += '<tr class="hover:bg-red-50/50 transition-colors group cursor-pointer" onclick="BuyerDisputes.viewDetail(' + dispute.disputeId + ')">';
            
            // Order info
            html += '<td class="py-4 px-4">';
            html += '<div class="font-mono text-sm font-bold text-gray-900">#' + (dispute.orderNumber || 'N/A') + '</div>';
            html += '<div class="text-sm font-bold text-primary">' + this.formatCurrency(dispute.orderAmount) + '</div>';
            html += '</td>';
            
            // Dispute info
            html += '<td class="py-4 px-4">';
            html += '<div class="font-bold text-gray-900 text-sm">' + this.formatDisputeType(dispute.disputeType) + '</div>';
            html += '<div class="text-xs text-gray-500 mt-1 max-w-xs truncate">' + (dispute.reason || 'Không có lý do') + '</div>';
            html += '</td>';
            
            // Parties
            html += '<td class="py-4 px-4">';
            html += '<div class="flex items-center gap-2 text-sm">';
            html += '<span class="font-bold text-gray-900">' + (dispute.sellerName || 'N/A') + '</span>';
            html += '</div>';
            html += '</td>';
            
            // Status
            html += '<td class="py-4 px-4">';
            html += this.getStatusBadge(dispute.status);
            html += '</td>';
            
            // Time
            html += '<td class="py-4 px-4">';
            html += '<span class="text-xs font-bold text-gray-500"><i class="ph-bold ph-clock"></i> ' + this.formatRelativeTime(dispute.openedAt) + '</span>';
            html += '</td>';
            
            // Actions
            html += '<td class="py-4 px-4 text-right">';
            html += '<button onclick="BuyerDisputes.viewDetail(' + dispute.disputeId + ')" class="btn-primary text-xs">';
            html += '<i class="ph ph-eye"></i> Xem chi tiết';
            html += '</button>';
            html += '</td>';
            html += '</tr>';
        }
        
        container.innerHTML = html;
    },

    /**
     * View dispute detail
     */
    viewDetail(disputeId) {
        window.location.href = '/buyer/disputes/' + disputeId;
    },

    /**
     * Refresh disputes
     */
    refresh() {
        this.loadDisputes();
    },

    /**
     * Show loading state
     */
    showLoading() {
        var loadingEl = document.getElementById('disputes-loading');
        var contentEl = document.getElementById('disputes-content');
        var errorEl = document.getElementById('disputes-error');
        
        if (loadingEl) loadingEl.classList.remove('hidden');
        if (contentEl) contentEl.classList.add('hidden');
        if (errorEl) errorEl.classList.add('hidden');
    },

    /**
     * Hide loading state
     */
    hideLoading() {
        var loadingEl = document.getElementById('disputes-loading');
        var contentEl = document.getElementById('disputes-content');
        
        if (loadingEl) loadingEl.classList.add('hidden');
        if (contentEl) contentEl.classList.remove('hidden');
    },

    /**
     * Show error state
     */
    showError(message) {
        var loadingEl = document.getElementById('disputes-loading');
        var contentEl = document.getElementById('disputes-content');
        var errorEl = document.getElementById('disputes-error');
        
        if (loadingEl) loadingEl.classList.add('hidden');
        if (contentEl) contentEl.classList.add('hidden');
        if (errorEl) errorEl.classList.remove('hidden');
        errorEl.innerHTML = '<div class="text-center py-8 text-error"><i class="ph ph-warning-circle text-4xl mb-2"></i><p>' + message + '</p></div>';
    },

    /**
     * Format currency
     */
    formatCurrency(amount) {
        if (!amount) return '0 ₫';
        return new Intl.NumberFormat('vi-VN').format(amount) + ' ₫';
    },

    /**
     * Format relative time
     */
    formatRelativeTime(dateString) {
        if (!dateString) return 'N/A';
        
        var date = new Date(dateString);
        var now = new Date();
        var diff = now - date;
        var minutes = Math.floor(diff / 60000);
        var hours = Math.floor(diff / 3600000);
        var days = Math.floor(diff / 86400000);
        
        if (days > 0) {
            return days + ' ngày trước';
        } else if (hours > 0) {
            return hours + ' giờ trước';
        } else if (minutes > 0) {
                return minutes + ' phút trước';
        } else {
                return 'Vừa xây';
            }
        }
    },

    /**
     * Get status badge HTML
     */
    getStatusBadge(status) {
        switch (status) {
            case 'OPENED':
                return '<span class="bg-red-100 text-error px-2 py-1 rounded-full text-xs font-bold"><i class="ph-fill ph-warning-circle"></i> Chờ xử lý</span>';
            case 'UNDER_REVIEW':
                return '<span class="bg-orange-100 text-warning px-2 py-1 rounded-full text-xs font-bold"><i class="ph-fill ph-magnifying-glass"></i> Đang điều tra</span>';
            case 'RESOLVED':
                return '<span class="bg-green-100 text-success px-2 py-1 rounded-full text-xs font-bold"><i class="ph-fill ph-check-circle"></i> Đã giải quyết</span>';
            case 'CANCELLED':
                return '<span class="bg-gray-100 text-gray-600 px-2 py-1 rounded-full text-xs font-bold"><i class="ph-fill ph-x-circle"></i> Đã hủy</span>';
            default:
                return '<span class="bg-gray-100 text-gray-600 px-2 py-1 rounded-full text-xs font-bold">' + status + '</span>';
        }
    },

    /**
     * Format dispute type
     */
    formatDisputeType(type) {
        var types = {
            'INVALID_CREDENTIAL': 'Thông tin không hợp lệ',
            'CREDENTIAL_USED': 'Tài khoản đã bị sử dụng',
            'CREDENTIAL_EXPIRED': 'Tài khoản đã hết hạn',
            'DESCRIPTION_MISMATCH': 'Không đúng mô tả',
            'NON_DELIVERY': 'Không nhận được tài khoản',
            'OTHER': 'Lý do khác'
        };
        return types[type] || type;
    }
};

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', function() {
    BuyerDisputes.init();
});
