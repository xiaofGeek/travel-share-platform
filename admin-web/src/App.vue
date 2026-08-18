<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  DataAnalysis, Location, Document, Checked, Guide, CollectionTag, ChatDotRound,
  Picture, User, Warning, Promotion, Setting, Tickets, Fold, Expand, ArrowDown
} from '@element-plus/icons-vue'
import { useAdminAuth } from './stores/auth.js'

const route = useRoute()
const router = useRouter()
const auth = useAdminAuth()
const collapsed = ref(false)
const publicPage = computed(() => route.meta.public)
const ADMIN = ['ADMIN']
const BACKOFFICE = ['ADMIN', 'AUDITOR']

const groups = computed(() => [
  { name: '概览', items: [['/dashboard', '数据看板', DataAnalysis, ADMIN]] },
  {
    name: '内容管理',
    items: [
      ['/destinations', '目的地管理', Location, ADMIN],
      ['/guides', '攻略管理', Document, ADMIN],
      ['/audits', '攻略审核', Checked, BACKOFFICE],
      ['/routes', '路线管理', Guide, ADMIN],
      ['/topics', '专题与标签', CollectionTag, ADMIN],
      ['/comments', '评论管理', ChatDotRound, ADMIN],
      ['/banners', '轮播图管理', Picture, ADMIN]
    ]
  },
  {
    name: '社区治理',
    items: [
      ['/users', '用户管理', User, ADMIN],
      ['/reports', '举报处理', Warning, BACKOFFICE]
    ]
  },
  {
    name: '运营与系统',
    items: [
      ['/operations', '运营推荐', Promotion, ADMIN],
      ['/system', '系统参数', Setting, ADMIN],
      ['/logs', '操作日志', Tickets, ADMIN]
    ]
  }
].map(group => ({
  ...group,
  items: group.items.filter(item => item[3].includes(auth.user?.role))
})).filter(group => group.items.length))

const homePath = computed(() => auth.isAdmin ? '/dashboard' : '/audits')
const logout = () => {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <router-view v-if="publicPage" />
  <div v-else class="admin-shell" :class="{ collapsed }">
    <aside>
      <div class="admin-brand">
        <img src="/uploads/demo/logo.png" alt="山海迹">
        <div><b>山海迹</b><span>内容运营中心</span></div>
      </div>
      <div class="menu-scroll">
        <section v-for="group in groups" :key="group.name">
          <p>{{ group.name }}</p>
          <router-link v-for="item in group.items" :key="item[0]" :to="item[0]">
            <el-icon><component :is="item[2]" /></el-icon><span>{{ item[1] }}</span>
          </router-link>
        </section>
      </div>
      <div class="admin-version">v1.0 · 本地演示版</div>
    </aside>
    <div class="admin-main">
      <header>
        <button class="fold-button" @click="collapsed = !collapsed">
          <el-icon><Expand v-if="collapsed" /><Fold v-else /></el-icon>
        </button>
        <div class="breadcrumb"><span>山海迹管理后台</span><b>/</b><strong>{{ route.meta.title }}</strong></div>
        <div class="header-actions">
          <span class="role-tag">{{ auth.user?.role === 'ADMIN' ? '系统管理员' : '内容审核员' }}</span>
          <el-dropdown>
            <button class="admin-user">
              <img :src="auth.user?.avatar" alt="管理员头像"><span>{{ auth.user?.nickname }}</span><el-icon><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push(homePath)">返回工作台</el-dropdown-item>
                <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <main class="admin-content"><router-view /></main>
    </div>
  </div>
</template>
