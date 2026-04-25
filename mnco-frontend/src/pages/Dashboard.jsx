import React, { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import Sidebar from '../components/Sidebar';
import api from '../api/axios';
import { Play, Square, MoreHorizontal, Plus, FlaskConical, Monitor, ExternalLink, ChevronDown, ChevronUp } from 'lucide-react';
import './Dashboard.css';

const Dashboard = () => {
  const [labs, setLabs] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [labNodes, setLabNodes] = useState({});
  const [expandedLab, setExpandedLab] = useState(null);

  const fetchLabs = async () => {
    try {
      const response = await api.get('/labs');
      setLabs(Array.isArray(response.data) ? response.data : response.data.data || []);
    } catch (error) {
      console.error('Error fetching labs', error);
    } finally {
      setIsLoading(false);
    }
  };

  const fetchNodes = async (labId) => {
    try {
      const response = await api.get(`/labs/${labId}/nodes`);
      const nodesData = response.data.data || response.data;
      setLabNodes(prev => ({ ...prev, [labId]: nodesData }));
    } catch (error) {
      console.error('Error fetching nodes', error);
    }
  };

  const toggleNodes = (labId) => {
    if (expandedLab === labId) {
      setExpandedLab(null);
    } else {
      setExpandedLab(labId);
      if (!labNodes[labId]) {
        fetchNodes(labId);
      }
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
      setExpandedLab(null); // Close nodes view on stop
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
                        <>
                          <button className="action-btn-main stop" onClick={() => handleStop(lab.id)}>
                            <Square size={18} fill="currentColor" /> Stop
                          </button>
                          <button className="action-btn-main secondary" onClick={() => toggleNodes(lab.id)}>
                            {expandedLab === lab.id ? <ChevronUp size={18} /> : <ChevronDown size={18} />} 
                            {expandedLab === lab.id ? 'Hide Nodes' : 'View Nodes'}
                          </button>
                        </>
                      ) : (
                        <button className="action-btn-main start" onClick={() => handleStart(lab.id)}>
                          <Play size={18} fill="currentColor" /> Start
                        </button>
                      )}
                    </div>

                    {expandedLab === lab.id && labNodes[lab.id] && (
                      <div className="nodes-section fade-in">
                        <h4 className="nodes-title"><Monitor size={16} /> Lab Nodes</h4>
                        <div className="nodes-list">
                          {Object.values(labNodes[lab.id]).map(node => (
                            <div key={node.id} className="node-item">
                              <div className="node-info">
                                <span className="node-name">{node.name}</span>
                                <span className={`node-status ${node.status === 2 ? 'online' : 'offline'}`}>
                                  {node.status === 2 ? 'Running' : 'Stopped'}
                                </span>
                              </div>
                              {node.url && node.status === 2 && (
                                <a 
                                  href={node.url} 
                                  target="_blank" 
                                  rel="noopener noreferrer" 
                                  className="console-link"
                                >
                                  <ExternalLink size={14} /> Console
                                </a>
                              )}
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
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
