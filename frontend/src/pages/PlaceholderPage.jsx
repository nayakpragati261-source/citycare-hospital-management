import React from 'react';

function PlaceholderPage({ title }) {
  return (
    <div className="placeholder-container">
      <div className="placeholder-card">
        <div className="placeholder-icon">🛠️</div>
        <h2>{title} Module</h2>
        <p className="placeholder-text">
          The <strong>{title}</strong> management module is scheduled for development in a future stage.
        </p>
        <div className="placeholder-badge">
          Module Coming Soon
        </div>
      </div>
    </div>
  );
}

export default PlaceholderPage;
