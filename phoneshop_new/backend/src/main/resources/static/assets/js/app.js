const API_BASE_URL = 'http://localhost:8080/api/';
const AUTH_URL = 'http://localhost:8080/login'; // Form login
const VNPAY_URL = 'http://localhost:8080/api/payments/vnpay/create-payment';

let cart = [];
let currentUser = null;

// Initialization
document.addEventListener('DOMContentLoaded', () => {
    switchView('home');
    fetchProducts();
});

// View Navigation
function switchView(viewId) {
    document.querySelectorAll('.page-view').forEach(view => view.classList.add('hidden'));
    document.getElementById('view-' + viewId).classList.remove('hidden');
    window.scrollTo(0,0);
}

// Format Price
const formatPrice = (price) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);

// --- 1. PRODUCTS ---
async function fetchProducts(keyword = '') {
    const productsGrid = document.getElementById('productsGrid');
    productsGrid.innerHTML = `<div style="grid-column: 1 / -1;"><div class="loading"><div class="spinner"></div></div></div>`;
    
    try {
        const url = keyword ? `${API_BASE_URL}/products?keyword=${encodeURIComponent(keyword)}` : `${API_BASE_URL}/products`;
        const response = await fetch(url);
        if (!response.ok) throw new Error('Network response was not ok');
        const json = await response.json();
        
        if (json.status === 200 && json.data && json.data.content) {
            renderProducts(json.data.content, productsGrid);
        } else {
            productsGrid.innerHTML = `<div class="loading">Không tìm thấy sản phẩm.</div>`;
        }
    } catch (error) {
        productsGrid.innerHTML = `<div class="loading" style="color: #ef4444;"><h3>Lỗi kết nối Server!</h3><p>Hãy chắc chắn bạn đã chạy Backend (Spring Boot).</p></div>`;
    }
}

function renderProducts(products, container) {
    if (products.length === 0) {
        container.innerHTML = `<div class="loading">Không tìm thấy sản phẩm.</div>`;
        return;
    }
    container.innerHTML = products.map(product => {
        let imageUrl = 'https://images.unsplash.com/photo-1598327105666-5b89351cb31b?q=80&w=600&auto=format&fit=crop';
        if (product.images && product.images.length > 0) imageUrl = product.images[0].imageUrl;
        
        return `
            <div class="product-card" onclick="showProductDetail(${product.productId})">
                <img src="${imageUrl}" alt="${product.name}" class="product-img">
                <div class="product-info">
                    <h3 class="product-title">${product.name}</h3>
                    <div class="product-price">${formatPrice(product.price)}</div>
                    <button class="add-to-cart" onclick="event.stopPropagation(); addToCart(${product.productId}, '${product.name.replace(/'/g, "\\'")}', ${product.price}, '${imageUrl}')">Thêm vào giỏ</button>
                </div>
            </div>
        `;
    }).join('');
}

function searchProducts() {
    const keyword = document.getElementById('searchInput').value;
    fetchProducts(keyword);
}

// --- 2. PRODUCT DETAIL ---
async function showProductDetail(id) {
    switchView('detail');
    const container = document.getElementById('detailContainer');
    container.innerHTML = `<div class="loading"><div class="spinner"></div></div>`;
    
    try {
        const response = await fetch(`${API_BASE_URL}/products/${id}`);
        const json = await response.json();
        
        if (json.status === 200) {
            const product = json.data.product;
            let imageUrl = 'https://images.unsplash.com/photo-1598327105666-5b89351cb31b?q=80&w=600&auto=format&fit=crop';
            if (product.images && product.images.length > 0) imageUrl = product.images[0].imageUrl;
            
            container.innerHTML = `
                <div style="flex:1"><img src="${imageUrl}" style="width:100%; border-radius:16px;"></div>
                <div style="flex:1; display:flex; flex-direction:column; gap:20px;">
                    <h2>${product.name}</h2>
                    <h1 style="color:var(--accent-solid)">${formatPrice(product.price)}</h1>
                    <p style="color:var(--text-secondary)">Hãng: ${product.brand}</p>
                    <p>Kho: ${product.stockQuantity} sản phẩm</p>
                    <p>${product.description || 'Chưa có mô tả'}</p>
                    <button class="btn-primary" onclick="addToCart(${product.productId}, '${product.name.replace(/'/g, "\\'")}', ${product.price}, '${imageUrl}')">Thêm vào giỏ hàng</button>
                </div>
            `;
        }
    } catch (e) {
        container.innerHTML = `Lỗi tải chi tiết sản phẩm.`;
    }
}

// --- 3. CART LOGIC ---
function addToCart(id, name, price, image) {
    const existing = cart.find(i => i.id === id);
    if (existing) {
        existing.quantity += 1;
    } else {
        cart.push({ id, name, price, image, quantity: 1 });
    }
    updateCartCount();
    alert('Đã thêm ' + name + ' vào giỏ!');
}

function updateCartCount() {
    document.getElementById('cartCount').innerText = cart.reduce((sum, item) => sum + item.quantity, 0);
    renderCart();
}

function renderCart() {
    const container = document.getElementById('cartItems');
    const totalEl = document.getElementById('cartTotal');
    
    if (cart.length === 0) {
        container.innerHTML = '<p>Giỏ hàng trống.</p>';
        totalEl.innerText = '0 ₫';
        return;
    }
    
    let total = 0;
    container.innerHTML = cart.map((item, index) => {
        total += item.price * item.quantity;
        return `
            <div class="cart-item">
                <img src="${item.image}" style="width:60px; height:60px; border-radius:8px; object-fit:cover;">
                <div style="flex:1; margin:0 20px;">
                    <h4 style="margin-bottom:5px;">${item.name}</h4>
                    <span style="color:var(--accent-solid)">${formatPrice(item.price)}</span> x ${item.quantity}
                </div>
                <button class="btn-secondary" style="padding:5px 15px;" onclick="removeFromCart(${index})">Xóa</button>
            </div>
        `;
    }).join('');
    totalEl.innerText = formatPrice(total);
}

function removeFromCart(index) {
    cart.splice(index, 1);
    updateCartCount();
}

// --- 4. LOGIN LOGIC ---
function openLoginModal() { document.getElementById('loginModal').classList.remove('hidden'); }
function closeLoginModal() { document.getElementById('loginModal').classList.add('hidden'); }

async function handleLogin(e) {
    e.preventDefault();
    const u = document.getElementById('loginUsername').value;
    const p = document.getElementById('loginPassword').value;
    
    try {
        const formData = new URLSearchParams();
        formData.append('username', u);
        formData.append('password', p);
        
        // Disable auto-redirect manually by checking response type
        const response = await auth.login(u, p);
        
        // Since Fetch with redirect:'manual' returns opaque response type for 302,
        // we just assume success if it doesn't throw network error, or we can just try fetching profile
        
        alert('Gửi yêu cầu đăng nhập thành công. Vui lòng kiểm tra tab Network.');
        closeLoginModal();
        
        document.getElementById('loginBtn').classList.add('hidden');
        document.getElementById('userProfile').classList.remove('hidden');
        document.getElementById('userNameDisplay').innerText = u.split('@')[0];
        
    } catch (e) {
        alert('Lỗi đăng nhập: ' + e);
    }
}

// --- 5. CHECKOUT LOGIC ---
async function handleCheckout(e) {
    e.preventDefault();
    if (cart.length === 0) return alert('Giỏ hàng trống!');
    
    const method = document.getElementById('checkoutMethod').value;
    const amount = cart.reduce((sum, item) => sum + item.price * item.quantity, 0);
    
    if (method === 'VNPAY') {
        // Redirect to VNPAY sandbox
        const url = `${VNPAY_URL}?amount=${amount}&orderInfo=ThanhToanDonHang`;
        window.location.href = url;
    } else {
        alert('Đặt hàng thành công với phương thức thanh toán COD!');
        cart = [];
        updateCartCount();
        switchView('home');
    }
}
