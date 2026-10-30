import React, { useState, useEffect } from 'react';

const API_BASE_URL = 'http://localhost:8080';

function Header({ user, onToggleMobileSidebar, onNavigateToNotifications }) {
  const [unreadCount, setUnreadCount] = useState(0);
  const [showDropdown, setShowDropdown] = useState(false);
  const [recentNotifications, setRecentNotifications] = useState([]);

  const fetchUnreadCountAndRecent = async () => {
    if (!user) return;
    try {
      const [countRes, listRes] = await Promise.all([
        fetch(`${API_BASE_URL}/api/notifications/unread-count`, { credentials: 'include' }),
        fetch(`${API_BASE_URL}/api/notifications`, { credentials: 'include' })
      ]);

      if (countRes.ok) {
        const countData = await countRes.json();
        setUnreadCount(countData.count || 0);
      }

      if (listRes.ok) {
        const listData = await listRes.json();
        setRecentNotifications(listData.slice(0, 5)); // top 5
      }
    } catch (err) {
      // ignore header poll errors
    }
  };

  useEffect(() => {
    fetchUnreadCountAndRecent();
    const interval = setInterval(fetchUnreadCountAndRecent, 15000); // refresh every 15s
    return () => clearInterval(interval);
  }, [user]);

  const handleMarkAsRead = async (id, e) => {
    e.stopPropagation();
    try {
      const res = await fetch(`${API_BASE_URL}/api/notifications/${id}/read`, {
        method: 'PUT',
        credentials: 'include'
      });
      if (res.ok) {
        fetchUnreadCountAndRecent();
      }
    } catch (err) {
      console.error(err);
    }
  };

  const handleMarkAllRead = async () => {
    try {
      const res = await fetch(`${API_BASE_URL}/api/notifications/read-all`, {
        method: 'PUT',
        credentials: 'include'
      });
      if (res.ok) {
        fetchUnreadCountAndRecent();
      }
    } catch (err) {
      console.error(err);
    }
  };

  // Change Password Modal State
  const [isChangePasswordOpen, setIsChangePasswordOpen] = useState(false);
  const [changeForm, setChangeForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });
  const [showPasswords, setShowPasswords] = useState(false);
  const [changeError, setChangeError] = useState('');
  const [changeSuccess, setChangeSuccess] = useState('');
  const [changeSubmitting, setChangeSubmitting] = useState(false);

  const openChangePasswordModal = () => {
    setChangeForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
    setShowPasswords(false);
    setChangeError('');
    setChangeSuccess('');
    setIsChangePasswordOpen(true);
  };

  const closeChangePasswordModal = () => {
    setIsChangePasswordOpen(false);
    setChangeError('');
    setChangeSuccess('');
  };

  const handleChangePasswordSubmit = async (e) => {
    e.preventDefault();
    setChangeError('');
    setChangeSuccess('');

    if (!changeForm.currentPassword) {
      setChangeError('Current password is required.');
      return;
    }
    if (!changeForm.newPassword) {
      setChangeError('New password is required.');
      return;
    }
    if (changeForm.newPassword.length < 6) {
      setChangeError('New password must be at least 6 characters long.');
      return;
    }
    if (changeForm.newPassword !== changeForm.confirmPassword) {
      setChangeError('New password and confirm password do not match.');
      return;
    }

    setChangeSubmitting(true);
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/change-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
          currentPassword: changeForm.currentPassword,
          newPassword: changeForm.newPassword,
          confirmPassword: changeForm.confirmPassword
        })
      });

      if (response.ok) {
        setChangeSuccess('Password changed successfully.');
        setTimeout(() => {
          closeChangePasswordModal();
        }, 1500);
      } else {
        const errorData = await response.json().catch(() => ({}));
        setChangeError(errorData.message || 'Failed to change password.');
      }
    } catch (err) {
      setChangeError('Server connection error while changing password.');
    } finally {
      setChangeSubmitting(false);
    }
  };

  return (
    <header className="app-header">
      <div className="header-left">
        <button className="mobile-menu-btn" onClick={onToggleMobileSidebar}>
          ☰
        </button>
        <div className="header-title">
          <h1>CityCare Hospital</h1>
          <span className="header-subtitle">Hospital Management System</span>
        </div>
      </div>

      {user && (
        <div className="header-right" style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <button
            className="action-btn edit-btn"
            style={{ backgroundColor: '#0284c7', color: '#fff', fontSize: '0.85rem', padding: '0.45rem 0.8rem', display: 'flex', alignItems: 'center', gap: '0.35rem' }}
            onClick={openChangePasswordModal}
            title="Change Password"
          >
            🔑 Change Password
          </button>

          {/* Notification Bell with Badge */}
          <div className="notification-bell-container" style={{ position: 'relative' }}>
            <button
              className="icon-btn"
              onClick={() => setShowDropdown(!showDropdown)}
              title="Notifications"
              style={{
                background: 'none',
                border: 'none',
                fontSize: '1.3rem',
                cursor: 'pointer',
                position: 'relative',
                padding: '4px 8px',
                borderRadius: '6px',
                transition: 'background 0.2s'
              }}
            >
              🔔
              {unreadCount > 0 && (
                <span
                  style={{
                    position: 'absolute',
                    top: '0',
                    right: '2px',
                    background: '#ef4444',
                    color: '#ffffff',
                    fontSize: '0.7rem',
                    fontWeight: 'bold',
                    borderRadius: '10px',
                    padding: '2px 5px',
                    minWidth: '16px',
                    textAlign: 'center',
                    lineHeight: '1'
                  }}
                >
                  {unreadCount > 99 ? '99+' : unreadCount}
                </span>
              )}
            </button>

            {/* Dropdown Panel */}
            {showDropdown && (
              <div
                className="notification-dropdown"
                style={{
                  position: 'absolute',
                  right: 0,
                  top: '120%',
                  width: '320px',
                  backgroundColor: '#ffffff',
                  boxShadow: '0 10px 25px rgba(0,0,0,0.15)',
                  borderRadius: '8px',
                  border: '1px solid #e2e8f0',
                  zIndex: 1000,
                  overflow: 'hidden'
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.75rem 1rem', background: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
                  <strong style={{ fontSize: '0.9rem', color: '#0f172a' }}>Notifications ({unreadCount} unread)</strong>
                  {unreadCount > 0 && (
                    <button
                      onClick={handleMarkAllRead}
                      style={{ background: 'none', border: 'none', color: '#2563eb', fontSize: '0.75rem', cursor: 'pointer', fontWeight: 500 }}
                    >
                      Mark all read
                    </button>
                  )}
                </div>

                <div style={{ maxHeight: '280px', overflowY: 'auto' }}>
                  {recentNotifications.length === 0 ? (
                    <div style={{ padding: '1rem', textAlign: 'center', color: '#94a3b8', fontSize: '0.85rem' }}>
                      No notifications
                    </div>
                  ) : (
                    recentNotifications.map(n => (
                      <div
                        key={n.id}
                        style={{
                          padding: '0.75rem 1rem',
                          borderBottom: '1px solid #f1f5f9',
                          background: n.isRead ? '#ffffff' : '#f0f9ff',
                          display: 'flex',
                          justify: 'space-between',
                          alignItems: 'flex-start'
                        }}
                      >
                        <div style={{ paddingRight: '0.5rem' }}>
                          <p style={{ margin: 0, fontSize: '0.85rem', fontWeight: 600, color: '#1e293b' }}>{n.title}</p>
                          <p style={{ margin: '2px 0 0 0', fontSize: '0.8rem', color: '#475569' }}>{n.message}</p>
                        </div>
                        {!n.isRead && (
                          <button
                            onClick={(e) => handleMarkAsRead(n.id, e)}
                            style={{ background: 'none', border: 'none', color: '#2563eb', fontSize: '0.75rem', cursor: 'pointer' }}
                            title="Mark read"
                          >
                            ✓
                          </button>
                        )}
                      </div>
                    ))
                  )}
                </div>

                <div
                  style={{
                    padding: '0.6rem',
                    textAlign: 'center',
                    background: '#f8fafc',
                    borderTop: '1px solid #e2e8f0',
                    fontSize: '0.8rem'
                  }}
                >
                  <button
                    onClick={() => {
                      setShowDropdown(false);
                      if (onNavigateToNotifications) onNavigateToNotifications();
                    }}
                    style={{ background: 'none', border: 'none', color: '#2563eb', cursor: 'pointer', fontWeight: 600 }}
                  >
                    View All Notifications →
                  </button>
                </div>
              </div>
            )}
          </div>

          <div className="user-profile-summary">
            <div className="avatar-circle">
              {user.name ? user.name.charAt(0).toUpperCase() : 'U'}
            </div>
            <div className="user-meta">
              <span className="user-name">{user.name}</span>
              <span className={`role-badge ${user.role ? user.role.toLowerCase() : ''}`}>
                {user.role}
              </span>
            </div>
          </div>
        </div>
      )}

      {/* Change Password Modal */}
      {isChangePasswordOpen && (
        <div className="modal-overlay">
          <div className="modal-card" style={{ maxWidth: '450px' }}>
            <div className="modal-header">
              <h3>Change Password</h3>
              <button className="modal-close" onClick={closeChangePasswordModal}>&times;</button>
            </div>

            {changeError && (
              <div className="error-banner">
                {changeError}
              </div>
            )}

            {changeSuccess && (
              <div className="success-banner" style={{ marginBottom: '1rem' }}>
                {changeSuccess}
              </div>
            )}

            <form onSubmit={handleChangePasswordSubmit}>
              <div className="form-group">
                <label className="form-label">Current Password *</label>
                <div className="input-wrapper">
                  <input
                    type={showPasswords ? 'text' : 'password'}
                    className="form-input"
                    placeholder="Enter current password"
                    value={changeForm.currentPassword}
                    onChange={(e) => setChangeForm({ ...changeForm, currentPassword: e.target.value })}
                    required
                  />
                  <button
                    type="button"
                    className="toggle-password-btn"
                    onClick={() => setShowPasswords(!showPasswords)}
                  >
                    {showPasswords ? 'Hide' : 'Show'}
                  </button>
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">New Password *</label>
                <div className="input-wrapper">
                  <input
                    type={showPasswords ? 'text' : 'password'}
                    className="form-input"
                    placeholder="Enter new password (min 6 chars)"
                    value={changeForm.newPassword}
                    onChange={(e) => setChangeForm({ ...changeForm, newPassword: e.target.value })}
                    required
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Confirm New Password *</label>
                <div className="input-wrapper">
                  <input
                    type={showPasswords ? 'text' : 'password'}
                    className="form-input"
                    placeholder="Confirm new password"
                    value={changeForm.confirmPassword}
                    onChange={(e) => setChangeForm({ ...changeForm, confirmPassword: e.target.value })}
                    required
                  />
                </div>
              </div>

              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={closeChangePasswordModal} disabled={changeSubmitting}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary" disabled={changeSubmitting}>
                  {changeSubmitting ? 'Updating...' : 'Change Password'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </header>
  );
}

export default Header;
