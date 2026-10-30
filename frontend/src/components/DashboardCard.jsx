import React from 'react';

function DashboardCard({ title, count, icon, colorClass = 'blue' }) {
  return (
    <div className={`metric-card card-${colorClass}`}>
      <div className="card-icon">{icon}</div>
      <div className="card-info">
        <div className="card-count">{count ?? 0}</div>
        <div className="card-title">{title}</div>
      </div>
    </div>
  );
}

export default DashboardCard;
