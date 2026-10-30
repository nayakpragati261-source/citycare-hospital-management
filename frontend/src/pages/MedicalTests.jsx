import React, { useState, useEffect } from 'react';

const API_BASE_URL = 'http://localhost:8080';

function MedicalTests({ user }) {
  const [tests, setTests] = useState([]);
  const [patients, setPatients] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [appointments, setAppointments] = useState([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [currentTestId, setCurrentTestId] = useState(null);
  const [displayTestId, setDisplayTestId] = useState('');

  // Form Fields
  const [formData, setFormData] = useState({
    patientId: '',
    doctorId: '',
    appointmentId: '',
    testName: '',
    testDate: '',
    result: '',
    status: 'PENDING'
  });
  const [formError, setFormError] = useState('');

  // Delete Target State
  const [deleteTarget, setDeleteTarget] = useState(null);

  const isAdmin = user?.role?.toUpperCase() === 'ADMIN';
  const isDoctor = user?.role?.toUpperCase() === 'DOCTOR';

  const fetchData = async () => {
    setLoading(true);
    setError('');
    try {
      const [testsRes, patientsRes, docsRes, apptsRes] = await Promise.all([
        fetch(`${API_BASE_URL}/api/medical-tests`, { credentials: 'include' }),
        fetch(`${API_BASE_URL}/api/patients`, { credentials: 'include' }),
        fetch(`${API_BASE_URL}/api/doctors`, { credentials: 'include' }),
        fetch(`${API_BASE_URL}/api/appointments`, { credentials: 'include' })
      ]);

      if (testsRes.ok) setTests(await testsRes.json());
      else setError('Unable to load medical tests.');

      if (patientsRes.ok) setPatients(await patientsRes.json());
      if (docsRes.ok) setDoctors(await docsRes.json());
      if (apptsRes.ok) setAppointments(await apptsRes.json());
    } catch (err) {
      setError('Connection error while fetching medical tests.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const showNotification = (msg) => {
    setSuccessMessage(msg);
    setTimeout(() => setSuccessMessage(''), 4000);
  };

  const getLoggedInDoctorProfile = () => {
    if (!isDoctor) return null;
    return doctors.find(d => d.email === user?.email || d.userId === user?.id) || null;
  };

  const openAddModal = () => {
    setIsEditing(false);
    setCurrentTestId(null);
    setDisplayTestId('');
    setFormError('');

    const loggedInDoc = getLoggedInDoctorProfile();
    const todayStr = new Date().toISOString().split('T')[0];

    setFormData({
      patientId: patients.length > 0 ? patients[0].id : '',
      doctorId: loggedInDoc ? loggedInDoc.id : (doctors.length > 0 ? doctors[0].id : ''),
      appointmentId: '',
      testName: '',
      testDate: todayStr,
      result: '',
      status: 'PENDING'
    });
    setIsModalOpen(true);
  };

  const openEditModal = (t) => {
    setIsEditing(true);
    setCurrentTestId(t.id);
    setDisplayTestId(t.testId);
    setFormError('');

    setFormData({
      patientId: t.patientId,
      doctorId: t.doctorId,
      appointmentId: t.appointmentId || '',
      testName: t.testName,
      testDate: t.testDate,
      result: t.result || '',
      status: t.status
    });
    setIsModalOpen(true);
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setFormError('');
  };

  const handleAppointmentChange = (apptIdStr) => {
    const selectedApptId = apptIdStr ? Number(apptIdStr) : '';
    if (!selectedApptId) {
      setFormData(prev => ({ ...prev, appointmentId: '' }));
      return;
    }

    const appt = appointments.find(a => a.id === selectedApptId);
    if (appt) {
      setFormData(prev => ({
        ...prev,
        appointmentId: selectedApptId,
        patientId: appt.patientId,
        doctorId: isDoctor ? prev.doctorId : appt.doctorId
      }));
    }
  };

  const handleFormSubmit = async (e) => {
    e.preventDefault();
    setFormError('');

    if (!formData.patientId) return setFormError('Patient is required.');
    if (!formData.doctorId) return setFormError('Doctor is required.');
    if (!formData.testDate) return setFormError('Test date is required.');
    if (!formData.testName.trim()) return setFormError('Test name is required.');

    if (formData.status === 'COMPLETED' && !formData.result.trim()) {
      return setFormError('A result is required when the medical test is completed.');
    }

    const payload = {
      patientId: Number(formData.patientId),
      doctorId: Number(formData.doctorId),
      appointmentId: formData.appointmentId ? Number(formData.appointmentId) : null,
      testName: formData.testName.trim(),
      testDate: formData.testDate,
      result: formData.result?.trim() || null,
      status: formData.status
    };

    try {
      const url = isEditing
        ? `${API_BASE_URL}/api/medical-tests/${currentTestId}`
        : `${API_BASE_URL}/api/medical-tests`;
      const method = isEditing ? 'PUT' : 'POST';

      const response = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(payload)
      });

      if (response.ok) {
        closeModal();
        fetchData();
        showNotification(isEditing ? 'Medical test updated successfully.' : 'Medical test ordered successfully.');
      } else {
        const errorData = await response.json().catch(() => ({}));
        setFormError(errorData.message || 'Failed to save medical test.');
      }
    } catch (err) {
      setFormError('Server connection error.');
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      const response = await fetch(`${API_BASE_URL}/api/medical-tests/${deleteTarget.id}`, {
        method: 'DELETE',
        credentials: 'include'
      });

      if (response.ok) {
        setDeleteTarget(null);
        fetchData();
        showNotification('Medical test deleted successfully.');
      } else {
        const errorData = await response.json().catch(() => ({}));
        alert(errorData.message || 'Unable to delete medical test.');
      }
    } catch (err) {
      alert('Server error while deleting medical test.');
    }
  };

  const filteredTests = tests.filter(t => {
    const q = searchQuery.toLowerCase().trim();
    const matchesQuery = !q || (
      t.testId.toLowerCase().includes(q) ||
      t.patientName.toLowerCase().includes(q) ||
      t.doctorName.toLowerCase().includes(q) ||
      t.testName.toLowerCase().includes(q) ||
      (t.result && t.result.toLowerCase().includes(q))
    );
    const matchesStatus = statusFilter === 'ALL' || t.status === statusFilter;
    return matchesQuery && matchesStatus;
  });

  const totalCount = tests.length;
  const pendingCount = tests.filter(t => t.status === 'PENDING').length;
  const completedCount = tests.filter(t => t.status === 'COMPLETED').length;

  return (
    <div className="medical-tests-page">
      <div className="page-header">
        <div>
          <h2 className="page-title">Medical Test Management</h2>
          <p className="page-subtitle">Order, track, and record diagnostic medical test results.</p>
        </div>
        <button className="primary-btn" onClick={openAddModal}>
          + Order Medical Test
        </button>
      </div>

      {successMessage && <div className="success-banner">{successMessage}</div>}

      {/* Metrics Cards */}
      <div className="dashboard-grid" style={{ marginBottom: '1.5rem', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))' }}>
        <div className="metric-card">
          <div className="metric-header">
            <span className="metric-title">Total Tests</span>
            <span className="metric-icon" style={{ backgroundColor: '#e0f2fe', color: '#0369a1' }}>🧪</span>
          </div>
          <div className="metric-value">{totalCount}</div>
        </div>

        <div className="metric-card">
          <div className="metric-header">
            <span className="metric-title">Pending Tests</span>
            <span className="metric-icon" style={{ backgroundColor: '#fef3c7', color: '#b45309' }}>⏳</span>
          </div>
          <div className="metric-value" style={{ color: '#d97706' }}>{pendingCount}</div>
        </div>

        <div className="metric-card">
          <div className="metric-header">
            <span className="metric-title">Completed Tests</span>
            <span className="metric-icon" style={{ backgroundColor: '#dcfce7', color: '#15803d' }}>✓</span>
          </div>
          <div className="metric-value" style={{ color: '#16a34a' }}>{completedCount}</div>
        </div>
      </div>

      <div className="patients-toolbar" style={{ flexWrap: 'wrap', gap: '1rem' }}>
        <div className="search-box" style={{ flex: '1 1 250px' }}>
          <input
            type="text"
            className="search-input"
            placeholder="Search tests by ID, patient, doctor, test name..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        <select
          className="form-input"
          style={{ width: 'auto', padding: '0.5rem 1rem' }}
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
        >
          <option value="ALL">All Statuses</option>
          <option value="PENDING">Pending</option>
          <option value="COMPLETED">Completed</option>
        </select>
      </div>

      {error ? (
        <div className="dashboard-error-box">
          <p>{error}</p>
          <button className="retry-btn" onClick={fetchData}>Try again</button>
        </div>
      ) : loading ? (
        <div className="dashboard-loading">Loading medical tests...</div>
      ) : filteredTests.length === 0 ? (
        <div className="empty-state-box">
          <div className="empty-icon">🔬</div>
          <h3>{searchQuery || statusFilter !== 'ALL' ? 'No matching medical tests found' : 'No medical tests ordered'}</h3>
          <p>Order a new diagnostic test to get started.</p>
        </div>
      ) : (
        <div className="table-responsive">
          <table className="custom-table">
            <thead>
              <tr>
                <th>Test ID</th>
                <th>Patient</th>
                <th>Doctor</th>
                <th>Test Name</th>
                <th>Date</th>
                <th>Result</th>
                <th style={{ textAlign: 'center' }}>Status</th>
                <th style={{ textAlign: 'center' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredTests.map(t => {
                const statusStyle = t.status === 'COMPLETED'
                  ? { backgroundColor: '#dcfce7', color: '#166534' }
                  : { backgroundColor: '#fef3c7', color: '#92400e' };

                return (
                  <tr key={t.id}>
                    <td><span className="patient-id-badge">{t.testId}</span></td>
                    <td className="font-semibold">{t.patientName}</td>
                    <td>{t.doctorName}</td>
                    <td className="font-semibold" style={{ color: '#6366f1' }}>{t.testName}</td>
                    <td>{t.testDate}</td>
                    <td className="address-cell" title={t.result || 'Pending Result'}>
                      {t.result || <em style={{ color: '#94a3b8' }}>Pending Result</em>}
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      <span className="user-badge" style={statusStyle}>
                        {t.status}
                      </span>
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      <div className="action-buttons" style={{ justifyContent: 'center' }}>
                        <button className="action-btn edit-btn" onClick={() => openEditModal(t)}>Edit</button>
                        <button className="action-btn delete-btn" onClick={() => setDeleteTarget(t)}>Delete</button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Add / Edit Modal */}
      {isModalOpen && (
        <div className="modal-overlay">
          <div className="modal-card" style={{ maxWidth: '550px' }}>
            <div className="modal-header">
              <h3>{isEditing ? `Edit Medical Test (${displayTestId})` : 'Order Medical Test'}</h3>
              <button className="modal-close" onClick={closeModal}>&times;</button>
            </div>

            {formError && <div className="error-banner">{formError}</div>}

            <form onSubmit={handleFormSubmit}>
              <div className="form-group">
                <label className="form-label">Link Appointment (Optional)</label>
                <select
                  className="form-input"
                  value={formData.appointmentId}
                  onChange={(e) => handleAppointmentChange(e.target.value)}
                >
                  <option value="">-- Direct Medical Test (No Appointment) --</option>
                  {appointments.map(a => (
                    <option key={a.id} value={a.id}>{a.appointmentId} — {a.patientName} with {a.doctorName} ({a.appointmentDate})</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Patient *</label>
                <select
                  className="form-input"
                  value={formData.patientId}
                  onChange={(e) => setFormData({ ...formData, patientId: e.target.value })}
                  required
                >
                  <option value="">-- Select Patient --</option>
                  {patients.map(p => (
                    <option key={p.id} value={p.id}>{p.patientId} — {p.name}</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Doctor *</label>
                <select
                  className="form-input"
                  value={formData.doctorId}
                  onChange={(e) => setFormData({ ...formData, doctorId: e.target.value })}
                  disabled={isDoctor}
                  required
                >
                  <option value="">-- Select Doctor --</option>
                  {doctors.map(d => (
                    <option key={d.id} value={d.id}>{d.doctorId} — {d.name}</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Test Name *</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. Complete Blood Count (CBC), Lipid Profile, Chest X-Ray"
                  value={formData.testName}
                  onChange={(e) => setFormData({ ...formData, testName: e.target.value })}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Test Date *</label>
                <input
                  type="date"
                  className="form-input"
                  value={formData.testDate}
                  onChange={(e) => setFormData({ ...formData, testDate: e.target.value })}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Status *</label>
                <select
                  className="form-input"
                  value={formData.status}
                  onChange={(e) => setFormData({ ...formData, status: e.target.value })}
                  required
                >
                  <option value="PENDING">Pending</option>
                  <option value="COMPLETED">Completed</option>
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Test Result {formData.status === 'COMPLETED' ? '*' : '(Optional while Pending)'}</label>
                <textarea
                  className="form-input"
                  rows="3"
                  placeholder={formData.status === 'COMPLETED' ? 'Enter test findings and numerical values (Required)...' : 'Result can be entered after test completion...'}
                  value={formData.result}
                  onChange={(e) => setFormData({ ...formData, result: e.target.value })}
                  required={formData.status === 'COMPLETED'}
                />
              </div>

              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={closeModal}>Cancel</button>
                <button type="submit" className="btn-primary">{isEditing ? 'Update Test' : 'Order Test'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {deleteTarget && (
        <div className="modal-overlay">
          <div className="modal-card small-modal">
            <div className="modal-header">
              <h3>Confirm Deletion</h3>
              <button className="modal-close" onClick={() => setDeleteTarget(null)}>&times;</button>
            </div>
            <p className="delete-confirm-text">
              Are you sure you want to delete test <strong>{deleteTarget.testId}</strong> ({deleteTarget.testName}) for <strong>{deleteTarget.patientName}</strong>?
            </p>
            <div className="modal-actions">
              <button className="btn-secondary" onClick={() => setDeleteTarget(null)}>Cancel</button>
              <button className="btn-danger" onClick={handleDelete}>Delete Test</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default MedicalTests;
