import React, { useState, useEffect } from 'react';

const API_BASE_URL = 'http://localhost:8080';

function Departments({ user }) {
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [currentDeptId, setCurrentDeptId] = useState(null);
  const [displayDeptId, setDisplayDeptId] = useState('');

  // Form Fields
  const [formData, setFormData] = useState({
    name: '',
    description: ''
  });
  const [formError, setFormError] = useState('');

  // Delete Confirmation State
  const [deleteDeptTarget, setDeleteDeptTarget] = useState(null);
  const [deleteError, setDeleteError] = useState('');

  const isAdmin = user?.role?.toUpperCase() === 'ADMIN';

  const fetchDepartments = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await fetch(`${API_BASE_URL}/api/departments`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include'
      });

      if (response.ok) {
        const data = await response.json();
        setDepartments(data);
      } else {
        setError('Unable to load departments. Please try again.');
      }
    } catch (err) {
      setError('Unable to load departments. Please verify server connection.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
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
    setCurrentDeptId(null);
    setDisplayDeptId('');
    setFormData({ name: '', description: '' });
    setFormError('');
    setIsModalOpen(true);
  };

  const openEditModal = (dept) => {
    setIsEditing(true);
    setCurrentDeptId(dept.id);
    setDisplayDeptId(dept.departmentId);
    setFormData({
      name: dept.name,
      description: dept.description || ''
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

    if (!formData.name.trim()) {
      setFormError('Department name is required');
      return;
    }

    const payload = {
      name: formData.name.trim(),
      description: formData.description.trim()
    };

    try {
      const url = isEditing
        ? `${API_BASE_URL}/api/departments/${currentDeptId}`
        : `${API_BASE_URL}/api/departments`;
      const method = isEditing ? 'PUT' : 'POST';

      const response = await fetch(url, {
        method: method,
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(payload)
      });

      if (response.ok) {
        closeModal();
        fetchDepartments();
        showNotification(
          isEditing ? 'Department updated successfully.' : 'Department created successfully.'
        );
      } else {
        const errorData = await response.json().catch(() => ({}));
        setFormError(errorData.message || 'Operation failed. A department with this name may already exist.');
      }
    } catch (err) {
      setFormError('Server connection error. Please try again.');
    }
  };

  const confirmDelete = (dept) => {
    setDeleteDeptTarget(dept);
    setDeleteError('');
  };

  const handleDelete = async () => {
    if (!deleteDeptTarget) return;

    setDeleteError('');
    try {
      const response = await fetch(`${API_BASE_URL}/api/departments/${deleteDeptTarget.id}`, {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include'
      });

      if (response.ok) {
        setDeleteDeptTarget(null);
        fetchDepartments();
        showNotification('Department deleted successfully.');
      } else {
        const errorData = await response.json().catch(() => ({}));
        setDeleteError(errorData.message || 'This department cannot be deleted because it is currently assigned to doctors or patients.');
      }
    } catch (err) {
      setDeleteError('Server connection error while deleting department.');
    }
  };

  // Client-side Filtering
  const filteredDepartments = departments.filter((dept) => {
    const q = searchQuery.toLowerCase().trim();
    if (!q) return true;
    return (
      dept.name.toLowerCase().includes(q) ||
      dept.departmentId.toLowerCase().includes(q) ||
      (dept.description && dept.description.toLowerCase().includes(q))
    );
  });

  return (
    <div className="departments-page">
      {/* Page Header */}
      <div className="page-header">
        <div>
          <h2 className="page-title">Department Management</h2>
          <p className="page-subtitle">Manage hospital departments and their availability.</p>
        </div>
        {isAdmin && (
          <button className="primary-btn" onClick={openAddModal}>
            + Add Department
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
          <span className="pill-label">Total Departments:</span>
          <span className="pill-count">{departments.length}</span>
        </div>

        <div className="search-box">
          <input
            type="text"
            className="search-input"
            placeholder="Search departments by name or ID..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
      </div>

      {/* Departments Table View */}
      {error ? (
        <div className="dashboard-error-box">
          <p>{error}</p>
          <button className="retry-btn" onClick={fetchDepartments}>Try again</button>
        </div>
      ) : loading ? (
        <div className="dashboard-loading">Loading departments...</div>
      ) : filteredDepartments.length === 0 ? (
        <div className="empty-state-box">
          <div className="empty-icon">🏥</div>
          <h3>{searchQuery ? 'No matching departments found' : 'No departments found'}</h3>
          <p>{searchQuery ? 'Try adjusting your search criteria.' : 'Add a department to get started.'}</p>
        </div>
      ) : (
        <div className="table-responsive">
          <table className="custom-table">
            <thead>
              <tr>
                <th>Department ID</th>
                <th>Department Name</th>
                <th>Description</th>
                <th style={{ textAlign: 'center' }}>Doctors</th>
                <th style={{ textAlign: 'center' }}>Patients</th>
                {isAdmin && <th style={{ textAlign: 'center' }}>Actions</th>}
              </tr>
            </thead>
            <tbody>
              {filteredDepartments.map((dept) => (
                <tr key={dept.id}>
                  <td><span className="patient-id-badge">{dept.departmentId}</span></td>
                  <td className="font-semibold">{dept.name}</td>
                  <td className="address-cell" title={dept.description}>{dept.description || 'N/A'}</td>
                  <td style={{ textAlign: 'center' }}>
                    <span className="user-badge">{dept.doctorCount || 0}</span>
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    <span className="user-badge" style={{ backgroundColor: '#fef3c7', color: '#92400e' }}>
                      {dept.patientCount || 0}
                    </span>
                  </td>
                  {isAdmin && (
                    <td style={{ textAlign: 'center' }}>
                      <div className="action-buttons">
                        <button className="action-btn edit-btn" onClick={() => openEditModal(dept)}>
                          Edit
                        </button>
                        <button className="action-btn delete-btn" onClick={() => confirmDelete(dept)}>
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

      {/* Add / Edit Department Modal Dialog (ADMIN ONLY) */}
      {isModalOpen && isAdmin && (
        <div className="modal-overlay">
          <div className="modal-card">
            <div className="modal-header">
              <h3>{isEditing ? `Edit Department (${displayDeptId})` : 'Add New Department'}</h3>
              <button className="modal-close" onClick={closeModal}>&times;</button>
            </div>

            {formError && (
              <div className="error-banner">
                {formError}
              </div>
            )}

            <form onSubmit={handleFormSubmit}>
              <div className="form-group">
                <label className="form-label">Department Name *</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. Neurology, Cardiology"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Description</label>
                <textarea
                  className="form-input"
                  rows="3"
                  placeholder="Enter department scope and description..."
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                />
              </div>

              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={closeModal}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  {isEditing ? 'Update Department' : 'Save Department'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal (ADMIN ONLY) */}
      {deleteDeptTarget && isAdmin && (
        <div className="modal-overlay">
          <div className="modal-card small-modal">
            <div className="modal-header">
              <h3>Confirm Deletion</h3>
              <button className="modal-close" onClick={() => setDeleteDeptTarget(null)}>&times;</button>
            </div>

            {deleteError && (
              <div className="error-banner">
                {deleteError}
              </div>
            )}

            <p className="delete-confirm-text">
              Are you sure you want to delete department <strong>{deleteDeptTarget.name}</strong> ({deleteDeptTarget.departmentId})?
            </p>
            <div className="modal-actions">
              <button className="btn-secondary" onClick={() => setDeleteDeptTarget(null)}>
                Cancel
              </button>
              <button className="btn-danger" onClick={handleDelete}>
                Delete Department
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Departments;
