import React, { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import Sidebar from '../components/Sidebar';
import api from '../api/axios';
import { BarChart3, Cpu, Database, HardDrive, Info } from 'lucide-react';
import './Quota.css';

const Quota = () => {
  const [quota, setQuota] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  const fetchQuota = async () => {
    try {
      const response = await api.get('/quota');
      setQuota(response.data.data);
    } catch (error) {
      console.error('Error fetching quota', error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchQuota();
  }, []);

  const calculatePercentage = (used, max) => {
    if (!max || max === 0) return 0;
    return Math.min(Math.round((used / max) * 100), 100);
  };

  if (isLoading) return <div className="loading">Loading quota details...</div>;

  return (
    <div className="app-layout">
      <Navbar />
      <div className="main-content">
        <Sidebar />
        <main className="feed">
          <div className="feed-container">
            <div className="page-header card fade-in">
              <div className="header-info">
                <h2>My Resource Quota</h2>
                <p>Monitor your resource usage and limits.</p>
              </div>
              <div className="quota-icon-main">
                <BarChart3 size={32} color="#1877F2" />
              </div>
            </div>

            <div className="quota-grid">
              {/* Labs Quota */}
              <div className="card quota-card fade-in">
                <div className="quota-card-header">
                  <div className="icon-circle" style={{ background: '#e7f3ff' }}>
                    <Database size={24} color="#1877F2" />
                  </div>
                  <h3>Laboratories</h3>
                </div>
                <div className="quota-usage">
                  <div className="usage-numbers">
                    <strong>{quota?.usedLabs}</strong> <span>/ {quota?.maxLabs} Labs</span>
                  </div>
                  <div className="progress-container">
                    <div 
                      className="progress-bar" 
                      style={{ width: `${calculatePercentage(quota?.usedLabs, quota?.maxLabs)}%`, background: '#1877F2' }}
                    ></div>
                  </div>
                  <p className="quota-hint">Maximum number of concurrent labs allowed.</p>
                </div>
              </div>

              {/* CPU Quota */}
              <div className="card quota-card fade-in">
                <div className="quota-card-header">
                  <div className="icon-circle" style={{ background: '#e7f8ed' }}>
                    <Cpu size={24} color="#31a24c" />
                  </div>
                  <h3>CPU Cores</h3>
                </div>
                <div className="quota-usage">
                  <div className="usage-numbers">
                    <strong>{quota?.usedCpu}</strong> <span>/ {quota?.maxCpu} vCPUs</span>
                  </div>
                  <div className="progress-container">
                    <div 
                      className="progress-bar" 
                      style={{ width: `${calculatePercentage(quota?.usedCpu, quota?.maxCpu)}%`, background: '#31a24c' }}
                    ></div>
                  </div>
                  <p className="quota-hint">Total CPU cores allocated across all running labs.</p>
                </div>
              </div>

              {/* RAM Quota */}
              <div className="card quota-card fade-in">
                <div className="quota-card-header">
                  <div className="icon-circle" style={{ background: '#fff9e6' }}>
                    <HardDrive size={24} color="#f7b928" />
                  </div>
                  <h3>Memory (RAM)</h3>
                </div>
                <div className="quota-usage">
                  <div className="usage-numbers">
                    <strong>{quota?.usedRamGb} GB</strong> <span>/ {quota?.maxRamGb} GB</span>
                  </div>
                  <div className="progress-container">
                    <div 
                      className="progress-bar" 
                      style={{ width: `${calculatePercentage(quota?.usedRamGb, quota?.maxRamGb)}%`, background: '#f7b928' }}
                    ></div>
                  </div>
                  <p className="quota-hint">Total RAM memory available for your virtual nodes.</p>
                </div>
              </div>

              {/* Storage Quota */}
              <div className="card quota-card fade-in">
                <div className="quota-card-header">
                  <div className="icon-circle" style={{ background: '#ffebe9' }}>
                    <Database size={24} color="#fa3e3e" />
                  </div>
                  <h3>Storage</h3>
                </div>
                <div className="quota-usage">
                  <div className="usage-numbers">
                    <strong>{quota?.usedStorageGb} GB</strong> <span>/ {quota?.maxStorageGb} GB</span>
                  </div>
                  <div className="progress-container">
                    <div 
                      className="progress-bar" 
                      style={{ width: `${calculatePercentage(quota?.usedStorageGb, quota?.maxStorageGb)}%`, background: '#fa3e3e' }}
                    ></div>
                  </div>
                  <p className="quota-hint">Disk space used by your lab instances and snapshots.</p>
                </div>
              </div>
            </div>

            <div className="card info-card fade-in">
              <Info size={20} color="#65676b" />
              <p>Need more resources? Please contact your administrator or instructor to request a quota increase.</p>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
};

export default Quota;
