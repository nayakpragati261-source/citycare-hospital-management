import React from 'react';

const ALL_NAV_ITEMS = [
  { id: 'dashboard', label: 'Dashboard', icon: '📊', roles: ['ADMIN', 'DOCTOR'] },
  { id: 'patients', label: 'Patients', icon: '👥', roles: ['ADMIN', 'DOCTOR'] },
  { id: 'doctors', label: 'Doctors', icon: '👨‍⚕️', roles: ['ADMIN'] },
  { id: 'appointments', label: 'Appointments', icon: '📅', roles: ['ADMIN', 'DOCTOR'] },
  { id: 'departments', label: 'Departments', icon: '🏥', roles: ['ADMIN', 'DOCTOR'] },
  { id: 'prescriptions', label: 'Prescriptions', icon: '💊', roles: ['ADMIN', 'DOCTOR'] },
  { id: 'medical-records', label: 'Medical Records', icon: '📁', roles: ['ADMIN', 'DOCTOR'] },
  { id: 'medical-tests', label: 'Medical Tests', icon: '🧪', roles: ['ADMIN', 'DOCTOR'] },
  { id: 'billing', label: 'Billing', icon: '💳', roles: ['ADMIN'] },
  { id: 'notifications', label: 'Notifications', icon: '🔔', roles: ['ADMIN', 'DOCTOR'] },
];

function Sidebar({ role, activeTab, onTabSelect, onLogout, isMobileOpen, onCloseMobile }) {
  const normalizedRole = role ? role.toUpperCase() : '';
  const allowedItems = ALL_NAV_ITEMS.filter(item => item.roles.includes(normalizedRole));

  return (
    <aside className={`sidebar ${isMobileOpen ? 'mobile-open' : ''}`}>
      <div className="sidebar-header">
        <div className="sidebar-brand">
          <span className="sidebar-logo">+</span>
          <div>
            <h2 className="sidebar-title">CityCare</h2>
            <p className="sidebar-subtitle">Hospital System</p>
          </div>
        </div>
        {isMobileOpen && (
          <button className="sidebar-close-btn" onClick={onCloseMobile}>&times;</button>
        )}
      </div>

      <nav className="sidebar-nav">
        {allowedItems.map((item) => (
          <button
            key={item.id}
            className={`nav-item ${activeTab === item.id ? 'active' : ''}`}
            onClick={() => {
              onTabSelect(item.id);
              if (onCloseMobile) onCloseMobile();
            }}
          >
            <span className="nav-icon">{item.icon}</span>
            <span className="nav-label">{item.label}</span>
          </button>
        ))}
      </nav>

      <div className="sidebar-footer">
        <button className="sidebar-logout-btn" onClick={onLogout}>
          <span className="nav-icon">🚪</span>
          <span className="nav-label">Logout</span>
        </button>
      </div>
    </aside>
  );
}

export default Sidebar;
