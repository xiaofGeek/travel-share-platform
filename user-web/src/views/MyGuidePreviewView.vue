<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, Calendar, Clock, Location, Wallet } from '@element-plus/icons-vue'
import api from '../services/api.js'
import { confirmGuideDeletion, isDeleteCancelled } from '../services/guideDeletion.js'

const route = useRoute()
const router = useRouter()
const data = ref(null)
const loading = ref(true)
const loadError = ref('')
const coverBroken = ref(false)
const deleting = ref(false)

const statusMap = {
  DRAFT: {
    label: '草稿',
    tone: 'draft',
    title: '这篇攻略还保存在草稿箱',
    message: '当前内容仅你本人可见，完善后可提交内容审核。'
  },
  PENDING: {
    label: '待审核',
    tone: 'pending',
    title: '攻略已提交，正在等待审核',
    message: '审核通过后才会出现在公开攻略页面，当前预览仅你本人可见。'
  },
  REJECTED: {
    label: '审核未通过',
    tone: 'rejected',
    title: '这篇攻略需要修改后重新提交',
    message: '请根据审核意见调整内容，未通过审核前不会公开展示。'
  },
  PUBLISHED: {
    label: '已发布',
    tone: 'published',
    title: '这篇攻略已经公开发布',
    message: '所有旅行者都可以在公开攻略页面查看这篇内容。'
  },
  OFFLINE: {
    label: '已下架',
    tone: 'offline',
    title: '这篇攻略当前已下架',
    message: '内容当前不会公开展示，请根据处理原因修改原稿后重新提交审核。'
  }
}

const status = computed(() => statusMap[data.value?.status] || {
  label: data.value?.status || '未知状态',
  tone: 'offline',
  title: '攻略状态待确认',
  message: '请返回我的攻略刷新状态，或稍后重试。'
})
const canEdit = computed(() => ['DRAFT', 'REJECTED', 'OFFLINE'].includes(data.value?.status))

const load = async () => {
  loading.value = true
  loadError.value = ''
  data.value = null
  coverBroken.value = false
  try {
    data.value = await api.get(`/user/guides/${route.params.id}`)
  } catch (error) {
    loadError.value = error.message || '攻略加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const remove = async () => {
  try {
    await confirmGuideDeletion(data.value)
    deleting.value = true
    await api.delete(`/user/guides/${data.value.id}`)
    ElMessage.success('攻略已删除')
    await router.replace('/user/guides')
  } catch (error) {
    if (!isDeleteCancelled(error)) ElMessage.error(error.message || '删除失败，请稍后重试')
  } finally {
    deleting.value = false
  }
}

watch(() => route.params.id, load, { immediate: true })
</script>

<template>
  <div v-if="loading" class="loading-panel"><el-skeleton :rows="14" animated /></div>
  <section v-else-if="loadError" class="preview-error container">
    <span>!</span>
    <h1>无法打开这篇攻略</h1>
    <p>{{ loadError }}</p>
    <router-link to="/user/guides">返回我的攻略</router-link>
  </section>
  <article v-else-if="data" class="owner-preview">
    <section :class="['status-banner', status.tone]">
      <div class="container status-inner">
        <div>
          <span class="status-label">{{ status.label }}</span>
          <h1>{{ status.title }}</h1>
          <p>{{ status.message }}</p>
          <p v-if="['REJECTED', 'OFFLINE'].includes(data.status) && data.auditOpinion" class="audit-opinion">
            {{ data.status === 'OFFLINE' ? '处理原因' : '审核意见' }}：{{ data.auditOpinion }}
          </p>
        </div>
        <div class="status-actions">
          <router-link to="/user/guides" class="secondary-action"><el-icon><Back /></el-icon>返回我的攻略</router-link>
          <router-link v-if="canEdit" :to="`/user/guide/${data.id}/edit`" class="primary-action">{{ data.status === 'DRAFT' ? '继续编辑' : '修改并重新提交' }}</router-link>
          <router-link v-if="data.status === 'PUBLISHED'" :to="`/guide/${data.id}`" class="primary-action">查看公开页面</router-link>
          <button type="button" class="danger-action" :disabled="deleting" @click="remove">{{ deleting ? '删除中…' : '删除攻略' }}</button>
        </div>
      </div>
    </section>

    <header class="preview-header container">
      <span class="eyebrow">AUTHOR PREVIEW · 作者预览</span>
      <h1>{{ data.title }}</h1>
      <p>{{ data.subtitle }}</p>
      <div class="preview-meta">
        <span><el-icon><Location /></el-icon>{{ data.destination?.name || '未设置目的地' }}</span>
        <span><el-icon><Calendar /></el-icon>{{ data.days || 0 }} 天</span>
        <span><el-icon><Wallet /></el-icon>人均 ¥{{ Number(data.budget || 0).toLocaleString() }}</span>
        <span><el-icon><Clock /></el-icon>更新于 {{ data.updateTime?.replace('T', ' ').slice(0, 16) }}</span>
      </div>
    </header>

    <div class="preview-cover container" :class="{ broken: coverBroken }">
      <img v-if="data.coverImage && !coverBroken" :src="data.coverImage" :alt="data.title" @error="coverBroken = true">
      <div v-else class="cover-placeholder"><span>山海迹</span><p>封面图片暂时无法显示</p></div>
    </div>

    <div class="preview-layout container">
      <aside>
        <span>攻略信息</span>
        <dl>
          <div><dt>推荐月份</dt><dd>{{ data.months || '未填写' }}</dd></div>
          <div><dt>出行方式</dt><dd>{{ data.travelMode || '未填写' }}</dd></div>
          <div><dt>适合人群</dt><dd>{{ data.audience || '未填写' }}</dd></div>
          <div v-if="data.topic"><dt>旅行专题</dt><dd>{{ data.topic.name }}</dd></div>
        </dl>
      </aside>
      <main>
        <p class="summary">{{ data.summary }}</p>
        <div class="rich-content" v-html="data.content"></div>
        <section v-if="data.expenses" class="info-box">
          <h2>费用参考</h2>
          <p>{{ data.expenses }}</p>
        </section>
        <section v-if="data.tips" class="info-box warning">
          <h2>出发前提醒</h2>
          <p>{{ data.tips }}</p>
        </section>
      </main>
    </div>
  </article>
</template>

<style scoped>
.owner-preview{background:#f7f9f7;padding-bottom:90px}.status-banner{border-bottom:1px solid;padding:26px 0}.status-banner.draft{background:#edf4f4;border-color:#d6e5e4}.status-banner.pending{background:#fff7e7;border-color:#f0dcae}.status-banner.rejected{background:#fff0ed;border-color:#efc6bd}.status-banner.published{background:#eaf6ef;border-color:#c8e4d3}.status-banner.offline{background:#f0f2f3;border-color:#d8dfe0}.status-inner{display:flex;justify-content:space-between;align-items:center;gap:35px}.status-label{display:inline-flex;border-radius:20px;background:white;padding:5px 12px;color:#48646a;font-size:12px;font-weight:800}.status-inner h1{font:26px Georgia,"Songti SC",serif;margin:9px 0 5px}.status-inner p{margin:0;color:#60767a;font-size:13px}.audit-opinion{margin-top:10px!important;padding-left:12px;border-left:3px solid #d96d54;color:#a04535!important}.status-actions{display:flex;align-items:center;gap:10px;flex:none}.status-actions a,.status-actions button{height:39px;padding:0 16px;border-radius:20px;display:flex;align-items:center;gap:6px;font-size:13px;font-weight:700}.secondary-action{background:white;border:1px solid #cadbd9;color:#42636a}.primary-action{background:#167c8c;color:white}.danger-action{border:1px solid #d99184;background:white;color:#b44f3d}.danger-action:hover{background:#fff0ed}.danger-action:disabled{cursor:wait;opacity:.65}.preview-header{text-align:center;padding:54px 0 32px}.preview-header h1{font:46px/1.35 Georgia,"Songti SC",serif;margin:12px auto;max-width:920px}.preview-header>p{color:#71878b;font-size:17px}.preview-meta{margin-top:24px;display:flex;justify-content:center;flex-wrap:wrap;gap:20px;color:#617a7f;font-size:12px}.preview-meta span{display:flex;align-items:center;gap:5px}.preview-cover{height:560px;padding:0;border-radius:20px;overflow:hidden;background:#dceae9}.preview-cover img{width:100%;height:100%;object-fit:cover}.cover-placeholder{height:100%;display:grid;place-content:center;text-align:center;background:linear-gradient(145deg,#d7eced,#9bc9cb);color:#255b65}.cover-placeholder span{font:38px Georgia,"Songti SC",serif;letter-spacing:8px}.cover-placeholder p{margin:8px 0;color:#4c747b}.preview-layout{display:grid;grid-template-columns:220px minmax(0,760px);justify-content:center;gap:70px;padding-top:55px}.preview-layout aside{align-self:start;position:sticky;top:100px;border-top:2px solid #17353d;padding-top:17px}.preview-layout aside>span{font-size:11px;letter-spacing:2px;color:#167c8c;font-weight:800}.preview-layout dl{margin-top:18px}.preview-layout dl>div{padding:13px 0;border-bottom:1px solid #dce6e5}.preview-layout dt{font-size:10px;color:#8a9c9f}.preview-layout dd{margin:5px 0 0;font-size:13px;color:#284a51}.summary{font:21px/1.9 Georgia,"Songti SC",serif;color:#36585f;border-left:4px solid #f2c46d;padding-left:24px;margin-top:0}.rich-content{font-size:16px;line-height:2;color:#395b62;min-height:100px}.rich-content :deep(h2){font:30px Georgia,"Songti SC",serif;margin-top:45px;color:#17353d}.rich-content :deep(img){width:100%;height:auto;border-radius:15px;margin:18px auto}.info-box{background:#eaf5f3;border-radius:14px;padding:23px;margin-top:28px}.info-box.warning{background:#fcf3df}.info-box h2{font:21px Georgia,"Songti SC",serif;margin:0 0 10px}.info-box p{margin:0;white-space:pre-line;color:#526e74;line-height:1.9}.preview-error{min-height:620px;display:grid;place-content:center;justify-items:center;text-align:center}.preview-error>span{width:52px;height:52px;border-radius:50%;display:grid;place-items:center;background:#fff0ed;color:#d06450;font-size:28px;font-weight:700}.preview-error h1{font:32px Georgia,"Songti SC",serif;margin:18px 0 6px}.preview-error p{color:#74898d}.preview-error a{margin-top:15px;background:#167c8c;color:white;border-radius:20px;padding:10px 18px;font-weight:700}@media(max-width:800px){.status-inner{align-items:flex-start;flex-direction:column}.status-actions{flex-wrap:wrap}.preview-header{padding-left:16px;padding-right:16px}.preview-header h1{font-size:34px}.preview-cover{height:350px;border-radius:0;width:100%}.preview-layout{grid-template-columns:1fr;gap:25px;padding-top:30px}.preview-layout aside{position:static}.preview-meta{gap:10px 15px}}
</style>
