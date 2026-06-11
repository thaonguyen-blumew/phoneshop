# coding: utf-8
import re

with open('backend/src/main/resources/templates/admin/dashboard.html', 'r', encoding='utf-8') as f:
    dashboard = f.read()

with open('backend/src/main/resources/templates/admin/staff-dashboard.html', 'r', encoding='utf-8') as f:
    staff = f.read()

# Extract sidebar from staff
staff_sidebar_match = re.search(r'<aside class="sidebar">.*?</aside>', staff, re.DOTALL)
if staff_sidebar_match:
    staff_sidebar = staff_sidebar_match.group(0)
    
    # Replace sidebar in dashboard
    dashboard = re.sub(r'<aside class="sidebar">.*?</aside>', staff_sidebar, dashboard, flags=re.DOTALL)

# Update Title
dashboard = dashboard.replace('Dashboard Hôm Nay — PhoneShop Admin', 'Dashboard Nhân viên — PhoneShop')
dashboard = dashboard.replace('DASHBOARD HÔM NAY', 'DASHBOARD NHÂN VIÊN')
dashboard = dashboard.replace('>Dashboard Hôm Nay<', '>Dashboard Nhân viên<')

with open('backend/src/main/resources/templates/admin/staff-dashboard.html', 'w', encoding='utf-8') as f:
    f.write(dashboard)
print('Done!')
