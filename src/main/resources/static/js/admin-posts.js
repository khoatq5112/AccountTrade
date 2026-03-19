/**
 * Admin Posts Module
 * Handles fetching and rendering post data for the admin posts page.
 */
const AdminPosts = (function() {
    // Configuration
    const config = {
        apiBaseUrl: '/api/admin',
        refreshInterval: 60000 // 60 seconds
    };

    // State management
    const state = {
        posts: [],
        stats: {
            total: 0,
            pending: 0,
            approved: 0,
            rejected: 0
        },
        loading: false,
        error: null
    };

    // CSRF token helper
    function getCsrfToken() {
        const metaTag = document.querySelector('meta[name="_csrf"]');
        return metaTag ? metaTag.getAttribute('content') : '';
    }

    function getCsrfHeader() {
        const metaTag = document.querySelector('meta[name="_csrf_header"]');
        return metaTag ? metaTag.getAttribute('content') : 'X-CSRF-TOKEN';
    }

    // API methods
    async function fetchPendingPosts() {
        const response = await fetch(`${config.apiBaseUrl}/pending-posts?limit=100`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'same-origin'
        });

        if (!response.ok) {
            if (response.status === 401 || response.status === 403) {
                throw new Error('Authentication required. Please log in again.');
            }
            throw new Error(`Failed to fetch posts: ${response.statusText}`);
        }

        return response.json();
    }

    async function approvePost(postId) {
        const headers = {
            'Content-Type': 'application/json'
        };
        headers[getCsrfHeader()] = getCsrfToken();

        const response = await fetch(`${config.apiBaseUrl}/posts/${postId}/approve`, {
            method: 'POST',
            headers: headers,
            credentials: 'same-origin'
        });

        if (!response.ok) {
            throw new Error(`Failed to approve post: ${response.statusText}`);
        }

        return response.json();
    }

    async function rejectPost(postId) {
        const headers = {
            'Content-Type': 'application/json'
        };
        headers[getCsrfHeader()] = getCsrfToken();

        const response = await fetch(`${config.apiBaseUrl}/posts/${postId}/reject`, {
            method: 'POST',
            headers: headers,
            credentials: 'same-origin'
        });

        if (!response.ok) {
            throw new Error(`Failed to reject post: ${response.statusText}`);
        }

        return response.json();
    }

    // UI methods
    function showLoading() {
        state.loading = true;
        const loadingEl = document.getElementById('posts-loading');
        const contentEl = document.getElementById('posts-content');
        const errorEl = document.getElementById('posts-error');

        if (loadingEl) loadingEl.classList.remove('hidden');
        if (contentEl) contentEl.classList.add('hidden');
        if (errorEl) errorEl.classList.add('hidden');
    }

    function hideLoading() {
        state.loading = false;
        const loadingEl = document.getElementById('posts-loading');
        const contentEl = document.getElementById('posts-content');

        if (loadingEl) loadingEl.classList.add('hidden');
        if (contentEl) contentEl.classList.remove('hidden');
    }

    function showError(message) {
        state.error = message;
        const loadingEl = document.getElementById('posts-loading');
        const contentEl = document.getElementById('posts-content');
        const errorEl = document.getElementById('posts-error');

        if (loadingEl) loadingEl.classList.add('hidden');
        if (contentEl) contentEl.classList.add('hidden');
        if (errorEl) {
            errorEl.innerHTML = `
                <div class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-center gap-2">
                    <i class="ph-fill ph-warning-circle"></i>
                    <span>${message}</span>
                    <button onclick="AdminPosts.refresh()" 
                            class="ml-auto text-red-600 hover:text-red-800 font-bold flex items-center gap-1">
                        <i class="ph-bold ph-arrow-clockwise"></i> Retry
                    </button>
                </div>
            `;
            errorEl.classList.remove('hidden');
        }
    }

    function formatCurrency(amount) {
        if (amount === null || amount === undefined) return '0₫';
        return new Intl.NumberFormat('vi-VN', {
            style: 'currency',
            currency: 'VND'
        }).format(amount);
    }

    function updateStats(posts) {
        state.stats.pending = posts.length;
        state.stats.total = posts.length; // In real system, would fetch all posts
        
        document.getElementById('stat-total-posts').textContent = state.stats.total;
        document.getElementById('stat-pending-posts').textContent = state.stats.pending;
        document.getElementById('stat-approved-posts').textContent = state.stats.approved;
        document.getElementById('stat-rejected-posts').textContent = state.stats.rejected;
    }

    function renderPosts(posts) {
        const container = document.getElementById('posts-table-body');
        if (!container) return;

        if (posts.length === 0) {
            container.innerHTML = `
                <tr>
                    <td colspan="6" class="py-8 text-center text-gray-500">
                        <i class="ph ph-check-circle text-4xl text-success mb-2"></i>
                        <p>Không có sản phẩm chờ duyệt</p>
                    </td>
                </tr>
            `;
            return;
        }

        const iconMap = {
            'Gaming': 'ph-game-controller',
            'Streaming': 'ph-play-circle',
            'Software': 'ph-app-window',
            'Email': 'ph-envelope',
            'VPN': 'ph-shield-check',
            'default': 'ph-package'
        };

        container.innerHTML = posts.map(post => `
            <tr class="hover:bg-gray-50 transition-colors group">
                <td class="py-4 px-4">
                    <div class="flex items-center gap-3">
                        <div class="w-10 h-10 rounded bg-gray-200 flex items-center justify-center shrink-0">
                            <i class="ph-fill ${iconMap[post.categoryName] || iconMap['default']} text-gray-400"></i>
                        </div>
                        <div>
                            <div class="font-bold text-gray-900">${post.title}</div>
                            <div class="text-xs text-gray-500">ID: ${post.postId}</div>
                        </div>
                    </div>
                </td>
                <td class="py-4 px-4">
                    <span class="text-sm font-bold text-primary">${post.sellerName}</span>
                </td>
                <td class="py-4 px-4">
                    <span class="text-sm text-gray-600">${post.categoryName || 'N/A'}</span>
                </td>
                <td class="py-4 px-4">
                    <span class="text-sm font-bold text-gray-900">${formatCurrency(post.price)}</span>
                </td>
                <td class="py-4 px-4">
                    <span class="text-sm text-gray-600">
                        ${post.createdAt ? new Date(post.createdAt).toLocaleDateString('vi-VN') : 'N/A'}
                    </span>
                </td>
                <td class="py-4 px-4 text-right">
                    <div class="flex justify-end gap-2">
                        <button onclick="AdminPosts.approvePost(${post.postId})" 
                                class="text-xs font-bold text-white bg-success px-3 py-2 rounded shadow-sm hover:bg-green-600 flex items-center gap-1">
                            <i class="ph-bold ph-check"></i> Duyệt
                        </button>
                        <button onclick="AdminPosts.rejectPost(${post.postId})" 
                                class="text-xs font-bold text-white bg-error px-3 py-2 rounded shadow-sm hover:bg-red-600 flex items-center gap-1">
                            <i class="ph-bold ph-x"></i> Từ chối
                        </button>
                    </div>
                </td>
            </tr>
        `).join('');
    }

    // Data loading
    async function loadPosts() {
        showLoading();
        try {
            const posts = await fetchPendingPosts();
            state.posts = posts;
            updateStats(posts);
            renderPosts(posts);
            hideLoading();
        } catch (error) {
            console.error('Error loading posts:', error);
            showError(error.message);
        }
    }

    // Public methods
    return {
        init() {
            console.log('Initializing Admin Posts...');
            loadPosts();

            // Set up auto-refresh
            setInterval(loadPosts, config.refreshInterval);
        },

        refresh() {
            loadPosts();
        },

        async approvePost(postId) {
            try {
                await approvePost(postId);
                // Reload posts
                loadPosts();
            } catch (error) {
                alert('Failed to approve post: ' + error.message);
            }
        },

        async rejectPost(postId) {
            try {
                await rejectPost(postId);
                // Reload posts
                loadPosts();
            } catch (error) {
                alert('Failed to reject post: ' + error.message);
            }
        }
    };
})();
