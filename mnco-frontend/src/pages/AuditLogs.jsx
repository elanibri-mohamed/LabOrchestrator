import React, { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import Sidebar from '../components/Sidebar';
import api from '../api/axios';
import { ShieldCheck, Info, AlertTriangle, User } from 'lucide-react';
import './AuditLogs.css';

const AuditLogs = () => {
  const [logs, setLogs] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  const fetchLogs = async () => {
    try {
      const response = await api.get('/admin/audit-logs');
      setLogs(Array.isArray(response.data) ? response.data : response.data.data || []);
    } catch (error) {
      console.error('Error fetching logs', error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, []);

  const getEventIcon = (type) => {
    if (type?.includes('LOGIN')) return <User size={18} color="#1877F2" />;
    if (type?.includes('LAB')) return <ShieldCheck size={18} color="#45BD62" />;
    return <Info size={18} color="#65676B" />;
  };

  return (
    <div className="app-layout">
      <Navbar />
      <div className="main-content">
        <Sidebar />
        <main className="feed">
          <div className="feed-container">
            <div className="page-header card fade-in">
              <div className="header-info">
                <h2>Audit Logs</h2>
                <p>Monitor system activity and security events.</p>
              </div>
              <button className="btn-refresh" onClick={fetchLogs}>Refresh Logs</button>
            </div>

            <div className="card logs-card fade-in">
              <table className="logs-table">
                <thead>
                  <tr>
                    <th>Event</th>
                    <th>User</th>
                    <th>IP Address</th>
                    <th>Result</th>
                    <th>Date</th>
                  </tr>
                </thead>
                <tbody>
                  {isLoading ? (
                    <tr><td colSpan="5" className="text-center">Loading logs...</td></tr>
                  ) : logs.length === 0 ? (
                    <tr><td colSpan="5" className="text-center">No logs found.</td></tr>
                  ) : (
                    logs.map(log => (
                      <tr key={log.id}>
                        <td>
                          <div className="event-cell">
                            {getEventIcon(log.eventType)}
                            <span>{log.eventType}</span>
                          </div>
                        </td>
                        <td>{log.actorUsername || 'System'}</td>
                        <td className="monospace">{log.ipAddress}</td>
                        <td>
                          <span className={`result-tag ${log.result?.toLowerCase()}`}>
                            {log.result}
                          </span>
                        </td>
                        <td>{new Date(log.createdAt).toLocaleString()}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
};

export default AuditLogs;
