import React, { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import Sidebar from '../components/Sidebar';
import api from '../api/axios';
import { Play, Square, MoreHorizontal, Plus, FlaskConical } from 'lucide-react';
import './Dashboard.css';

const Dashboard = () => {
  const [labs, setLabs] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  const fetchLabs = async () => {
    try {
      const response = await api.get('/labs');
      // The backend returns the list directly or wrapped? 
      // Based on AuthController, it's likely direct or response.data
      setLabs(Array.isArray(response.data) ? response.data : response.data.data || []);
    } catch (error) {
      console.error('Error fetching labs', error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchLabs();
    const interval = setInterval(fetchLabs, 10000); // Poll every 10s
    return () => clearInterval(interval);
  }, []);

  const handleStart = async (id) => {
    try {
      await api.post(`/labs/${id}/start`);
      fetchLabs();
    } catch (error) {
      alert('Failed to start lab: ' + (error.response?.data?.message || error.message));
    }
  };

  const handleStop = async (id) => {
    try {
      await api.post(`/labs/${id}/stop`);
      fetchLabs();
    } catch (error) {
      alert('Failed to stop lab: ' + (error.response?.data?.message || error.message));
    }
  };

  return (
    <div className="app-layout">
      <Navbar />
      <div className="main-content">
        <Sidebar />
        <main className="feed">
          <div className="feed-container">
            <div className="labs-grid">
              {isLoading ? (
                <div className="loading">Loading your labs...</div>
              ) : labs.length === 0 ? (
                <div className="card empty-state">
                  <h3>No labs found</h3>
                  <p>Create your first network lab to get started.</p>
                </div>
              ) : (
                labs.map(lab => (
                  <div key={lab.id} className="card lab-card fade-in">
                    <div className="lab-header">
                      <div className="lab-info">
                        <h3 className="lab-name">{lab.name}</h3>
                        <span className="lab-date">{new Date(lab.createdAt).toLocaleDateString()}</span>
                      </div>
                      <MoreHorizontal className="lab-menu" />
                    </div>
                    
                    <p className="lab-description">{lab.description}</p>
                    
                    <div className="lab-stats">
                      <div className="stat"><span>CPU</span> <strong>{lab.cpuAllocated}</strong></div>
                      <div className="stat"><span>RAM</span> <strong>{lab.ramAllocated}GB</strong></div>
                      <div className="stat"><span>Status</span> <strong className={`status-${lab.status.toLowerCase()}`}>{lab.status}</strong></div>
                    </div>

                    <div className="lab-actions">
                      {lab.status === 'RUNNING' ? (
                        <button className="action-btn-main stop" onClick={() => handleStop(lab.id)}>
                          <Square size={18} fill="currentColor" /> Stop
                        </button>
                      ) : (
                        <button className="action-btn-main start" onClick={() => handleStart(lab.id)}>
                          <Play size={18} fill="currentColor" /> Start
                        </button>
                      )}
                      <button className="action-btn-main secondary">Open Console</button>
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

export default Dashboard;
