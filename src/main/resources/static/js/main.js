// Main JavaScript file for ruomi.fi

document.addEventListener('DOMContentLoaded', function() {
    
    // Initialize tooltips
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
    
    // Initialize popovers
    var popoverTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="popover"]'));
    var popoverList = popoverTriggerList.map(function (popoverTriggerEl) {
        return new bootstrap.Popover(popoverTriggerEl);
    });
    
    // Smooth scrolling for anchor links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });
    
    // Add fade-in animation to cards
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };
    
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('fade-in-up');
                observer.unobserve(entry.target);
            }
        });
    }, observerOptions);
    
    // Observe all cards
    document.querySelectorAll('.card, .stat-card, .feature-card').forEach(card => {
        observer.observe(card);
    });
    
    // Search functionality enhancement
    const searchForm = document.querySelector('form[action="/advertisements"]');
    if (searchForm) {
        const searchInput = searchForm.querySelector('input[name="search"]');
        const searchButton = searchForm.querySelector('button[type="submit"]');
        
        // Add loading state to search button
        searchForm.addEventListener('submit', function() {
            searchButton.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Поиск...';
            searchButton.disabled = true;
        });
        
        // Auto-search suggestions (if needed)
        if (searchInput) {
            let searchTimeout;
            searchInput.addEventListener('input', function() {
                clearTimeout(searchTimeout);
                searchTimeout = setTimeout(() => {
                    // Here you could implement AJAX search suggestions
                    console.log('Search query:', this.value);
                }, 300);
            });
        }
    }
    
    // Price range filter enhancement
    const priceInputs = document.querySelectorAll('input[name="minPrice"], input[name="maxPrice"]');
    priceInputs.forEach(input => {
        input.addEventListener('input', function() {
            // Format price input
            let value = this.value.replace(/[^\d]/g, '');
            if (value) {
                value = parseInt(value).toLocaleString();
                this.value = value;
            }
        });
    });
    
    // Category filter enhancement
    const categoryLinks = document.querySelectorAll('.dropdown-item[href*="category="]');
    categoryLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            // Add active state to selected category
            categoryLinks.forEach(l => l.classList.remove('active'));
            this.classList.add('active');
        });
    });
    
    // Advertisement card interactions
    document.querySelectorAll('.card').forEach(card => {
        // Add click effect
        card.addEventListener('click', function(e) {
            if (!e.target.closest('a, button')) {
                const detailLink = this.querySelector('a[href*="/advertisement/"]');
                if (detailLink) {
                    detailLink.click();
                }
            }
        });
        
        // Add hover effect for images (if they exist)
        const cardImage = card.querySelector('img');
        if (cardImage) {
            card.addEventListener('mouseenter', function() {
                cardImage.style.transform = 'scale(1.05)';
            });
            
            card.addEventListener('mouseleave', function() {
                cardImage.style.transform = 'scale(1)';
            });
        }
    });
    
    // Premium and urgent badge animations
    document.querySelectorAll('.badge').forEach(badge => {
        if (badge.textContent.includes('Премиум')) {
            badge.style.animation = 'pulse 2s infinite';
        } else if (badge.textContent.includes('Срочно')) {
            badge.style.animation = 'shake 0.5s infinite';
        }
    });
    
    // Add CSS animations
    const style = document.createElement('style');
    style.textContent = `
        @keyframes pulse {
            0% { transform: scale(1); }
            50% { transform: scale(1.05); }
            100% { transform: scale(1); }
        }
        
        @keyframes shake {
            0%, 100% { transform: translateX(0); }
            25% { transform: translateX(-2px); }
            75% { transform: translateX(2px); }
        }
        
        .card {
            cursor: pointer;
        }
        
        .card img {
            transition: transform 0.3s ease;
        }
        
        .dropdown-item.active {
            background-color: var(--primary-color);
            color: white;
        }
    `;
    document.head.appendChild(style);
    
    // Newsletter subscription
    const newsletterForm = document.querySelector('footer form');
    if (newsletterForm) {
        newsletterForm.addEventListener('submit', function(e) {
            e.preventDefault();
            const email = this.querySelector('input[type="email"]').value;
            
            if (email && isValidEmail(email)) {
                // Show success message
                showNotification('Спасибо за подписку!', 'success');
                this.reset();
            } else {
                showNotification('Пожалуйста, введите корректный email', 'error');
            }
        });
    }
    
    // Social media links enhancement
    document.querySelectorAll('.social-links a').forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            const platform = this.querySelector('i').className;
            let url = '#';
            
            if (platform.includes('facebook')) {
                url = 'https://facebook.com/ruomi';
            } else if (platform.includes('telegram')) {
                url = 'https://t.me/ruomi_fi_bot';
            } else if (platform.includes('vk')) {
                url = 'https://vk.com/ruomi';
            } else if (platform.includes('instagram')) {
                url = 'https://instagram.com/ruomi';
            }
            
            if (url !== '#') {
                window.open(url, '_blank');
            }
        });
    });
    
    // Back to top button
    const backToTopButton = document.createElement('button');
    backToTopButton.innerHTML = '<i class="fas fa-arrow-up"></i>';
    backToTopButton.className = 'btn btn-primary position-fixed ruomi-back-to-top';
    backToTopButton.style.cssText = 'bottom: 20px; right: 20px; z-index: 1000; border-radius: 50%; width: 50px; height: 50px; display: none;';
    document.body.appendChild(backToTopButton);
    
    window.addEventListener('scroll', function() {
        if (window.pageYOffset > 300) {
            backToTopButton.style.display = 'block';
        } else {
            backToTopButton.style.display = 'none';
        }
    });
    
    backToTopButton.addEventListener('click', function() {
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        });
    });
    
    // Utility functions
    function isValidEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }
    
    function showNotification(message, type = 'info') {
        const notification = document.createElement('div');
        notification.className = `alert alert-${type === 'error' ? 'danger' : type} alert-dismissible fade show position-fixed`;
        notification.style.cssText = 'top: 20px; right: 20px; z-index: 9999; min-width: 300px;';
        notification.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        
        document.body.appendChild(notification);
        
        // Auto-remove after 5 seconds
        setTimeout(() => {
            if (notification.parentNode) {
                notification.remove();
            }
        }, 5000);
    }
    
    // Performance optimization: Lazy loading for images
    if ('IntersectionObserver' in window) {
        const imageObserver = new IntersectionObserver((entries, observer) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    const img = entry.target;
                    img.src = img.dataset.src;
                    img.classList.remove('lazy');
                    imageObserver.unobserve(img);
                }
            });
        });
        
        document.querySelectorAll('img[data-src]').forEach(img => {
            imageObserver.observe(img);
        });
    }
    
    // Mobile menu enhancement
    const navbarToggler = document.querySelector('.navbar-toggler');
    const navbarCollapse = document.querySelector('.navbar-collapse');
    
    if (navbarToggler && navbarCollapse) {
        // Close mobile menu when clicking on a link
        navbarCollapse.querySelectorAll('.nav-link, .dropdown-item').forEach(link => {
            link.addEventListener('click', function() {
                if (window.innerWidth < 992) {
                    navbarCollapse.classList.remove('show');
                }
            });
        });
        
        // Close mobile menu when clicking outside
        document.addEventListener('click', function(e) {
            if (!navbarToggler.contains(e.target) && !navbarCollapse.contains(e.target)) {
                navbarCollapse.classList.remove('show');
            }
        });
    }
    
    // Console welcome message
    console.log(`
        🎉 Добро пожаловать на ruomi.fi!
        
        🚀 Современная доска объявлений для русскоязычного населения в Финляндии
        
        📱 Адаптивный дизайн
        🔒 Безопасность
        💰 Платные услуги
        📧 Email уведомления
        
        Сделано с ❤️ в Финляндии
    `);
    
});

// Global functions that can be called from other scripts
window.vfinke = {
    
    // Show loading state
    showLoading: function(element) {
        if (element) {
            element.classList.add('loading');
        }
    },
    
    // Hide loading state
    hideLoading: function(element) {
        if (element) {
            element.classList.remove('loading');
        }
    },
    
    // Format price
    formatPrice: function(price) {
        if (typeof price === 'number') {
            return '€' + price.toLocaleString();
        }
        return price;
    },
    
    // Format date
    formatDate: function(date) {
        if (date instanceof Date) {
            return date.toLocaleDateString('ru-RU');
        }
        return date;
    },
    
    // Show success message
    showSuccess: function(message) {
        this.showNotification(message, 'success');
    },
    
    // Show error message
    showError: function(message) {
        this.showNotification(message, 'error');
    },
    
    // Show notification
    showNotification: function(message, type = 'info') {
        const notification = document.createElement('div');
        notification.className = `alert alert-${type === 'error' ? 'danger' : type} alert-dismissible fade show position-fixed`;
        notification.style.cssText = 'top: 20px; right: 20px; z-index: 9999; min-width: 300px;';
        notification.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        
        document.body.appendChild(notification);
        
        setTimeout(() => {
            if (notification.parentNode) {
                notification.remove();
            }
        }, 5000);
    }
}; 