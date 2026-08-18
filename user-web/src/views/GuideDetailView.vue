<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { View, Star, Pointer, Share, Calendar, Wallet, Location, Warning } from '@element-plus/icons-vue'
import api from '../services/api.js'
import GuideCard from '../components/GuideCard.vue'
import SectionTitle from '../components/SectionTitle.vue'
import { useAuthStore } from '../stores/auth.js'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const data = ref(null)
const loading = ref(true)
const loadError = ref('')
const comment = ref('')
const commenting = ref(false)
const liked = ref(false)
const favorited = ref(false)
const followed = ref(false)
const liking = ref(false)
const favoriting = ref(false)
const following = ref(false)
const reportDialog = ref(false)
const reporting = ref(false)
const reportForm = ref({ targetType: 'GUIDE', targetId: null, targetName: '', reason: '', description: '' })
const reportReasons = ['内容不实', '广告营销', '不友善内容', '侵权或抄袭', '危险行为', '重复内容', '其他']
const isOwnAuthor = computed(() => auth.loggedIn && Number(auth.user?.id) === Number(data.value?.author?.id))

const loadState = async () => {
  if (!auth.loggedIn || !data.value) return
  try {
    const state = await api.get(`/user/guides/${data.value.id}/state`)
    liked.value = Boolean(state.liked)
    favorited.value = Boolean(state.favorited)
    followed.value = Boolean(state.followed)
  } catch {
    liked.value = false
    favorited.value = false
    followed.value = false
  }
}

const load = async () => {
  loading.value = true
  loadError.value = ''
  data.value = null
  try {
    data.value = await api.get(`/public/guides/${route.params.id}`)
    await loadState()
  } catch (error) {
    loadError.value = error.message || '攻略暂时无法加载，请稍后重试'
  } finally {
    loading.value = false
  }
}

watch(() => route.params.id, load, { immediate: true })

const requireLogin = () => {
  if (auth.loggedIn) return true
  router.push(`/login?redirect=${route.fullPath}`)
  return false
}

const like = async () => {
  if (!requireLogin()) return
  liking.value = true
  try {
    const result = await api.post(`/user/guides/${data.value.id}/like`)
    liked.value = Boolean(result.liked)
    data.value.likeCount = result.likeCount
    ElMessage.success(liked.value ? '已点赞' : '已取消点赞')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    liking.value = false
  }
}

const favorite = async () => {
  if (!requireLogin()) return
  favoriting.value = true
  try {
    const result = await api.post(`/user/favorites/GUIDE/${data.value.id}`)
    favorited.value = Boolean(result.favorited)
    data.value.favoriteCount = result.favoriteCount
    ElMessage.success(favorited.value ? '已收藏' : '已取消收藏')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    favoriting.value = false
  }
}

const follow = async () => {
  if (!requireLogin()) return
  following.value = true
  try {
    const result = await api.post(`/user/follow/${data.value.author.id}`)
    followed.value = Boolean(result.followed)
    ElMessage.success(followed.value ? '已关注作者' : '已取消关注')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    following.value = false
  }
}

const submitComment = async () => {
  if (!comment.value.trim() || !requireLogin()) return
  commenting.value = true
  try {
    await api.post('/user/comments', { guideId: data.value.id, content: comment.value })
    comment.value = ''
    ElMessage.success('评论发布成功')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    commenting.value = false
  }
}

const share = async () => {
  await navigator.clipboard?.writeText(location.href)
  ElMessage.success('链接已复制')
}

const openReport = (targetType, target) => {
  if (!requireLogin()) return
  const ownerId = targetType === 'GUIDE' ? data.value?.author?.id : target?.user_id
  if (Number(ownerId) === Number(auth.user?.id)) {
    ElMessage.info('不能举报自己发布的内容')
    return
  }
  reportForm.value = {
    targetType,
    targetId: targetType === 'GUIDE' ? data.value.id : target.id,
    targetName: targetType === 'GUIDE' ? `攻略《${data.value.title}》` : `“${target.content.slice(0, 30)}${target.content.length > 30 ? '…' : ''}”`,
    reason: '',
    description: ''
  }
  reportDialog.value = true
}

const submitReport = async () => {
  if (!reportForm.value.reason) {
    ElMessage.warning('请选择举报原因')
    return
  }
  reporting.value = true
  try {
    await api.post('/user/reports', {
      targetType: reportForm.value.targetType,
      targetId: reportForm.value.targetId,
      reason: reportForm.value.reason,
      description: reportForm.value.description.trim()
    })
    reportDialog.value = false
    ElMessage.success('举报已提交，将进入人工核查，不会自动删除内容')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    reporting.value = false
  }
}
</script>
<template><div v-if="loading" class="loading-panel"><el-skeleton :rows="14" animated/></div><section v-else-if="loadError" class="detail-error container"><span>!</span><h1>这篇攻略暂时无法公开查看</h1><p>{{loadError}}</p><small>如果这是你刚提交的攻略，它需要审核通过后才会公开；作者可在“我的攻略”中查看审核状态和内容预览。</small><div><router-link v-if="auth.loggedIn" to="/user/guides" class="primary-error-action">查看我的攻略</router-link><router-link to="/guides" class="secondary-error-action">浏览公开攻略</router-link></div></section><article v-else-if="data" class="guide-detail"><header class="article-header container"><div class="breadcrumb"><router-link to="/guides">旅行攻略</router-link><span>/</span><router-link :to="`/destination/${data.destination?.id}`">{{data.destination?.name}}</router-link></div><span class="eyebrow">{{data.destination?.name}} · {{data.months}}</span><h1>{{data.title}}</h1><p class="subtitle">{{data.subtitle}}</p><div class="article-meta"><router-link :to="`/creator/${data.author?.id}`" class="article-author"><img :src="data.author?.avatar"><div><b>{{data.author?.nickname}}</b><span>{{data.publishedAt?.slice(0,10)}} 发布</span></div></router-link><button v-if="!isOwnAuthor" :class="{active:followed}" :aria-pressed="followed" :disabled="following" @click="follow">{{following?'处理中…':followed?'✓ 已关注':'+ 关注'}}</button><div class="meta-counts"><span><el-icon><View/></el-icon>{{data.viewCount}}</span><span><el-icon><Pointer/></el-icon>{{data.likeCount}}</span><span><el-icon><Star/></el-icon>{{data.favoriteCount}}</span></div></div></header><div class="cover-wrap"><img :src="data.coverImage" :alt="data.title"></div><div class="article-layout container"><aside class="article-aside"><div><el-icon><Calendar/></el-icon><span>出行天数<b>{{data.days}} 天</b></span></div><div><el-icon><Wallet/></el-icon><span>人均预算<b>¥{{Number(data.budget).toLocaleString()}}</b></span></div><div><el-icon><Location/></el-icon><span>目的地<b>{{data.destination?.name}}</b></span></div><p>适合：{{data.audience}}</p><p>方式：{{data.travelMode}}</p></aside><main class="article-content"><p class="lead">{{data.summary}}</p><div class="rich-content" v-html="data.content"></div><section class="info-box"><h3>费用参考</h3><p>{{data.expenses}}</p></section><section class="info-box warning"><h3>出发前再看一遍</h3><p>{{data.tips}}</p></section><div class="article-tags"><span v-for="tag in data.tags" :key="tag"># {{tag}}</span></div><div class="article-actions"><button :class="{active:liked}" :aria-pressed="liked" :disabled="liking" @click="like"><el-icon><Pointer/></el-icon>{{liked?'已点赞':'点赞'}} {{data.likeCount}}</button><button :class="{active:favorited}" :aria-pressed="favorited" :disabled="favoriting" @click="favorite"><el-icon><Star/></el-icon>{{favorited?'已收藏':'收藏'}} {{data.favoriteCount}}</button><button @click="share"><el-icon><Share/></el-icon>分享</button><button v-if="!isOwnAuthor" class="report-action" @click="openReport('GUIDE',data)"><el-icon><Warning/></el-icon>举报</button></div><section class="comments"><h2>旅行者留言 <small>{{data.commentCount}}</small></h2><div class="comment-form"><img :src="auth.user?.avatar||'/uploads/demo/avatars/avatar-004.png'"><el-input v-model="comment" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="写下你的问题或旅行经验……"/><el-button type="primary" :loading="commenting" @click="submitComment">发布评论</el-button></div><div v-for="item in data.comments" :key="item.id" class="comment"><img :src="item.avatar"><div><b>{{item.nickname}}</b><button v-if="Number(item.user_id)!==Number(auth.user?.id)" class="comment-report" @click="openReport('COMMENT',item)">举报</button><p>{{item.content}}</p><span>{{item.create_time}} · {{item.like_count}} 人赞同</span></div></div></section></main></div><section class="section soft"><div class="container"><SectionTitle eyebrow="MORE STORIES" title="也许你还会喜欢"/><div class="guide-grid"><GuideCard v-for="guide in data.related" :key="guide.id" :guide="guide"/></div></div></section></article><el-dialog v-model="reportDialog" title="提交内容举报" width="min(540px,95%)" :close-on-click-modal="false"><p class="report-target">举报对象：{{reportForm.targetName}}</p><el-alert title="举报提交后只会进入人工核查，不会自动删除或下架内容。" type="info" :closable="false" show-icon/><el-form label-position="top" class="report-form"><el-form-item label="举报原因（必选）"><el-select v-model="reportForm.reason" placeholder="请选择最符合的原因"><el-option v-for="reason in reportReasons" :key="reason" :label="reason" :value="reason"/></el-select></el-form-item><el-form-item label="补充说明"><el-input v-model="reportForm.description" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="请说明具体问题，方便审核人员核查"/></el-form-item></el-form><template #footer><el-button @click="reportDialog=false">取消</el-button><el-button type="danger" :loading="reporting" @click="submitReport">确认举报</el-button></template></el-dialog></template>
<style scoped>.detail-error{min-height:620px;display:grid;place-content:center;justify-items:center;text-align:center;padding-top:50px;padding-bottom:50px}.detail-error>span{width:54px;height:54px;border-radius:50%;display:grid;place-items:center;background:#fff0ed;color:#d06450;font-size:28px;font-weight:800}.detail-error h1{font:34px Georgia,"Songti SC",serif;margin:18px 0 7px}.detail-error p{margin:0;color:#5f777c}.detail-error small{display:block;max-width:610px;margin-top:12px;color:#87989b;line-height:1.8}.detail-error>div{display:flex;gap:10px;margin-top:24px}.detail-error a{border-radius:21px;padding:10px 18px;font-size:13px;font-weight:700}.primary-error-action{background:#167c8c;color:white}.secondary-error-action{border:1px solid #cbdcda;background:white;color:#42636a}.article-header{padding:55px 0 36px;text-align:center}.breadcrumb{font-size:12px;color:#84969a;margin-bottom:25px}.breadcrumb span{margin:0 8px}.article-header h1{font-family:Georgia,"Songti SC",serif;font-size:48px;line-height:1.35;max-width:950px;margin:12px auto}.subtitle{font-size:18px;color:#75898c}.article-meta{margin-top:28px;display:flex;justify-content:center;align-items:center;gap:16px}.article-author{display:flex;align-items:center;text-align:left;gap:10px}.article-author img{width:44px;height:44px;border-radius:50%;object-fit:cover}.article-author span{display:block;font-size:11px;color:#8b9a9d;margin-top:5px}.article-meta>button{min-width:90px;border:1px solid #167c8c;color:#167c8c;background:white;border-radius:18px;padding:7px 14px}.article-meta>button.active{background:#167c8c;color:white}.article-meta>button:disabled,.article-actions button:disabled{cursor:wait;opacity:.7}.meta-counts{display:flex;gap:15px;color:#788b8f;font-size:12px;margin-left:15px}.meta-counts span{display:flex;align-items:center;gap:4px}.cover-wrap{width:min(1280px,calc(100% - 40px));margin:auto;height:620px;border-radius:22px;overflow:hidden}.cover-wrap img{width:100%;height:100%;object-fit:cover}.article-layout{display:grid;grid-template-columns:220px minmax(0,760px);gap:70px;justify-content:center;padding-top:60px}.article-aside{position:sticky;top:100px;align-self:start;border-top:2px solid #17353d;padding-top:20px}.article-aside>div{display:flex;gap:11px;align-items:center;margin-bottom:22px;color:#167c8c}.article-aside span{font-size:11px;color:#7b8d91}.article-aside b{display:block;color:#17353d;margin-top:4px;font-size:14px}.article-aside p{font-size:12px;color:#6b8185}.lead{font-family:Georgia,"Songti SC",serif;font-size:22px;line-height:1.9;color:#36585f;border-left:4px solid #f2c46d;padding-left:25px}.rich-content{font-size:16px;line-height:2;color:#395b62}.rich-content :deep(h2){font-family:Georgia,"Songti SC",serif;font-size:30px;margin-top:48px;color:#17353d}.rich-content :deep(img){width:100%;border-radius:16px;margin-top:20px}.rich-content :deep(figcaption){text-align:center;color:#8a9b9e;font-size:12px}.info-box{background:#eaf5f3;border-radius:14px;padding:24px;margin-top:32px}.info-box.warning{background:#fcf3df}.info-box h3{font-family:Georgia,"Songti SC",serif;margin-top:0}.info-box p{line-height:1.9;color:#526e74}.article-tags{margin:30px 0}.article-tags span{display:inline-block;background:#eef4f3;color:#167c8c;padding:7px 13px;border-radius:18px;margin:5px}.article-actions{display:flex;justify-content:center;flex-wrap:wrap;gap:12px;border-top:1px solid #e0e8e7;border-bottom:1px solid #e0e8e7;padding:25px}.article-actions button{min-width:112px;border:1px solid #cddfdd;background:white;border-radius:24px;padding:10px 19px;display:flex;justify-content:center;align-items:center;gap:6px;color:#365b62;transition:.2s}.article-actions button:hover{color:#167c8c;border-color:#167c8c}.article-actions button.active{border-color:#167c8c;background:#e5f3f1;color:#0f6f7b;font-weight:700}.article-actions .report-action{min-width:auto;color:#9b5a4f;border-color:#e5c9c3}.comments{padding:45px 0}.comments h2{font-family:Georgia,"Songti SC",serif;font-size:28px}.comments small{color:#9aabac}.comment-form{display:grid;grid-template-columns:46px 1fr auto;gap:12px;align-items:start;background:#f2f6f5;padding:18px;border-radius:14px}.comment-form>img,.comment>img{width:42px;height:42px;border-radius:50%;object-fit:cover}.comment{display:flex;gap:14px;padding:22px 0;border-bottom:1px solid #e3eae9}.comment>div{flex:1;position:relative}.comment p{line-height:1.7;margin:8px 0}.comment span{font-size:11px;color:#8a9a9d}.comment-report{position:absolute;right:0;top:0;border:0;background:transparent;color:#98a7a9;font-size:11px}.comment-report:hover{color:#b34f3e}.report-target{background:#f2f6f5;border-radius:9px;padding:11px 13px;color:#4d6b71}.report-form{margin-top:18px}.report-form :deep(.el-select){width:100%}@media(max-width:800px){.article-header{padding-left:16px;padding-right:16px}.article-header h1{font-size:34px}.article-meta{flex-wrap:wrap}.cover-wrap{height:360px}.article-layout{grid-template-columns:1fr;gap:20px;padding-top:30px}.article-aside{position:static;display:grid;grid-template-columns:repeat(3,1fr);border-top:0;border-bottom:1px solid #dfe8e7}.article-aside p{display:none}.comment-form{grid-template-columns:38px 1fr}.comment-form button{grid-column:2;justify-self:end}.detail-error>div{flex-wrap:wrap;justify-content:center}}
</style>
