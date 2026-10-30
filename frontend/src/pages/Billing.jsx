import React, { useState, useEffect } from 'react';

const API_BASE_URL = 'http://localhost:8080';

function Billing({ user }) {
  const [bills, setBills] = useState([]);
  const [patients, setPatients] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Filters
  const [statusFilter, setStatusFilter] = useState('');
  const [patientFilter, setPatientFilter] = useState('');
  const [doctorFilter, setDoctorFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  // Modals
  const [showModal, setShowModal] = useState(false);
  const [editingBill, setEditingBill] = useState(null);
  const [viewingBill, setViewingBill] = useState(null);

  // Form state
  const [formData, setFormData] = useState({
    patientId: '',
    doctorId: '',
    consultationFee: '0.00',
    testCharges: '0.00',
    medicineCharges: '0.00',
    paymentStatus: 'UNPAID',
    paymentMethod: 'Cash',
    billDate: new Date().toISOString().split('T')[0],
    notes: ''
  });
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const isAdmin = user?.role?.toUpperCase() === 'ADMIN';

  const fetchData = async () => {
    setLoading(true);
    setError('');
    try {
      let queryParams = [];
      if (statusFilter) queryParams.push(`status=${statusFilter}`);
      if (patientFilter) queryParams.push(`patientId=${patientFilter}`);
      if (doctorFilter) queryParams.push(`doctorId=${doctorFilter}`);
      const queryString = queryParams.length > 0 ? `?${queryParams.join('&')}` : '';

      const [billsRes, patientsRes, doctorsRes] = await Promise.all([
        fetch(`${API_BASE_URL}/api/bills${queryString}`, { credentials: 'include' }),
        fetch(`${API_BASE_URL}/api/patients`, { credentials: 'include' }),
        fetch(`${API_BASE_URL}/api/doctors`, { credentials: 'include' })
      ]);

      if (billsRes.ok) {
        const billsData = await billsRes.json();
        setBills(billsData);
      } else {
        setError('Failed to fetch billing records.');
      }

      if (patientsRes.ok) setPatients(await patientsRes.json());
      if (doctorsRes.ok) setDoctors(await doctorsRes.json());
    } catch (err) {
      setError('Unable to load billing data. Please verify backend connection.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [statusFilter, patientFilter, doctorFilter]);

  const handleOpenAddModal = () => {
    setEditingBill(null);
    setFormData({
      patientId: patients.length > 0 ? patients[0].id : '',
      doctorId: doctors.length > 0 ? doctors[0].id : '',
      consultationFee: '500.00',
      testCharges: '0.00',
      medicineCharges: '0.00',
      paymentStatus: 'UNPAID',
      paymentMethod: 'Cash',
      billDate: new Date().toISOString().split('T')[0],
      notes: ''
    });
    setFormError('');
    setShowModal(true);
  };

  const handleOpenEditModal = (bill) => {
    setEditingBill(bill);
    setFormData({
      patientId: bill.patientId,
      doctorId: bill.doctorId,
      consultationFee: bill.consultationFee ? bill.consultationFee.toString() : '0.00',
      testCharges: bill.testCharges ? bill.testCharges.toString() : '0.00',
      medicineCharges: bill.medicineCharges ? bill.medicineCharges.toString() : '0.00',
      paymentStatus: bill.paymentStatus || 'UNPAID',
      paymentMethod: bill.paymentMethod || 'Cash',
      billDate: bill.billDate || new Date().toISOString().split('T')[0],
      notes: bill.notes || ''
    });
    setFormError('');
    setShowModal(true);
  };

  const handleFormSubmit = async (e) => {
    e.preventDefault();
    setFormError('');
    setSubmitting(true);

    if (!formData.patientId) {
      setFormError('Patient is required.');
      setSubmitting(false);
      return;
    }
    if (!formData.doctorId) {
      setFormError('Doctor is required.');
      setSubmitting(false);
      return;
    }

    const cFee = parseFloat(formData.consultationFee || 0);
    const tFee = parseFloat(formData.testCharges || 0);
    const mFee = parseFloat(formData.medicineCharges || 0);

    if (isNaN(cFee) || cFee < 0 || isNaN(tFee) || tFee < 0 || isNaN(mFee) || mFee < 0) {
      setFormError('Fees and charges cannot be negative numbers.');
      setSubmitting(false);
      return;
    }

    const payload = {
      patientId: Number(formData.patientId),
      doctorId: Number(formData.doctorId),
      consultationFee: cFee,
      testCharges: tFee,
      medicineCharges: mFee,
      paymentStatus: formData.paymentStatus,
      paymentMethod: formData.paymentMethod,
      billDate: formData.billDate,
      notes: formData.notes
    };

    try {
      const url = editingBill
        ? `${API_BASE_URL}/api/bills/${editingBill.id}`
        : `${API_BASE_URL}/api/bills`;
      const method = editingBill ? 'PUT' : 'POST';

      const response = await fetch(url, {
        method: method,
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(payload)
      });

      if (response.ok) {
        setShowModal(false);
        setSuccessMessage(editingBill ? 'Bill updated successfully!' : 'Bill created successfully!');
        setTimeout(() => setSuccessMessage(''), 4000);
        fetchData();
      } else {
        const errorData = await response.json().catch(() => ({}));
        setFormError(errorData.message || 'Operation failed.');
      }
    } catch (err) {
      setFormError('Server connection error.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteBill = async (id) => {
    if (!window.confirm('Are you sure you want to delete this bill record?')) return;
    try {
      const response = await fetch(`${API_BASE_URL}/api/bills/${id}`, {
        method: 'DELETE',
        credentials: 'include'
      });
      if (response.ok) {
        setSuccessMessage('Bill deleted successfully!');
        setTimeout(() => setSuccessMessage(''), 4000);
        fetchData();
      } else {
        setError('Failed to delete bill.');
      }
    } catch (err) {
      setError('Server error during bill deletion.');
    }
  };

  // Calculations for summary cards
  const totalRevenue = bills
    .filter(b => b.paymentStatus === 'PAID')
    .reduce((sum, b) => sum + (parseFloat(b.totalAmount) || 0), 0);
  const totalBillsCount = bills.length;
  const paidBillsCount = bills.filter(b => b.paymentStatus === 'PAID').length;
  const unpaidBillsCount = bills.filter(b => b.paymentStatus === 'UNPAID').length;

  // Filtered bills by search query
  const filteredBills = bills.filter(bill => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return (
      (bill.billId && bill.billId.toLowerCase().includes(q)) ||
      (bill.patientName && bill.patientName.toLowerCase().includes(q)) ||
      (bill.doctorName && bill.doctorName.toLowerCase().includes(q))
    );
  });

  const liveTotal = (
    (parseFloat(formData.consultationFee) || 0) +
    (parseFloat(formData.testCharges) || 0) +
    (parseFloat(formData.medicineCharges) || 0)
  ).toFixed(2);

  return (
    <div className="module-page">
      <div className="module-header">
        <div>
          <h2>Billing Management</h2>
          <p className="module-subtitle">Manage patient bills, invoices, and payment status tracking</p>
        </div>
        {isAdmin && (
          <button className="primary-btn" onClick={handleOpenAddModal}>
            + Create New Bill
          </button>
        )}
      </div>

      {successMessage && <div className="success-banner">{successMessage}</div>}
      {error && <div className="error-banner">{error}</div>}

      {/* Metrics Summary Cards */}
      <div className="metrics-grid" style={{ marginBottom: '1.5rem' }}>
        <div className="metric-card blue">
          <div className="metric-icon">💵</div>
          <div className="metric-info">
            <span className="metric-title">Total Revenue Collected</span>
            <span className="metric-value">₹{totalRevenue.toFixed(2)}</span>
          </div>
        </div>
        <div className="metric-card indigo">
          <div className="metric-icon">📄</div>
          <div className="metric-info">
            <span className="metric-title">Total Bills</span>
            <span className="metric-value">{totalBillsCount}</span>
          </div>
        </div>
        <div className="metric-card green">
          <div className="metric-icon">✅</div>
          <div className="metric-info">
            <span className="metric-title">Paid Bills</span>
            <span className="metric-value">{paidBillsCount}</span>
          </div>
        </div>
        <div className="metric-card amber">
          <div className="metric-icon">⏳</div>
          <div className="metric-info">
            <span className="metric-title">Pending Unpaid Bills</span>
            <span className="metric-value">{unpaidBillsCount}</span>
          </div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="filter-bar">
        <div className="search-box">
          <input
            type="text"
            placeholder="Search by Bill ID, Patient or Doctor..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="search-input"
          />
        </div>

        <div className="filter-group">
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="filter-select"
          >
            <option value="">All Statuses</option>
            <option value="UNPAID">Unpaid</option>
            <option value="PAID">Paid</option>
            <option value="CANCELLED">Cancelled</option>
          </select>

          <select
            value={patientFilter}
            onChange={(e) => setPatientFilter(e.target.value)}
            className="filter-select"
          >
            <option value="">All Patients</option>
            {patients.map(p => (
              <option key={p.id} value={p.id}>{p.name} ({p.patientId})</option>
            ))}
          </select>
        </div>
      </div>

      {/* Bills Table */}
      {loading ? (
        <div className="dashboard-loading">Loading bills...</div>
      ) : filteredBills.length === 0 ? (
        <div className="empty-state">No billing records found.</div>
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Bill ID</th>
                <th>Patient</th>
                <th>Doctor</th>
                <th>Date</th>
                <th>Consultation</th>
                <th>Test Charges</th>
                <th>Medicine</th>
                <th>Total Amount</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredBills.map((bill) => (
                <tr key={bill.id}>
                  <td><strong>{bill.billId}</strong></td>
                  <td>{bill.patientName}</td>
                  <td>{bill.doctorName}</td>
                  <td>{bill.billDate}</td>
                  <td>₹{parseFloat(bill.consultationFee || 0).toFixed(2)}</td>
                  <td>₹{parseFloat(bill.testCharges || 0).toFixed(2)}</td>
                  <td>₹{parseFloat(bill.medicineCharges || 0).toFixed(2)}</td>
                  <td><strong>₹{parseFloat(bill.totalAmount || 0).toFixed(2)}</strong></td>
                  <td>
                    <span className={`status-badge ${bill.paymentStatus ? bill.paymentStatus.toLowerCase() : ''}`}>
                      {bill.paymentStatus}
                    </span>
                  </td>
                  <td>
                    <div className="action-buttons">
                      <button
                        className="action-btn view-btn"
                        title="View Invoice"
                        onClick={() => setViewingBill(bill)}
                      >
                        👁️ View
                      </button>
                      {isAdmin && (
                        <>
                          <button
                            className="action-btn edit-btn"
                            title="Edit Bill"
                            onClick={() => handleOpenEditModal(bill)}
                          >
                            ✏️ Edit
                          </button>
                          <button
                            className="action-btn delete-btn"
                            title="Delete Bill"
                            onClick={() => handleDeleteBill(bill.id)}
                          >
                            🗑️ Delete
                          </button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* View Invoice Modal */}
      {viewingBill && (
        <div className="modal-overlay">
          <div className="modal-card" style={{ maxWidth: '550px' }}>
            <div className="modal-header">
              <h3>Invoice Details — {viewingBill.billId}</h3>
              <button className="modal-close" onClick={() => setViewingBill(null)}>&times;</button>
            </div>
            <div className="modal-body invoice-body" style={{ lineHeight: '1.6' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #e2e8f0', paddingBottom: '0.8rem', marginBottom: '1rem' }}>
                <div>
                  <h4 style={{ margin: 0, color: '#0f172a' }}>CityCare Hospital</h4>
                  <p style={{ margin: 0, fontSize: '0.85rem', color: '#64748b' }}>Hospital Invoice Statement</p>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <span className={`status-badge ${viewingBill.paymentStatus.toLowerCase()}`}>
                    {viewingBill.paymentStatus}
                  </span>
                  <p style={{ margin: '4px 0 0 0', fontSize: '0.85rem', color: '#64748b' }}>Date: {viewingBill.billDate}</p>
                </div>
              </div>

              <div style={{ marginBottom: '1rem' }}>
                <p><strong>Patient:</strong> {viewingBill.patientName}</p>
                <p><strong>Attending Doctor:</strong> {viewingBill.doctorName}</p>
                <p><strong>Payment Method:</strong> {viewingBill.paymentMethod || 'N/A'}</p>
                {viewingBill.notes && <p><strong>Notes:</strong> {viewingBill.notes}</p>}
              </div>

              <table className="data-table" style={{ marginTop: '1rem' }}>
                <thead>
                  <tr>
                    <th>Description</th>
                    <th style={{ textAlign: 'right' }}>Amount</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>Consultation Fee</td>
                    <td style={{ textAlign: 'right' }}>₹{parseFloat(viewingBill.consultationFee || 0).toFixed(2)}</td>
                  </tr>
                  <tr>
                    <td>Medical Test Charges</td>
                    <td style={{ textAlign: 'right' }}>₹{parseFloat(viewingBill.testCharges || 0).toFixed(2)}</td>
                  </tr>
                  <tr>
                    <td>Medicine Charges</td>
                    <td style={{ textAlign: 'right' }}>₹{parseFloat(viewingBill.medicineCharges || 0).toFixed(2)}</td>
                  </tr>
                  <tr style={{ background: '#f8fafc', fontWeight: 'bold' }}>
                    <td>Total Amount Payable</td>
                    <td style={{ textAlign: 'right', fontSize: '1.1rem', color: '#0f172a' }}>
                      ₹{parseFloat(viewingBill.totalAmount || 0).toFixed(2)}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div className="modal-actions">
              <button className="btn-secondary" onClick={() => setViewingBill(null)}>Close</button>
              <button className="btn-primary" onClick={() => window.print()}>🖨️ Print Invoice</button>
            </div>
          </div>
        </div>
      )}

      {/* Add / Edit Bill Modal */}
      {showModal && (
        <div className="modal-overlay">
          <div className="modal-card billing-modal">
            <div className="modal-header">
              <h3>{editingBill ? 'Edit Bill Record' : 'Create New Bill'}</h3>
              <button className="modal-close" onClick={() => setShowModal(false)}>&times;</button>
            </div>
            {formError && <div className="error-banner">{formError}</div>}
            <form onSubmit={handleFormSubmit}>
              <div className="modal-body">
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
                      <option key={p.id} value={p.id}>{p.name} ({p.patientId})</option>
                    ))}
                  </select>
                </div>

                <div className="form-group">
                  <label className="form-label">Doctor *</label>
                  <select
                    className="form-input"
                    value={formData.doctorId}
                    onChange={(e) => setFormData({ ...formData, doctorId: e.target.value })}
                    required
                  >
                    <option value="">-- Select Doctor --</option>
                    {doctors.map(d => (
                      <option key={d.id} value={d.id}>{d.name} ({d.specialization})</option>
                    ))}
                  </select>
                </div>

                <div className="form-row-three">
                  <div className="form-group">
                    <label className="form-label">Consultation Fee (₹)</label>
                    <input
                      type="number"
                      step="0.01"
                      min="0"
                      className="form-input"
                      value={formData.consultationFee}
                      onChange={(e) => setFormData({ ...formData, consultationFee: e.target.value })}
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">Test Charges (₹)</label>
                    <input
                      type="number"
                      step="0.01"
                      min="0"
                      className="form-input"
                      value={formData.testCharges}
                      onChange={(e) => setFormData({ ...formData, testCharges: e.target.value })}
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">Medicine Charges (₹)</label>
                    <input
                      type="number"
                      step="0.01"
                      min="0"
                      className="form-input"
                      value={formData.medicineCharges}
                      onChange={(e) => setFormData({ ...formData, medicineCharges: e.target.value })}
                    />
                  </div>
                </div>

                {/* Total Preview */}
                <div className="total-preview-card">
                  <span className="total-preview-label">Calculated Total Amount:</span>
                  <span className="total-preview-amount">₹{liveTotal}</span>
                </div>

                <div className="form-row-three">
                  <div className="form-group">
                    <label className="form-label">Payment Status *</label>
                    <select
                      className="form-input"
                      value={formData.paymentStatus}
                      onChange={(e) => setFormData({ ...formData, paymentStatus: e.target.value })}
                      required
                    >
                      <option value="UNPAID">UNPAID</option>
                      <option value="PAID">PAID</option>
                      <option value="CANCELLED">CANCELLED</option>
                    </select>
                  </div>

                  <div className="form-group">
                    <label className="form-label">Payment Method</label>
                    <select
                      className="form-input"
                      value={formData.paymentMethod}
                      onChange={(e) => setFormData({ ...formData, paymentMethod: e.target.value })}
                    >
                      <option value="Cash">Cash</option>
                      <option value="Credit Card">Credit Card</option>
                      <option value="Debit Card">Debit Card</option>
                      <option value="UPI">UPI</option>
                      <option value="Insurance">Insurance</option>
                      <option value="Bank Transfer">Bank Transfer</option>
                    </select>
                  </div>

                  <div className="form-group">
                    <label className="form-label">Bill Date</label>
                    <input
                      type="date"
                      className="form-input"
                      value={formData.billDate}
                      onChange={(e) => setFormData({ ...formData, billDate: e.target.value })}
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label">Notes / Remarks</label>
                  <textarea
                    className="form-input"
                    rows="2"
                    placeholder="Additional payment or billing notes..."
                    value={formData.notes}
                    onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                  />
                </div>
              </div>

              <div className="modal-actions">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => setShowModal(false)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn-primary"
                  disabled={submitting}
                >
                  {submitting ? 'Saving...' : editingBill ? 'Update Bill' : 'Create Bill'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default Billing;
