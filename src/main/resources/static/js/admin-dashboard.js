/**
 * Admin Dashboard Module
 * Handles fetching and rendering dynamic data for the admin dashboard.
 */
const AdminDashboard = (function() {
    // Configuration
    const config = {
        apiBaseUrl: '/api/admin',
        refreshInterval: 30000, // 30 seconds
        chartDays: 7,
        pendingPostsLimit: 5,
        disputesLimit: 5,
        usersLimit: 10
    };

    // State management
    const state = {
        stats: null,
        pendingPosts: [],
        disputes: [],
        chartData: [],
        users: [],
        loading: {
            stats: false,
            pendingPosts: false,
            disputes: false,
            chart: false,
            users: false
        },
        errors: {
            stats: null,
            pendingPosts: null,
            disputes: null,
            chart: null,
            users: null
        },
        chartInstance: null
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
    const api = {
        async fetchStats() {
            const response = await fetch(`${config.apiBaseUrl}/stats`, {
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
                throw new Error(`Failed to fetch stats: ${response.statusText}`);
            }

            return response.json();
        },

        async fetchPendingPosts() {
            const response = await fetch(
                `${config.apiBaseUrl}/pending-posts?limit=${config.pendingPostsLimit}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'same-origin'
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch pending posts: ${response.statusText}`);
            }

            return response.json();
        },

        async fetchDisputes() {
            const response = await fetch(
                `${config.apiBaseUrl}/disputes?limit=${config.disputesLimit}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'same-origin'
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch disputes: ${response.statusText}`);
            }

            return response.json();
        },

        async fetchChartData() {
            const response = await fetch(
                `${config.apiBaseUrl}/chart-data?days=${config.chartDays}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'same-origin'
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch chart data: ${response.statusText}`);
            }

            return response.json();
        },

        async approvePost(postId) {
            const headers = {
                'Content-Type': 'application/json'
            };
            headers[getCsrfHeader()] = getCsrfToken();

            const response = await fetch(
                `${config.apiBaseUrl}/posts/${postId}/approve`, {
                method: 'POST',
                headers: headers,
                credentials: 'same-origin'
            });

            if (!response.ok) {
                throw new Error(`Failed to approve post: ${response.statusText}`);
            }

            return response.json();
        },

        async rejectPost(postId) {
            const headers = {
                'Content-Type': 'application/json'
            };
            headers[getCsrfHeader()] = getCsrfToken();

            const response = await fetch(
                `${config.apiBaseUrl}/posts/${postId}/reject`, {
                method: 'POST',
                headers: headers,
                credentials: 'same-origin'
            });

            if (!response.ok) {
                throw new Error(`Failed to reject post: ${response.statusText}`);
            }

            return response.json();
        },

        async fetchUsers() {
            const response = await fetch(
                `${config.apiBaseUrl}/users?limit=${config.usersLimit}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'same-origin'
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch users: ${response.statusText}`);
            }

            return response.json();
        }
    };

    // UI methods
    const ui = {
        showLoading(section) {
            state.loading[section] = true;
            const loadingEl = document.getElementById(`${section}-loading`);
            const contentEl = document.getElementById(`${section}-content`);
            const errorEl = document.getElementById(`${section}-error`);

            if (loadingEl) loadingEl.classList.remove('hidden');
            if (contentEl) contentEl.classList.add('hidden');
            if (errorEl) errorEl.classList.add('hidden');
        },

        hideLoading(section) {
            state.loading[section] = false;
            const loadingEl = document.getElementById(`${section}-loading`);
            const contentEl = document.getElementById(`${section}-content`);

            if (loadingEl) loadingEl.classList.add('hidden');
            if (contentEl) contentEl.classList.remove('hidden');
        },

        showError(section, message) {
            state.errors[section] = message;
            const loadingEl = document.getElementById(`${section}-loading`);
            const contentEl = document.getElementById(`${section}-content`);
            const errorEl = document.getElementById(`${section}-error`);

            if (loadingEl) loadingEl.classList.add('hidden');
            if (contentEl) contentEl.classList.add('hidden');
            if (errorEl) {
                errorEl.innerHTML = `
                    <div class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-center gap-2">
                        <i class="ph-fill ph-warning-circle"></i>
                        <span>${message}</span>
                        <button onclick="AdminDashboard.retry('${section}')" 
                                class="ml-auto text-red-600 hover:text-red-800 font-bold flex items-center gap-1">
                            <i class="ph-bold ph-arrow-clockwise"></i> Retry
                        </button>
                    </div>
                `;
                errorEl.classList.remove('hidden');
            }
        },

        formatCurrency(amount) {
            if (amount === null || amount === undefined) return '0₫';
            return new Intl.NumberFormat('vi-VN', {
                style: 'currency',
                currency: 'VND'
            }).format(amount);
        },

        formatRelativeTime(dateString) {
            const date = new Date(dateString);
            const now = new Date();
            const diffMs = now - date;
            const diffMins = Math.floor(diffMs / 60000);
            const diffHours = Math.floor(diffMs / 3600000);
            const diffDays = Math.floor(diffMs / 86400000);

            if (diffMins < 1) return 'Vừa xong';
            if (diffMins < 60) return `${diffMins} phút trước`;
            if (diffHours < 24) return `${diffHours} giờ trước`;
            if (diffDays < 7) return `${diffDays} ngày trước`;
            return date.toLocaleDateString('vi-VN');
        },

        renderStats(stats) {
            state.stats = stats;

            // Update transaction volume
            const volumeEl = document.getElementById('stat-volume');
            const volumeTrendEl = document.getElementById('stat-volume-trend');
            if (volumeEl) {
                volumeEl.textContent = ui.formatCurrency(stats.dailyTransactionVolume);
            }
            if (volumeTrendEl) {
                const trendClass = stats.dailyVolumeChangePercent >= 0 ? 'text-success' : 'text-error';
                const trendIcon = stats.dailyVolumeChangePercent >= 0 ? 'ph-trend-up' : 'ph-trend-down';
                volumeTrendEl.className = `flex items-center gap-1 ${trendClass} text-sm font-bold relative z-10`;
                volumeTrendEl.innerHTML = `
                    <i class="ph-bold ${trendIcon}"></i>
                    ${stats.dailyVolumeChangePercent >= 0 ? '+' : ''}${stats.dailyVolumeChangePercent.toFixed(1)}%
                `;
            }

            // Update escrow holdings
            const escrowEl = document.getElementById('stat-escrow');
            const escrowCountEl = document.getElementById('stat-escrow-count');
            if (escrowEl) {
                escrowEl.textContent = ui.formatCurrency(stats.escrowHoldings);
            }
            if (escrowCountEl) {
                escrowCountEl.textContent = `${stats.escrowTransactionCount} giao dịch`;
            }

            // Update monthly revenue
            const revenueEl = document.getElementById('stat-revenue');
            const revenueTrendEl = document.getElementById('stat-revenue-trend');
            if (revenueEl) {
                revenueEl.textContent = ui.formatCurrency(stats.monthlyFeeRevenue);
            }
            if (revenueTrendEl) {
                const trendClass = stats.monthlyRevenueChangePercent >= 0 ? 'text-green-600' : 'text-red-600';
                revenueTrendEl.className = `text-xs ${trendClass} mt-1 font-bold`;
                revenueTrendEl.textContent = `${stats.monthlyRevenueChangePercent >= 0 ? '+' : ''}${stats.monthlyRevenueChangePercent.toFixed(1)}% so với tháng trước`;
            }

            // Update alerts
            const disputeCountEl = document.getElementById('stat-dispute-count');
            const approvalCountEl = document.getElementById('stat-approval-count');
            const sidebarDisputeCountEl = document.getElementById('sidebar-dispute-count');
            
            if (disputeCountEl) disputeCountEl.textContent = stats.pendingDisputeCount;
            if (approvalCountEl) approvalCountEl.textContent = stats.pendingApprovalCount;
            if (sidebarDisputeCountEl) sidebarDisputeCountEl.textContent = stats.pendingDisputeCount;
        },

        renderPendingPosts(posts) {
            state.pendingPosts = posts;
            const container = document.getElementById('pending-posts-content');
            if (!container) return;

            if (posts.length === 0) {
                container.innerHTML = `
                    <div class="text-center py-8 text-gray-500">
                        <i class="ph ph-check-circle text-4xl text-success mb-2"></i>
                        <p>Không có sản phẩm chờ duyệt</p>
                    </div>
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
                <div class="flex items-start gap-3 p-3 rounded-lg border border-gray-100 bg-gray-50 hover:bg-white transition-colors cursor-pointer group"
                     data-post-id="${post.postId}">
                    <div class="w-10 h-10 rounded bg-gray-200 flex items-center justify-center shrink-0">
                        <i class="ph-fill ${iconMap[post.categoryName] || iconMap['default']} text-gray-400"></i>
                    </div>
                    <div class="flex-1 min-w-0">
                        <div class="text-sm font-bold text-gray-900 truncate">${post.title}</div>
                        <div class="text-xs text-gray-500">
                            Bởi: <span class="text-primary">${post.sellerName}</span> • ${ui.formatCurrency(post.price)}
                        </div>
                    </div>
                    <div class="flex flex-col gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                        <button onclick="AdminDashboard.approvePost(${post.postId})" 
                                class="text-success hover:text-green-700 bg-green-100 p-1 rounded" 
                                title="Duyệt">
                            <i class="ph-bold ph-check"></i>
                        </button>
                    </div>
                </div>
            `).join('');
        },

        renderDisputes(disputes) {
            state.disputes = disputes;
            const container = document.getElementById('disputes-content');
            if (!container) return;

            if (disputes.length === 0) {
                container.innerHTML = `
                    <div class="text-center py-8 text-gray-500">
                        <i class="ph ph-check-circle text-4xl text-success mb-2"></i>
                        <p>Không có khiếu nại cần xử lý</p>
                    </div>
                `;
                return;
            }

            container.innerHTML = disputes.map(dispute => `
                <tr class="hover:bg-red-50/50 transition-colors group">
                    <td class="py-4 px-4">
                        <div class="font-mono text-sm font-bold text-gray-900">#${dispute.orderNumber}</div>
                        <div class="text-sm font-bold text-primary">${ui.formatCurrency(dispute.orderAmount)}</div>
                    </td>
                    <td class="py-4 px-4">
                        <div class="font-bold text-gray-900 text-sm">${dispute.reason || 'Không có lý do'}</div>
                        <div class="text-xs text-gray-500 mt-1 max-w-xs truncate">${dispute.reason || ''}</div>
                    </td>
                    <td class="py-4 px-4">
                        <div class="flex items-center gap-2 text-sm">
                            <span class="font-bold text-gray-900">${dispute.buyerName}</span>
                            <i class="ph-bold ph-arrow-right text-gray-400"></i>
                            <span class="font-bold text-gray-900">${dispute.sellerName}</span>
                        </div>
                    </td>
                    <td class="py-4 px-4">
                        <span class="text-xs font-bold text-error">
                            <i class="ph-bold ph-clock"></i> ${ui.formatRelativeTime(dispute.openedAt)}
                        </span>
                    </td>
                    <td class="py-4 px-4 text-right">
                        <button class="text-xs font-bold bg-error text-white px-3 py-2 rounded shadow-sm hover:bg-red-600">
                            Can thiệp
                        </button>
                    </td>
                </tr>
            `).join('');
        },

        renderChart(data) {
            state.chartData = data;
            const canvas = document.getElementById('platformVolumeChart');
            if (!canvas) return;

            const ctx = canvas.getContext('2d');

            // Destroy existing chart if any
            if (state.chartInstance) {
                state.chartInstance.destroy();
            }

            // Create gradient
            const gradient = ctx.createLinearGradient(0, 0, 0, 400);
            gradient.addColorStop(0, 'rgba(19, 127, 236, 0.5)');
            gradient.addColorStop(1, 'rgba(19, 127, 236, 0.05)');

            state.chartInstance = new Chart(ctx, {
                type: 'line',
                data: {
                    labels: data.map(d => d.label),
                    datasets: [{
                        label: 'Khối lượng (₫)',
                        data: data.map(d => d.value),
                        borderColor: '#137fec',
                        backgroundColor: gradient,
                        borderWidth: 3,
                        pointBackgroundColor: '#fff',
                        pointBorderColor: '#137fec',
                        pointBorderWidth: 2,
                        pointRadius: 4,
                        fill: true,
                        tension: 0.4
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            display: false
                        },
                        tooltip: {
                            backgroundColor: '#1a1a1a',
                            titleFont: { family: 'Manrope', size: 13 },
                            bodyFont: { family: 'Manrope', size: 14, weight: 'bold' },
                            padding: 12,
                            displayColors: false,
                            callbacks: {
                                label: function(context) {
                                    return ui.formatCurrency(context.parsed.y);
                                }
                            }
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true,
                            grid: {
                                color: '#f5f7fa',
                                drawBorder: false
                            },
                            ticks: {
                                font: { family: 'Manrope', size: 12 },
                                color: '#9ca3af',
                                callback: function(value) {
                                    if (value >= 1000000) {
                                        return (value / 1000000).toFixed(0) + 'M₫';
                                    } else if (value >= 1000) {
                                        return (value / 1000).toFixed(0) + 'K₫';
                                    }
                                    return value + '₫';
                                }
                            },
                            border: { display: false }
                        },
                        x: {
                            grid: {
                                display: false,
                                drawBorder: false
                            },
                            ticks: {
                                font: { family: 'Manrope', size: 12, weight: 'bold' },
                                color: '#6b7280'
                            },
                            border: { display: false }
                        }
                    }
                }
            });
        },

        renderUsers(users) {
            state.users = users;
            const container = document.getElementById('users-table-body');
            const badge = document.getElementById('user-count-badge');
            
            if (badge) {
                badge.textContent = `${users.length} users`;
            }
            
            if (!container) return;

            if (users.length === 0) {
                container.innerHTML = `
                    <tr>
                        <td colspan="6" class="py-8 text-center text-gray-500">
                            <i class="ph ph-users text-4xl text-gray-300 mb-2"></i>
                            <p>Không có người dùng nào</p>
                        </td>
                    </tr>
                `;
                return;
            }

            container.innerHTML = users.map(user => {
                const roleClass = user.roleName === 'ADMIN' ? 'bg-purple-100 text-purple-700' :
                                  user.roleName === 'SELLER' ? 'bg-blue-100 text-blue-700' :
                                  'bg-gray-100 text-gray-700';
                const statusClass = user.active ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700';
                const statusText = user.active ? 'Hoạt động' : 'Vô hiệu';
                const initials = user.username ? user.username.substring(0, 2).toUpperCase() : 'U';
                
                return `
                    <tr class="hover:bg-gray-50 transition-colors group">
                        <td class="py-4 px-4">
                            <div class="flex items-center gap-3">
                                <div class="w-10 h-10 rounded-full bg-gray-900 text-white flex items-center justify-center font-bold text-sm">
                                    ${initials}
                                </div>
                                <div>
                                    <div class="font-bold text-gray-900">${user.username}</div>
                                    <div class="text-xs text-gray-500">ID: ${user.userId}</div>
                                </div>
                            </div>
                        </td>
                        <td class="py-4 px-4">
                            <span class="text-sm text-gray-600">${user.email}</span>
                        </td>
                        <td class="py-4 px-4">
                            <span class="text-xs font-bold px-2 py-1 rounded-full ${roleClass}">
                                ${user.roleName || 'N/A'}
                            </span>
                        </td>
                        <td class="py-4 px-4">
                            <span class="text-xs font-bold px-2 py-1 rounded-full ${statusClass}">
                                ${statusText}
                            </span>
                        </td>
                        <td class="py-4 px-4">
                            <span class="text-sm text-gray-600">
                                ${user.createdAt ? new Date(user.createdAt).toLocaleDateString('vi-VN') : 'N/A'}
                            </span>
                        </td>
                        <td class="py-4 px-4 text-right">
                            <div class="flex justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                <button class="text-xs font-bold text-primary hover:text-blue-700 bg-blue-50 px-2 py-1 rounded"
                                        title="Xem chi tiết">
                                    <i class="ph-bold ph-eye"></i>
                                </button>
                                <button class="text-xs font-bold text-gray-600 hover:text-gray-800 bg-gray-100 px-2 py-1 rounded"
                                        title="Chỉnh sửa">
                                    <i class="ph-bold ph-pencil-simple"></i>
                                </button>
                            </div>
                        </td>
                    </tr>
                `;
            }).join('');
        }
    };

    // Data loading methods
    async function loadStats() {
        ui.showLoading('stats');
        try {
            const stats = await api.fetchStats();
            ui.renderStats(stats);
            ui.hideLoading('stats');
        } catch (error) {
            console.error('Error loading stats:', error);
            ui.showError('stats', error.message);
        }
    }

    async function loadPendingPosts() {
        ui.showLoading('pending-posts');
        try {
            const posts = await api.fetchPendingPosts();
            ui.renderPendingPosts(posts);
            ui.hideLoading('pending-posts');
        } catch (error) {
            console.error('Error loading pending posts:', error);
            ui.showError('pending-posts', error.message);
        }
    }

    async function loadDisputes() {
        ui.showLoading('disputes');
        try {
            const disputes = await api.fetchDisputes();
            ui.renderDisputes(disputes);
            ui.hideLoading('disputes');
        } catch (error) {
            console.error('Error loading disputes:', error);
            ui.showError('disputes', error.message);
        }
    }

    async function loadChartData() {
        ui.showLoading('chart');
        try {
            const data = await api.fetchChartData();
            ui.renderChart(data);
            ui.hideLoading('chart');
        } catch (error) {
            console.error('Error loading chart data:', error);
            ui.showError('chart', error.message);
        }
    }

    async function loadUsers() {
        ui.showLoading('users');
        try {
            const users = await api.fetchUsers();
            ui.renderUsers(users);
            ui.hideLoading('users');
        } catch (error) {
            console.error('Error loading users:', error);
            ui.showError('users', error.message);
        }
    }

    // Public methods
    return {
        init() {
            console.log('Initializing Admin Dashboard...');
            
            // Load all data
            loadStats();
            loadPendingPosts();
            loadDisputes();
            loadChartData();
            loadUsers();

            // Set up auto-refresh
            setInterval(() => {
                loadStats();
                loadPendingPosts();
                loadDisputes();
            }, config.refreshInterval);
        },

        retry(section) {
            switch (section) {
                case 'stats':
                    loadStats();
                    break;
                case 'pending-posts':
                    loadPendingPosts();
                    break;
                case 'disputes':
                    loadDisputes();
                    break;
                case 'chart':
                    loadChartData();
                    break;
                case 'users':
                    loadUsers();
                    break;
            }
        },

        async approvePost(postId) {
            try {
                await api.approvePost(postId);
                // Reload pending posts
                loadPendingPosts();
                loadStats();
            } catch (error) {
                alert('Failed to approve post: ' + error.message);
            }
        },

        async rejectPost(postId) {
            try {
                await api.rejectPost(postId);
                // Reload pending posts
                loadPendingPosts();
                loadStats();
            } catch (error) {
                alert('Failed to reject post: ' + error.message);
            }
        },

        refresh() {
            loadStats();
            loadPendingPosts();
            loadDisputes();
            loadChartData();
            loadUsers();
        },

        loadChartData(days) {
            if (days) {
                config.chartDays = parseInt(days);
            }
            loadChartData();
        }
    };
})();
