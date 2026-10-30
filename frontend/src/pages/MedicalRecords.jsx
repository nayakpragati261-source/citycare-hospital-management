import React, { useState, useEffect } from 'react';

const API_BASE_URL = 'http://localhost:8080';

function MedicalRecords({ user }) {
  const [records, setRecords] = useState([]);
  const [patients, setPatients] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [appointments, setAppointments] = useState([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [currentRecordId, setCurrentRecordId] = useState(null);
  const [displayRecordId, setDisplayRecordId] = useState('');

  // Form Fields
  const [formData, setFormData] = useState({
    patientId: '',
    doctorId: '',
    appointmentId: '',
    previousDisease: '',
    diagnosis: '',
    treatment: '',
    recordDate: '',
    notes: ''
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
      const [mrRes, patientsRes, docsRes, apptsRes] = await Promise.all([
        fetch(`${API_BASE_URL}/api/medical-records`, { credentials: 'include' }),
        fetch(`${API_BASE_URL}/api/patients`, { credentials: 'include' }),
        fetch(`${API_BASE_URL}/api/doctors`, { credentials: 'include' }),
        fetch(`${API_BASE_URL}/api/appointments`, { credentials: 'include' })
      ]);

      if (mrRes.ok) setRecords(await mrRes.json());
      else setError('Unable to load medical records.');

      if (patientsRes.ok) setPatients(await patientsRes.json());
      if (docsRes.ok) setDoctors(await docsRes.json());
      if (apptsRes.ok) setAppointments(await apptsRes.json());
    } catch (err) {
      setError('Connection error while fetching medical records.');
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
    setCurrentRecordId(null);
    setDisplayRecordId('');
    setFormError('');

    const loggedInDoc = getLoggedInDoctorProfile();
    const todayStr = new Date().toISOString().split('T')[0];

    setFormData({
      patientId: patients.length > 0 ? patients[0].id : '',
      doctorId: loggedInDoc ? loggedInDoc.id : (doctors.length > 0 ? doctors[0].id : ''),
      appointmentId: '',
      previousDisease: '',
      diagnosis: '',
      treatment: '',
      recordDate: todayStr,
      notes: ''
    });
    setIsModalOpen(true);
  };

  const openEditModal = (rec) => {
    setIsEditing(true);
    setCurrentRecordId(rec.id);
    setDisplayRecordId(rec.recordId);
    setFormError('');

    setFormData({
      patientId: rec.patientId,
      doctorId: rec.doctorId,
      appointmentId: rec.appointmentId || '',
      previousDisease: rec.previousDisease || '',
      diagnosis: rec.diagnosis,
      treatment: rec.treatment,
      recordDate: rec.recordDate,
      notes: rec.notes || ''
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
    if (!formData.recordDate) return setFormError('Record date is required.');
    if (!formData.diagnosis.trim()) return setFormError('Diagnosis is required.');
    if (!formData.treatment.trim()) return setFormError('Treatment details are required.');

    const payload = {
      patientId: Number(formData.patientId),
      doctorId: Number(formData.doctorId),
      appointmentId: formData.appointmentId ? Number(formData.appointmentId) : null,
      previousDisease: formData.previousDisease?.trim() || null,
      diagnosis: formData.diagnosis.trim(),
      treatment: formData.treatment.trim(),
      recordDate: formData.recordDate,
      notes: formData.notes?.trim() || null
    };

    try {
      const url = isEditing
        ? `${API_BASE_URL}/api/medical-records/${currentRecordId}`
        : `${API_BASE_URL}/api/medical-records`;
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
        showNotification(isEditing ? 'Medical record updated successfully.' : 'Medical record created successfully.');
      } else {
        const errorData = await response.json().catch(() => ({}));
        setFormError(errorData.message || 'Failed to save medical record.');
      }
    } catch (err) {
      setFormError('Server connection error.');
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      const response = await fetch(`${API_BASE_URL}/api/medical-records/${deleteTarget.id}`, {
        method: 'DELETE',
        credentials: 'include'
      });

      if (response.ok) {
        setDeleteTarget(null);
        fetchData();
        showNotification('Medical record deleted successfully.');
      } else {
        const errorData = await response.json().catch(() => ({}));
        alert(errorData.message || 'Unable to delete medical record.');
      }
    } catch (err) {
      alert('Server error while deleting medical record.');
    }
  };

  const filteredRecords = records.filter(rec => {
    const q = searchQuery.toLowerCase().trim();
    if (!q) return true;
    return (
      rec.recordId.toLowerCase().includes(q) ||
      rec.patientName.toLowerCase().includes(q) ||
      rec.doctorName.toLowerCase().includes(q) ||
      rec.diagnosis.toLowerCase().includes(q) ||
      rec.treatment.toLowerCase().includes(q)
    );
  });

  return (
    <div className="medical-records-page">
      <div className="page-header">
        <div>
          <h2 className="page-title">Medical Record Management</h2>
          <p className="page-subtitle">Track patient medical history, diagnoses, and treatment plans.</p>
        </div>
        <button className="primary-btn" onClick={openAddModal}>
          + Create Medical Record
        </button>
      </div>

      {successMessage && <div className="success-banner">{successMessage}</div>}

      <div className="patients-toolbar">
        <div className="summary-pill">
          <span className="pill-label">Total Medical Records:</span>
          <span className="pill-count">{records.length}</span>
        </div>

        <div className="search-box">
          <input
            type="text"
            className="search-input"
            placeholder="Search records by ID, patient, doctor, diagnosis, treatment..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
      </div>

      {error ? (
        <div className="dashboard-error-box">
          <p>{error}</p>
          <button className="retry-btn" onClick={fetchData}>Try again</button>
        </div>
      ) : loading ? (
        <div className="dashboard-loading">Loading medical records...</div>
      ) : filteredRecords.length === 0 ? (
        <div className="empty-state-box">
          <div className="empty-icon">📋</div>
          <h3>{searchQuery ? 'No matching medical records found' : 'No medical records found'}</h3>
          <p>Create a medical record to document patient care.</p>
        </div>
      ) : (
        <div className="table-responsive">
          <table className="custom-table">
            <thead>
              <tr>
                <th>Record ID</th>
                <th>Patient</th>
                <th>Doctor</th>
                <th>Diagnosis</th>
                <th>Treatment</th>
                <th>Date</th>
                <th style={{ textAlign: 'center' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredRecords.map(rec => (
                <tr key={rec.id}>
                  <td><span className="patient-id-badge">{rec.recordId}</span></td>
                  <td className="font-semibold">{rec.patientName}</td>
                  <td>{rec.doctorName}</td>
                  <td className="font-semibold" style={{ color: '#0369a1' }}>{rec.diagnosis}</td>
                  <td className="address-cell" title={rec.treatment}>{rec.treatment}</td>
                  <td>{rec.recordDate}</td>
                  <td style={{ textAlign: 'center' }}>
                    <div className="action-buttons" style={{ justifyContent: 'center' }}>
                      <button className="action-btn edit-btn" onClick={() => openEditModal(rec)}>Edit</button>
                      <button className="action-btn delete-btn" onClick={() => setDeleteTarget(rec)}>Delete</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Add / Edit Modal */}
      {isModalOpen && (
        <div className="modal-overlay">
          <div className="modal-card" style={{ maxWidth: '550px' }}>
            <div className="modal-header">
              <h3>{isEditing ? `Edit Medical Record (${displayRecordId})` : 'Create Medical Record'}</h3>
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
                  <option value="">-- Direct Medical Record (No Appointment) --</option>
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
                <label className="form-label">Record Date *</label>
                <input
                  type="date"
                  className="form-input"
                  value={formData.recordDate}
                  onChange={(e) => setFormData({ ...formData, recordDate: e.target.value })}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Previous Medical History / Diseases</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. Hypertension, Diabetes, None"
                  value={formData.previousDisease}
                  onChange={(e) => setFormData({ ...formData, previousDisease: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Diagnosis *</label>
                <textarea
                  className="form-input"
                  rows="2"
                  placeholder="e.g. Acute Upper Respiratory Tract Infection"
                  value={formData.diagnosis}
                  onChange={(e) => setFormData({ ...formData, diagnosis: e.target.value })}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Treatment Plan *</label>
                <textarea
                  className="form-input"
                  rows="2"
                  placeholder="e.g. Oral antibiotics, fluid therapy, follow-up in 5 days"
                  value={formData.treatment}
                  onChange={(e) => setFormData({ ...formData, treatment: e.target.value })}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Additional Clinical Notes</label>
                <textarea
                  className="form-input"
                  rows="2"
                  placeholder="Patient reports mild fever and dry cough..."
                  value={formData.notes}
                  onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                />
              </div>

              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={closeModal}>Cancel</button>
                <button type="submit" className="btn-primary">{isEditing ? 'Update Medical Record' : 'Save Medical Record'}</button>
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
              Are you sure you want to delete medical record <strong>{deleteTarget.recordId}</strong> for <strong>{deleteTarget.patientName}</strong>?
            </p>
            <div className="modal-actions">
              <button className="btn-secondary" onClick={() => setDeleteTarget(null)}>Cancel</button>
              <button className="btn-danger" onClick={handleDelete}>Delete Record</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default MedicalRecords;
