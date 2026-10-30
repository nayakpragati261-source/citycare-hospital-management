import React, { useState, useEffect } from 'react';

const API_BASE_URL = 'http://localhost:8080';

function Notifications({ user }) {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState('ALL'); // ALL, UNREAD, READ

  const fetchNotifications = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await fetch(`${API_BASE_URL}/api/notifications`, {
        credentials: 'include'
      });
      if (response.ok) {
        const data = await response.json();
        setNotifications(data);
      } else {
        setError('Failed to fetch notifications.');
      }
    } catch (err) {
      setError('Unable to load notifications. Please check backend connection.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchNotifications();
  }, []);

  const handleMarkAsRead = async (id) => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/notifications/${id}/read`, {
        method: 'PUT',
        credentials: 'include'
      });
      if (response.ok) {
        setNotifications(prev =>
          prev.map(n => (n.id === id ? { ...n, isRead: true } : n))
        );
      }
    } catch (err) {
      console.error('Error marking notification as read:', err);
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/notifications/read-all`, {
        method: 'PUT',
        credentials: 'include'
      });
      if (response.ok) {
        setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
      }
    } catch (err) {
      console.error('Error marking all notifications as read:', err);
    }
  };

  const handleDeleteNotification = async (id) => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/notifications/${id}`, {
        method: 'DELETE',
        credentials: 'include'
      });
      if (response.ok) {
        setNotifications(prev => prev.filter(n => n.id !== id));
      }
    } catch (err) {
      console.error('Error deleting notification:', err);
    }
  };

  const filteredNotifications = notifications.filter(n => {
    if (filter === 'UNREAD') return !n.isRead;
    if (filter === 'READ') return n.isRead;
    return true;
  });

  const unreadCount = notifications.filter(n => !n.isRead).length;
  const readCount = notifications.length - unreadCount;

  const getTypeIcon = (type) => {
    switch (type) {
      case 'APPOINTMENT_BOOKED':
      case 'APPOINTMENT_SCHEDULED': return '📅';
      case 'APPOINTMENT_COMPLETED': return '✅';
      case 'MEDICAL_TEST_COMPLETED': return '🧪';
      case 'PRESCRIPTION_ADDED': return '💊';
      case 'PATIENT_REGISTERED': return '👤';
      default: return '🔔';
    }
  };

  return (
    <div className="notifications-page">
      {/* Page Header */}
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <div>
          <h2 className="page-title" style={{ fontSize: '1.75rem', fontWeight: 700, color: '#0f172a', margin: 0 }}>Notifications</h2>
          <p className="page-subtitle" style={{ color: '#64748b', margin: '0.25rem 0 0 0', fontSize: '0.95rem' }}>Stay updated with appointments, medical activities and important alerts.</p>
        </div>
        {unreadCount > 0 && (
          <button 
            className="primary-btn" 
            onClick={handleMarkAllAsRead}
            style={{ backgroundColor: '#2563eb', color: '#ffffff', border: 'none', padding: '0.6rem 1.25rem', borderRadius: '6px', fontWeight: 600, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '0.5rem', boxShadow: '0 2px 4px rgba(37, 99, 235, 0.2)' }}
          >
            ✓ Mark All as Read
          </button>
        )}
      </div>

      {error && <div className="error-banner" style={{ backgroundColor: '#fef2f2', color: '#dc2626', padding: '0.75rem 1rem', borderRadius: '6px', marginBottom: '1rem' }}>{error}</div>}

      {/* Summary Cards */}
      <div className="summary-cards" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', marginBottom: '1.5rem' }}>
        <div 
          onClick={() => setFilter('ALL')}
          style={{ 
            backgroundColor: filter === 'ALL' ? '#eff6ff' : '#ffffff', 
            border: filter === 'ALL' ? '2px solid #2563eb' : '1px solid #e2e8f0', 
            borderRadius: '10px', 
            padding: '1.25rem', 
            cursor: 'pointer',
            boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
            transition: 'all 0.2s'
          }}
        >
          <div style={{ color: '#64748b', fontSize: '0.85rem', fontWeight: 600, textTransform: 'uppercase', tracking: '0.05em' }}>All Notifications</div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#1e293b', marginTop: '0.5rem' }}>{notifications.length}</div>
        </div>

        <div 
          onClick={() => setFilter('UNREAD')}
          style={{ 
            backgroundColor: filter === 'UNREAD' ? '#eff6ff' : '#ffffff', 
            border: filter === 'UNREAD' ? '2px solid #2563eb' : '1px solid #e2e8f0', 
            borderRadius: '10px', 
            padding: '1.25rem', 
            cursor: 'pointer',
            boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
            transition: 'all 0.2s'
          }}
        >
          <div style={{ color: '#d97706', fontSize: '0.85rem', fontWeight: 600, textTransform: 'uppercase' }}>Unread</div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#d97706', marginTop: '0.5rem' }}>{unreadCount}</div>
        </div>

        <div 
          onClick={() => setFilter('READ')}
          style={{ 
            backgroundColor: filter === 'READ' ? '#eff6ff' : '#ffffff', 
            border: filter === 'READ' ? '2px solid #2563eb' : '1px solid #e2e8f0', 
            borderRadius: '10px', 
            padding: '1.25rem', 
            cursor: 'pointer',
            boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
            transition: 'all 0.2s'
          }}
        >
          <div style={{ color: '#16a34a', fontSize: '0.85rem', fontWeight: 600, textTransform: 'uppercase' }}>Read</div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#16a34a', marginTop: '0.5rem' }}>{readCount}</div>
        </div>
      </div>

      {/* Filter Tabs Bar */}
      <div className="filter-bar" style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.25rem', borderBottom: '1px solid #e2e8f0', paddingBottom: '0.75rem' }}>
        <button
          onClick={() => setFilter('ALL')}
          style={{
            padding: '0.5rem 1rem',
            borderRadius: '6px',
            border: 'none',
            backgroundColor: filter === 'ALL' ? '#2563eb' : '#f1f5f9',
            color: filter === 'ALL' ? '#ffffff' : '#475569',
            fontWeight: 600,
            fontSize: '0.875rem',
            cursor: 'pointer'
          }}
        >
          All ({notifications.length})
        </button>
        <button
          onClick={() => setFilter('UNREAD')}
          style={{
            padding: '0.5rem 1rem',
            borderRadius: '6px',
            border: 'none',
            backgroundColor: filter === 'UNREAD' ? '#2563eb' : '#f1f5f9',
            color: filter === 'UNREAD' ? '#ffffff' : '#475569',
            fontWeight: 600,
            fontSize: '0.875rem',
            cursor: 'pointer'
          }}
        >
          Unread ({unreadCount})
        </button>
        <button
          onClick={() => setFilter('READ')}
          style={{
            padding: '0.5rem 1rem',
            borderRadius: '6px',
            border: 'none',
            backgroundColor: filter === 'READ' ? '#2563eb' : '#f1f5f9',
            color: filter === 'READ' ? '#ffffff' : '#475569',
            fontWeight: 600,
            fontSize: '0.875rem',
            cursor: 'pointer'
          }}
        >
          Read ({readCount})
        </button>
      </div>

      {/* Notifications List */}
      {loading ? (
        <div className="dashboard-loading" style={{ padding: '2rem', textAlign: 'center', color: '#64748b' }}>Loading notifications...</div>
      ) : filteredNotifications.length === 0 ? (
        <div className="empty-state-box" style={{ textAlign: 'center', padding: '3rem', backgroundColor: '#ffffff', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
          <div className="empty-icon" style={{ fontSize: '2.5rem', marginBottom: '0.5rem' }}>🔔</div>
          <h3 style={{ margin: 0, color: '#1e293b' }}>No notifications found</h3>
          <p style={{ color: '#64748b', marginTop: '0.25rem' }}>There are no notifications matching your current filter.</p>
        </div>
      ) : (
        <div className="notifications-list" style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          {filteredNotifications.map((notif) => (
            <div
              key={notif.id}
              className={`notification-card ${!notif.isRead ? 'unread' : ''}`}
              style={{
                display: 'flex',
                alignItems: 'flex-start',
                justifyContent: 'space-between',
                padding: '1.25rem',
                borderRadius: '8px',
                border: !notif.isRead ? '1.5px solid #93c5fd' : '1px solid #e2e8f0',
                backgroundColor: !notif.isRead ? '#f0f9ff' : '#ffffff',
                boxShadow: !notif.isRead ? '0 2px 4px rgba(37, 99, 235, 0.06)' : 'none',
                transition: 'all 0.2s ease'
              }}
            >
              <div style={{ display: 'flex', gap: '1rem', alignItems: 'flex-start' }}>
                <div style={{ fontSize: '1.75rem', lineHeight: '1', backgroundColor: !notif.isRead ? '#dbeafe' : '#f1f5f9', padding: '0.6rem', borderRadius: '8px' }}>
                  {getTypeIcon(notif.type)}
                </div>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.35rem' }}>
                    <h4 style={{ margin: 0, color: '#0f172a', fontSize: '1rem', fontWeight: 600 }}>{notif.title}</h4>
                    {!notif.isRead && (
                      <span className="badge-new" style={{ fontSize: '0.7rem', padding: '2px 8px', background: '#2563eb', color: '#fff', borderRadius: '12px', fontWeight: 600 }}>
                        UNREAD
                      </span>
                    )}
                  </div>
                  <p style={{ margin: '0 0 0.5rem 0', color: '#334155', fontSize: '0.925rem', lineHeight: 1.5 }}>{notif.message}</p>
                  <span style={{ fontSize: '0.75rem', color: '#94a3b8', fontWeight: 500 }}>
                    {notif.createdAt ? new Date(notif.createdAt).toLocaleString('en-US', { dateStyle: 'medium', timeStyle: 'short' }) : ''}
                  </span>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                {!notif.isRead && (
                  <button
                    className="action-btn view-btn"
                    style={{ fontSize: '0.8rem', padding: '6px 12px', backgroundColor: '#e0f2fe', color: '#0284c7', border: 'none', borderRadius: '4px', fontWeight: 600, cursor: 'pointer' }}
                    title="Mark as Read"
                    onClick={() => handleMarkAsRead(notif.id)}
                  >
                    Mark as Read
                  </button>
                )}
                <button
                  className="action-btn delete-btn"
                  style={{ fontSize: '0.8rem', padding: '6px 10px', backgroundColor: '#fee2e2', color: '#dc2626', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
                  title="Delete"
                  onClick={() => handleDeleteNotification(notif.id)}
                >
                  🗑️
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default Notifications;
