/**
 * Admin Dispute Detail Module
 * Handles loading and displaying dispute details for admin intervention
 */
const AdminDisputeDetail = (function() {
    const config = {
        apiBaseUrl: '/api/admin'
    };

    const state = {
        disputeId: null,
        dispute: null,
        loading: false,
        error: null
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

    // API Functions
    async function fetchDisputeDetail(disputeId) {
        const response = await fetch(config.apiBaseUrl + '/disputes/' + disputeId, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin'
        });
        if (!response.ok) {
            if (response.status === 404) {
                throw new Error('Không tìm thấy khiếu nại');
            }
            throw new Error('Không thể tải thông tin khiếu nại: ' + response.statusText);
        }
        return response.json();
    }

    async function apiPost(endpoint, params) {
        const headers = {
            'Content-Type': 'application/x-www-form-urlencoded',
        };
        headers[getCsrfHeader()] = getCsrfToken();

        const response = await fetch(config.apiBaseUrl + endpoint, {
            method: 'POST',
            headers: headers,
            body: params,
            credentials: 'same-origin'
        });
        
        // Check Content-Type before parsing
        const contentType = response.headers.get('Content-Type') || '';
        
        if (!response.ok) {
            if (contentType.includes('application/json')) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || 'Request failed with status ' + response.status);
            } else {
                // Handle HTML error pages
                const errorText = await response.text().catch(() => 'Unknown error');
                console.error('Server returned HTML error:', errorText.substring(0, 500));
                
                // Check for common error scenarios
                if (response.status === 403) {
                    throw new Error('Bạn không có quyền thực hiện hành động này.');
                }
                if (response.status === 401) {
                    throw new Error('Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.');
                }
                throw new Error('Lỗi server. Vui lòng kiểm tra lại hoặc liên hệ hỗ trợ.');
            }
        }
        
        if (!contentType.includes('application/json')) {
            console.error('Server returned non-JSON response. Content-Type:', contentType);
            throw new Error('Server trả về phản hồi không hợp lệ. Vui lòng thử lại.');
        }
        
        return response.json();
    }

    // UI State Functions
    function showLoading() {
        state.loading = true;
        document.getElementById('detail-loading').classList.remove('hidden');
        document.getElementById('detail-content').classList.add('hidden');
        document.getElementById('detail-error').classList.add('hidden');
    }

    function hideLoading() {
        state.loading = false;
        document.getElementById('detail-loading').classList.add('hidden');
        document.getElementById('detail-content').classList.remove('hidden');
    }

    function showError(message) {
        state.error = message;
        document.getElementById('detail-loading').classList.add('hidden');
        document.getElementById('detail-content').classList.add('hidden');
        document.getElementById('detail-error').classList.remove('hidden');
        document.getElementById('error-message').textContent = message;
    }

    // Formatting Functions
    function formatCurrency(amount) {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);
    }

    function formatDateTime(dateString) {
        if (!dateString) return '--';
        const date = new Date(dateString);
        return date.toLocaleDateString('vi-VN') + ' ' + date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    }

    function formatRelativeTime(dateString) {
        if (!dateString) return '--';
        const date = new Date(dateString);
        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMs / 3600000);
        const diffDays = Math.floor(diffMs / 86400000);
        if (diffMins < 1) return 'Vừa xong';
        if (diffMins < 60) return diffMins + ' phút trước';
        if (diffHours < 24) return diffHours + ' giờ trước';
        if (diffDays < 7) return diffDays + ' ngày trước';
        return date.toLocaleDateString('vi-VN');
    }

    // Render Functions
    function getStatusConfig(status) {
        const configs = {
            'OPENED': {
                bgColor: 'bg-red-100',
                textColor: 'text-red-700',
                icon: 'ph-warning-circle',
                text: 'Chờ xử lý',
                description: 'Khiếu nại đang chờ admin xem xét'
            },
            'UNDER_REVIEW': {
                bgColor: 'bg-orange-100',
                textColor: 'text-orange-700',
                icon: 'ph-magnifying-glass',
                text: 'Đang điều tra',
                description: 'Admin đang xem xét bằng chứng'
            },
            'RESOLVED': {
                bgColor: 'bg-green-100',
                textColor: 'text-green-700',
                icon: 'ph-check-circle',
                text: 'Đã giải quyết',
                description: 'Khiếu nại đã được giải quyết'
            },
            'CANCELLED': {
                bgColor: 'bg-gray-100',
                textColor: 'text-gray-700',
                icon: 'ph-x-circle',
                text: 'Đã hủy',
                description: 'Khiếu nại đã bị hủy'
            }
        };
        return configs[status] || configs['OPENED'];
    }

    function renderStatusBanner(dispute) {
        const config = getStatusConfig(dispute.status);
        const banner = document.getElementById('status-banner');
        banner.className = 'rounded-xl p-4 mb-6 flex items-center justify-between ' + config.bgColor;
        
        document.getElementById('status-icon').className = 'ph-fill text-2xl ' + config.textColor + ' ph-' + config.icon;
        document.getElementById('status-text').className = 'font-bold text-lg ' + config.textColor;
        document.getElementById('status-text').textContent = config.text;
        document.getElementById('status-description').className = 'text-sm opacity-80 ' + config.textColor;
        document.getElementById('status-description').textContent = config.description;
        
        document.getElementById('status-badge').className = 'px-4 py-2 rounded-full font-bold text-sm ' + config.bgColor + ' ' + config.textColor;
        document.getElementById('status-badge').textContent = config.text;
    }

    function renderStats(dispute) {
        document.getElementById('dispute-number').textContent = '#' + (dispute.disputeNumber || dispute.disputeId);
        document.getElementById('stat-amount').textContent = formatCurrency(dispute.order ? dispute.order.totalAmount : 0);
        document.getElementById('stat-opened').textContent = formatRelativeTime(dispute.openedAt);
        
        if (dispute.sellerResponseDeadline) {
            const deadline = new Date(dispute.sellerResponseDeadline);
            const now = new Date();
            const diffMs = deadline - now;
            if (diffMs < 0) {
                document.getElementById('stat-deadline').textContent = 'Đã hết hạn';
                document.getElementById('stat-deadline').classList.add('text-error');
            } else {
                const diffHours = Math.floor(diffMs / 3600000);
                const diffDays = Math.floor(diffHours / 24);
                if (diffDays > 0) {
                    document.getElementById('stat-deadline').textContent = diffDays + ' ngày';
                } else {
                    document.getElementById('stat-deadline').textContent = diffHours + ' giờ';
                }
            }
        } else {
            document.getElementById('stat-deadline').textContent = '--';
        }
        
        if (dispute.assignedAdmin) {
            document.getElementById('stat-assigned').textContent = dispute.assignedAdmin.username;
        } else {
            document.getElementById('stat-assigned').textContent = 'Chưa giao';
        }
    }

    function renderOrderInfo(dispute) {
        if (dispute.order) {
            document.getElementById('order-number').textContent = dispute.order.orderNumber || ('#' + dispute.order.orderId);
            document.getElementById('order-status').textContent = dispute.order.orderStatus || '--';
            document.getElementById('order-date').textContent = formatDateTime(dispute.order.createdAt);
        }
    }

    function renderParties(dispute) {
        if (dispute.buyer) {
            document.getElementById('buyer-username').textContent = dispute.buyer.username || '--';
            document.getElementById('buyer-email').textContent = dispute.buyer.email || '--';
        }
        if (dispute.seller) {
            document.getElementById('seller-username').textContent = dispute.seller.username || '--';
            document.getElementById('seller-email').textContent = dispute.seller.email || '--';
        }
    }

    function renderReason(dispute) {
        document.getElementById('dispute-reason').textContent = dispute.reason || 'Không có lý do được cung cấp';
    }

    /**
     * Parse evidence string to extract text and image URLs
     */
    function parseEvidence(evidence) {
        if (!evidence) return { text: '', images: [] };
        
        // Check if evidence contains image URLs (comma-separated or newline-separated)
        const lines = evidence.split(/[\n,]+/);
        const images = [];
        const textParts = [];
        
        lines.forEach(function(line) {
            const trimmed = line.trim();
            if (trimmed.startsWith('http') && (
                trimmed.includes('cloudinary') || 
                trimmed.includes('.jpg') || 
                trimmed.includes('.png') || 
                trimmed.includes('.gif') ||
                trimmed.includes('.jpeg') ||
                trimmed.includes('.webp')
            )) {
                images.push(trimmed);
            } else if (trimmed.length > 0) {
                textParts.push(trimmed);
            }
        });
        
        return {
            text: textParts.join('\n'),
            images: images
        };
    }

    /**
     * Render evidence images in a grid
     */
    function renderEvidenceImages(container, images) {
        if (!container) return;
        
        if (images.length === 0) {
            container.classList.add('hidden');
            container.innerHTML = '';
            return;
        }
        
        container.classList.remove('hidden');
        container.innerHTML = '';
        
        images.forEach(function(url, index) {
            const imgWrapper = document.createElement('div');
            imgWrapper.className = 'relative group cursor-pointer';
            imgWrapper.innerHTML = 
                '<img src="' + url + '" ' +
                     'alt="Evidence ' + (index + 1) + '" ' +
                     'class="w-full h-24 object-cover rounded-lg border border-gray-200 hover:border-primary transition-colors" ' +
                     'onclick="AdminDisputeDetail.showImageModal(\'' + url + '\')">' +
                '<div class="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-10 transition-all rounded-lg flex items-center justify-center">' +
                    '<i class="ph ph-magnifying-glass text-white text-xl opacity-0 group-hover:opacity-100 transition-opacity"></i>' +
                '</div>';
            container.appendChild(imgWrapper);
        });
    }

    /**
     * Show image in a modal for full-size viewing
     */
    function showImageModal(imageUrl) {
        // Remove any existing modal
        const existingModal = document.getElementById('image-modal');
        if (existingModal) existingModal.remove();
        
        const modal = document.createElement('div');
        modal.id = 'image-modal';
        modal.className = 'fixed inset-0 bg-black bg-opacity-75 flex items-center justify-center z-50 p-4';
        modal.onclick = function() { modal.remove(); };
        modal.innerHTML = 
            '<div class="relative max-w-4xl max-h-full">' +
                '<img src="' + imageUrl + '" alt="Evidence" class="max-w-full max-h-[90vh] object-contain rounded-lg">' +
                '<button class="absolute top-2 right-2 text-white bg-black bg-opacity-50 rounded-full p-2 hover:bg-opacity-75 transition-colors">' +
                    '<i class="ph ph-x text-xl"></i>' +
                '</button>' +
            '</div>';
        document.body.appendChild(modal);
    }

    function renderEvidence(dispute) {
        // Buyer evidence
        const buyerEvidenceEl = document.getElementById('buyer-evidence');
        const buyerImagesEl = document.getElementById('buyer-evidence-images');
        
        if (dispute.buyerEvidence) {
            const parsed = parseEvidence(dispute.buyerEvidence);
            if (parsed.text) {
                buyerEvidenceEl.innerHTML = '<p class="whitespace-pre-wrap">' + parsed.text + '</p>';
            } else {
                buyerEvidenceEl.innerHTML = '<p class="text-gray-400 italic">Xem hình ảnh bằng chứng bên dưới</p>';
            }
            renderEvidenceImages(buyerImagesEl, parsed.images);
        } else {
            buyerEvidenceEl.innerHTML = '<p class="text-gray-400 italic">Chưa có bằng chứng</p>';
            if (buyerImagesEl) {
                buyerImagesEl.classList.add('hidden');
                buyerImagesEl.innerHTML = '';
            }
        }

        // Seller response
        const sellerResponseEl = document.getElementById('seller-response');
        const sellerImagesEl = document.getElementById('seller-evidence-images');
        
        if (dispute.sellerResponse) {
            const parsed = parseEvidence(dispute.sellerResponse);
            if (parsed.text) {
                sellerResponseEl.innerHTML = '<p class="whitespace-pre-wrap">' + parsed.text + '</p>';
            } else {
                sellerResponseEl.innerHTML = '<p class="text-gray-400 italic">Xem hình ảnh bằng chứng bên dưới</p>';
            }
            renderEvidenceImages(sellerImagesEl, parsed.images);
        } else if (dispute.sellerEvidence) {
            // Check for separate sellerEvidence field
            const parsed = parseEvidence(dispute.sellerEvidence);
            if (parsed.text) {
                sellerResponseEl.innerHTML = '<p class="whitespace-pre-wrap">' + parsed.text + '</p>';
            } else {
                sellerResponseEl.innerHTML = '<p class="text-gray-400 italic">Xem hình ảnh bằng chứng bên dưới</p>';
            }
            renderEvidenceImages(sellerImagesEl, parsed.images);
        } else {
            sellerResponseEl.innerHTML = '<p class="text-gray-400 italic">Chưa có phản hồi</p>';
            if (sellerImagesEl) {
                sellerImagesEl.classList.add('hidden');
                sellerImagesEl.innerHTML = '';
            }
        }
    }

    function renderTimeline(dispute) {
        const container = document.getElementById('timeline-container');
        container.innerHTML = '';

        const events = [];
        
        // Dispute opened
        events.push({
            type: 'opened',
            title: 'Khiếu nại được mở',
            description: 'Người mua đã mở khiếu nại',
            timestamp: dispute.openedAt,
            icon: 'ph-chat-centered-dots',
            color: 'text-error'
        });

        // Seller responded
        if (dispute.sellerRespondedAt) {
            events.push({
                type: 'seller_response',
                title: 'Người bán đã phản hồi',
                description: 'Người bán đã gửi phản hồi',
                timestamp: dispute.sellerRespondedAt,
                icon: 'ph-megaphone',
                color: 'text-success'
            });
        }

        // Admin review started
        if (dispute.adminReviewStartedAt) {
            events.push({
                type: 'review',
                title: 'Bắt đầu điều tra',
                description: 'Admin đã bắt đầu xem xét khiếu nại',
                timestamp: dispute.adminReviewStartedAt,
                icon: 'ph-magnifying-glass',
                color: 'text-warning'
            });
        }

        // Resolved
        if (dispute.resolvedAt) {
            events.push({
                type: 'resolved',
                title: 'Đã giải quyết',
                description: dispute.resolutionNotes || 'Khiếu nại đã được giải quyết',
                timestamp: dispute.resolvedAt,
                icon: 'ph-check-circle',
                color: 'text-success'
            });
        }

        // Sort by timestamp (newest first)
        events.sort(function(a, b) { return new Date(b.timestamp) - new Date(a.timestamp); });

        events.forEach(function(event) {
            const eventEl = document.createElement('div');
            eventEl.className = 'flex items-start gap-4 p-3 bg-gray-50 rounded-lg';
            eventEl.innerHTML = 
                '<div class="w-10 h-10 rounded-full bg-white flex items-center justify-center ' + event.color + ' shadow-sm">' +
                    '<i class="ph ' + event.icon + ' text-lg"></i>' +
                '</div>' +
                '<div class="flex-1">' +
                    '<div class="font-bold text-gray-900">' + event.title + '</div>' +
                    '<div class="text-sm text-gray-600">' + event.description + '</div>' +
                    '<div class="text-xs text-gray-400 mt-1">' + formatDateTime(event.timestamp) + '</div>' +
                '</div>';
            container.appendChild(eventEl);
        });

        if (events.length === 0) {
            container.innerHTML = '<p class="text-gray-400 text-center py-4">Chưa có sự kiện nào</p>';
        }
    }

    function renderActions(dispute) {
        const startReviewEl = document.getElementById('action-start-review');
        const resolutionActionsEl = document.getElementById('resolution-actions');
        const alreadyResolvedEl = document.getElementById('already-resolved');

        // Hide all first
        startReviewEl.classList.add('hidden');
        resolutionActionsEl.classList.add('hidden');
        alreadyResolvedEl.classList.add('hidden');

        if (dispute.status === 'RESOLVED' || dispute.status === 'CANCELLED') {
            // Show resolved message
            alreadyResolvedEl.classList.remove('hidden');
            if (dispute.resolutionNotes) {
                document.getElementById('resolution-summary').textContent = dispute.resolutionNotes;
            } else if (dispute.resolutionType) {
                document.getElementById('resolution-summary').textContent = 'Loại giải quyết: ' + dispute.resolutionType;
            }
        } else if (dispute.status === 'OPENED') {
            // Show start review button
            startReviewEl.classList.remove('hidden');
        } else if (dispute.status === 'UNDER_REVIEW') {
            // Show resolution actions
            resolutionActionsEl.classList.remove('hidden');
        }
    }

    function renderDispute(dispute) {
        state.dispute = dispute;
        renderStatusBanner(dispute);
        renderStats(dispute);
        renderOrderInfo(dispute);
        renderParties(dispute);
        renderReason(dispute);
        renderEvidence(dispute);
        renderTimeline(dispute);
        renderActions(dispute);
    }

    // Action Functions
    async function startReview() {
        if (!state.disputeId) return;
        
        if (!confirm('Bạn có chắc chắn muốn bắt đầu xem xét khiếu nại này?')) return;
        
        try {
            const result = await apiPost('/disputes/' + state.disputeId + '/review', '');
            if (result.success) {
                alert('Đã bắt đầu xem xét khiếu nại');
                await loadDispute();
            } else {
                alert('Lỗi: ' + result.message);
            }
        } catch (error) {
            console.error('Error starting review:', error);
            alert('Không thể bắt đầu xem xét: ' + error.message);
        }
    }

    async function resolveForBuyer() {
        if (!state.disputeId) return;
        
        const resolutionText = document.getElementById('resolution-text').value.trim();
        if (!resolutionText) {
            alert('Vui lòng nhập nội dung quyết định');
            return;
        }
        
        if (!confirm('Bạn có chắc chắn muốn HOÀN TIỀN cho người mua? Hành động này không thể hoàn tác.')) return;
        
        try {
            const params = 'resolution=' + encodeURIComponent(resolutionText);
            const result = await apiPost('/disputes/' + state.disputeId + '/resolve/buyer', params);
            if (result.success) {
                alert('Đã giải quyết khiếu nại - Hoàn tiền cho người mua');
                await loadDispute();
            } else {
                alert('Lỗi: ' + result.message);
            }
        } catch (error) {
            console.error('Error resolving for buyer:', error);
            alert('Không thể giải quyết khiếu nại: ' + error.message);
        }
    }

    async function resolveForSeller() {
        if (!state.disputeId) return;
        
        const resolutionText = document.getElementById('resolution-text').value.trim();
        if (!resolutionText) {
            alert('Vui lòng nhập nội dung quyết định');
            return;
        }
        
        if (!confirm('Bạn có chắc chắn muốn GIẢI NGÂN cho người bán? Hành động này không thể hoàn tác.')) return;
        
        try {
            const params = 'resolution=' + encodeURIComponent(resolutionText);
            const result = await apiPost('/disputes/' + state.disputeId + '/resolve/seller', params);
            if (result.success) {
                alert('Đã giải quyết khiếu nại - Giải ngân cho người bán');
                await loadDispute();
            } else {
                alert('Lỗi: ' + result.message);
            }
        } catch (error) {
            console.error('Error resolving for seller:', error);
            alert('Không thể giải quyết khiếu nại: ' + error.message);
        }
    }

    async function addNotes() {
        if (!state.disputeId) return;
        
        const notes = document.getElementById('admin-notes-input').value.trim();
        if (!notes) {
            alert('Vui lòng nhập ghi chú');
            return;
        }
        
        try {
            const params = 'notes=' + encodeURIComponent(notes);
            const result = await apiPost('/disputes/' + state.disputeId + '/notes', params);
            if (result.success) {
                alert('Đã lưu ghi chú');
                document.getElementById('admin-notes-input').value = '';
            } else {
                alert('Lỗi: ' + result.message);
            }
        } catch (error) {
            console.error('Error adding notes:', error);
            alert('Không thể lưu ghi chú: ' + error.message);
        }
    }

    async function loadDispute() {
        showLoading();
        try {
            const dispute = await fetchDisputeDetail(state.disputeId);
            renderDispute(dispute);
            hideLoading();
        } catch (error) {
            console.error('Error loading dispute:', error);
            showError(error.message);
        }
    }

    return {
        init: function(disputeId) {
            console.log('Initializing Admin Dispute Detail for dispute:', disputeId);
            state.disputeId = disputeId;
            loadDispute();
        },
        refresh: function() {
            if (state.disputeId) {
                loadDispute();
            }
        },
        startReview: startReview,
        resolveForBuyer: resolveForBuyer,
        resolveForSeller: resolveForSeller,
        addNotes: addNotes,
        showImageModal: showImageModal
    };
})();
