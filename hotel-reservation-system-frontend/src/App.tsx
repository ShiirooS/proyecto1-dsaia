import './index.css'
import './App.css'
import Home from './pages/Home'
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import Product from './pages/Rooms';
import About from './pages/About';
import RoomDetails from './pages/RoomDetails';
import AdminPanel from './pages/AdminPanel';
import Login from './pages/Login';
import Register from './pages/Register';
import ProtectedRoute from './components/ProtectedRoute';
import { AuthProvider } from './context/AuthContext';

function App() {

  return (
    <Router>
      <AuthProvider>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/admin" element={<ProtectedRoute role="ADMIN"><AdminPanel /></ProtectedRoute>} />
        <Route path="/products" element={<Product />} /> {/* Ruta para Products */}
        <Route path="/about" element={<About />} /> 
        <Route path="/productDetail" element={<RoomDetails />} /> 
        {/* Agrega otras rutas aquí */}
      </Routes>
    </AuthProvider>
    </Router>
  )
}

export default App
