import React, { useState, useEffect } from 'react';
import DashboardCard from '../components/DashboardCard';

const API_BASE_URL = 'http://localhost:8080';

function Dashboard({ user }) {
  const [metrics, setMetrics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchDashboardData = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await fetch(`${API_BASE_URL}/api/dashboard`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include'
      });

      if (response.ok) {
        const data = await response.json();
        setMetrics(data);
      } else {
        setError('Unable to load dashboard data. Please try again.');
      }
    } catch (err) {
      setError('Unable to load dashboard data. Please verify your connection.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const isAdmin = user?.role?.toUpperCase() === 'ADMIN';

  return (
    <div className="dashboard-page">
      <div className="dashboard-welcome">
        <h2>Welcome, {user?.name}</h2>
        <p className="welcome-subtext">
          {isAdmin ? 'System Administrator Portal' : 'Doctor Medical Portal'} — CityCare Hospital
        </p>
      </div>

      {error ? (
        <div className="dashboard-error-box">
          <p>{error}</p>
          <button className="retry-btn" onClick={fetchDashboardData}>Try again</button>
        </div>
      ) : loading ? (
        <div className="dashboard-loading">Loading dashboard metrics...</div>
      ) : (
        <div className="metrics-grid">
          {isAdmin ? (
            <>
              <DashboardCard
                title="Total Revenue Collected"
                count={metrics?.totalRevenue !== undefined ? `₹${parseFloat(metrics.totalRevenue || 0).toFixed(2)}` : '₹0.00'}
                icon="💵"
                colorClass="green"
              />
              <DashboardCard
                title="Total Patients"
                count={metrics?.totalPatients}
                icon="👥"
                colorClass="blue"
              />
              <DashboardCard
                title="Total Doctors"
                count={metrics?.totalDoctors}
                icon="👨‍⚕️"
                colorClass="teal"
              />
              <DashboardCard
                title="Total Departments"
                count={metrics?.totalDepartments}
                icon="🏥"
                colorClass="amber"
              />
              <DashboardCard
                title="Total Appointments"
                count={metrics?.totalAppointments}
                icon="📅"
                colorClass="indigo"
              />
              <DashboardCard
                title="Pending Appointments"
                count={metrics?.pendingAppointments}
                icon="⏳"
                colorClass="amber"
              />
              <DashboardCard
                title="Total Bills"
                count={metrics?.totalBills}
                icon="💳"
                colorClass="indigo"
              />
              <DashboardCard
                title="Paid Bills"
                count={metrics?.paidBills}
                icon="✅"
                colorClass="green"
              />
              <DashboardCard
                title="Pending Bills"
                count={metrics?.pendingBills}
                icon="⏳"
                colorClass="red"
              />
              <DashboardCard
                title="Total Prescriptions"
                count={metrics?.totalPrescriptions}
                icon="💊"
                colorClass="teal"
              />
              <DashboardCard
                title="Medical Records"
                count={metrics?.totalMedicalRecords}
                icon="📁"
                colorClass="blue"
              />
              <DashboardCard
                title="Medical Tests"
                count={metrics?.totalMedicalTests}
                icon="🧪"
                colorClass="amber"
              />
            </>
          ) : (
            <>
              <DashboardCard
                title="My Patients"
                count={metrics?.totalPatients}
                icon="👥"
                colorClass="blue"
              />
              <DashboardCard
                title="Total Departments"
                count={metrics?.totalDepartments}
                icon="🏥"
                colorClass="teal"
              />
              <DashboardCard
                title="My Appointments"
                count={metrics?.totalAppointments}
                icon="📅"
                colorClass="indigo"
              />
              <DashboardCard
                title="Pending Appointments"
                count={metrics?.pendingAppointments}
                icon="⏳"
                colorClass="amber"
              />
              <DashboardCard
                title="Completed Appointments"
                count={metrics?.completedAppointments}
                icon="✅"
                colorClass="green"
              />
              <DashboardCard
                title="Prescriptions Issued"
                count={metrics?.totalPrescriptions}
                icon="💊"
                colorClass="teal"
              />
              <DashboardCard
                title="Medical Records"
                count={metrics?.totalMedicalRecords}
                icon="📁"
                colorClass="blue"
              />
              <DashboardCard
                title="Medical Tests"
                count={metrics?.totalMedicalTests}
                icon="🧪"
                colorClass="amber"
              />
            </>
          )}
        </div>
      )}
    </div>
  );
}

export default Dashboard;
