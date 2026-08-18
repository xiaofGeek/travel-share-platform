import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'

const list = () => import('../views/ListView.vue')
const ADMIN = ['ADMIN']
const BACKOFFICE = ['ADMIN', 'AUDITOR']

const routes = [
  { path: '/login', component: () => import('../views/LoginView.vue'), meta: { public: true, title: '登录' } },
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', component: DashboardView, meta: { title: '数据看板', roles: ADMIN } },
  { path: '/destinations', component: list, meta: { title: '目的地管理', resource: 'destinations', roles: ADMIN } },
  { path: '/guides', component: list, meta: { title: '攻略管理', resource: 'guides', roles: ADMIN } },
  { path: '/audits', component: () => import('../views/AuditView.vue'), meta: { title: '攻略审核', roles: BACKOFFICE } },
  { path: '/routes', component: list, meta: { title: '路线管理', resource: 'routes', roles: ADMIN } },
  { path: '/topics', component: list, meta: { title: '专题管理', resource: 'topics', roles: ADMIN } },
  { path: '/comments', component: list, meta: { title: '评论管理', resource: 'comments', roles: ADMIN } },
  { path: '/banners', component: list, meta: { title: '轮播图管理', resource: 'banners', roles: ADMIN } },
  { path: '/users', component: list, meta: { title: '用户管理', resource: 'users', roles: ADMIN } },
  { path: '/reports', component: () => import('../views/ReportsView.vue'), meta: { title: '举报处理', roles: BACKOFFICE } },
  { path: '/operations', component: list, meta: { title: '运营推荐', resource: 'recommendations', roles: ADMIN } },
  { path: '/system', component: list, meta: { title: '系统参数', resource: 'configs', roles: ADMIN } },
  { path: '/logs', component: list, meta: { title: '操作日志', resource: 'logs', roles: ADMIN } },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(to => {
  if (!to.meta.public && !localStorage.getItem('admin_token')) return '/login'
  const user = JSON.parse(localStorage.getItem('admin_user') || 'null')
  if (to.meta.roles && !to.meta.roles.includes(user?.role)) return user?.role === 'AUDITOR' ? '/audits' : '/dashboard'
  document.title = `${to.meta.title || '管理后台'} · 山海迹`
})

export default router
