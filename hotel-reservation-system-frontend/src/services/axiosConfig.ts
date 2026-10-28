import axios from 'axios';
import { getToken, logout } from '../services/authService';
import { notifyUnauthorized } from './authEvents';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:3000',
});

api.interceptors.request.use(
  (config) => {
    const token = getToken();
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

/**
 * A 401 on a request that carried a token means the session is invalid or expired:
 * drop the token and let AuthContext close the session. A 403 keeps the backend
 * `message` ("No tiene permisos...") as the Error message so admin screens can show it.
 */
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error)) {
      const status = error.response?.status;
      const sentToken = !!error.config?.headers?.get?.('Authorization');
      if (status === 401 && sentToken) {
        logout();
        notifyUnauthorized();
      } else if (status === 403) {
        const message = (error.response?.data as { message?: string } | undefined)?.message;
        if (message) error.message = message;
      }
    }
    return Promise.reject(error);
  }
);

export default api;
