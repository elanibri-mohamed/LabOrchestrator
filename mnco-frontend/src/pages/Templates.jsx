import React, { useEffect, useRef, useState } from 'react';
import Navbar from '../components/Navbar';
import Sidebar from '../components/Sidebar';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';
import { Package, MoreHorizontal, FlaskConical, Search, RefreshCw, UserPlus, X } from 'lucide-react';
import DescriptionRenderer, { insertImageTokenAtCursor } from '../components/DescriptionRenderer';
import './Templates.css';

const Templates = () => {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';
  const isTeacher = user?.role === 'TEACHER';
  
  const [templates, setTemplates] = useState([]);
  const [users, setUsers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSyncing, setIsSyncing] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  
  // Assignment Modal State
  const [showAssignModal, setShowAssignModal] = useState(false);
  const [selectedTemplate, setSelectedTemplate] = useState(null);
  const [selectedUserId, setSelectedUserId] = useState('');

  // Description Modal State
  const [showDescriptionModal, setShowDescriptionModal] = useState(false);
  const [descriptionValue, setDescriptionValue] = useState('');
  const [isSavingDescription, setIsSavingDescription] = useState(false);
  const descriptionTextareaRef = useRef(null);

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

  const fetchUsers = async () => {
    try {
      // Teachers assign to STUDENTS, Admins can assign to anyone
      const roleFilter = isTeacher ? '?role=STUDENT' : '';
      const response = await api.get('/users' + roleFilter);
      setUsers(Array.isArray(response.data) ? response.data : response.data.data || []);
    } catch (error) {
      console.error('Error fetching users', error);
    }
  };

  useEffect(() => {
    fetchTemplates();
    if (isAdmin || isTeacher) fetchUsers();
  }, [isAdmin, isTeacher]);

  const handleSync = async () => {
    setIsSyncing(true);
    try {
      await api.post('/templates/sync');
      fetchTemplates();
      alert('Templates synced successfully');
    } catch (error) {
      alert('Sync failed');
    } finally {
      setIsSyncing(false);
    }
  };

  const handleOpenAssign = (template) => {
    setSelectedTemplate(template);
    setShowAssignModal(true);
  };

  const handleOpenDescriptionEditor = (template) => {
    setSelectedTemplate(template);
    setDescriptionValue(template.description || '');
    setShowDescriptionModal(true);
  };

  const handleDescriptionPaste = (event) => {
    const clipboardFiles = Array.from(event.clipboardData?.files || []);
    const pastedImage = clipboardFiles.find(file => file.type.startsWith('image/'));

    if (!pastedImage || !descriptionTextareaRef.current) {
      return;
    }

    event.preventDefault();

    const reader = new FileReader();
    reader.onload = () => {
      const textarea = descriptionTextareaRef.current;
      const currentValue = descriptionValue;
      const selectionStart = textarea.selectionStart ?? currentValue.length;
      const selectionEnd = textarea.selectionEnd ?? currentValue.length;
      const { value: updatedValue, token } = insertImageTokenAtCursor(currentValue, selectionStart, selectionEnd, String(reader.result || ''), pastedImage.name || 'pasted image');
      setDescriptionValue(updatedValue);

      window.requestAnimationFrame(() => {
        textarea.focus();
        const cursorPosition = selectionStart + token.length;
        textarea.setSelectionRange(cursorPosition, cursorPosition);
      });
    };
    reader.readAsDataURL(pastedImage);
  };

  const handleSaveDescription = async () => {
    if (!selectedTemplate) return;
    setIsSavingDescription(true);
    try {
      const response = await api.patch(`/templates/${selectedTemplate.id}/description`, {
        description: descriptionValue,
      });
      const updatedTemplate = response.data?.data || response.data;
      setTemplates(prev => prev.map(template => (template.id === updatedTemplate.id ? updatedTemplate : template)));
      setShowDescriptionModal(false);
      setSelectedTemplate(null);
      setDescriptionValue('');
      alert('Lab description updated successfully');
    } catch (error) {
      alert('Description update failed: ' + (error.response?.data?.message || error.message));
    } finally {
      setIsSavingDescription(false);
    }
  };

  const handleAssign = async () => {
    if (!selectedUserId) return;
    try {
      const endpoint = isTeacher 
        ? `/assignments/student/${selectedUserId}/template/${selectedTemplate.id}`
        : `/assignments/teacher/${selectedUserId}/template/${selectedTemplate.id}`; // Simpler for admin
      
      // Note: Backend has specific endpoints for teacher vs student, 
      // but for simplicity in this UI we pick the right one based on role.
      // If Admin is assigning to a Student, we should use the student endpoint.
      // Let's check target user role.
      const targetUser = users.find(u => u.id === selectedUserId);
      const finalEndpoint = targetUser.role === 'STUDENT'
        ? `/assignments/student/${selectedUserId}/template/${selectedTemplate.id}`
        : `/assignments/teacher/${selectedUserId}/template/${selectedTemplate.id}`;

      await api.post(finalEndpoint);
      alert(`Template assigned to ${targetUser.username}`);
      setShowAssignModal(false);
      setSelectedUserId('');
    } catch (error) {
      alert('Assignment failed: ' + (error.response?.data?.message || error.message));
    }
  };

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
                <p>Manage and assign network topologies to users.</p>
              </div>
              <div className="header-actions">
                {isAdmin && (
                  <button className={`btn-sync ${isSyncing ? 'loading' : ''}`} onClick={handleSync} disabled={isSyncing}>
                    <RefreshCw size={18} className={isSyncing ? 'spin' : ''} /> {isSyncing ? 'Syncing...' : 'Sync from EVE-NG'}
                  </button>
                )}
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
            </div>

            <div className="templates-grid">
              {isLoading ? (
                <div className="loading">Loading templates...</div>
              ) : filteredTemplates.length === 0 ? (
                <div className="card empty-state">
                  <h3>No templates found</h3>
                  <p>Sync from EVE-NG to see available topologies.</p>
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
                        {(isAdmin || isTeacher) && (
                          <button
                            type="button"
                            className="btn-edit-description"
                            onClick={() => handleOpenDescriptionEditor(template)}
                          >
                            Edit Description
                          </button>
                        )}
                      </div>
                      <DescriptionRenderer text={template.description} className="template-desc template-desc-rendered" />
                      <div className="template-stats-mini">
                        <span>CPU: {template.cpuAllocated}</span>
                        <span>RAM: {template.ramAllocated}GB</span>
                      </div>
                    </div>
                    <div className="template-footer">
                      <button className="btn-assign" onClick={() => handleOpenAssign(template)}>
                        <UserPlus size={18} /> Assign to User
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </main>
      </div>

      {/* Assignment Modal */}
      {showAssignModal && (
        <div className="modal-overlay" onClick={() => setShowAssignModal(false)}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Assign Template</h3>
              <button className="close-btn" onClick={() => setShowAssignModal(false)}><X /></button>
            </div>
            <div className="modal-body">
              <p>Assigning <strong>{selectedTemplate?.name}</strong></p>
              <div className="form-group">
                <label>Select User</label>
                <select 
                  value={selectedUserId} 
                  onChange={(e) => setSelectedUserId(e.target.value)}
                  className="modal-select"
                >
                  <option value="">-- Choose a user --</option>
                  {users.map(u => (
                    <option key={u.id} value={u.id}>
                      {u.username} ({u.role})
                    </option>
                  ))}
                </select>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setShowAssignModal(false)}>Cancel</button>
              <button className="btn-primary" onClick={handleAssign} disabled={!selectedUserId}>
                Confirm Assignment
              </button>
            </div>
          </div>
        </div>
      )}

      {showDescriptionModal && (
        <div className="modal-overlay" onClick={() => setShowDescriptionModal(false)}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Edit Lab Description</h3>
              <button className="close-btn" onClick={() => setShowDescriptionModal(false)}><X /></button>
            </div>
            <div className="modal-body">
              <p>Update the lab description for <strong>{selectedTemplate?.name}</strong>.</p>
              <div className="form-group">
                <label>Lab Description</label>
                <textarea
                  ref={descriptionTextareaRef}
                  value={descriptionValue}
                  onChange={(e) => setDescriptionValue(e.target.value)}
                  className="modal-textarea"
                  rows="7"
                  placeholder="Describe the topology, learning goals, and any important notes for students."
                  onPaste={handleDescriptionPaste}
                />
                <p className="modal-hint">Tip: paste an image to embed it in the description.</p>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setShowDescriptionModal(false)}>Cancel</button>
              <button className="btn-primary" onClick={handleSaveDescription} disabled={isSavingDescription}>
                {isSavingDescription ? 'Saving...' : 'Save Description'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Templates;
