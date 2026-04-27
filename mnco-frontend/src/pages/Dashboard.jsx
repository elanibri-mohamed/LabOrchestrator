import React, { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import Sidebar from '../components/Sidebar';
import api from '../api/axios';
import { Play, Square, MoreHorizontal, Monitor, ExternalLink, ChevronDown, ChevronUp, RotateCcw } from 'lucide-react';
import './Dashboard.css';

const Dashboard = () => {
  const [labs, setLabs] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [labNodes, setLabNodes] = useState({});
  const [expandedLab, setExpandedLab] = useState(null);
  const [now, setNow] = useState(Date.now());

  const fetchLabs = async () => {
    try {
      const response = await api.get('/instances/my');
      setLabs(Array.isArray(response.data) ? response.data : response.data.data || []);
    } catch (error) {
      console.error('Error fetching labs', error);
    } finally {
      setIsLoading(false);
    }
  };

  const fetchNodes = async (templateId) => {
    try {
      const response = await api.get(`/instances/template/${templateId}/nodes`);
      const nodesData = response.data.data || response.data;
      setLabNodes(prev => ({ ...prev, [templateId]: nodesData }));
    } catch (error) {
      console.error('Error fetching nodes', error);
    }
  };

  const toggleNodes = (templateId) => {
    if (expandedLab === templateId) {
      setExpandedLab(null);
    } else {
      setExpandedLab(templateId);
      if (!labNodes[templateId]) {
        fetchNodes(templateId);
      }
    }
  };

  useEffect(() => {
    fetchLabs();
    const interval = setInterval(fetchLabs, 10000); // Poll every 10s
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    const timerInterval = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timerInterval);
  }, []);

  const handleStart = async (templateId) => {
    try {
      await api.post(`/instances/template/${templateId}/start`);
      fetchLabs();
    } catch (error) {
      alert('Failed to start lab: ' + (error.response?.data?.message || error.message));
    }
  };

  const handleStop = async (templateId) => {
    try {
      await api.post(`/instances/template/${templateId}/stop`);
      setExpandedLab(null); // Close nodes view on stop
      fetchLabs();
    } catch (error) {
      alert('Failed to stop lab: ' + (error.response?.data?.message || error.message));
    }
  };

  const handleReset = async (templateId) => {
    if (!window.confirm("Are you sure? This will wipe all node configurations and reset the lab to its original state?")) return;
    try {
      await api.post(`/instances/template/${templateId}/reset`);
      setExpandedLab(null);
      fetchLabs();
    } catch (error) {
      alert('Failed to reset lab: ' + (error.response?.data?.message || error.message));
    }
  };

  const handleResetTimer = async (templateId) => {
    try {
      await api.post(`/instances/template/${templateId}/timer/reset`);
      fetchLabs();
    } catch (error) {
      alert('Failed to reset timer: ' + (error.response?.data?.message || error.message));
    }
  };

  const getRemainingSeconds = (lab) => {
    if (lab.status !== 'RUNNING' || !lab.expiresAt) {
      return null;
    }
    const remainingMs = new Date(lab.expiresAt).getTime() - now;
    return Math.max(0, Math.floor(remainingMs / 1000));
  };

  const formatCountdown = (seconds) => {
    if (seconds === null) return '--:--';
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  };

  const runningLabTemplateId = labs.find(lab => lab.status === 'RUNNING')?.templateId;

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
                      <div className="stat"><span>Timer</span> <strong className={getRemainingSeconds(lab) === 0 ? 'timer-expired' : ''}>{formatCountdown(getRemainingSeconds(lab))}</strong></div>
                    </div>

                    <div className="lab-actions">
                      {lab.status === 'RUNNING' ? (
                        <>
                          <button className="action-btn-main stop" onClick={() => handleStop(lab.templateId)}>
                            <Square size={18} fill="currentColor" /> Stop
                          </button>
                          <button className="action-btn-main secondary" onClick={() => toggleNodes(lab.templateId)}>
                            {expandedLab === lab.templateId ? <ChevronUp size={18} /> : <ChevronDown size={18} />} 
                            {expandedLab === lab.templateId ? 'Hide Nodes' : 'View Nodes'}
                          </button>
                          <button className="action-btn-main outline" onClick={() => handleReset(lab.templateId)}>
                              Reset Lab
                          </button>
                          <button className="action-btn-main secondary" onClick={() => handleResetTimer(lab.templateId)}>
                            <RotateCcw size={16} /> Reset Timer
                          </button>
                        </>
                      ) : (
                        <button
                          className="action-btn-main start"
                          onClick={() => handleStart(lab.templateId)}
                          disabled={Boolean(runningLabTemplateId && runningLabTemplateId !== lab.templateId)}
                          title={runningLabTemplateId && runningLabTemplateId !== lab.templateId ? 'Stop your running lab before starting another one.' : ''}
                        >
                          <Play size={18} fill="currentColor" /> Start
                        </button>
                      )}
                    </div>

                    {expandedLab === lab.templateId && labNodes[lab.templateId] && (
                      <div className="nodes-section fade-in">
                        <h4 className="nodes-title"><Monitor size={16} /> Lab Nodes</h4>
                        <div className="nodes-list">
                          {Object.values(labNodes[lab.templateId]).map(node => (
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
