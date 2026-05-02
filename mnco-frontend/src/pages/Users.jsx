import React, { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import Sidebar from '../components/Sidebar';
import api from '../api/axios';
import { Users as UsersIcon, UserCheck, UserMinus, ShieldAlert } from 'lucide-react';
import './Users.css';

const Users = () => {
  const [users, setUsers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  const fetchUsers = async () => {
    try {
      const response = await api.get('/admin/users');
      setUsers(Array.isArray(response.data) ? response.data : response.data.data || []);
    } catch (error) {
      console.error('Error fetching users', error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  return (
    <div className="app-layout">
      <Navbar />
      <div className="main-content">
        <Sidebar />
        <main className="feed">
          <div className="feed-container">
            <div className="page-header card fade-in">
              <div className="header-info">
                <h2>User Management</h2>
                <p>Manage platform users, roles and quotas.</p>
              </div>
              <button className="btn-primary-sm">Add New User</button>
            </div>

            <div className="users-list">
              {isLoading ? (
                <div className="loading">Loading users...</div>
              ) : users.length === 0 ? (
                <div className="card empty-state">
                  <h3>No users found</h3>
                </div>
              ) : (
                users.map(user => (
                  <div key={user.id} className="card user-card fade-in">
                    <div className="user-card-header">
                      <div className="user-avatar">
                        {user.username.charAt(0).toUpperCase()}
                      </div>
                      <div className="user-info-main">
                        <h3>{user.username}</h3>
                        <span className="user-email">{user.email}</span>
                      </div>
                      <div className={`role-badge ${user.role.toLowerCase()}`}>
                        {user.role}
                      </div>
                    </div>
                    
                    <div className="user-stats-compact">
                      <div className="stat">
                        <span>Status</span>
                        <strong className={user.enabled ? 'text-success' : 'text-danger'}>
                          {user.enabled ? 'Active' : 'Disabled'}
                        </strong>
                      </div>
                    </div>

                    <div className="user-actions-row">
                      <button className="action-btn-sm">Edit Profile</button>
                      <button className="action-btn-sm">Quota</button>
                      {user.enabled ? (
                        <button className="action-btn-sm danger">Disable</button>
                      ) : (
                        <button className="action-btn-sm success">Enable</button>
                      )}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </main>
      </div>
    </div>
  );
};

export default Users;
