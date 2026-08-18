import { defineStore } from 'pinia'
import api from '../services/api.js'
export const useAuthStore=defineStore('auth',{state:()=>({token:localStorage.getItem('travel_token')||'',user:JSON.parse(localStorage.getItem('travel_user')||'null')}),getters:{loggedIn:s=>Boolean(s.token)},actions:{async login(form){const data=await api.post('/public/auth/login',form);this.save(data);return data},async register(form){const data=await api.post('/public/auth/register',form);this.save(data);return data},save(data){this.token=data.token;this.user=data;localStorage.setItem('travel_token',data.token);localStorage.setItem('travel_user',JSON.stringify(data))},logout(){this.token='';this.user=null;localStorage.removeItem('travel_token');localStorage.removeItem('travel_user')}}})

