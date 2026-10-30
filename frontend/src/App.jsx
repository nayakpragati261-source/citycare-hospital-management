import React, { useState, useEffect } from 'react';
import './App.css';
import Sidebar from './components/Sidebar';
import Header from './components/Header';
import Dashboard from './pages/Dashboard';
import Patients from './pages/Patients';
import Doctors from './pages/Doctors';
import Departments from './pages/Departments';
import Appointments from './pages/Appointments';
import Prescriptions from './pages/Prescriptions';
import MedicalRecords from './pages/MedicalRecords';
import MedicalTests from './pages/MedicalTests';
import Billing from './pages/Billing';
import Notifications from './pages/Notifications';
import PlaceholderPage from './pages/PlaceholderPage';

const API_BASE_URL = 'http://localhost:8080';

function App() {
  const [currentUser, setCurrentUser] = useState(null);
  const [checkingSession, setCheckingSession] = useState(true);
  const [currentPath, setCurrentPath] = useState(window.location.pathname);
  const [isMobileSidebarOpen, setIsMobileSidebarOpen] = useState(false);
  
  // Login Form state
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const navigate = (path) => {
    window.history.pushState({}, '', path);
    setCurrentPath(path);
  };

  useEffect(() => {
    const onPopState = () => setCurrentPath(window.location.pathname);
    window.addEventListener('popstate', onPopState);
    return () => window.removeEventListener('popstate', onPopState);
  }, []);

  // Check active session on startup
  const checkSession = async () => {
    setCheckingSession(true);
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/me`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include'
      });

      if (response.ok) {
        const userData = await response.json();
        setCurrentUser(userData);
      } else {
        setCurrentUser(null);
      }
    } catch (error) {
      setCurrentUser(null);
    } finally {
      setCheckingSession(false);
    }
  };

  useEffect(() => {
    checkSession();
  }, []);

  // Routing Guard Effect
  useEffect(() => {
    if (checkingSession) return;

    const publicPaths = ['/login', '/admin-login', '/doctor-login', '/'];

    if (currentUser) {
      if (publicPaths.includes(currentPath)) {
        navigate('/dashboard');
      }
    } else {
      if (!publicPaths.includes(currentPath)) {
        navigate('/login');
      }
    }
  }, [currentUser, currentPath, checkingSession]);

  const handleAdminLogin = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    if (!email || !email.includes('@')) {
      setErrorMessage('Please enter a valid email address');
      return;
    }

    if (!password) {
      setErrorMessage('Password is required');
      return;
    }

    setLoading(true);

    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/admin-login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ email, password })
      });

      if (response.ok) {
        const userData = await response.json();
        setCurrentUser(userData);
        setEmail('');
        setPassword('');
        navigate('/dashboard');
      } else if (response.status === 403) {
        const errorData = await response.json().catch(() => ({}));
        setErrorMessage(errorData.message || 'This account is not authorized for Admin access.');
        setCurrentUser(null);
      } else if (response.status === 401) {
        setErrorMessage('Invalid email or password');
        setCurrentUser(null);
      } else {
        const errorData = await response.json().catch(() => ({}));
        setErrorMessage(errorData.message || 'Admin login failed. Please try again.');
        setCurrentUser(null);
      }
    } catch (error) {
      setErrorMessage('Server connection error. Please make sure the backend is running.');
    } finally {
      setLoading(false);
    }
  };

  const handleDoctorLogin = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    if (!email || !email.includes('@')) {
      setErrorMessage('Please enter a valid email address');
      return;
    }

    if (!password) {
      setErrorMessage('Password is required');
      return;
    }

    setLoading(true);

    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/doctor-login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ email, password })
      });

      if (response.ok) {
        const userData = await response.json();
        setCurrentUser(userData);
        setEmail('');
        setPassword('');
        navigate('/dashboard');
      } else if (response.status === 403) {
        const errorData = await response.json().catch(() => ({}));
        setErrorMessage(errorData.message || 'This account is not authorized for Doctor access.');
        setCurrentUser(null);
      } else if (response.status === 401) {
        setErrorMessage('Invalid email or password');
        setCurrentUser(null);
      } else {
        const errorData = await response.json().catch(() => ({}));
        setErrorMessage(errorData.message || 'Doctor login failed. Please try again.');
        setCurrentUser(null);
      }
    } catch (error) {
      setErrorMessage('Server connection error. Please make sure the backend is running.');
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = async () => {
    try {
      await fetch(`${API_BASE_URL}/api/auth/logout`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include'
      });
    } catch (error) {
      console.error('Logout request error:', error);
    } finally {
      setCurrentUser(null);
      setEmail('');
      setPassword('');
      setErrorMessage('');
      navigate('/login');
    }
  };

  if (checkingSession) {
    return (
      <div className="auth-center">
        <p style={{ color: '#64748b', fontSize: '1.1rem' }}>Checking session...</p>
      </div>
    );
  }

  // Unauthenticated Views
  if (!currentUser) {
    if (currentPath === '/admin-login') {
      return (
        <div className="auth-center">
          <div className="login-card">
            <button
              type="button"
              className="back-to-landing-btn"
              onClick={() => {
                setErrorMessage('');
                setEmail('');
                setPassword('');
                navigate('/login');
              }}
            >
              ← Back to Selection
            </button>

            <div className="login-header">
              <h2>Admin Sign In</h2>
              <p>Sign in to access the CityCare Hospital administration portal.</p>
            </div>

            {errorMessage && (
              <div className="error-banner">
                {errorMessage}
              </div>
            )}

            <form onSubmit={handleAdminLogin}>
              <div className="form-group">
                <label className="form-label" htmlFor="admin-email">Email Address</label>
                <div className="input-wrapper">
                  <input
                    id="admin-email"
                    type="email"
                    className="form-input"
                    placeholder="Enter email address"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="admin-password">Password</label>
                <div className="input-wrapper">
                  <input
                    id="admin-password"
                    type={showPassword ? 'text' : 'password'}
                    className="form-input"
                    placeholder="Enter password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                  />
                  <button
                    type="button"
                    className="toggle-password-btn"
                    onClick={() => setShowPassword(!showPassword)}
                  >
                    {showPassword ? 'Hide' : 'Show'}
                  </button>
                </div>
              </div>

              <button
                type="submit"
                className="submit-btn"
                disabled={loading}
              >
                {loading ? 'Authenticating...' : 'Sign In as Admin'}
              </button>
            </form>
          </div>
        </div>
      );
    }

    if (currentPath === '/doctor-login') {
      return (
        <div className="auth-center">
          <div className="login-card">
            <button
              type="button"
              className="back-to-landing-btn"
              onClick={() => {
                setErrorMessage('');
                setEmail('');
                setPassword('');
                navigate('/login');
              }}
            >
              ← Back to Selection
            </button>

            <div className="login-header">
              <h2>Doctor Sign In</h2>
              <p>Sign in to access the CityCare Hospital medical portal.</p>
            </div>

            {errorMessage && (
              <div className="error-banner">
                {errorMessage}
              </div>
            )}

            <form onSubmit={handleDoctorLogin}>
              <div className="form-group">
                <label className="form-label" htmlFor="doctor-email">Email Address</label>
                <div className="input-wrapper">
                  <input
                    id="doctor-email"
                    type="email"
                    className="form-input"
                    placeholder="Enter doctor email @citycare.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="doctor-password">Password</label>
                <div className="input-wrapper">
                  <input
                    id="doctor-password"
                    type={showPassword ? 'text' : 'password'}
                    className="form-input"
                    placeholder="Enter password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                  />
                  <button
                    type="button"
                    className="toggle-password-btn"
                    onClick={() => setShowPassword(!showPassword)}
                  >
                    {showPassword ? 'Hide' : 'Show'}
                  </button>
                </div>
              </div>

              <button
                type="submit"
                className="submit-btn"
                disabled={loading}
              >
                {loading ? 'Authenticating...' : 'Sign In as Doctor'}
              </button>
            </form>
          </div>
        </div>
      );
    }

    // Default Landing Login Page (/login or /)
    return (
      <div className="auth-center">
        <div className="portal-card-container">
          <div className="login-header">
            <h2>CityCare Hospital</h2>
            <p>Hospital Management System</p>
          </div>

          <div className="portal-grid">
            <div className="portal-option-card">
              <div className="portal-icon">⚙️</div>
              <h3 className="portal-title">Admin Login</h3>
              <p className="portal-desc">
                Access hospital administration, billing, doctor management, and system settings.
              </p>
              <button
                className="portal-btn portal-btn-admin"
                onClick={() => {
                  setErrorMessage('');
                  setEmail('');
                  setPassword('');
                  navigate('/admin-login');
                }}
              >
                Admin Login
              </button>
            </div>

            <div className="portal-option-card">
              <div className="portal-icon">👨‍⚕️</div>
              <h3 className="portal-title">Doctor Login</h3>
              <p className="portal-desc">
                Access patient appointments, medical records, test results, and prescriptions.
              </p>
              <button
                className="portal-btn portal-btn-doctor"
                onClick={() => {
                  setErrorMessage('');
                  setEmail('');
                  setPassword('');
                  navigate('/doctor-login');
                }}
              >
                Doctor Login
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Derive active tab from URL path (e.g. /patients -> 'patients', default 'dashboard')
  const rawTab = currentPath.substring(1);
  const activeTab = rawTab || 'dashboard';

  const handleTabSelect = (tab) => {
    navigate(`/${tab}`);
  };

  // Authenticated view: Main Application Layout
  return (
    <div className="app-wrapper">
      <Sidebar
        role={currentUser.role}
        activeTab={activeTab}
        onTabSelect={handleTabSelect}
        onLogout={handleLogout}
        isMobileOpen={isMobileSidebarOpen}
        onCloseMobile={() => setIsMobileSidebarOpen(false)}
      />

      <div className="main-layout">
        <Header
          user={currentUser}
          onToggleMobileSidebar={() => setIsMobileSidebarOpen(!isMobileSidebarOpen)}
          onNavigateToNotifications={() => handleTabSelect('notifications')}
        />

        <main className="content-area">
          {activeTab === 'dashboard' ? (
            <Dashboard user={currentUser} />
          ) : activeTab === 'patients' ? (
            <Patients />
          ) : activeTab === 'doctors' ? (
            <Doctors user={currentUser} />
          ) : activeTab === 'departments' ? (
            <Departments user={currentUser} />
          ) : activeTab === 'appointments' ? (
            <Appointments user={currentUser} />
          ) : activeTab === 'prescriptions' ? (
            <Prescriptions user={currentUser} />
          ) : activeTab === 'medical-records' ? (
            <MedicalRecords user={currentUser} />
          ) : activeTab === 'medical-tests' ? (
            <MedicalTests user={currentUser} />
          ) : activeTab === 'billing' ? (
            <Billing user={currentUser} />
          ) : activeTab === 'notifications' ? (
            <Notifications user={currentUser} />
          ) : (
            <PlaceholderPage title={activeTab.charAt(0).toUpperCase() + activeTab.slice(1).replace('-', ' ')} />
          )}
        </main>
      </div>
    </div>
  );
}

export default App;
