import axios from'axios';const api=axios.create({baseURL:'/api',timeout:15000});api.interceptors.request.use(c=>{const t=localStorage.getItem('admin_token');if(t)c.headers.Authorization=`Bearer ${t}`;return c});api.interceptors.response.use(r=>r.data?.data??r.data,e=>Promise.reject(new Error(e.response?.data?.message||e.message||'请求失败')));export default api

