import axios from 'axios'
const api=axios.create({baseURL:'/api',timeout:12000})
api.interceptors.request.use(config=>{const token=localStorage.getItem('travel_token');if(token)config.headers.Authorization=`Bearer ${token}`;return config})
api.interceptors.response.use(response=>response.data?.data??response.data,error=>Promise.reject(new Error(error.response?.data?.message||error.message||'请求失败')))
export default api

