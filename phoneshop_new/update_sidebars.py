import os
import re

directory = 'backend/src/main/resources/templates/admin/'

sidebar_template = """    <aside class="sidebar">
      <div class="sidebar-logo">PHONE<span>SHOP</span></div>
      <a sec:authorize="hasAnyRole('MANAGER', 'ADMIN')" th:href="@{/admin/dashboard}" class="sidebar-item{dash_active}"><i class="fas fa-calendar-day"></i> Hôm nay</a>
      <a sec:authorize="hasRole('EMPLOYEE')" th:href="@{/admin/staff-dashboard}" class="sidebar-item{staff_dash_active}"><i class="fas fa-calendar-day"></i> Hôm nay</a>
      <a sec:authorize="hasAnyRole('MANAGER', 'ADMIN')" th:href="@{/admin/analytics}" class="sidebar-item{analytics_active}"><i class="fas fa-chart-line"></i> BI Dashboard</a>
      <a sec:authorize="hasAnyRole('MANAGER', 'ADMIN')" th:href="@{/admin/products}" class="sidebar-item{products_active}"><i class="fas fa-box"></i> Sản phẩm</a>
      <a th:href="@{/admin/categories}" class="sidebar-item{categories_active}"><i class="fas fa-tags"></i> Danh mục</a>
      <a th:href="@{/admin/orders}" class="sidebar-item{orders_active}"><i class="fas fa-shopping-cart"></i> Đơn hàng</a>
      <a sec:authorize="hasAnyRole('MANAGER', 'ADMIN')" th:href="@{/admin/employees}" class="sidebar-item{employees_active}"><i class="fas fa-users"></i> Nhân viên</a>
      <a sec:authorize="hasAnyRole('MANAGER', 'ADMIN')" th:href="@{/admin/reports}" class="sidebar-item{reports_active}"><i class="fas fa-chart-bar"></i> Báo cáo</a>
      <a sec:authorize="hasRole('MANAGER')" th:href="@{/admin/powerbi}" class="sidebar-item{powerbi_active}"><i class="fas fa-chart-line"></i> Power BI</a>
      <a th:href="@{/admin/feedbacks}" class="sidebar-item{feedbacks_active}"><i class="fas fa-headset"></i> CSKH</a>
      <div style="border-top:1px solid rgba(255,255,255,0.1); margin:16px 0"></div>
      <a th:href="@{/profile}" class="sidebar-item"><i class="fas fa-user-cog"></i> Hồ sơ</a>
      <form th:action="@{/logout}" method="post" style="margin:0;">
        <button type="submit" class="sidebar-item" style="width:100%; text-align:left; background:none; border:none; cursor:pointer;">
          <i class="fas fa-sign-out-alt"></i> Đăng xuất
        </button>
      </form>
    </aside>"""

for filename in os.listdir(directory):
    if filename.endswith('.html'):
        filepath = os.path.join(directory, filename)
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()

        basename = filename.replace('.html', '')

        new_sidebar = sidebar_template
        new_sidebar = new_sidebar.replace('{dash_active}', ' active' if basename == 'dashboard' else '')
        new_sidebar = new_sidebar.replace('{staff_dash_active}', ' active' if basename == 'staff-dashboard' else '')
        new_sidebar = new_sidebar.replace('{analytics_active}', ' active' if basename == 'analytics' else '')
        new_sidebar = new_sidebar.replace('{products_active}', ' active' if basename == 'products' else '')
        new_sidebar = new_sidebar.replace('{categories_active}', ' active' if basename == 'categories' else '')
        new_sidebar = new_sidebar.replace('{orders_active}', ' active' if basename == 'orders' else '')
        new_sidebar = new_sidebar.replace('{employees_active}', ' active' if basename == 'employees' else '')
        new_sidebar = new_sidebar.replace('{reports_active}', ' active' if basename == 'reports' else '')
        new_sidebar = new_sidebar.replace('{powerbi_active}', ' active' if basename == 'powerbi' else '')
        new_sidebar = new_sidebar.replace('{feedbacks_active}', ' active' if basename == 'feedbacks' else '')

        # Replace existing sidebar
        new_content = re.sub(r'<aside class="sidebar">.*?</aside>', new_sidebar, content, flags=re.DOTALL)

        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)

print("Updated all sidebars successfully!")
