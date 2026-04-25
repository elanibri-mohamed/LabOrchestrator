import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Login.css';

const Login = () => {
  const [credentials, setCredentials] = useState({ usernameOrEmail: '', password: '' });
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e) => {
    setCredentials({ ...credentials, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');
    try {
      await login(credentials.usernameOrEmail, credentials.password);
      navigate('/');
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed. Please check your credentials.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-content container">
        <div className="login-brand">
          <h1>mnco</h1>
          <p>Multi-Tenant Network and Cybersecurity Lab Orchestrator.</p>
        </div>
        
        <div className="login-card-wrapper">
          <div className="card login-card fade-in">
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <input
                  type="text"
                  name="usernameOrEmail"
                  placeholder="Email or Username"
                  value={credentials.usernameOrEmail}
                  onChange={handleChange}
                  required
                />
              </div>
              <div className="form-group">
                <input
                  type="password"
                  name="password"
                  placeholder="Password"
                  value={credentials.password}
                  onChange={handleChange}
                  required
                />
              </div>
              
              {error && <div className="error-message">{error}</div>}
              
              <button type="submit" className="btn-primary" disabled={isLoading}>
                {isLoading ? 'Logging in...' : 'Log In'}
              </button>
              
              <div className="forgot-password">
                <a href="#">Forgotten password?</a>
              </div>
              
              <hr className="divider" />
              
              <div className="register-btn-wrapper">
                <Link to="/register" className="btn-secondary">
                  Create new account
                </Link>
              </div>
            </form>
          </div>
          <p className="login-footer"><b>Create a Page</b> for a celebrity, brand or business.</p>
        </div>
      </div>
    </div>
  );
};

export default Login;
