import React, { useState, useEffect } from 'react';

const API_BASE_URL = 'http://localhost:8080';

function Patients() {
  const [patients, setPatients] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [currentPatientId, setCurrentPatientId] = useState(null);
  const [displayPatientId, setDisplayPatientId] = useState('');

  // Form Fields
  const [formData, setFormData] = useState({
    name: '',
    age: '',
    gender: 'Male',
    disease: '',
    phone: '',
    address: '',
    departmentId: ''
  });
  const [formError, setFormError] = useState('');

  // Delete Confirmation State
  const [deletePatientTarget, setDeletePatientTarget] = useState(null);

  const fetchPatients = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await fetch(`${API_BASE_URL}/api/patients`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include'
      });

      if (response.ok) {
        const data = await response.json();
        setPatients(data);
      } else {
        setError('Unable to load patients. Please try again.');
      }
    } catch (err) {
      setError('Unable to load patients. Please verify server connection.');
    } finally {
      setLoading(false);
    }
  };

  const fetchDepartments = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/departments`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include'
      });
      if (response.ok) {
        const data = await response.json();
        setDepartments(data);
      }
    } catch (err) {
      console.error('Error fetching departments', err);
    }
  };

  useEffect(() => {
    fetchPatients();
    fetchDepartments();
  }, []);

  const showNotification = (msg) => {
    setSuccessMessage(msg);
    setTimeout(() => {
      setSuccessMessage('');
    }, 4000);
  };

  const openAddModal = async () => {
    setIsEditing(false);
    setCurrentPatientId(null);
    setDisplayPatientId('');
    await fetchDepartments();
    setFormData({
      name: '',
      age: '',
      gender: 'Male',
      disease: '',
      phone: '',
      address: '',
      departmentId: departments.length > 0 ? departments[0].id.toString() : ''
    });
    setFormError('');
    setIsModalOpen(true);
  };

  const openEditModal = async (patient) => {
    setIsEditing(true);
    setCurrentPatientId(patient.id);
    setDisplayPatientId(patient.patientId);
    await fetchDepartments();
    setFormData({
      name: patient.name,
      age: patient.age.toString(),
      gender: patient.gender,
      disease: patient.disease,
      phone: patient.phone,
      address: patient.address,
      departmentId: patient.departmentId ? patient.departmentId.toString() : (departments.length > 0 ? departments[0].id.toString() : '')
    });
    setFormError('');
    setIsModalOpen(true);
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setFormError('');
  };

  const handleFormSubmit = async (e) => {
    e.preventDefault();
    setFormError('');

    // Basic frontend validations
    if (!formData.name.trim()) {
      setFormError('Patient name is required');
      return;
    }

    const ageNum = parseInt(formData.age, 10);
    if (isNaN(ageNum) || ageNum < 0 || ageNum > 120) {
      setFormError('Age must be between 0 and 120');
      return;
    }

    if (!['Male', 'Female', 'Other'].includes(formData.gender)) {
      setFormError('Please select a valid gender');
      return;
    }

    if (!formData.disease.trim()) {
      setFormError('Disease / Condition is required');
      return;
    }

    const phoneRegex = /^[6-9]\d{9}$/;
    if (!phoneRegex.test(formData.phone.trim())) {
      setFormError('Phone must be a valid 10-digit Indian mobile number (e.g. 9876543210)');
      return;
    }

    if (!formData.address.trim()) {
      setFormError('Address is required');
      return;
    }

    const payload = {
      name: formData.name.trim(),
      age: ageNum,
      gender: formData.gender,
      disease: formData.disease.trim(),
      phone: formData.phone.trim(),
      address: formData.address.trim(),
      departmentId: formData.departmentId ? parseInt(formData.departmentId, 10) : null
    };

    try {
      const url = isEditing
        ? `${API_BASE_URL}/api/patients/${currentPatientId}`
        : `${API_BASE_URL}/api/patients`;
      const method = isEditing ? 'PUT' : 'POST';

      const response = await fetch(url, {
        method: method,
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(payload)
      });

      if (response.ok) {
        closeModal();
        await fetchPatients();
        await fetchDepartments();
        showNotification(
          isEditing ? 'Patient updated successfully.' : 'Patient added successfully.'
        );
      } else {
        const errorData = await response.json().catch(() => ({}));
        setFormError(errorData.message || 'Operation failed. Please check form fields.');
      }
    } catch (err) {
      setFormError('Server connection error. Please try again.');
    }
  };

  const confirmDelete = (patient) => {
    setDeletePatientTarget(patient);
  };

  const handleDelete = async () => {
    if (!deletePatientTarget) return;

    try {
      const response = await fetch(`${API_BASE_URL}/api/patients/${deletePatientTarget.id}`, {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include'
      });

      if (response.ok) {
        setDeletePatientTarget(null);
        fetchPatients();
        showNotification('Patient deleted successfully.');
      } else {
        alert('Failed to delete patient. Please try again.');
      }
    } catch (err) {
      alert('Server connection error while deleting patient.');
    }
  };

  // Client-side Filtering
  const filteredPatients = patients.filter((p) => {
    const q = searchQuery.toLowerCase().trim();
    if (!q) return true;
    return (
      p.name.toLowerCase().includes(q) ||
      p.patientId.toLowerCase().includes(q) ||
      p.phone.toLowerCase().includes(q) ||
      (p.departmentName && p.departmentName.toLowerCase().includes(q))
    );
  });

  return (
    <div className="patients-page">
      {/* Page Header */}
      <div className="page-header">
        <div>
          <h2 className="page-title">Patients</h2>
          <p className="page-subtitle">Manage patient information and records</p>
        </div>
        <button className="primary-btn" onClick={openAddModal}>
          + Add Patient
        </button>
      </div>

      {/* Notification Toast */}
      {successMessage && (
        <div className="success-banner">
          {successMessage}
        </div>
      )}

      {/* Metrics & Filter Bar */}
      <div className="patients-toolbar">
        <div className="summary-pill">
          <span className="pill-label">Total Patients:</span>
          <span className="pill-count">{patients.length}</span>
        </div>

        <div className="search-box">
          <input
            type="text"
            className="search-input"
            placeholder="Search patients by name, ID, department or phone..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
      </div>

      {/* Patients Table View */}
      {error ? (
        <div className="dashboard-error-box">
          <p>{error}</p>
          <button className="retry-btn" onClick={fetchPatients}>Try again</button>
        </div>
      ) : loading ? (
        <div className="dashboard-loading">Loading patients...</div>
      ) : filteredPatients.length === 0 ? (
        <div className="empty-state-box">
          <div className="empty-icon">📁</div>
          <h3>{searchQuery ? 'No matching patients found' : 'No patients found'}</h3>
          <p>{searchQuery ? 'Try adjusting your search criteria.' : 'Add the first patient to get started.'}</p>
        </div>
      ) : (
        <div className="table-responsive">
          <table className="custom-table">
            <thead>
              <tr>
                <th>Patient ID</th>
                <th>Name</th>
                <th>Department</th>
                <th>Age</th>
                <th>Gender</th>
                <th>Disease / Condition</th>
                <th>Phone</th>
                <th>Address</th>
                <th style={{ textAlign: 'center' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredPatients.map((p) => (
                <tr key={p.id}>
                  <td><span className="patient-id-badge">{p.patientId}</span></td>
                  <td className="font-semibold">{p.name}</td>
                  <td>
                    <span className="dept-badge" style={{ padding: '0.2rem 0.6rem', borderRadius: '4px', fontSize: '0.8rem', backgroundColor: '#e0f2fe', color: '#0369a1', fontWeight: 500 }}>
                      {p.departmentName || 'Unassigned'}
                    </span>
                  </td>
                  <td>{p.age} yrs</td>
                  <td>{p.gender}</td>
                  <td><span className="disease-badge">{p.disease}</span></td>
                  <td>{p.phone}</td>
                  <td className="address-cell" title={p.address}>{p.address}</td>
                  <td style={{ textAlign: 'center' }}>
                    <div className="action-buttons">
                      <button className="action-btn edit-btn" onClick={() => openEditModal(p)}>
                        Edit
                      </button>
                      <button className="action-btn delete-btn" onClick={() => confirmDelete(p)}>
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Add / Edit Patient Modal Dialog */}
      {isModalOpen && (
        <div className="modal-overlay">
          <div className="modal-card">
            <div className="modal-header">
              <h3>{isEditing ? `Edit Patient (${displayPatientId})` : 'Add New Patient'}</h3>
              <button className="modal-close" onClick={closeModal}>&times;</button>
            </div>

            {formError && (
              <div className="error-banner">
                {formError}
              </div>
            )}

            <form onSubmit={handleFormSubmit}>
              <div className="form-group">
                <label className="form-label">Patient Name *</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. Aarav Sharma"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Department *</label>
                <select
                  className="form-input"
                  value={formData.departmentId}
                  onChange={(e) => setFormData({ ...formData, departmentId: e.target.value })}
                  required
                >
                  <option value="">Select Department</option>
                  {departments.map((dept) => (
                    <option key={dept.id} value={dept.id}>
                      {dept.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-row">
                <div className="form-group half">
                  <label className="form-label">Age *</label>
                  <input
                    type="number"
                    min="0"
                    max="120"
                    className="form-input"
                    placeholder="e.g. 35"
                    value={formData.age}
                    onChange={(e) => setFormData({ ...formData, age: e.target.value })}
                    required
                  />
                </div>

                <div className="form-group half">
                  <label className="form-label">Gender *</label>
                  <select
                    className="form-input"
                    value={formData.gender}
                    onChange={(e) => setFormData({ ...formData, gender: e.target.value })}
                    required
                  >
                    <option value="Male">Male</option>
                    <option value="Female">Female</option>
                    <option value="Other">Other</option>
                  </select>
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Disease / Condition *</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. Viral Fever, Hypertension"
                  value={formData.disease}
                  onChange={(e) => setFormData({ ...formData, disease: e.target.value })}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Phone Number * (10-digit Indian Mobile)</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. 9876543210"
                  value={formData.phone}
                  onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Address *</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. Mumbai, Maharashtra"
                  value={formData.address}
                  onChange={(e) => setFormData({ ...formData, address: e.target.value })}
                  required
                />
              </div>

              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={closeModal}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  {isEditing ? 'Update Patient' : 'Save Patient'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {deletePatientTarget && (
        <div className="modal-overlay">
          <div className="modal-card small-modal">
            <div className="modal-header">
              <h3>Confirm Deletion</h3>
              <button className="modal-close" onClick={() => setDeletePatientTarget(null)}>&times;</button>
            </div>
            <p className="delete-confirm-text">
              Are you sure you want to delete patient <strong>{deletePatientTarget.name}</strong> ({deletePatientTarget.patientId})?
            </p>
            <div className="modal-actions">
              <button className="btn-secondary" onClick={() => setDeletePatientTarget(null)}>
                Cancel
              </button>
              <button className="btn-danger" onClick={handleDelete}>
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Patients;
