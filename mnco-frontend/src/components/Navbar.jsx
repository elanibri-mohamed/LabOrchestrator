import React from 'react';
import { useAuth } from '../context/AuthContext';
import { LogOut, User, Bell, Search, Home, LayoutGrid } from 'lucide-react';
import './Navbar.css';

const Navbar = () => {
  const { user, logout } = useAuth();

  return (
    <nav className="navbar">
      <div className="navbar-left">
        <div className="navbar-logo">mnco</div>
        <div className="navbar-search">
          <Search size={18} />
          <input type="text" placeholder="Search labs, templates..." />
        </div>
      </div>

      <div className="navbar-center">
        <div className="nav-item active"><Home size={28} /></div>
        <div className="nav-item"><LayoutGrid size={28} /></div>
      </div>

      <div className="navbar-right">
        <div className="user-profile">
          <div className="avatar">
            <User size={20} />
          </div>
          <span>{user?.username}</span>
        </div>
        <div className="nav-icon-btn"><Bell size={20} /></div>
        <div className="nav-icon-btn" onClick={logout} title="Logout">
          <LogOut size={20} />
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
