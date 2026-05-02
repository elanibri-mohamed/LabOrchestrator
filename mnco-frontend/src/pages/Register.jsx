import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../api/axios';
import './Login.css'; // Reuse Login styles for consistency

const REGISTER_ROLE_OPTIONS = [
  { value: 'STUDENT', label: 'Register as Student' },
  { value: 'TEACHER', label: 'Register as Teacher' },
];

const Register = () => {
  const [formData, setFormData] = useState({ username: '', email: '', password: '', role: 'STUDENT' });
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');
    try {
      await api.post('/auth/register', formData);
      alert('Registration successful! Please log in.');
      navigate('/login');
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-content container">
        <div className="login-brand">
          <h1>mnco</h1>
          <p>Join the Multi-Tenant Network and Cybersecurity Lab Orchestrator.</p>
        </div>
        
        <div className="login-card-wrapper">
          <div className="card login-card fade-in">
            <h2 style={{marginBottom: '20px'}}>Create a new account</h2>
            <form onSubmit={handleSubmit}>
              <div className="login-role-tabs" role="radiogroup" aria-label="Register role">
                {REGISTER_ROLE_OPTIONS.map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    role="radio"
                    aria-checked={formData.role === option.value}
                    className={`login-role-tab ${formData.role === option.value ? 'active' : ''}`}
                    onClick={() => setFormData({ ...formData, role: option.value })}
                  >
                    {option.label}
                  </button>
                ))}
              </div>

              <div className="form-group">
                <input
                  type="text"
                  name="username"
                  placeholder="Username"
                  value={formData.username}
                  onChange={handleChange}
                  required
                />
              </div>
              <div className="form-group">
                <input
                  type="email"
                  name="email"
                  placeholder="Email"
                  value={formData.email}
                  onChange={handleChange}
                  required
                />
              </div>
              <div className="form-group">
                <input
                  type="password"
                  name="password"
                  placeholder="New Password"
                  value={formData.password}
                  onChange={handleChange}
                  required
                />
              </div>
              
              {error && <div className="error-message">{error}</div>}
              
              <button type="submit" className="btn-primary" disabled={isLoading} style={{backgroundColor: '#42b72a'}}>
                {isLoading ? 'Creating account...' : `Sign Up as ${formData.role === 'TEACHER' ? 'Teacher' : 'Student'}`}
              </button>
              
              <hr className="divider" />
              
              <div className="forgot-password">
                <Link to="/login">Already have an account?</Link>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Register;
