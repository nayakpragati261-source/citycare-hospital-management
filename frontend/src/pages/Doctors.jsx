import React, { useState, useEffect } from 'react';

const API_BASE_URL = 'http://localhost:8080';

function Doctors({ user }) {
  const [doctors, setDoctors] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [currentDoctorId, setCurrentDoctorId] = useState(null);
  const [displayDoctorId, setDisplayDoctorId] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  // Form Fields
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    password: '',
    specialization: '',
    experience: '',
    phone: '',
    departmentId: '',
    availability: ''
  });
  const [formError, setFormError] = useState('');

  // Delete Confirmation State
  const [deleteDoctorTarget, setDeleteDoctorTarget] = useState(null);

  // Reset Password State
  const [resetTargetDoctor, setResetTargetDoctor] = useState(null);
  const [resetPasswordForm, setResetPasswordForm] = useState({
    newPassword: '',
    confirmPassword: ''
  });
  const [showResetPassword, setShowResetPassword] = useState(false);
  const [resetError, setResetError] = useState('');
  const [resetSubmitting, setResetSubmitting] = useState(false);

  const openResetPasswordModal = (doc) => {
    setResetTargetDoctor(doc);
    setResetPasswordForm({ newPassword: '', confirmPassword: '' });
    setShowResetPassword(false);
    setResetError('');
  };

  const closeResetPasswordModal = () => {
    setResetTargetDoctor(null);
    setResetPasswordForm({ newPassword: '', confirmPassword: '' });
    setResetError('');
  };

  const handleResetPasswordSubmit = async (e) => {
    e.preventDefault();
    setResetError('');

    if (!resetPasswordForm.newPassword) {
      setResetError('New password is required.');
      return;
    }

    if (resetPasswordForm.newPassword.length < 6) {
      setResetError('Password must be at least 6 characters long.');
      return;
    }

    if (resetPasswordForm.newPassword !== resetPasswordForm.confirmPassword) {
      setResetError('New password and confirm password do not match.');
      return;
    }

    setResetSubmitting(true);
    try {
      const response = await fetch(`${API_BASE_URL}/api/doctors/${resetTargetDoctor.id}/reset-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
          newPassword: resetPasswordForm.newPassword,
          confirmPassword: resetPasswordForm.confirmPassword
        })
      });

      if (response.ok) {
        closeResetPasswordModal();
        showNotification(`Password for ${resetTargetDoctor.name} has been reset successfully.`);
      } else {
        const errorData = await response.json().catch(() => ({}));
        setResetError(errorData.message || 'Failed to reset doctor password.');
      }
    } catch (err) {
      setResetError('Server connection error while resetting doctor password.');
    } finally {
      setResetSubmitting(false);
    }
  };

  const isAdmin = user?.role?.toUpperCase() === 'ADMIN';

  const fetchDoctors = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await fetch(`${API_BASE_URL}/api/doctors`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include'
      });

      if (response.ok) {
        const data = await response.json();
        setDoctors(data);
      } else {
        setError('Unable to load doctors. Please try again.');
      }
    } catch (err) {
      setError('Unable to load doctors. Please verify server connection.');
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
      console.error('Error fetching departments:', err);
    }
  };

  useEffect(() => {
    fetchDoctors();
    fetchDepartments();
  }, []);

  const showNotification = (msg) => {
    setSuccessMessage(msg);
    setTimeout(() => {
      setSuccessMessage('');
    }, 4000);
  };

  const openAddModal = () => {
    setIsEditing(false);
    setCurrentDoctorId(null);
    setDisplayDoctorId('');
    setShowPassword(false);
    fetchDepartments();
    setFormData({
      name: '',
      email: '',
      password: '',
      specialization: '',
      experience: '',
      phone: '',
      departmentId: departments.length > 0 ? departments[0].id.toString() : '',
      availability: ''
    });
    setFormError('');
    setIsModalOpen(true);
  };

  const openEditModal = (doctor) => {
    setIsEditing(true);
    setCurrentDoctorId(doctor.id);
    setDisplayDoctorId(doctor.doctorId);
    setShowPassword(false);
    setFormData({
      name: doctor.name,
      email: doctor.email,
      password: '',
      specialization: doctor.specialization,
      experience: doctor.experience.toString(),
      phone: doctor.phone,
      departmentId: doctor.departmentId ? doctor.departmentId.toString() : '',
      availability: doctor.availability
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

    if (!isEditing) {
      if (!formData.name.trim()) {
        setFormError('Doctor Name is required.');
        return;
      }
      if (!formData.email.trim() || !formData.email.includes('@')) {
        setFormError('A valid Email Address is required.');
        return;
      }
      if (!formData.password) {
        setFormError('Password is required.');
        return;
      }
    }

    if (!formData.specialization.trim()) {
      setFormError('Specialization is required.');
      return;
    }

    const expNum = parseInt(formData.experience, 10);
    if (isNaN(expNum) || expNum < 0 || expNum > 60) {
      setFormError('Experience must be between 0 and 60 years.');
      return;
    }

    const phoneRegex = /^[6-9]\d{9}$/;
    if (!phoneRegex.test(formData.phone.trim())) {
      setFormError('Phone must be a valid 10-digit Indian mobile number (e.g. 9876543210).');
      return;
    }

    if (!formData.availability.trim()) {
      setFormError('Availability schedule is required.');
      return;
    }

    try {
      if (!isEditing) {
        const payload = {
          name: formData.name.trim(),
          email: formData.email.trim(),
          password: formData.password,
          specialization: formData.specialization.trim(),
          experience: expNum,
          phone: formData.phone.trim(),
          departmentId: formData.departmentId ? Number(formData.departmentId) : null,
          availability: formData.availability.trim()
        };

        const response = await fetch(`${API_BASE_URL}/api/doctors/create-account`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
          body: JSON.stringify(payload)
        });

        if (response.ok) {
          closeModal();
          await fetchDoctors();
          await fetchDepartments();
          showNotification('Doctor account and profile created successfully.');
        } else {
          const errorData = await response.json().catch(() => ({}));
          setFormError(errorData.message || 'Operation failed. Please verify form inputs.');
        }
      } else {
        const payload = {
          userEmail: formData.email.trim(),
          specialization: formData.specialization.trim(),
          experience: expNum,
          phone: formData.phone.trim(),
          availability: formData.availability.trim()
        };

        const response = await fetch(`${API_BASE_URL}/api/doctors/${currentDoctorId}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
          body: JSON.stringify(payload)
        });

        if (response.ok) {
          closeModal();
          await fetchDoctors();
          await fetchDepartments();
          showNotification('Doctor profile updated successfully.');
        } else {
          const errorData = await response.json().catch(() => ({}));
          setFormError(errorData.message || 'Update failed. Please verify form inputs.');
        }
      }
    } catch (err) {
      setFormError('Server connection error. Please try again.');
    }
  };

  const confirmDelete = (doctor) => {
    setDeleteDoctorTarget(doctor);
  };

  const handleDelete = async () => {
    if (!deleteDoctorTarget) return;

    try {
      const response = await fetch(`${API_BASE_URL}/api/doctors/${deleteDoctorTarget.id}`, {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include'
      });

      if (response.ok) {
        setDeleteDoctorTarget(null);
        fetchDoctors();
        showNotification('Doctor profile deleted successfully. (Linked User account remains active)');
      } else {
        const errorData = await response.json().catch(() => ({}));
        alert(errorData.message || 'Failed to delete doctor profile.');
      }
    } catch (err) {
      alert('Server connection error while deleting doctor profile.');
    }
  };

  // Client-side Filtering
  const filteredDoctors = doctors.filter((doc) => {
    const q = searchQuery.toLowerCase().trim();
    if (!q) return true;
    return (
      doc.name.toLowerCase().includes(q) ||
      doc.doctorId.toLowerCase().includes(q) ||
      doc.specialization.toLowerCase().includes(q) ||
      doc.email.toLowerCase().includes(q)
    );
  });

  return (
    <div className="doctors-page">
      {/* Page Header */}
      <div className="page-header">
        <div>
          <h2 className="page-title">Doctors</h2>
          <p className="page-subtitle">Manage doctor profiles and availability</p>
        </div>
        {isAdmin && (
          <button className="primary-btn" onClick={openAddModal}>
            + Add Doctor
          </button>
        )}
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
          <span className="pill-label">Total Doctors:</span>
          <span className="pill-count">{doctors.length}</span>
        </div>

        <div className="search-box">
          <input
            type="text"
            className="search-input"
            placeholder="Search doctors by name, doctor ID, specialization or email..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
      </div>

      {/* Doctors Table View */}
      {error ? (
        <div className="dashboard-error-box">
          <p>{error}</p>
          <button className="retry-btn" onClick={fetchDoctors}>Try again</button>
        </div>
      ) : loading ? (
        <div className="dashboard-loading">Loading doctors...</div>
      ) : filteredDoctors.length === 0 ? (
        <div className="empty-state-box">
          <div className="empty-icon">👨‍⚕️</div>
          <h3>{searchQuery ? 'No matching doctors found' : 'No doctors found'}</h3>
          <p>{searchQuery ? 'Try adjusting your search query.' : 'Add a doctor profile to get started.'}</p>
        </div>
      ) : (
        <div className="table-responsive">
          <table className="custom-table">
            <thead>
              <tr>
                <th>Doctor ID</th>
                <th>Doctor Name</th>
                <th>Email</th>
                <th>Specialization</th>
                <th>Department</th>
                <th>Experience</th>
                <th>Phone</th>
                <th>Availability</th>
                {isAdmin && <th style={{ textAlign: 'center' }}>Actions</th>}
              </tr>
            </thead>
            <tbody>
              {filteredDoctors.map((doc) => (
                <tr key={doc.id}>
                  <td><span className="patient-id-badge">{doc.doctorId}</span></td>
                  <td className="font-semibold">{doc.name}</td>
                  <td>{doc.email}</td>
                  <td><span className="disease-badge" style={{ backgroundColor: '#e0e7ff', color: '#3730a3' }}>{doc.specialization}</span></td>
                  <td>{doc.departmentName || doc.specialization}</td>
                  <td>{doc.experience} yrs</td>
                  <td>{doc.phone}</td>
                  <td>{doc.availability}</td>
                  {isAdmin && (
                    <td style={{ textAlign: 'center' }}>
                      <div className="action-buttons">
                        <button className="action-btn edit-btn" onClick={() => openEditModal(doc)}>
                          Edit
                        </button>
                        <button
                          className="action-btn edit-btn"
                          style={{ backgroundColor: '#4f46e5', color: '#fff' }}
                          onClick={() => openResetPasswordModal(doc)}
                        >
                          Reset Password
                        </button>
                        <button className="action-btn delete-btn" onClick={() => confirmDelete(doc)}>
                          Delete
                        </button>
                      </div>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Add / Edit Doctor Modal Dialog (ADMIN ONLY) */}
      {isModalOpen && isAdmin && (
        <div className="modal-overlay">
          <div className="modal-card" style={{ maxWidth: '600px' }}>
            <div className="modal-header">
              <h3>{isEditing ? `Edit Doctor Profile (${displayDoctorId})` : 'Add New Doctor'}</h3>
              <button className="modal-close" onClick={closeModal}>&times;</button>
            </div>

            {formError && (
              <div className="error-banner">
                {formError}
              </div>
            )}

            <form onSubmit={handleFormSubmit}>
              {!isEditing ? (
                <>
                  <div className="form-group">
                    <label className="form-label">Doctor Name *</label>
                    <input
                      type="text"
                      className="form-input"
                      placeholder="Enter doctor name"
                      value={formData.name}
                      onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                      required
                    />
                  </div>

                  <div className="form-row">
                    <div className="form-group half">
                      <label className="form-label">Email Address *</label>
                      <input
                        type="email"
                        className="form-input"
                        placeholder="doctor@citycare.com"
                        value={formData.email}
                        onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                        required
                      />
                    </div>

                    <div className="form-group half">
                      <label className="form-label">Password *</label>
                      <div className="input-wrapper" style={{ position: 'relative' }}>
                        <input
                          type={showPassword ? 'text' : 'password'}
                          className="form-input"
                          placeholder="Create a password"
                          value={formData.password}
                          onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                          required
                        />
                        <button
                          type="button"
                          className="toggle-password-btn"
                          onClick={() => setShowPassword(!showPassword)}
                          style={{
                            position: 'absolute',
                            right: '10px',
                            top: '50%',
                            transform: 'translateY(-50%)',
                            background: 'none',
                            border: 'none',
                            color: '#64748b',
                            cursor: 'pointer',
                            fontSize: '0.8rem',
                            fontWeight: '600'
                          }}
                        >
                          {showPassword ? 'Hide' : 'Show'}
                        </button>
                      </div>
                    </div>
                  </div>
                </>
              ) : (
                <div className="form-row">
                  <div className="form-group half">
                    <label className="form-label">Doctor Name</label>
                    <input
                      type="text"
                      className="form-input"
                      value={formData.name}
                      disabled
                      style={{ backgroundColor: '#f1f5f9', cursor: 'not-allowed' }}
                    />
                  </div>
                  <div className="form-group half">
                    <label className="form-label">Email Address</label>
                    <input
                      type="text"
                      className="form-input"
                      value={formData.email}
                      disabled
                      style={{ backgroundColor: '#f1f5f9', cursor: 'not-allowed' }}
                    />
                  </div>
                </div>
              )}

              <div className="form-row">
                <div className="form-group half">
                  <label className="form-label">Department *</label>
                  <select
                    className="form-input"
                    value={formData.departmentId}
                    onChange={(e) => {
                      const selectedDeptId = e.target.value;
                      const selectedDept = departments.find(d => d.id.toString() === selectedDeptId);
                      setFormData({
                        ...formData,
                        departmentId: selectedDeptId,
                        specialization: selectedDept ? selectedDept.name : formData.specialization
                      });
                    }}
                    required
                  >
                    <option value="">-- Select Department --</option>
                    {departments.map((d) => (
                      <option key={d.id} value={d.id}>
                        {d.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="form-group half">
                  <label className="form-label">Specialization *</label>
                  <input
                    type="text"
                    className="form-input"
                    placeholder="e.g. Cardiology"
                    value={formData.specialization}
                    onChange={(e) => setFormData({ ...formData, specialization: e.target.value })}
                    required
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group half">
                  <label className="form-label">Experience (Years) *</label>
                  <input
                    type="number"
                    min="0"
                    max="60"
                    className="form-input"
                    placeholder="e.g. 8"
                    value={formData.experience}
                    onChange={(e) => setFormData({ ...formData, experience: e.target.value })}
                    required
                  />
                </div>

                <div className="form-group half">
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
              </div>

              <div className="form-group">
                <label className="form-label">Availability Schedule *</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. Mon-Fri, 9:00 AM - 2:00 PM"
                  value={formData.availability}
                  onChange={(e) => setFormData({ ...formData, availability: e.target.value })}
                  required
                />
              </div>

              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={closeModal}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  {isEditing ? 'Update Doctor' : 'Save Doctor'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal (ADMIN ONLY) */}
      {deleteDoctorTarget && isAdmin && (
        <div className="modal-overlay">
          <div className="modal-card small-modal">
            <div className="modal-header">
              <h3>Confirm Deletion</h3>
              <button className="modal-close" onClick={() => setDeleteDoctorTarget(null)}>&times;</button>
            </div>
            <p className="delete-confirm-text">
              Are you sure you want to delete doctor profile <strong>{deleteDoctorTarget.name}</strong> ({deleteDoctorTarget.doctorId})?
            </p>
            <p style={{ fontSize: '0.8rem', color: '#64748b', marginBottom: '1.25rem' }}>
              Note: The linked user account ({deleteDoctorTarget.email}) will remain active in the system.
            </p>
            <div className="modal-actions">
              <button className="btn-secondary" onClick={() => setDeleteDoctorTarget(null)}>
                Cancel
              </button>
              <button className="btn-danger" onClick={handleDelete}>
                Delete Profile
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Reset Password Modal (ADMIN ONLY) */}
      {resetTargetDoctor && isAdmin && (
        <div className="modal-overlay">
          <div className="modal-card" style={{ maxWidth: '450px' }}>
            <div className="modal-header">
              <h3>Reset Doctor Password</h3>
              <button className="modal-close" onClick={closeResetPasswordModal}>&times;</button>
            </div>

            {resetError && (
              <div className="error-banner">
                {resetError}
              </div>
            )}

            <form onSubmit={handleResetPasswordSubmit}>
              <div className="form-group">
                <label className="form-label">Doctor</label>
                <div style={{ fontWeight: '600', color: '#1e293b' }}>
                  {resetTargetDoctor.name} ({resetTargetDoctor.email})
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">New Password *</label>
                <div className="input-wrapper">
                  <input
                    type={showResetPassword ? 'text' : 'password'}
                    className="form-input"
                    placeholder="Enter new password (min 6 chars)"
                    value={resetPasswordForm.newPassword}
                    onChange={(e) => setResetPasswordForm({ ...resetPasswordForm, newPassword: e.target.value })}
                    required
                  />
                  <button
                    type="button"
                    className="toggle-password-btn"
                    onClick={() => setShowResetPassword(!showResetPassword)}
                  >
                    {showResetPassword ? 'Hide' : 'Show'}
                  </button>
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Confirm New Password *</label>
                <div className="input-wrapper">
                  <input
                    type={showResetPassword ? 'text' : 'password'}
                    className="form-input"
                    placeholder="Confirm new password"
                    value={resetPasswordForm.confirmPassword}
                    onChange={(e) => setResetPasswordForm({ ...resetPasswordForm, confirmPassword: e.target.value })}
                    required
                  />
                </div>
              </div>

              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={closeResetPasswordModal} disabled={resetSubmitting}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary" disabled={resetSubmitting}>
                  {resetSubmitting ? 'Resetting...' : 'Reset Password'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default Doctors;
