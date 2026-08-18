<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../services/api.js'
import GuideCard from '../components/GuideCard.vue'
import SectionTitle from '../components/SectionTitle.vue'
import { useAuthStore } from '../stores/auth.js'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const data = ref(null)
const followed = ref(false)
const following = ref(false)
const isSelf = computed(() => auth.loggedIn && Number(auth.user?.id) === Number(data.value?.id))

const load = async () => {
  data.value = await api.get(`/public/creators/${route.params.id}`)
  if (auth.loggedIn && !isSelf.value) {
    try {
      const state = await api.get(`/user/follow/${route.params.id}/state`)
      followed.value = Boolean(state.followed)
    } catch {
      followed.value = false
    }
  }
}

onMounted(load)

const follow = async () => {
  if (!auth.loggedIn) return router.push(`/login?redirect=${route.fullPath}`)
  following.value = true
  try {
    const result = await api.post(`/user/follow/${route.params.id}`)
    followed.value = Boolean(result.followed)
    data.value.followerCount = result.followerCount
    ElMessage.success(followed.value ? '已关注该创作者' : '已取消关注')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    following.value = false
  }
}
</script>

<template>
  <div v-if="!data" class="loading-panel"><el-skeleton :rows="10" animated /></div>
  <div v-else>
    <section class="creator-hero" :style="{ backgroundImage: `linear-gradient(rgba(7,38,44,.2),rgba(7,38,44,.72)),url(${data.coverImage})` }">
      <div class="container creator-profile">
        <img :src="data.avatar" :alt="data.nickname">
        <div>
          <span>TRAVEL CREATOR</span>
          <h1>{{ data.nickname }}</h1>
          <p>{{ data.bio }}</p>
          <small>{{ data.city }} · {{ data.preferences }}</small>
        </div>
        <button v-if="!isSelf" :class="{ followed }" :aria-pressed="followed" :disabled="following" :title="followed ? '点击取消关注' : '关注创作者'" @click="follow">
          {{ following ? '处理中…' : followed ? '✓ 已关注' : '+ 关注创作者' }}
        </button>
      </div>
    </section>
    <div class="creator-stats">
      <div><b>{{ data.visitedCities }}</b><span>走过城市</span></div>
      <div><b>{{ data.guideCount }}</b><span>发布攻略</span></div>
      <div><b>{{ data.routeCount }}</b><span>公开路线</span></div>
      <div><b>{{ Number(data.followerCount).toLocaleString() }}</b><span>粉丝</span></div>
      <div><b>{{ Number(data.receivedLikes).toLocaleString() }}</b><span>累计获赞</span></div>
    </div>
    <section class="section">
      <div class="container">
        <SectionTitle eyebrow="TRAVEL STORIES" :title="`${data.nickname} 的旅行记录`" />
        <div class="guide-grid"><GuideCard v-for="guide in data.guides" :key="guide.id" :guide="guide" /></div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.creator-hero{height:450px;background-size:cover;background-position:center;display:flex;align-items:end;color:white;padding-bottom:45px}.creator-profile{display:grid;grid-template-columns:120px 1fr auto;gap:25px;align-items:center}.creator-profile>img{width:120px;height:120px;border-radius:50%;object-fit:cover;border:5px solid white}.creator-profile span{font-size:10px;letter-spacing:3px;color:#b9e9e9}.creator-profile h1{font:40px Georgia,"Songti SC",serif;margin:8px 0}.creator-profile p{margin:0 0 7px}.creator-profile small{color:rgba(255,255,255,.7)}.creator-profile button{min-width:126px;border:1px solid transparent;border-radius:20px;background:white;color:#167c8c;padding:11px 18px;font-weight:700;transition:.2s}.creator-profile button.followed{border-color:rgba(255,255,255,.72);background:rgba(6,52,61,.45);color:white}.creator-profile button:disabled{cursor:wait;opacity:.72}.creator-stats{display:flex;justify-content:center;gap:70px;background:white;border-bottom:1px solid #e2e9e8;padding:22px}.creator-stats div{text-align:center}.creator-stats b{display:block;font:24px Georgia;color:#17353d}.creator-stats span{font-size:11px;color:#829296}@media(max-width:700px){.creator-profile{grid-template-columns:80px 1fr}.creator-profile>img{width:80px;height:80px}.creator-profile button{grid-column:2;justify-self:start}.creator-profile h1{font-size:30px}.creator-stats{gap:24px;overflow:auto;justify-content:start;padding-left:25px}.creator-stats div{min-width:70px}}
</style>
