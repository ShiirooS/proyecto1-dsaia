import './index.css'
import './App.css'
import Home from './pages/HomePage'
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import Room from './pages/RoomsPages';
import About from './pages/AboutPage';
import RoomDetails from './pages/RoomDetailsPage';
import { lazy, Suspense } from 'react';
import LoginAdmin from './pages/AdminLoginPage';
import AuthProvider from './contexts/AuthContext';
import { ProtectedRoute } from './routes/ProtectedRoute';
import Bookings from './pages/Bookings';
import PaymentMethodsPage from './pages/PaymentMethodsPage';

// HU-03: the admin panel carries the charts library (recharts); guests never download it
const AdminPanel = lazy(() => import('./pages/DashboardPage'));

function App() {

  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/admin/login" element={<LoginAdmin />} />
          <Route element={<ProtectedRoute requiredRole="ADMIN" />}>
            <Route
              path="/admin/dashboard"
              element={
                <Suspense fallback={<div role="status" className="p-8 text-center text-gray-500">Cargando...</div>}>
                  <AdminPanel />
                </Suspense>
              }
            />
          </Route>
          <Route path="/rooms" element={<Room />} />
          <Route path="/roomDetail/:id" element={<RoomDetails />} />
          <Route element={<ProtectedRoute redirectPath="/?login=1&redirect=%2Fbookings" />}>
            <Route path="/bookings" element={<Bookings />} />
          </Route>
          <Route element={<ProtectedRoute redirectPath="/?login=1&redirect=%2Fpayment-methods" />}>
            <Route path="/payment-methods" element={<PaymentMethodsPage />} />
          </Route>
          <Route path="/about" element={<About />} />
        </Routes>
      </Router>
    </AuthProvider>
  )
}
export default App
