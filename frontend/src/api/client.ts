import axios from 'axios';

const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const stored = localStorage.getItem('oqh_auth');
  if (stored) {
    try {
      const auth = JSON.parse(stored);
      if (auth.accessToken) {
        config.headers.Authorization = `Bearer ${auth.accessToken}`;
      }
    } catch { /* ignore */ }
  }
  return config;
});

let isRedirectingToLogin = false;

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && !isRedirectingToLogin) {
      isRedirectingToLogin = true;
      localStorage.removeItem('oqh_auth');
      window.location.href = '/login';
      return Promise.reject(error);
    }
    if (error.response?.status !== 401) {
      const message = error.response?.data?.message || error.message || 'An unexpected error occurred';
      console.error('API Error:', message);
    }
    return Promise.reject(error);
  }
);

export default api;
