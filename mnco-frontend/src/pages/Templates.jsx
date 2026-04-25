import React, { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import Sidebar from '../components/Sidebar';
import api from '../api/axios';
import { Package, MoreHorizontal, FlaskConical, Search } from 'lucide-react';
import './Templates.css';

const Templates = () => {
  const [templates, setTemplates] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  const fetchTemplates = async () => {
    try {
      const response = await api.get('/templates');
      setTemplates(Array.isArray(response.data) ? response.data : response.data.data || []);
    } catch (error) {
      console.error('Error fetching templates', error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchTemplates();
  }, []);

  const filteredTemplates = templates.filter(t => 
    t.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    t.description?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="app-layout">
      <Navbar />
      <div className="main-content">
        <Sidebar />
        <main className="feed">
          <div className="feed-container">
            <div className="page-header card fade-in">
              <div className="header-info">
                <h2>Lab Templates</h2>
                <p>Browse and deploy pre-configured network topologies.</p>
              </div>
              <div className="search-bar-inline">
                <Search size={18} />
                <input 
                  type="text" 
                  placeholder="Search templates..." 
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
            </div>

            <div className="templates-grid">
              {isLoading ? (
                <div className="loading">Loading templates...</div>
              ) : filteredTemplates.length === 0 ? (
                <div className="card empty-state">
                  <h3>No templates found</h3>
                  <p>Templates are managed by administrators via EVE-NG.</p>
                </div>
              ) : (
                filteredTemplates.map(template => (
                  <div key={template.id} className="card template-card fade-in">
                    <div className="template-icon-wrapper">
                      <Package size={40} color="#1877F2" />
                    </div>
                    <div className="template-body">
                      <div className="template-header">
                        <h3>{template.name}</h3>
                        <span className="version-badge">v{template.version}</span>
                      </div>
                      <p className="template-desc">{template.description}</p>
                      <div className="template-meta">
                        <span>By {template.authorUsername || 'System'}</span>
                        <span className="dot">•</span>
                        <span>{template.isPublic ? 'Public' : 'Private'}</span>
                      </div>
                    </div>
                    <div className="template-footer">
                      <button className="btn-use-template">Use Template</button>
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

export default Templates;
