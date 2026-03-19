/**
 * Admin Users Module
 * Handles fetching and rendering user data for the admin users page.
 */
const AdminUsers = (function() {
    // Configuration
    const config = {
        apiBaseUrl: '/api/admin',
        refreshInterval: 60000, // 60 seconds
        usersLimit: 50
    };

    // State management
    const state = {
        users: [],
        filteredUsers: [],
        stats: {
            total: 0,
            active: 0,
            sellers: 0,
            inactive: 0
        },
        filters: {
            role: '',
            status: ''
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
    async function fetchUsers() {
        const response = await fetch(`${config.apiBaseUrl}/users?limit=${config.usersLimit}`, {
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
            throw new Error(`Failed to fetch users: ${response.statusText}`);
        }

        return response.json();
    }

    // UI methods
    function showLoading() {
        state.loading = true;
        const loadingEl = document.getElementById('users-loading');
        const contentEl = document.getElementById('users-content');
        const errorEl = document.getElementById('users-error');

        if (loadingEl) loadingEl.classList.remove('hidden');
        if (contentEl) contentEl.classList.add('hidden');
        if (errorEl) errorEl.classList.add('hidden');
    }

    function hideLoading() {
        state.loading = false;
        const loadingEl = document.getElementById('users-loading');
        const contentEl = document.getElementById('users-content');

        if (loadingEl) loadingEl.classList.add('hidden');
        if (contentEl) contentEl.classList.remove('hidden');
    }

    function showError(message) {
        state.error = message;
        const loadingEl = document.getElementById('users-loading');
        const contentEl = document.getElementById('users-content');
        const errorEl = document.getElementById('users-error');

        if (loadingEl) loadingEl.classList.add('hidden');
        if (contentEl) contentEl.classList.add('hidden');
        if (errorEl) {
            errorEl.innerHTML = `
                <div class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-center gap-2">
                    <i class="ph-fill ph-warning-circle"></i>
                    <span>${message}</span>
                    <button onclick="AdminUsers.refresh()" 
                            class="ml-auto text-red-600 hover:text-red-800 font-bold flex items-center gap-1">
                        <i class="ph-bold ph-arrow-clockwise"></i> Retry
                    </button>
                </div>
            `;
            errorEl.classList.remove('hidden');
        }
    }

    function updateStats(users) {
        state.stats.total = users.length;
        state.stats.active = users.filter(u => u.active).length;
        state.stats.inactive = users.filter(u => !u.active).length;
        state.stats.sellers = users.filter(u => u.roleName === 'SELLER').length;

        document.getElementById('stat-total-users').textContent = state.stats.total;
        document.getElementById('stat-active-users').textContent = state.stats.active;
        document.getElementById('stat-inactive-users').textContent = state.stats.inactive;
        document.getElementById('stat-sellers').textContent = state.stats.sellers;
    }

    function renderUsers(users) {
        const container = document.getElementById('users-table-body');
        if (!container) return;

        if (users.length === 0) {
            container.innerHTML = `
                <tr>
                    <td colspan="6" class="py-8 text-center text-gray-500">
                        <i class="ph ph-users text-4xl text-gray-300 mb-2"></i>
                        <p>Không tìm thấy người dùng nào</p>
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
                            ${user.active ? 
                                `<button class="text-xs font-bold text-error hover:text-red-700 bg-red-50 px-2 py-1 rounded"
                                        title="Vô hiệu hóa" onclick="AdminUsers.toggleUserStatus(${user.userId}, false)">
                                    <i class="ph-bold ph-prohibit"></i>
                                </button>` :
                                `<button class="text-xs font-bold text-success hover:text-green-700 bg-green-50 px-2 py-1 rounded"
                                        title="Kích hoạt" onclick="AdminUsers.toggleUserStatus(${user.userId}, true)">
                                    <i class="ph-bold ph-check"></i>
                                </button>`
                            }
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    }

    function applyFilters() {
        let filtered = [...state.users];

        if (state.filters.role) {
            filtered = filtered.filter(u => u.roleName === state.filters.role);
        }

        if (state.filters.status === 'active') {
            filtered = filtered.filter(u => u.active);
        } else if (state.filters.status === 'inactive') {
            filtered = filtered.filter(u => !u.active);
        }

        state.filteredUsers = filtered;
        renderUsers(filtered);
    }

    // Data loading
    async function loadUsers() {
        showLoading();
        try {
            const users = await fetchUsers();
            state.users = users;
            updateStats(users);
            applyFilters();
            hideLoading();
        } catch (error) {
            console.error('Error loading users:', error);
            showError(error.message);
        }
    }

    // Public methods
    return {
        init() {
            console.log('Initializing Admin Users...');
            loadUsers();

            // Set up auto-refresh
            setInterval(loadUsers, config.refreshInterval);
        },

        refresh() {
            loadUsers();
        },

        filterByRole(role) {
            state.filters.role = role;
            applyFilters();
        },

        filterByStatus(status) {
            state.filters.status = status;
            applyFilters();
        },

        async toggleUserStatus(userId, activate) {
            // This would need a backend endpoint to implement
            alert(`Tính năng ${activate ? 'kích hoạt' : 'vô hiệu hóa'} người dùng đang được phát triển`);
        }
    };
})();
