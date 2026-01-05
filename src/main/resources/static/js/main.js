// ExpertMatch UI JavaScript

// Test users configuration
const TEST_USERS = [
    {
        id: 'test-user-001',
        email: 'user@example.com',
        roles: ['ROLE_USER'],
        displayName: 'Regular User'
    },
    {
        id: 'test-user-002',
        email: 'admin@example.com',
        roles: ['ROLE_USER', 'ROLE_ADMIN'],
        displayName: 'Administrator'
    },
    {
        id: 'test-user-003',
        email: 'hr@example.com',
        roles: ['ROLE_USER', 'ROLE_HR'],
        displayName: 'HR Manager'
    },
    {
        id: 'test-user-004',
        email: 'manager@example.com',
        roles: ['ROLE_USER', 'ROLE_MANAGER'],
        displayName: 'Project Manager'
    },
    {
        id: 'anonymous-user',
        email: '',
        roles: [],
        displayName: 'Anonymous User'
    }
];

const DEFAULT_USER = TEST_USERS[4]; // Anonymous user

// User management functions
function getCurrentUser() {
    if (typeof window === 'undefined') {
        return DEFAULT_USER;
    }
    try {
        const stored = localStorage.getItem('expertmatch-selected-user');
        if (stored) {
            const user = JSON.parse(stored);
            const found = TEST_USERS.find(u => u.id === user.id);
            if (found) {
                return found;
            }
        }
    } catch (e) {
        console.error('Failed to load user from localStorage:', e);
    }
    return DEFAULT_USER;
}

function setCurrentUser(user) {
    if (typeof window === 'undefined') {
        return;
    }
    try {
        localStorage.setItem('expertmatch-selected-user', JSON.stringify(user));
        updateUserDisplay();
    } catch (e) {
        console.error('Failed to save user to localStorage:', e);
    }
}

function getUserHeaders() {
    const user = getCurrentUser();
    return {
        'X-User-Id': user.id,
        'X-User-Roles': user.roles.join(','),
        'X-User-Email': user.email
    };
}

function updateUserDisplay() {
    const currentUser = getCurrentUser();
    const displayElement = document.getElementById('currentUserDisplay');
    if (displayElement) {
        displayElement.textContent = currentUser.displayName;
        if (currentUser.roles.length > 0) {
            displayElement.textContent += ' (' + currentUser.roles.join(', ') + ')';
        }
    }
}

function initializeUserSelector() {
    const menu = document.getElementById('userSelectorMenu');
    if (!menu) {
        return;
    }

    // Clear existing items
    menu.innerHTML = '';

    // Add user options
    TEST_USERS.forEach(user => {
        const li = document.createElement('li');
        const button = document.createElement('button');
        button.className = 'dropdown-item';
        button.type = 'button';

        const currentUser = getCurrentUser();
        if (currentUser.id === user.id) {
            button.classList.add('active');
        }

        button.innerHTML = `
            <div class="fw-bold">${user.displayName}</div>
            <div class="small text-muted">${user.email || 'No email'}</div>
            <div class="small text-muted">${user.roles.length > 0 ? user.roles.join(', ') : 'No roles'}</div>
        `;

        button.addEventListener('click', function () {
            setCurrentUser(user);
            // Reload page to apply new user context
            window.location.reload();
        });

        li.appendChild(button);
        menu.appendChild(li);
    });

    updateUserDisplay();
}

// Toggle Sources section visibility based on Include Sources checkbox state
function toggleSourcesSection() {
    const checkbox = document.getElementById('includeSources');
    const sourcesSection = document.getElementById('sourcesSection');
    if (checkbox && sourcesSection) {
        sourcesSection.style.display = checkbox.checked ? 'block' : 'none';
    }
}

/**
 * Initialize SGR pattern validation.
 * Cascade and Cycle patterns are mutually exclusive:
 * - Cascade requires exactly 1 expert result
 * - Cycle requires multiple expert results (>1)
 * - Routing can be used with either
 */
function initializeSGRPatternValidation() {
    const cascadeCheckbox = document.getElementById('useCascadePattern');
    const cycleCheckbox = document.getElementById('useCyclePattern');
    
    if (!cascadeCheckbox || !cycleCheckbox) {
        return; // Checkboxes not found (might be on different page)
    }
    
    // When Cascade is checked, uncheck and disable Cycle
    cascadeCheckbox.addEventListener('change', function() {
        if (this.checked) {
            cycleCheckbox.checked = false;
            cycleCheckbox.disabled = true;
            // Add visual indicator
            cycleCheckbox.parentElement.classList.add('text-muted');
            // Update label to show why it's disabled
            const cycleLabel = cycleCheckbox.nextElementSibling;
            if (cycleLabel) {
                cycleLabel.title = 'Disabled: Cascade pattern requires exactly 1 expert, while Cycle requires multiple experts';
            }
        } else {
            cycleCheckbox.disabled = false;
            cycleCheckbox.parentElement.classList.remove('text-muted');
            const cycleLabel = cycleCheckbox.nextElementSibling;
            if (cycleLabel) {
                cycleLabel.title = '';
            }
        }
    });
    
    // When Cycle is checked, uncheck and disable Cascade
    cycleCheckbox.addEventListener('change', function() {
        if (this.checked) {
            cascadeCheckbox.checked = false;
            cascadeCheckbox.disabled = true;
            // Add visual indicator
            cascadeCheckbox.parentElement.classList.add('text-muted');
            // Update label to show why it's disabled
            const cascadeLabel = cascadeCheckbox.nextElementSibling;
            if (cascadeLabel) {
                cascadeLabel.title = 'Disabled: Cycle pattern requires multiple experts, while Cascade requires exactly 1 expert';
            }
        } else {
            cascadeCheckbox.disabled = false;
            cascadeCheckbox.parentElement.classList.remove('text-muted');
            const cascadeLabel = cascadeCheckbox.nextElementSibling;
            if (cascadeLabel) {
                cascadeLabel.title = '';
            }
        }
    });
    
    // Initialize state on page load
    if (cascadeCheckbox.checked) {
        cycleCheckbox.checked = false;
        cycleCheckbox.disabled = true;
        cycleCheckbox.parentElement.classList.add('text-muted');
    } else if (cycleCheckbox.checked) {
        cascadeCheckbox.checked = false;
        cascadeCheckbox.disabled = true;
        cascadeCheckbox.parentElement.classList.add('text-muted');
    }
}

/**
 * Validate SGR pattern combinations before form submission.
 * Returns true if valid, false otherwise (and shows error message).
 */
function validateSGRPatterns() {
    const cascadeCheckbox = document.getElementById('useCascadePattern');
    const cycleCheckbox = document.getElementById('useCyclePattern');
    
    if (!cascadeCheckbox || !cycleCheckbox) {
        return true; // Checkboxes not found, skip validation
    }
    
    const cascadeChecked = cascadeCheckbox.checked;
    const cycleChecked = cycleCheckbox.checked;
    
    // Both cannot be enabled simultaneously
    if (cascadeChecked && cycleChecked) {
        alert('Invalid SGR pattern combination:\n\n' +
              'Cascade and Cycle patterns cannot be enabled simultaneously.\n\n' +
              '• Cascade pattern requires exactly 1 expert result\n' +
              '• Cycle pattern requires multiple expert results (>1)\n\n' +
              'Please enable only one of them.');
        return false;
    }
    
    return true;
}

/**
 * Initialize Bootstrap tooltips for SGR pattern checkboxes.
 */
function initializeSGRPatternTooltips() {
    // Check if Bootstrap is available
    if (typeof bootstrap === 'undefined' && typeof window.bootstrap === 'undefined') {
        return; // Bootstrap not available
    }

    const Bootstrap = typeof bootstrap !== 'undefined' ? bootstrap : window.bootstrap;
    
    // Initialize tooltips for all checkboxes with data-bs-toggle="tooltip"
    const tooltipElements = document.querySelectorAll('[data-bs-toggle="tooltip"]');
    tooltipElements.forEach(function(element) {
        try {
            new Bootstrap.Tooltip(element);
        } catch (e) {
            console.warn('Failed to initialize tooltip for element:', element, e);
        }
    });
}

document.addEventListener('DOMContentLoaded', function () {
    // Initialize user selector
    initializeUserSelector();
    // Query form submission
    const queryForm = document.getElementById('queryForm');
    if (queryForm) {
        queryForm.addEventListener('submit', function (e) {
            e.preventDefault();
            submitQuery();
        });
    }

    // Add event listener for Include Sources checkbox
    const includeSourcesCheckbox = document.getElementById('includeSources');
    if (includeSourcesCheckbox) {
        includeSourcesCheckbox.addEventListener('change', toggleSourcesSection);
    }

    // Initialize Sources section visibility on page load
    toggleSourcesSection();

    // Initialize SGR pattern validation (Cascade and Cycle are mutually exclusive)
    initializeSGRPatternValidation();

    // Initialize Bootstrap tooltips for SGR pattern checkboxes
    initializeSGRPatternTooltips();

    // Enable/disable submit button based on query textarea content
    const queryTextarea = document.getElementById('query');
    const submitBtn = document.getElementById('submitBtn');
    if (queryTextarea && submitBtn) {
        // Check initial state
        updateSubmitButtonState();

        // Update on input
        queryTextarea.addEventListener('input', updateSubmitButtonState);
        queryTextarea.addEventListener('keyup', updateSubmitButtonState);

        function updateSubmitButtonState() {
            const hasContent = queryTextarea.value.trim().length > 0;
            submitBtn.disabled = !hasContent;
        }
    }

    // Chat form submission
    const chatForm = document.getElementById('chatForm');
    if (chatForm) {
        chatForm.addEventListener('submit', function (e) {
            e.preventDefault();
            sendMessage();
        });
    }

    // New chat button
    const newChatBtn = document.getElementById('newChatBtn');
    if (newChatBtn) {
        newChatBtn.addEventListener('click', function () {
            createNewChat();
        });
    }

    // Chat item click handler (event delegation)
    const chatList = document.getElementById('chatList');
    if (chatList) {
        chatList.addEventListener('click', function (e) {
            // Check if delete button was clicked
            const deleteBtn = e.target.closest('.chat-delete-btn');
            if (deleteBtn) {
                e.stopPropagation();
                e.preventDefault();
                const chatId = deleteBtn.getAttribute('data-chat-id');
                if (chatId) {
                    deleteChat(chatId);
                }
                return;
            }

            // Otherwise, handle chat selection
            const chatItem = e.target.closest('.chat-item');
            if (chatItem && !e.target.closest('.chat-delete-btn')) {
                const chatId = chatItem.getAttribute('data-chat-id');
                if (chatId) {
                    selectChat(chatId);
                }
            }
        });
    }

    // Render markdown on page load
    renderMarkdown();
});

// Helper function to create a fetch with timeout
function fetchWithTimeout(url, options, timeoutMs = 300000) { // 5 minutes default
    return new Promise((resolve, reject) => {
        const timeoutId = setTimeout(() => {
            reject(new Error('Request timeout: The query is taking longer than expected. Please try again or simplify your query.'));
        }, timeoutMs);

        fetch(url, options)
            .then(response => {
                clearTimeout(timeoutId);
                resolve(response);
            })
            .catch(error => {
                clearTimeout(timeoutId);
                reject(error);
            });
    });
}

// Helper function to update progress message with elapsed time
function updateProgressMessage(startTime, progressTextElement) {
    if (!progressTextElement) return;

    const elapsed = Math.floor((Date.now() - startTime) / 1000);
    const minutes = Math.floor(elapsed / 60);
    const seconds = elapsed % 60;

    let timeText = '';
    if (minutes > 0) {
        timeText = `${minutes}m ${seconds}s`;
    } else {
        timeText = `${seconds}s`;
    }

    progressTextElement.textContent = `Processing your query... (${timeText} elapsed)`;
}

function submitQuery() {
    const form = document.getElementById('queryForm');
    
    // Validate SGR pattern combinations before submission
    if (!validateSGRPatterns()) {
        return; // Validation failed, error message already shown
    }
    
    const formData = new FormData(form);
    const submitBtn = document.getElementById('submitBtn');
    const queryTextarea = document.getElementById('query');
    const progressIndicator = document.getElementById('progressIndicator');
    const resultsContent = document.getElementById('resultsContent');
    const progressText = progressIndicator ? progressIndicator.querySelector('p') : null;

    // Show progress indicator
    if (progressIndicator) {
        progressIndicator.classList.remove('d-none');
        progressIndicator.style.display = 'block';
    }
    if (resultsContent) {
        resultsContent.style.display = 'none';
    }

    submitBtn.disabled = true;
    submitBtn.textContent = 'Processing...';

    // Add user headers
    const headers = getUserHeaders();

    // Track start time for progress updates
    const startTime = Date.now();
    let progressInterval = null;

    // Update progress message every 5 seconds
    if (progressText) {
        updateProgressMessage(startTime, progressText);
        progressInterval = setInterval(() => {
            updateProgressMessage(startTime, progressText);
        }, 5000);
    }

    // Use fetchWithTimeout with 5 minute timeout (300000ms)
    fetchWithTimeout('/query', {
        method: 'POST',
        headers: headers,
        body: formData
    }, 300000) // 5 minutes timeout
        .then(response => {
            if (progressInterval) {
                clearInterval(progressInterval);
            }

            if (!response.ok) {
                throw new Error(`Server error: ${response.status} ${response.statusText}`);
            }
            return response.text();
        })
        .then(html => {
            // Update results section
            const parser = new DOMParser();
            const doc = parser.parseFromString(html, 'text/html');
            const results = doc.getElementById('results');
            if (results) {
                // Update entire results section
                const currentResults = document.getElementById('results');
                currentResults.innerHTML = results.innerHTML;

                // After replacing innerHTML, re-query DOM for new elements
                // Hide progress indicator and show results content
                const newProgressIndicator = document.getElementById('progressIndicator');
                const newResultsContent = document.getElementById('resultsContent');

                // Force hide progress indicator
                if (newProgressIndicator) {
                    newProgressIndicator.classList.add('d-none');
                    newProgressIndicator.style.display = 'none';
                }

                // Force show results content
                if (newResultsContent) {
                    newResultsContent.style.display = 'block';
                    newResultsContent.style.visibility = 'visible';
                    newResultsContent.classList.remove('d-none');
                }

                // Render markdown content
                renderMarkdown();

                // Toggle Sources section visibility based on checkbox state
                toggleSourcesSection();

                // Update chat history after query completes
                refreshChatHistory();

                // Refresh chat list to show updated chat name (if it was the first query)
                refreshChatList();
            }
        })
        .catch(error => {
            if (progressInterval) {
                clearInterval(progressInterval);
            }

            console.error('Error:', error);

            // Hide progress indicator and show error
            const currentProgressIndicator = document.getElementById('progressIndicator');
            const currentResultsContent = document.getElementById('resultsContent');

            if (currentProgressIndicator) {
                currentProgressIndicator.classList.add('d-none');
                currentProgressIndicator.style.display = 'none';
            }

            let errorMessage = error.message || 'Unknown error occurred';

            // Provide more helpful error messages
            if (errorMessage.includes('timeout') || errorMessage.includes('Timeout')) {
                errorMessage = 'Query timeout: The query took too long to process. This can happen with complex queries. Please try:\n' +
                    '1. Simplifying your query\n' +
                    '2. Reducing maxResults\n' +
                    '3. Disabling reranking or deep research options';
            } else if (errorMessage.includes('Failed to fetch') || errorMessage.includes('NetworkError')) {
                errorMessage = 'Network error: Unable to connect to the server. Please check your connection and try again.';
            }

            if (currentResultsContent) {
                currentResultsContent.style.display = 'block';
                currentResultsContent.style.visibility = 'visible';
                currentResultsContent.classList.remove('d-none');
                currentResultsContent.innerHTML =
                    '<div class="alert alert-danger"><strong>Error processing query:</strong><br>' +
                    escapeHtml(errorMessage).replace(/\n/g, '<br>') + '</div>';
            } else {
                document.getElementById('results').innerHTML =
                    '<div class="alert alert-danger"><strong>Error processing query:</strong><br>' +
                    escapeHtml(errorMessage).replace(/\n/g, '<br>') + '</div>';
            }
        })
        .finally(() => {
            if (progressInterval) {
                clearInterval(progressInterval);
            }

            // Re-enable button only if there's content in the textarea
            if (queryTextarea) {
                submitBtn.disabled = queryTextarea.value.trim().length === 0;
            } else {
                submitBtn.disabled = false;
            }
            submitBtn.textContent = 'Submit Query';
        });
}

function sendMessage() {
    const messageInput = document.getElementById('messageInput');
    const message = messageInput.value.trim();
    const chatId = getCurrentChatId();

    if (!message || !chatId) {
        return;
    }

    const formData = new FormData();
    formData.append('message', message);
    formData.append('chatId', chatId);

    const sendBtn = document.getElementById('sendBtn');
    sendBtn.disabled = true;

    // Add user headers
    const headers = getUserHeaders();

    fetch('/chats/send', {
        method: 'POST',
        headers: headers,
        body: formData
    })
        .then(response => {
            if (response.redirected) {
                window.location.href = response.url;
            } else {
                // If not redirected, render markdown for any new content
                setTimeout(renderMarkdown, 100);
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error sending message: ' + error.message);
        })
        .finally(() => {
            sendBtn.disabled = false;
            messageInput.value = '';
        });
}

function selectChat(chatId) {
    window.location.href = '/?chatId=' + chatId;
}

function createNewChat() {
    // Add user headers
    const headers = getUserHeaders();

    fetch('/chats/new', {
        method: 'POST',
        headers: headers
    })
        .then(response => {
            if (response.redirected) {
                window.location.href = response.url;
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error creating chat: ' + error.message);
        });
}

function deleteChat(chatId) {
    if (!confirm('Are you sure you want to delete this chat?')) {
        return;
    }

    // Add user headers
    const headers = getUserHeaders();

    // Add CSRF token if available (Spring Security)
    const csrfToken = document.querySelector('meta[name="_csrf"]');
    if (csrfToken) {
        headers['X-CSRF-TOKEN'] = csrfToken.getAttribute('content');
    }

    const formData = new FormData();
    formData.append('chatId', chatId);
    if (csrfToken) {
        formData.append('_csrf', csrfToken.getAttribute('content'));
    }

    fetch('/chats/delete', {
        method: 'POST',
        headers: headers,
        body: formData
    })
        .then(response => {
            if (response.redirected) {
                window.location.href = response.url;
            } else {
                // Reload page to refresh chat list
                window.location.reload();
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error deleting chat: ' + error.message);
        });
}

function getCurrentChatId() {
    const activeChat = document.querySelector('.list-group-item.active');
    return activeChat ? activeChat.getAttribute('data-chat-id') : null;
}

// Markdown rendering function
function renderMarkdown() {
    if (typeof marked === 'undefined') {
        console.warn('Marked.js not loaded, markdown rendering skipped');
        return;
    }

    // Configure marked options
    marked.setOptions({
        breaks: true,
        gfm: true,
        headerIds: true,
        mangle: false
    });

    // Find all elements with data-markdown attribute
    const markdownElements = document.querySelectorAll('.markdown-content[data-markdown]');
    markdownElements.forEach(element => {
        const markdownText = element.getAttribute('data-markdown');
        if (markdownText) {
            try {
                const html = marked.parse(markdownText);
                element.innerHTML = html;
            } catch (error) {
                console.error('Error rendering markdown:', error);
                element.textContent = markdownText; // Fallback to plain text
            }
        }
    });
}

// Query Examples functionality
let queryExamples = null;

function loadQueryExamples() {
    const loadingEl = document.getElementById('examplesLoading');
    const errorEl = document.getElementById('examplesError');
    const contentEl = document.getElementById('examplesContent');

    // Show loading, hide error and content
    loadingEl.classList.remove('d-none');
    errorEl.classList.add('d-none');
    contentEl.classList.add('d-none');

    // If examples are already loaded, show them immediately
    if (queryExamples) {
        displayQueryExamples(queryExamples);
        return;
    }

    // Fetch examples from API
    fetch('/api/v1/query/examples', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            ...getUserHeaders()
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to load examples: ' + response.statusText);
            }
            return response.json();
        })
        .then(data => {
            queryExamples = data.examples || [];
            displayQueryExamples(queryExamples);
        })
        .catch(error => {
            console.error('Error loading query examples:', error);
            loadingEl.classList.add('d-none');
            errorEl.classList.remove('d-none');
        });
}

function displayQueryExamples(examples) {
    const loadingEl = document.getElementById('examplesLoading');
    const contentEl = document.getElementById('examplesContent');
    const listEl = document.getElementById('examplesList');

    // Hide loading, show content
    loadingEl.classList.add('d-none');
    contentEl.classList.remove('d-none');

    // Clear existing content
    listEl.innerHTML = '';

    // Group examples by category
    const groupedExamples = {};
    examples.forEach(example => {
        const category = example.category || 'Other';
        if (!groupedExamples[category]) {
            groupedExamples[category] = [];
        }
        groupedExamples[category].push(example);
    });

    // Display examples grouped by category
    Object.keys(groupedExamples).sort().forEach(category => {
        // Category header
        const categoryHeader = document.createElement('div');
        categoryHeader.className = 'mb-2 mt-3';
        categoryHeader.innerHTML = `<h6 class="text-muted fw-bold mb-2">${category}</h6>`;
        listEl.appendChild(categoryHeader);

        // Examples in this category
        groupedExamples[category].forEach(example => {
            const exampleItem = document.createElement('button');
            exampleItem.type = 'button';
            exampleItem.className = 'list-group-item list-group-item-action text-start';
            exampleItem.innerHTML = `
                <div class="d-flex w-100 justify-content-between">
                    <h6 class="mb-1">${escapeHtml(example.title)}</h6>
                </div>
                <p class="mb-1 text-muted small">${escapeHtml(example.query)}</p>
            `;
            exampleItem.addEventListener('click', () => {
                insertExampleQuery(example.query);
            });
            listEl.appendChild(exampleItem);
        });
    });
}

function insertExampleQuery(queryText) {
    const queryTextarea = document.getElementById('query');
    if (queryTextarea) {
        queryTextarea.value = queryText;
        queryTextarea.focus();
        // Enable submit button if it was disabled
        const submitBtn = document.getElementById('submitBtn');
        if (submitBtn) {
            submitBtn.disabled = false;
        }
        // Close modal
        const modal = bootstrap.Modal.getInstance(document.getElementById('examplesModal'));
        if (modal) {
            modal.hide();
        }
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Function to refresh chat history
function refreshChatHistory() {
    // Try to get chatId from form first (more reliable), then from active chat item
    const form = document.getElementById('queryForm');
    let chatId = null;
    if (form) {
        const chatIdInput = form.querySelector('input[name="chatId"]');
        if (chatIdInput && chatIdInput.value) {
            chatId = chatIdInput.value;
        }
    }

    // Fallback to active chat item
    if (!chatId) {
        chatId = getCurrentChatId();
    }

    if (!chatId) {
        return; // No chat selected, nothing to refresh
    }

    const headers = getUserHeaders();

    // Fetch chat history from API
    fetch(`/api/v1/chats/${chatId}/history?page=0&size=100&sort=sequence_number,asc`, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            ...headers
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Failed to fetch chat history: ${response.status} ${response.statusText}`);
            }
            return response.json();
        })
        .then(data => {
            updateChatHistoryPanel(data.messages || []);
        })
        .catch(error => {
            console.error('Error refreshing chat history:', error);
            // Don't show error to user, just log it
        });
}

// Function to escape HTML attributes
function escapeHtmlAttribute(text) {
    if (!text) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#x27;');
}

// Function to update the chat history panel HTML
function updateChatHistoryPanel(messages) {
    const chatHistoryPanel = document.querySelector('.chat-history-panel');
    if (!chatHistoryPanel) {
        return; // Panel not found
    }

    if (!messages || messages.length === 0) {
        chatHistoryPanel.innerHTML = '<div class="text-center text-muted py-3"><small>No messages yet</small></div>';
        return;
    }

    // Build HTML for messages
    let html = '<div>';
    messages.forEach(message => {
        if (!message || !message.content) {
            return;
        }

        const isUser = message.role === 'user';
        const messageClass = isUser ? 'mb-2 text-end' : 'mb-2 text-start';
        const bubbleClass = isUser
            ? 'd-inline-block bg-primary text-white p-2 rounded markdown-content small'
            : 'd-inline-block bg-light p-2 rounded markdown-content small';

        html += `
            <div class="${messageClass}">
                <div class="${bubbleClass}" 
                     data-markdown="${escapeHtmlAttribute(message.content)}" 
                     style="max-width: 85%;"></div>
            </div>
        `;
    });
    html += '</div>';

    chatHistoryPanel.innerHTML = html;

    // Re-render markdown for the updated content
    renderMarkdown();

    // Scroll to bottom to show latest messages
    chatHistoryPanel.scrollTop = chatHistoryPanel.scrollHeight;
}

// Function to refresh the chat list in the sidebar
function refreshChatList() {
    const headers = getUserHeaders();

    // Fetch updated chat list from API
    fetch('/api/v1/chats', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            ...headers
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Failed to fetch chat list: ${response.status} ${response.statusText}`);
            }
            return response.json();
        })
        .then(data => {
            updateChatSidebar(data.chats || []);
        })
        .catch(error => {
            console.error('Error refreshing chat list:', error);
            // Don't show error to user, just log it
        });
}

// Function to update the chat sidebar HTML
function updateChatSidebar(chats) {
    const chatList = document.getElementById('chatList');
    if (!chatList) {
        return; // Chat list not found
    }

    // Get current active chat ID
    const activeChatItem = chatList.querySelector('.list-group-item.active');
    const currentChatId = activeChatItem ? activeChatItem.getAttribute('data-chat-id') : null;

    // Clear existing items
    chatList.innerHTML = '';

    // Build HTML for chat items
    chats.forEach(chat => {
        const isActive = chat.id === currentChatId;
        const chatName = chat.name && chat.name.trim() ? escapeHtml(chat.name) : 'Untitled Chat';

        // Format date
        let dateText = '';
        if (chat.createdAt) {
            try {
                const date = new Date(chat.createdAt);
                const year = date.getFullYear();
                const month = String(date.getMonth() + 1).padStart(2, '0');
                const day = String(date.getDate()).padStart(2, '0');
                const hours = String(date.getHours()).padStart(2, '0');
                const minutes = String(date.getMinutes()).padStart(2, '0');
                dateText = `${year}-${month}-${day} ${hours}:${minutes}`;
            } catch (e) {
                dateText = '';
            }
        }

        const deleteButton = (!chat.isDefault)
            ? `<button class="btn btn-sm btn-outline-danger ms-2 chat-delete-btn" data-chat-id="${chat.id}" title="Delete chat" type="button"><span aria-hidden="true">&times;</span></button>`
            : '';

        const listItem = document.createElement('li');
        listItem.className = `list-group-item list-group-item-action chat-item d-flex justify-content-between align-items-start${isActive ? ' active' : ''}`;
        listItem.setAttribute('data-chat-id', chat.id);
        listItem.innerHTML = `
            <div class="flex-grow-1" style="cursor: pointer;">
                <div class="fw-bold">${chatName}</div>
                <small class="text-muted">${dateText}</small>
            </div>
            ${deleteButton}
        `;

        chatList.appendChild(listItem);
    });

    // Event listeners are already attached via event delegation on the parent element
    // so they will automatically work with the new DOM elements
}

// Initialize examples modal when it's shown
document.addEventListener('DOMContentLoaded', function () {
    const examplesModal = document.getElementById('examplesModal');
    if (examplesModal) {
        examplesModal.addEventListener('show.bs.modal', function () {
            loadQueryExamples();
        });
    }
});


