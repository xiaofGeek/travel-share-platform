<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../services/api.js'
import { Calendar, Wallet, Sunny, CopyDocument, Star, Location } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth.js'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const data = ref(null)
const favorited = ref(false)
const favoriting = ref(false)

const load = async () => {
  data.value = await api.get(`/public/routes/${route.params.id}`)
  if (auth.loggedIn) {
    try {
      const state = await api.get(`/user/favorites/ROUTE/${route.params.id}/state`)
      favorited.value = Boolean(state.favorited)
    } catch {
      favorited.value = false
    }
  }
}

onMounted(load)

const copy = async () => {
  if (!auth.loggedIn) return router.push(`/login?redirect=${route.fullPath}`)
  try {
    await api.post(`/user/routes/${data.value.id}/copy`)
    ElMessage.success('已复制到“我的路线”')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const favorite = async () => {
  if (!auth.loggedIn) return router.push(`/login?redirect=${route.fullPath}`)
  favoriting.value = true
  try {
    const result = await api.post(`/user/favorites/ROUTE/${data.value.id}`)
    favorited.value = Boolean(result.favorited)
    data.value.favoriteCount = result.favoriteCount
    ElMessage.success(favorited.value ? '已收藏路线' : '已取消收藏')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    favoriting.value = false
  }
}
</script>
<template><div v-if="!data" class="loading-panel"><el-skeleton :rows="12" animated/></div><div v-else class="route-detail"><section class="route-hero" :style="{backgroundImage:`linear-gradient(90deg,rgba(7,35,42,.82),rgba(7,35,42,.12)),url(${data.coverImage})`}"><div class="container"><span>PUBLIC ITINERARY · 可复制公开路线</span><h1>{{data.name}}</h1><p>{{data.summary}}</p><div class="hero-facts"><b><el-icon><Calendar/></el-icon>{{data.totalDays}} 天</b><b><el-icon><Wallet/></el-icon>预算 ¥{{Number(data.budget).toLocaleString()}}</b><b><el-icon><Sunny/></el-icon>{{data.season}}</b></div><div class="hero-actions"><button @click="copy"><el-icon><CopyDocument/></el-icon>复制到我的行程</button><button :class="{active:favorited}" :aria-pressed="favorited" :disabled="favoriting" @click="favorite"><el-icon><Star/></el-icon>{{favoriting?'处理中…':favorited?'✓ 已收藏路线':`收藏路线 ${data.favoriteCount||0}`}}</button></div></div></section><section class="section"><div class="container route-layout"><main><div v-for="day in data.days" :key="day.id" class="day"><div class="day-marker"><b>D{{String(day.day_number||day.dayNumber).padStart(2,'0')}}</b><span>DAY</span></div><div class="day-content"><div class="day-title"><span>第 {{day.day_number||day.dayNumber}} 天</span><h2>{{day.title}}</h2><p>{{day.summary}}</p></div><div class="timeline"><div v-for="item in day.items" :key="item.id" class="timeline-item"><time>{{item.start_time||item.startTime}}</time><div class="timeline-dot"></div><div><span>{{item.type}} · {{item.transport}}</span><h3>{{item.name}}</h3><p>{{item.description}}</p><small><el-icon><Location/></el-icon>{{item.address}} · 预计 ¥{{item.cost}}</small></div></div></div><div class="daily-cost">当日费用参考 <b>¥{{Number(day.calculated_cost||day.dailyCost||0).toLocaleString()}}</b></div></div></div></main><aside><div class="route-map"><span class="eyebrow">ROUTE OVERVIEW</span><h3>{{data.startPoint}} → {{data.endPoint}}</h3><div class="map-line"><i v-for="n in data.totalDays" :key="n"><b>{{n}}</b></i></div><p>这是不依赖付费地图 API 的静态路线示意。地点顺序以每日时间轴为准。</p></div><router-link :to="`/creator/${data.creator?.id}`" class="creator-mini"><img :src="data.creator?.avatar"><div><span>路线作者</span><b>{{data.creator?.nickname}}</b></div></router-link></aside></div></section></div></template>
<style scoped>.route-hero{min-height:570px;background-size:cover;background-position:center;color:white;display:flex;align-items:end;padding-bottom:60px}.route-hero span{font-size:11px;letter-spacing:3px;color:#b9e6e8}.route-hero h1{font-family:Georgia,"Songti SC",serif;font-size:52px;max-width:820px;margin:12px 0}.route-hero p{max-width:720px;line-height:1.8;color:rgba(255,255,255,.8)}.hero-facts{display:flex;gap:25px;margin:25px 0}.hero-facts b{display:flex;align-items:center;gap:7px}.hero-actions{display:flex;gap:10px}.hero-actions button{min-width:150px;border:1px solid rgba(255,255,255,.7);background:rgba(255,255,255,.12);color:white;border-radius:22px;padding:11px 18px;display:flex;justify-content:center;gap:7px;align-items:center;transition:.2s}.hero-actions button:first-child{background:white;color:#155d69}.hero-actions button.active{border-color:#63d0cc;background:#167c8c;color:white;font-weight:700}.hero-actions button:disabled{cursor:wait;opacity:.72}.route-layout{display:grid;grid-template-columns:minmax(0,780px) 290px;gap:70px}.day{display:grid;grid-template-columns:70px 1fr;gap:20px;margin-bottom:55px}.day-marker{background:#167c8c;color:white;width:62px;height:72px;border-radius:5px 5px 28px 28px;display:grid;place-content:center;text-align:center}.day-marker b{font-family:Georgia;font-size:22px}.day-marker span{font-size:8px;letter-spacing:2px}.day-title>span{color:#167c8c;font-size:11px;font-weight:700}.day-title h2{font-family:Georgia,"Songti SC",serif;font-size:28px;margin:6px 0}.day-title p{color:#6f8589;line-height:1.7}.timeline{margin-top:24px}.timeline-item{display:grid;grid-template-columns:50px 14px 1fr;gap:12px;position:relative;padding-bottom:30px}.timeline-item time{font-family:Georgia;font-size:13px;color:#6c8387}.timeline-dot{width:10px;height:10px;border:2px solid #167c8c;border-radius:50%;margin-top:3px;background:white}.timeline-dot:after{content:"";position:absolute;width:1px;background:#c9dddb;top:13px;bottom:0;margin-left:2px}.timeline-item>div:last-child>span{font-size:10px;color:#167c8c}.timeline-item h3{margin:5px 0}.timeline-item p{font-size:13px;color:#677e83;line-height:1.7}.timeline-item small{display:flex;align-items:center;gap:5px;color:#94a2a4}.daily-cost{background:#f0f6f4;padding:13px;border-radius:8px;text-align:right;color:#6b8185}.daily-cost b{color:#df7d4c;font-size:18px}.route-layout aside{position:sticky;top:100px;align-self:start}.route-map{background:#e8f4f2;padding:23px;border-radius:16px}.route-map h3{font-family:Georgia,"Songti SC",serif}.route-map p{font-size:12px;color:#6c8185;line-height:1.7}.map-line{display:flex;align-items:center;margin:25px 0}.map-line i{flex:1;height:2px;background:#75b8b6;position:relative}.map-line i b{position:absolute;width:22px;height:22px;border-radius:50%;background:#167c8c;color:white;display:grid;place-items:center;top:-10px;left:0;font-style:normal;font-size:9px}.creator-mini{display:flex;gap:12px;align-items:center;margin-top:15px;background:white;border:1px solid #dce8e7;border-radius:13px;padding:14px}.creator-mini img{width:48px;height:48px;border-radius:50%}.creator-mini span{display:block;font-size:10px;color:#8b9b9d}.creator-mini b{display:block;margin-top:5px}@media(max-width:850px){.route-hero h1{font-size:38px}.hero-facts{flex-wrap:wrap}.route-layout{grid-template-columns:1fr}.route-layout aside{position:static}.day{grid-template-columns:52px 1fr;gap:10px}.day-marker{width:46px;height:58px}.timeline-item{grid-template-columns:42px 12px 1fr}}</style>
