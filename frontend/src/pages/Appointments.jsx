import React, { useState, useEffect } from 'react';

const API_BASE_URL = 'http://localhost:8080';

function Appointments({ user }) {
  const [appointments, setAppointments] = useState([]);
  const [patients, setPatients] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [departments, setDepartments] = useState([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Filters & Search
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [deptFilter, setDeptFilter] = useState('ALL');

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [currentApptId, setCurrentApptId] = useState(null);
  const [displayApptId, setDisplayApptId] = useState('');

  // Form Fields
  const [formData, setFormData] = useState({
    patientId: '',
    doctorId: '',
    departmentId: '',
    appointmentDate: '',
    appointmentTime: '10:00',
    reason: '',
    status: 'SCHEDULED'
  });
  const [formError, setFormError] = useState('');

  // Cancel Confirmation Modal State
  const [cancelTarget, setCancelTarget] = useState(null);
  const [cancelError, setCancelError] = useState('');

  const isAdmin = user?.role?.toUpperCase() === 'ADMIN';
  const isDoctor = user?.role?.toUpperCase() === 'DOCTOR';

  const fetchData = async () => {
    setLoading(true);
    setError('');
    try {
      const [apptsRes, patientsRes, docsRes, deptsRes] = await Promise.all([
        fetch(`${API_BASE_URL}/api/appointments`, { credentials: 'include' }),
        fetch(`${API_BASE_URL}/api/patients`, { credentials: 'include' }),
        fetch(`${API_BASE_URL}/api/doctors`, { credentials: 'include' }),
        fetch(`${API_BASE_URL}/api/departments`, { credentials: 'include' })
      ]);

      if (apptsRes.ok) {
        const data = await apptsRes.json();
        setAppointments(data);
      } else {
        setError('Unable to load appointments.');
      }

      if (patientsRes.ok) setPatients(await patientsRes.json());
      if (docsRes.ok) setDoctors(await docsRes.json());
      if (deptsRes.ok) setDepartments(await deptsRes.json());
    } catch (err) {
      setError('Connection error while fetching appointments.');
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

  // Smart Doctor-Department resolution helpers
  const getLoggedInDoctorProfile = () => {
    if (!isDoctor) return null;
    return doctors.find(d => d.email === user?.email || d.userId === user?.id) || null;
  };

  const resolvePatientDepartmentAndDoctor = (selectedPatientIdStr) => {
    const pId = Number(selectedPatientIdStr);
    if (!selectedPatientIdStr || !pId) {
      return { departmentId: '', doctorId: '' };
    }

    const patient = patients.find(p => p.id === pId);
    if (!patient) {
      return { departmentId: '', doctorId: '' };
    }

    // 1. Resolve Patient's Department ID
    let matchedDeptId = '';
    if (patient.departmentId) {
      matchedDeptId = patient.departmentId;
    } else if (patient.departmentName) {
      const foundDept = departments.find(d => d.name.toLowerCase() === patient.departmentName.toLowerCase());
      if (foundDept) matchedDeptId = foundDept.id;
    }

    // 2. Resolve Doctor ID (if patient has a specific doctor field/name)
    let matchedDocId = '';
    if (patient.doctorId) {
      matchedDocId = patient.doctorId;
    } else if (patient.doctorName) {
      const foundDoc = doctors.find(d => d.name.toLowerCase() === patient.doctorName.toLowerCase());
      if (foundDoc) matchedDocId = foundDoc.id;
    }

    // If patient has a specific department resolved:
    if (matchedDeptId) {
      const deptObj = departments.find(d => d.id === Number(matchedDeptId));
      const deptNameLower = deptObj ? deptObj.name.toLowerCase() : '';

      const deptDoctors = doctors.filter(d => 
        d.departmentId === Number(matchedDeptId) || 
        (d.departmentName && d.departmentName.toLowerCase() === deptNameLower) ||
        (d.specialization && d.specialization.toLowerCase() === deptNameLower)
      );

      // If patient already has an assigned doctor that belongs to that department:
      if (matchedDocId && deptDoctors.some(d => d.id === Number(matchedDocId))) {
        return { departmentId: matchedDeptId, doctorId: matchedDocId };
      }

      // If exactly ONE doctor belongs to that department, select that doctor automatically!
      if (deptDoctors.length === 1) {
        return { departmentId: matchedDeptId, doctorId: deptDoctors[0].id };
      }

      // If multiple doctors exist in that department, keep Doctor as '' (-- Select Doctor --)
      return { departmentId: matchedDeptId, doctorId: '' };
    }

    return { departmentId: '', doctorId: '' };
  };

  const openAddModal = () => {
    setIsEditing(false);
    setCurrentApptId(null);
    setDisplayApptId('');
    setFormError('');

    const loggedInDoc = getLoggedInDoctorProfile();
    let initialDocId = '';
    let initialDeptId = '';
    const initialPatientId = patients.length > 0 ? patients[0].id : '';

    if (loggedInDoc) {
      initialDocId = loggedInDoc.id;
      const matchedDept = departments.find(d => d.id === loggedInDoc.departmentId || d.name.toLowerCase() === loggedInDoc.specialization.toLowerCase());
      if (matchedDept) initialDeptId = matchedDept.id;
    } else if (initialPatientId) {
      const resolved = resolvePatientDepartmentAndDoctor(initialPatientId);
      initialDeptId = resolved.departmentId;
      initialDocId = resolved.doctorId;
    }

    const todayStr = new Date().toISOString().split('T')[0];

    setFormData({
      patientId: initialPatientId,
      doctorId: initialDocId,
      departmentId: initialDeptId,
      appointmentDate: todayStr,
      appointmentTime: '10:00',
      reason: '',
      status: 'SCHEDULED'
    });
    setIsModalOpen(true);
  };

  const openEditModal = (appt) => {
    setIsEditing(true);
    setCurrentApptId(appt.id);
    setDisplayApptId(appt.appointmentId);
    setFormError('');

    setFormData({
      patientId: appt.patientId,
      doctorId: appt.doctorId,
      departmentId: appt.departmentId,
      appointmentDate: appt.appointmentDate,
      appointmentTime: appt.appointmentTime?.substring(0, 5) || '10:00',
      reason: appt.reason,
      status: appt.status
    });
    setIsModalOpen(true);
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setFormError('');
  };

  // Handle patient change -> Auto-select department & doctor
  const handlePatientChange = (patientIdStr) => {
    if (isDoctor) {
      setFormData(prev => ({ ...prev, patientId: patientIdStr }));
      return;
    }

    const resolved = resolvePatientDepartmentAndDoctor(patientIdStr);
    setFormData(prev => ({
      ...prev,
      patientId: patientIdStr,
      departmentId: resolved.departmentId,
      doctorId: resolved.doctorId
    }));
  };

  // Handle department change -> Filter/auto-select doctor
  const handleDepartmentChange = (deptIdStr) => {
    const selectedDeptId = Number(deptIdStr);
    setFormData(prev => {
      let newDocId = prev.doctorId;
      if (selectedDeptId) {
        const deptObj = departments.find(d => d.id === selectedDeptId);
        const deptNameLower = deptObj ? deptObj.name.toLowerCase() : '';
        const deptDoctors = doctors.filter(d => 
          d.departmentId === selectedDeptId || 
          (d.departmentName && d.departmentName.toLowerCase() === deptNameLower) ||
          (d.specialization && d.specialization.toLowerCase() === deptNameLower)
        );
        const currentDocMatches = prev.doctorId && deptDoctors.some(d => d.id === Number(prev.doctorId));
        if (!currentDocMatches) {
          if (deptDoctors.length === 1) {
            newDocId = deptDoctors[0].id;
          } else {
            newDocId = '';
          }
        }
      } else {
        newDocId = '';
      }
      return { ...prev, departmentId: selectedDeptId, doctorId: newDocId };
    });
  };

  // Handle doctor change -> Auto-set department
  const handleDoctorChange = (docIdStr) => {
    const selectedDocId = Number(docIdStr);
    const doc = doctors.find(d => d.id === selectedDocId);
    let matchedDeptId = formData.departmentId;

    if (doc) {
      const foundDept = departments.find(d => d.id === doc.departmentId || d.name.toLowerCase() === doc.specialization.toLowerCase());
      if (foundDept) {
        matchedDeptId = foundDept.id;
      }
    }

    setFormData(prev => ({
      ...prev,
      doctorId: selectedDocId,
      departmentId: matchedDeptId
    }));
  };

  const handleFormSubmit = async (e) => {
    e.preventDefault();
    setFormError('');

    if (!formData.patientId) return setFormError('Please select a patient.');
    if (!formData.doctorId) return setFormError('Please select a doctor.');
    if (!formData.departmentId) return setFormError('Please select a department.');
    if (!formData.appointmentDate) return setFormError('Please select an appointment date.');
    if (!formData.appointmentTime) return setFormError('Please select an appointment time.');
    if (!formData.reason.trim()) return setFormError('Reason for appointment is required.');

    const payload = {
      patientId: Number(formData.patientId),
      doctorId: Number(formData.doctorId),
      departmentId: Number(formData.departmentId),
      appointmentDate: formData.appointmentDate,
      appointmentTime: formData.appointmentTime.length === 5 ? `${formData.appointmentTime}:00` : formData.appointmentTime,
      reason: formData.reason.trim(),
      status: formData.status
    };

    try {
      const url = isEditing
        ? `${API_BASE_URL}/api/appointments/${currentApptId}`
        : `${API_BASE_URL}/api/appointments`;
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
        showNotification(isEditing ? 'Appointment updated successfully.' : 'Appointment scheduled successfully.');
      } else {
        const errorData = await response.json().catch(() => ({}));
        setFormError(errorData.message || 'Failed to save appointment.');
      }
    } catch (err) {
      setFormError('Server connection error. Please try again.');
    }
  };

  const handleStatusAction = async (id, action) => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/appointments/${id}/${action}`, {
        method: 'PUT',
        credentials: 'include'
      });

      if (response.ok) {
        fetchData();
        showNotification(`Appointment marked as ${action === 'cancel' ? 'Cancelled' : 'Completed'}.`);
      } else {
        const errorData = await response.json().catch(() => ({}));
        alert(errorData.message || `Unable to ${action} appointment.`);
      }
    } catch (err) {
      alert(`Server error while processing ${action}.`);
    }
  };

  // Filtered Appointments
  const filteredAppointments = appointments.filter(appt => {
    const q = searchQuery.toLowerCase().trim();
    const matchesSearch = !q || (
      appt.appointmentId.toLowerCase().includes(q) ||
      appt.patientName.toLowerCase().includes(q) ||
      appt.doctorName.toLowerCase().includes(q) ||
      appt.departmentName.toLowerCase().includes(q)
    );

    const matchesStatus = statusFilter === 'ALL' || appt.status === statusFilter;
    const matchesDept = deptFilter === 'ALL' || appt.departmentId === Number(deptFilter) || appt.departmentName.toLowerCase() === deptFilter.toLowerCase();

    return matchesSearch && matchesStatus && matchesDept;
  });

  // Calculate Summary Counts
  const totalCount = appointments.length;
  const scheduledCount = appointments.filter(a => a.status === 'SCHEDULED').length;
  const completedCount = appointments.filter(a => a.status === 'COMPLETED').length;
  const cancelledCount = appointments.filter(a => a.status === 'CANCELLED').length;

  // Filter available doctors when department is selected in form
  const availableFormDoctors = formData.departmentId
    ? doctors.filter(d => d.departmentId === Number(formData.departmentId) || d.departmentName?.toLowerCase() === departments.find(dp => dp.id === Number(formData.departmentId))?.name.toLowerCase())
    : doctors;

  return (
    <div className="appointments-page">
      {/* Page Header */}
      <div className="page-header">
        <div>
          <h2 className="page-title">Appointment Management</h2>
          <p className="page-subtitle">Schedule and manage patient appointments.</p>
        </div>
        <button className="primary-btn" onClick={openAddModal}>
          + Schedule Appointment
        </button>
      </div>

      {/* Toast Notification */}
      {successMessage && (
        <div className="success-banner">
          {successMessage}
        </div>
      )}

      {/* Metrics Summary Cards */}
      <div className="metrics-grid" style={{ marginBottom: '1.5rem' }}>
        <div className="metric-card card-blue">
          <div className="card-icon">📅</div>
          <div className="card-info">
            <div className="card-count">{totalCount}</div>
            <div className="card-title">Total Appointments</div>
          </div>
        </div>

        <div className="metric-card card-indigo">
          <div className="card-icon">⏳</div>
          <div className="card-info">
            <div className="card-count">{scheduledCount}</div>
            <div className="card-title">Scheduled</div>
          </div>
        </div>

        <div className="metric-card card-green">
          <div className="card-icon">✅</div>
          <div className="card-info">
            <div className="card-count">{completedCount}</div>
            <div className="card-title">Completed</div>
          </div>
        </div>

        <div className="metric-card card-red">
          <div className="card-icon">✕</div>
          <div className="card-info">
            <div className="card-count">{cancelledCount}</div>
            <div className="card-title">Cancelled</div>
          </div>
        </div>
      </div>

      {/* Toolbar & Filters */}
      <div className="patients-toolbar" style={{ flexWrap: 'wrap', gap: '1rem' }}>
        <div className="search-box" style={{ flex: '1 1 250px' }}>
          <input
            type="text"
            className="search-input"
            placeholder="Search by ID, Patient, Doctor or Department..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
          <select
            className="form-input"
            style={{ width: 'auto', padding: '0.5rem 1rem' }}
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="ALL">All Statuses</option>
            <option value="SCHEDULED">Scheduled</option>
            <option value="COMPLETED">Completed</option>
            <option value="CANCELLED">Cancelled</option>
          </select>

          <select
            className="form-input"
            style={{ width: 'auto', padding: '0.5rem 1rem' }}
            value={deptFilter}
            onChange={(e) => setDeptFilter(e.target.value)}
          >
            <option value="ALL">All Departments</option>
            {departments.map(d => (
              <option key={d.id} value={d.id}>{d.name}</option>
            ))}
          </select>
        </div>
      </div>

      {/* Appointments Data Table */}
      {error ? (
        <div className="dashboard-error-box">
          <p>{error}</p>
          <button className="retry-btn" onClick={fetchData}>Try again</button>
        </div>
      ) : loading ? (
        <div className="dashboard-loading">Loading appointments...</div>
      ) : filteredAppointments.length === 0 ? (
        <div className="empty-state-box">
          <div className="empty-icon">📅</div>
          <h3>{searchQuery || statusFilter !== 'ALL' || deptFilter !== 'ALL' ? 'No matching appointments found' : 'No appointments scheduled'}</h3>
          <p>Schedule a new appointment to get started.</p>
        </div>
      ) : (
        <div className="table-responsive">
          <table className="custom-table">
            <thead>
              <tr>
                <th>Appointment ID</th>
                <th>Patient</th>
                <th>Doctor</th>
                <th>Department</th>
                <th>Date & Time</th>
                <th>Reason</th>
                <th style={{ textAlign: 'center' }}>Status</th>
                <th style={{ textAlign: 'center' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredAppointments.map((appt) => {
                const statusBadgeStyle = appt.status === 'SCHEDULED'
                  ? { backgroundColor: '#dbeafe', color: '#1e40af' }
                  : appt.status === 'COMPLETED'
                  ? { backgroundColor: '#dcfce7', color: '#166534' }
                  : { backgroundColor: '#fee2e2', color: '#991b1b' };

                return (
                  <tr key={appt.id}>
                    <td><span className="patient-id-badge">{appt.appointmentId}</span></td>
                    <td className="font-semibold">{appt.patientName}</td>
                    <td>{appt.doctorName}</td>
                    <td><span className="user-badge">{appt.departmentName}</span></td>
                    <td>
                      <div style={{ fontWeight: '500' }}>{appt.appointmentDate}</div>
                      <div style={{ fontSize: '0.8rem', color: '#64748b' }}>{appt.appointmentTime?.substring(0, 5)}</div>
                    </td>
                    <td className="address-cell" title={appt.reason}>{appt.reason}</td>
                    <td style={{ textAlign: 'center' }}>
                      <span className="user-badge" style={statusBadgeStyle}>
                        {appt.status}
                      </span>
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      <div className="action-buttons" style={{ justifyContent: 'center' }}>
                        {appt.status === 'SCHEDULED' && (
                          <>
                            <button
                              className="action-btn edit-btn"
                              style={{ backgroundColor: '#10b981', color: '#fff' }}
                              onClick={() => handleStatusAction(appt.id, 'complete')}
                              title="Mark Completed"
                            >
                              Complete
                            </button>
                            <button
                              className="action-btn edit-btn"
                              onClick={() => openEditModal(appt)}
                            >
                              Edit
                            </button>
                            <button
                              className="action-btn delete-btn"
                              onClick={() => setCancelTarget(appt)}
                            >
                              Cancel
                            </button>
                          </>
                        )}
                        {appt.status !== 'SCHEDULED' && (
                          <span style={{ fontSize: '0.85rem', color: '#94a3b8' }}>No actions</span>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Add / Edit Appointment Modal */}
      {isModalOpen && (
        <div className="modal-overlay">
          <div className="modal-card" style={{ maxWidth: '550px' }}>
            <div className="modal-header">
              <h3>{isEditing ? `Edit Appointment (${displayApptId})` : 'Schedule New Appointment'}</h3>
              <button className="modal-close" onClick={closeModal}>&times;</button>
            </div>

            {formError && (
              <div className="error-banner">
                {formError}
              </div>
            )}

            <form onSubmit={handleFormSubmit}>
              {/* Patient Selection */}
              <div className="form-group">
                <label className="form-label">Patient *</label>
                <select
                  className="form-input"
                  value={formData.patientId}
                  onChange={(e) => handlePatientChange(e.target.value)}
                  required
                >
                  <option value="">-- Select Patient --</option>
                  {patients.map(p => (
                    <option key={p.id} value={p.id}>{p.patientId} — {p.name}</option>
                  ))}
                </select>
              </div>

              {/* Department Selection */}
              <div className="form-group">
                <label className="form-label">Department *</label>
                <select
                  className="form-input"
                  value={formData.departmentId}
                  onChange={(e) => handleDepartmentChange(e.target.value)}
                  disabled={isDoctor}
                  required
                >
                  <option value="">-- Select Department --</option>
                  {departments.map(d => (
                    <option key={d.id} value={d.id}>{d.departmentId} — {d.name}</option>
                  ))}
                </select>
              </div>

              {/* Doctor Selection */}
              <div className="form-group">
                <label className="form-label">Doctor *</label>
                <select
                  className="form-input"
                  value={formData.doctorId}
                  onChange={(e) => handleDoctorChange(e.target.value)}
                  disabled={isDoctor}
                  required
                >
                  <option value="">-- Select Doctor --</option>
                  {availableFormDoctors.map(doc => (
                    <option key={doc.id} value={doc.id}>{doc.doctorId} — {doc.name} ({doc.specialization})</option>
                  ))}
                </select>
              </div>

              {/* Date and Time */}
              <div className="form-row" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div className="form-group">
                  <label className="form-label">Date *</label>
                  <input
                    type="date"
                    className="form-input"
                    value={formData.appointmentDate}
                    onChange={(e) => setFormData({ ...formData, appointmentDate: e.target.value })}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Time *</label>
                  <select
                    className="form-input"
                    value={formData.appointmentTime}
                    onChange={(e) => setFormData({ ...formData, appointmentTime: e.target.value })}
                    required
                  >
                    <option value="09:00">09:00 AM</option>
                    <option value="09:30">09:30 AM</option>
                    <option value="10:00">10:00 AM</option>
                    <option value="10:30">10:30 AM</option>
                    <option value="11:00">11:00 AM</option>
                    <option value="11:30">11:30 AM</option>
                    <option value="12:00">12:00 PM</option>
                    <option value="14:00">02:00 PM</option>
                    <option value="14:30">02:30 PM</option>
                    <option value="15:00">03:00 PM</option>
                    <option value="15:30">03:30 PM</option>
                    <option value="16:00">04:00 PM</option>
                  </select>
                </div>
              </div>

              {/* Reason */}
              <div className="form-group">
                <label className="form-label">Reason for Appointment *</label>
                <textarea
                  className="form-input"
                  rows="3"
                  placeholder="Describe health concern or reason for visit..."
                  value={formData.reason}
                  onChange={(e) => setFormData({ ...formData, reason: e.target.value })}
                  required
                />
              </div>

              {/* Status (Edit only) */}
              {isEditing && (
                <div className="form-group">
                  <label className="form-label">Status</label>
                  <select
                    className="form-input"
                    value={formData.status}
                    onChange={(e) => setFormData({ ...formData, status: e.target.value })}
                  >
                    <option value="SCHEDULED">Scheduled</option>
                    <option value="COMPLETED">Completed</option>
                    <option value="CANCELLED">Cancelled</option>
                  </select>
                </div>
              )}

              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={closeModal}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  {isEditing ? 'Update Appointment' : 'Schedule Appointment'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Cancel Confirmation Modal */}
      {cancelTarget && (
        <div className="modal-overlay">
          <div className="modal-card small-modal">
            <div className="modal-header">
              <h3>Confirm Cancellation</h3>
              <button className="modal-close" onClick={() => setCancelTarget(null)}>&times;</button>
            </div>

            <p className="delete-confirm-text">
              Are you sure you want to cancel appointment <strong>{cancelTarget.appointmentId}</strong> for <strong>{cancelTarget.patientName}</strong>?
            </p>
            <div className="modal-actions">
              <button className="btn-secondary" onClick={() => setCancelTarget(null)}>
                Keep Appointment
              </button>
              <button className="btn-danger" onClick={() => {
                const targetId = cancelTarget.id;
                setCancelTarget(null);
                handleStatusAction(targetId, 'cancel');
              }}>
                Cancel Appointment
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Appointments;
