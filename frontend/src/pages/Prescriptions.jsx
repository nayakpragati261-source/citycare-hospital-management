import React, { useState, useEffect } from 'react';

const API_BASE_URL = 'http://localhost:8080';

function Prescriptions({ user }) {
  const [prescriptions, setPrescriptions] = useState([]);
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
  const [currentRxId, setCurrentRxId] = useState(null);
  const [displayRxId, setDisplayRxId] = useState('');

  // Form Fields
  const [formData, setFormData] = useState({
    patientId: '',
    doctorId: '',
    appointmentId: '',
    prescriptionDate: '',
    medicine: '',
    dosage: '',
    duration: '',
    instructions: ''
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
      const [rxRes, patientsRes, docsRes, apptsRes] = await Promise.all([
        fetch(`${API_BASE_URL}/api/prescriptions`, { credentials: 'include' }),
        fetch(`${API_BASE_URL}/api/patients`, { credentials: 'include' }),
        fetch(`${API_BASE_URL}/api/doctors`, { credentials: 'include' }),
        fetch(`${API_BASE_URL}/api/appointments`, { credentials: 'include' })
      ]);

      if (rxRes.ok) setPrescriptions(await rxRes.json());
      else setError('Unable to load prescriptions.');

      if (patientsRes.ok) setPatients(await patientsRes.json());
      if (docsRes.ok) setDoctors(await docsRes.json());
      if (apptsRes.ok) setAppointments(await apptsRes.json());
    } catch (err) {
      setError('Connection error while fetching prescriptions.');
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
    setCurrentRxId(null);
    setDisplayRxId('');
    setFormError('');

    const loggedInDoc = getLoggedInDoctorProfile();
    const todayStr = new Date().toISOString().split('T')[0];

    setFormData({
      patientId: patients.length > 0 ? patients[0].id : '',
      doctorId: loggedInDoc ? loggedInDoc.id : (doctors.length > 0 ? doctors[0].id : ''),
      appointmentId: '',
      prescriptionDate: todayStr,
      medicine: '',
      dosage: '',
      duration: '',
      instructions: ''
    });
    setIsModalOpen(true);
  };

  const openEditModal = (rx) => {
    setIsEditing(true);
    setCurrentRxId(rx.id);
    setDisplayRxId(rx.prescriptionId);
    setFormError('');

    setFormData({
      patientId: rx.patientId,
      doctorId: rx.doctorId,
      appointmentId: rx.appointmentId || '',
      prescriptionDate: rx.prescriptionDate,
      medicine: rx.medicine,
      dosage: rx.dosage,
      duration: rx.duration,
      instructions: rx.instructions || ''
    });
    setIsModalOpen(true);
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setFormError('');
  };

  // Appointment select handler -> Auto populate patient and doctor
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
    if (!formData.prescriptionDate) return setFormError('Prescription date is required.');
    if (!formData.medicine.trim()) return setFormError('Medicine details are required.');
    if (!formData.dosage.trim()) return setFormError('Dosage is required.');
    if (!formData.duration.trim()) return setFormError('Duration is required.');

    const payload = {
      patientId: Number(formData.patientId),
      doctorId: Number(formData.doctorId),
      appointmentId: formData.appointmentId ? Number(formData.appointmentId) : null,
      prescriptionDate: formData.prescriptionDate,
      medicine: formData.medicine.trim(),
      dosage: formData.dosage.trim(),
      duration: formData.duration.trim(),
      instructions: formData.instructions?.trim() || null
    };

    try {
      const url = isEditing
        ? `${API_BASE_URL}/api/prescriptions/${currentRxId}`
        : `${API_BASE_URL}/api/prescriptions`;
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
        showNotification(isEditing ? 'Prescription updated successfully.' : 'Prescription created successfully.');
      } else {
        const errorData = await response.json().catch(() => ({}));
        setFormError(errorData.message || 'Failed to save prescription.');
      }
    } catch (err) {
      setFormError('Server connection error.');
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      const response = await fetch(`${API_BASE_URL}/api/prescriptions/${deleteTarget.id}`, {
        method: 'DELETE',
        credentials: 'include'
      });

      if (response.ok) {
        setDeleteTarget(null);
        fetchData();
        showNotification('Prescription deleted successfully.');
      } else {
        const errorData = await response.json().catch(() => ({}));
        alert(errorData.message || 'Unable to delete prescription.');
      }
    } catch (err) {
      alert('Server error while deleting prescription.');
    }
  };

  const filteredPrescriptions = prescriptions.filter(rx => {
    const q = searchQuery.toLowerCase().trim();
    if (!q) return true;
    return (
      rx.prescriptionId.toLowerCase().includes(q) ||
      rx.patientName.toLowerCase().includes(q) ||
      rx.doctorName.toLowerCase().includes(q) ||
      rx.medicine.toLowerCase().includes(q)
    );
  });

  return (
    <div className="prescriptions-page">
      <div className="page-header">
        <div>
          <h2 className="page-title">Prescription Management</h2>
          <p className="page-subtitle">Manage medical prescriptions for hospital patients.</p>
        </div>
        <button className="primary-btn" onClick={openAddModal}>
          + Create Prescription
        </button>
      </div>

      {successMessage && <div className="success-banner">{successMessage}</div>}

      <div className="patients-toolbar">
        <div className="summary-pill">
          <span className="pill-label">Total Prescriptions:</span>
          <span className="pill-count">{prescriptions.length}</span>
        </div>

        <div className="search-box">
          <input
            type="text"
            className="search-input"
            placeholder="Search prescriptions by ID, patient, doctor, or medicine..."
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
        <div className="dashboard-loading">Loading prescriptions...</div>
      ) : filteredPrescriptions.length === 0 ? (
        <div className="empty-state-box">
          <div className="empty-icon">💊</div>
          <h3>{searchQuery ? 'No matching prescriptions found' : 'No prescriptions recorded'}</h3>
          <p>Create a prescription to get started.</p>
        </div>
      ) : (
        <div className="table-responsive">
          <table className="custom-table">
            <thead>
              <tr>
                <th>Prescription ID</th>
                <th>Patient</th>
                <th>Doctor</th>
                <th>Medicine</th>
                <th>Dosage</th>
                <th>Duration</th>
                <th>Date</th>
                <th style={{ textAlign: 'center' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredPrescriptions.map(rx => (
                <tr key={rx.id}>
                  <td><span className="patient-id-badge">{rx.prescriptionId}</span></td>
                  <td className="font-semibold">{rx.patientName}</td>
                  <td>{rx.doctorName}</td>
                  <td className="font-semibold" style={{ color: '#0284c7' }}>{rx.medicine}</td>
                  <td>{rx.dosage}</td>
                  <td><span className="user-badge">{rx.duration}</span></td>
                  <td>{rx.prescriptionDate}</td>
                  <td style={{ textAlign: 'center' }}>
                    <div className="action-buttons" style={{ justifyContent: 'center' }}>
                      <button className="action-btn edit-btn" onClick={() => openEditModal(rx)}>Edit</button>
                      <button className="action-btn delete-btn" onClick={() => setDeleteTarget(rx)}>Delete</button>
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
              <h3>{isEditing ? `Edit Prescription (${displayRxId})` : 'Create Prescription'}</h3>
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
                  <option value="">-- Direct Prescription (No Appointment) --</option>
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
                <label className="form-label">Prescription Date *</label>
                <input
                  type="date"
                  className="form-input"
                  value={formData.prescriptionDate}
                  onChange={(e) => setFormData({ ...formData, prescriptionDate: e.target.value })}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Medicine Name & Formulation *</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. Amoxicillin 500mg, Paracetamol"
                  value={formData.medicine}
                  onChange={(e) => setFormData({ ...formData, medicine: e.target.value })}
                  required
                />
              </div>

              <div className="form-row" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div className="form-group">
                  <label className="form-label">Dosage *</label>
                  <input
                    type="text"
                    className="form-input"
                    placeholder="e.g. 1 tab twice daily"
                    value={formData.dosage}
                    onChange={(e) => setFormData({ ...formData, dosage: e.target.value })}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Duration *</label>
                  <input
                    type="text"
                    className="form-input"
                    placeholder="e.g. 5 days, 2 weeks"
                    value={formData.duration}
                    onChange={(e) => setFormData({ ...formData, duration: e.target.value })}
                    required
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Instructions / Notes</label>
                <textarea
                  className="form-input"
                  rows="2"
                  placeholder="Take after meals with warm water..."
                  value={formData.instructions}
                  onChange={(e) => setFormData({ ...formData, instructions: e.target.value })}
                />
              </div>

              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={closeModal}>Cancel</button>
                <button type="submit" className="btn-primary">{isEditing ? 'Update Prescription' : 'Save Prescription'}</button>
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
              Are you sure you want to delete prescription <strong>{deleteTarget.prescriptionId}</strong> for <strong>{deleteTarget.patientName}</strong>?
            </p>
            <div className="modal-actions">
              <button className="btn-secondary" onClick={() => setDeleteTarget(null)}>Cancel</button>
              <button className="btn-danger" onClick={handleDelete}>Delete Prescription</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Prescriptions;
