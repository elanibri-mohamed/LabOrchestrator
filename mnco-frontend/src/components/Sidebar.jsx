import React from 'react';
import { NavLink } from 'react-router-dom';
import { FlaskConical, Box, ShieldCheck, History, BarChart3, UserCog } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import './Sidebar.css';

const Sidebar = () => {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';

  return (
    <aside className="sidebar">
      <div className="sidebar-section">
        <NavLink to="/" className="sidebar-link">
          <FlaskConical size={24} color="#1877F2" />
          <span>My Labs</span>
        </NavLink>
        <NavLink to="/templates" className="sidebar-link">
          <Box size={24} color="#F7B928" />
          <span>Lab Templates</span>
        </NavLink>
      </div>

      <hr className="sidebar-divider" />

      <div className="sidebar-section">
        <h3 className="section-title">Account</h3>
        <NavLink to="/quota" className="sidebar-link">
          <BarChart3 size={24} color="#45BD62" />
          <span>My Quota</span>
        </NavLink>
        <NavLink to="/history" className="sidebar-link">
          <History size={24} color="#1877F2" />
          <span>Activity Log</span>
        </NavLink>
      </div>

      {isAdmin && (
        <>
          <hr className="sidebar-divider" />
          <div className="sidebar-section">
            <h3 className="section-title">Administration</h3>
            <NavLink to="/admin/users" className="sidebar-link">
              <UserCog size={24} color="#65676B" />
              <span>User Management</span>
            </NavLink>
            <NavLink to="/admin/audit-logs" className="sidebar-link">
              <ShieldCheck size={24} color="#F02849" />
              <span>Audit Logs</span>
            </NavLink>
          </div>
        </>
      )}
      
      <div className="sidebar-footer">
        <p>© 2026 MNCO Lab Orchestrator</p>
      </div>
    </aside>
  );
};

export default Sidebar;
