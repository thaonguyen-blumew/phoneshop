/* ===== PhoneShop API Client — FIXED ===== */
const API = '';

const api = {
  /**
   * BUG FIX 1: opts.headers chưa được khai báo → TypeError khi set Content-Type
   * BUG FIX 2: isFormData undefined → phải dùng isForm (tên param đúng)
   * BUG FIX 3: GET request không nên có body, chỉ append query string
   */
  async request(method, path, body, isForm = false) {
    const opts = {
      method,
      credentials: 'include',
      headers: {},            // ← FIX: khởi tạo headers trước
    };

    if (body) {
      if (isForm) {           // ← FIX: dùng isForm thay vì isFormData
        // Form-urlencoded: Spring Security /login và các @RequestParam endpoint
        opts.headers['Content-Type'] = 'application/x-www-form-urlencoded';
        opts.body = new URLSearchParams(body).toString();
      } else if (method !== 'GET' && method !== 'DELETE') {
        // JSON body cho POST/PUT
        opts.headers['Content-Type'] = 'application/json';
        opts.body = JSON.stringify(body);
      }
      // GET: không attach body, caller tự append ?key=val vào path nếu cần
    }

    const res = await fetch(API + path, opts);
    const contentType = res.headers.get('content-type') || '';
    if (contentType.includes('json')) return res.json();
    return { status: res.status, data: null, message: res.statusText };
  },

  get(p)           { return this.request('GET',    p); },
  post(p, b)       { return this.request('POST',   p, b); },
  postForm(p, b)   { return this.request('POST',   p, b, true); },  // dùng cho /login, /register, @RequestParam
  put(p, b)        { return this.request('PUT',    p, b); },
  putForm(p, b)    { return this.request('PUT',    p, b, true); },
  del(p)           { return this.request('DELETE', p); },
};

/* ===== Auth ===== */
const auth = {
  async login(email, password) {
    // Spring Security FormLogin nhận username/password dạng form-urlencoded
    return api.postForm('/login', { username: email, password });
  },
  async register(email, password, fullName, phone) {
    // AuthController: @RequestParam → dùng postForm
    return api.postForm('/register', { email, password, fullName, phone });
  },
  async logout() { return api.request('POST', '/logout'); },

  isLoggedIn() { return localStorage.getItem('ps_user') !== null; },
  getUser()    {
    try { return JSON.parse(localStorage.getItem('ps_user')); }
    catch { return null; }
  },
  setUser(u)   { localStorage.setItem('ps_user', JSON.stringify(u)); },
  clear()      { localStorage.removeItem('ps_user'); },
  hasRole(r)   {
    const u = this.getUser();
    return u && u.roles && u.roles.includes(r);
  },
};

/* ===== Price formatter ===== */
const fmt = (n) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(n || 0);

/* ===== Toast notifications ===== */
function showToast(msg, type = 'info') {
  const old = document.querySelector('.toast');
  if (old) old.remove();
  const t = document.createElement('div');
  t.className = `toast toast-${type}`;
  const iconMap = { success: 'check-circle', error: 'exclamation-circle', info: 'info-circle' };
  t.innerHTML = `<i class="fas fa-${iconMap[type] || 'info-circle'}"></i>${msg}`;
  document.body.appendChild(t);
  setTimeout(() => t.remove(), 3000);
}

/* ===== Navigation ===== */
function navigate(page, params = {}) {
  const query = Object.entries(params)
    .map(([k, v]) => `${k}=${encodeURIComponent(v)}`)
    .join('&');
  window.location.href = page + (query ? '?' + query : '');
}

function getParam(key) {
  return new URLSearchParams(window.location.search).get(key);
}

/* ===== Star renderer ===== */
function renderStars(rating) {
  let s = '';
  for (let i = 1; i <= 5; i++) {
    const full = i <= rating;
    s += `<i class="fas fa-star" style="color:${full ? 'var(--warning)' : 'var(--border)'}"></i>`;
  }
  return s;
}

/* ===== Header: hiển thị login/logout button ===== */
function updateHeader() {
  const loginBtn  = document.getElementById('loginBtn');
  const userArea  = document.getElementById('userArea');
  if (!loginBtn) return;
  if (auth.isLoggedIn()) {
    loginBtn.classList.add('hidden');
    if (userArea) userArea.classList.remove('hidden');
  } else {
    loginBtn.classList.remove('hidden');
    if (userArea) userArea.classList.add('hidden');
  }
}

/* ===== Mobile menu toggle ===== */
function toggleMenu() {
  const nav = document.querySelector('.desktop-nav');
  if (!nav) return;
  const isHidden = getComputedStyle(nav).display === 'none' || nav.style.display === 'none' || !nav.style.display;
  nav.style.cssText = isHidden
    ? 'display:flex;flex-direction:column;position:absolute;top:var(--header-h);left:0;width:100%;background:var(--bg-card);padding:16px;box-shadow:var(--shadow-md);z-index:100'
    : 'display:none';
}

function toggleFilter() {
  const sidebar = document.querySelector('.filter-sidebar');
  if (sidebar) sidebar.classList.toggle('desktop-only');
}
