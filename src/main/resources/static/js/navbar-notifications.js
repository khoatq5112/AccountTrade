(function () {
    const root = document.querySelector('[data-navbar-notifications]');
    if (!root) {
        return;
    }

    const unreadUrl = '/api/notifications/unread-count';
    const notificationsUrl = '/api/notifications?page=0&size=8';
    const bell = root.querySelector('[data-notification-bell]');
    const badge = root.querySelector('[data-notification-badge]');
    const dropdown = root.querySelector('[data-notification-dropdown]');
    const list = root.querySelector('[data-notification-list]');
    const loading = root.querySelector('[data-notification-loading]');
    const empty = root.querySelector('[data-notification-empty]');
    const markAllButton = root.querySelector('[data-mark-all-read]');
    const userMenuToggle = document.querySelector('[data-user-menu-toggle]');
    const userMenu = document.querySelector('[data-user-menu]');
    const csrfToken = root.dataset.csrfToken || '';
    const csrfHeader = root.dataset.csrfHeader || '';
    let unreadCount = Number(root.dataset.initialUnread || 0);
    let notificationsLoaded = false;

    function requestHeaders(includeJson) {
        const headers = {};
        if (includeJson) {
            headers['Content-Type'] = 'application/json';
        }
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }
        return headers;
    }

    async function requestJson(url, options) {
        const response = await fetch(url, {
            credentials: 'same-origin',
            headers: requestHeaders(options && options.method && options.method !== 'GET'),
            ...options
        });
        if (!response.ok) {
            throw new Error('Request failed with status ' + response.status);
        }
        return response.status === 204 ? null : response.json();
    }

    function setBadge(count) {
        unreadCount = Number(count || 0);
        if (!badge) {
            return;
        }

        if (unreadCount > 0) {
            badge.textContent = unreadCount > 99 ? '99+' : String(unreadCount);
            badge.classList.remove('hidden');
            badge.classList.add('inline-flex');
        } else {
            badge.textContent = '0';
            badge.classList.add('hidden');
            badge.classList.remove('inline-flex');
        }
    }

    function notificationIcon(type) {
        switch ((type || '').toUpperCase()) {
            case 'WALLET':
                return 'ph-wallet';
            case 'PAYMENT':
                return 'ph-credit-card';
            case 'ORDER':
                return 'ph-shopping-bag-open';
            case 'ESCROW':
                return 'ph-lock-key';
            case 'DISPUTE':
                return 'ph-warning-circle';
            case 'CREDENTIAL':
                return 'ph-key';
            case 'POST':
                return 'ph-package';
            default:
                return 'ph-bell';
        }
    }

    function relativeTime(isoValue) {
        if (!isoValue) {
            return '';
        }

        const parsed = new Date(isoValue);
        if (Number.isNaN(parsed.getTime())) {
            return '';
        }

        const diffMs = parsed.getTime() - Date.now();
        const diffMinutes = Math.round(diffMs / 60000);
        const formatter = new Intl.RelativeTimeFormat('vi', { numeric: 'auto' });

        if (Math.abs(diffMinutes) < 60) {
            return formatter.format(diffMinutes, 'minute');
        }

        const diffHours = Math.round(diffMinutes / 60);
        if (Math.abs(diffHours) < 24) {
            return formatter.format(diffHours, 'hour');
        }

        const diffDays = Math.round(diffHours / 24);
        return formatter.format(diffDays, 'day');
    }

    function escapeHtml(value) {
        return String(value || '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function renderNotifications(items) {
        list.innerHTML = '';
        loading.classList.add('hidden');

        if (!items || items.length === 0) {
            empty.classList.remove('hidden');
            return;
        }

        empty.classList.add('hidden');

        items.forEach((item) => {
            const isRead = item.read === true || item.isRead === true;
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'w-full text-left px-4 py-3 hover:bg-gray-50 transition-colors ' + (isRead ? 'bg-white' : 'bg-blue-50/60');
            button.dataset.notificationId = item.notificationId;
            button.dataset.notificationUrl = item.actionUrl || '';
            button.dataset.notificationRead = String(isRead);
            button.innerHTML = [
                '<div class="flex items-start gap-3">',
                '<div class="mt-0.5 w-9 h-9 rounded-full bg-gray-100 text-primary flex items-center justify-center shrink-0">',
                '<i class="ph ' + notificationIcon(item.notificationType) + ' text-lg"></i>',
                '</div>',
                '<div class="min-w-0 flex-1">',
                '<div class="flex items-start justify-between gap-3">',
                '<div class="text-sm font-bold text-gray-900 line-clamp-1">' + escapeHtml(item.title) + '</div>',
                '<span class="text-[11px] text-gray-400 whitespace-nowrap">' + escapeHtml(relativeTime(item.createdAt)) + '</span>',
                '</div>',
                '<p class="mt-1 text-sm text-gray-600 line-clamp-2">' + escapeHtml(item.message) + '</p>',
                '</div>',
                (!isRead ? '<span class="mt-1 w-2.5 h-2.5 rounded-full bg-primary shrink-0"></span>' : ''),
                '</div>'
            ].join('');

            button.addEventListener('click', async function () {
                const notificationId = button.dataset.notificationId;
                const destination = button.dataset.notificationUrl;
                const currentlyRead = button.dataset.notificationRead === 'true';

                try {
                    if (!currentlyRead) {
                        await requestJson('/api/notifications/' + notificationId + '/read', { method: 'PUT' });
                        setBadge(Math.max(0, unreadCount - 1));
                        button.dataset.notificationRead = 'true';
                    }
                } catch (error) {
                    console.error('Failed to mark notification as read', error);
                }

                if (destination) {
                    window.location.href = destination;
                }
            });

            list.appendChild(button);
        });
    }

    async function loadUnreadCount() {
        try {
            const response = await requestJson(unreadUrl, { method: 'GET' });
            setBadge(response.totalCount || 0);
        } catch (error) {
            console.error('Failed to load unread count', error);
        }
    }

    async function loadNotifications() {
        loading.classList.remove('hidden');
        empty.classList.add('hidden');

        try {
            const response = await requestJson(notificationsUrl, { method: 'GET' });
            renderNotifications(response.content || []);
            notificationsLoaded = true;
            if (typeof response.unreadCount === 'number') {
                setBadge(response.unreadCount);
            }
        } catch (error) {
            console.error('Failed to load notifications', error);
            loading.classList.add('hidden');
            empty.classList.remove('hidden');
            empty.textContent = 'Không tải được thông báo. Vui lòng thử lại.';
        }
    }

    function toggleDropdown(forceOpen) {
        const shouldOpen = forceOpen !== undefined ? forceOpen : dropdown.classList.contains('hidden');
        if (shouldOpen) {
            dropdown.classList.remove('hidden');
            if (!notificationsLoaded) {
                loadNotifications();
            }
            return;
        }
        dropdown.classList.add('hidden');
    }

    function toggleUserMenu(forceOpen) {
        if (!userMenu) {
            return;
        }
        const shouldOpen = forceOpen !== undefined ? forceOpen : userMenu.classList.contains('hidden');
        userMenu.classList.toggle('hidden', !shouldOpen);
    }

    bell.addEventListener('click', function (event) {
        event.stopPropagation();
        toggleUserMenu(false);
        toggleDropdown();
    });

    if (userMenuToggle && userMenu) {
        userMenuToggle.addEventListener('click', function (event) {
            event.stopPropagation();
            toggleDropdown(false);
            toggleUserMenu();
        });
    }

    if (markAllButton) {
        markAllButton.addEventListener('click', async function (event) {
            event.preventDefault();
            try {
                await requestJson('/api/notifications/read-all', { method: 'PUT' });
                setBadge(0);
                notificationsLoaded = false;
                await loadNotifications();
            } catch (error) {
                console.error('Failed to mark all notifications as read', error);
            }
        });
    }

    document.addEventListener('click', function (event) {
        if (!root.contains(event.target)) {
            toggleDropdown(false);
        }

        if (userMenu) {
            const userWrapper = document.getElementById('userMenuWrapper');
            if (userWrapper && !userWrapper.contains(event.target)) {
                toggleUserMenu(false);
            }
        }
    });

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') {
            toggleDropdown(false);
            toggleUserMenu(false);
        }
    });

    document.addEventListener('visibilitychange', function () {
        if (!document.hidden) {
            loadUnreadCount();
            if (!dropdown.classList.contains('hidden')) {
                loadNotifications();
            }
        }
    });

    window.toggleUserMenu = function () {
        toggleUserMenu();
    };

    setBadge(unreadCount);
    loadUnreadCount();
    window.setInterval(function () {
        if (!document.hidden) {
            loadUnreadCount();
        }
    }, 15000);
    window.setInterval(function () {
        if (!document.hidden && !dropdown.classList.contains('hidden')) {
            loadNotifications();
        }
    }, 30000);
})();
