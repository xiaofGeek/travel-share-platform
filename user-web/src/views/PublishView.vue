<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../services/api.js'
import RichEditor from '../components/RichEditor.vue'

const route = useRoute()
const router = useRouter()
const destinations = ref([])
const topics = ref([])
const saving = ref(false)
const loading = ref(false)
const preview = ref(false)
const originalStatus = ref('')
const persistedId = ref(route.params.id ? Number(route.params.id) : null)
const form = reactive({
  title: '', subtitle: '', coverImage: '', summary: '', destinationId: '', topicId: '',
  days: 3, budget: 2000, months: '3-5月、9-11月', travelMode: '公共交通',
  audience: '第一次自由行', content: '', expenses: '', tips: ''
})

const isEditing = computed(() => Boolean(persistedId.value))
const editLabel = computed(() => originalStatus.value === 'REJECTED' ? '修改被驳回的攻略' : originalStatus.value === 'OFFLINE' ? '修改已下架的攻略' : '继续编辑攻略')
const editableStatuses = new Set(['DRAFT', 'REJECTED', 'OFFLINE'])
const requestFields = ['title', 'subtitle', 'coverImage', 'summary', 'destinationId', 'topicId', 'days', 'budget', 'months', 'travelMode', 'audience', 'content', 'expenses', 'tips']

onMounted(async () => {
  loading.value = true
  try {
    const requests = [api.get('/public/destinations'), api.get('/public/topics')]
    if (persistedId.value) requests.push(api.get(`/user/guides/${persistedId.value}`))
    const [destinationData, topicData, guide] = await Promise.all(requests)
    destinations.value = destinationData
    topics.value = topicData
    if (guide) {
      if (!editableStatuses.has(guide.status)) {
        ElMessage.warning('当前攻略正在审核或已经发布，暂时不能修改')
        await router.replace(`/user/guide/${guide.id}`)
        return
      }
      originalStatus.value = guide.status
      requestFields.forEach(key => { form[key] = guide[key] ?? (key === 'topicId' ? '' : form[key]) })
    }
  } catch (error) {
    ElMessage.error(error.message)
    if (persistedId.value) await router.replace('/user/guides')
  } finally {
    loading.value = false
  }
})

const upload = async event => {
  const file = event.target.files?.[0]
  if (!file) return
  const body = new FormData()
  body.append('file', file)
  body.append('category', 'guide-cover')
  try {
    const result = await api.post('/user/upload', body)
    form.coverImage = result.url
    ElMessage.success('封面上传成功')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const save = async submit => {
  saving.value = true
  let contentSaved = false
  const editingExisting = Boolean(persistedId.value)
  try {
    const guide = persistedId.value
      ? await api.put(`/user/guides/${persistedId.value}`, form)
      : await api.post('/user/guides', form)
    persistedId.value = guide.id
    contentSaved = true
    if (submit) await api.post(`/user/guides/${guide.id}/submit`)
    ElMessage.success(submit
      ? editingExisting ? '已重新提交审核，审核通过后将公开发布' : '已提交审核，审核通过后将公开发布'
      : originalStatus.value === 'REJECTED' || originalStatus.value === 'OFFLINE'
        ? '修改已保存，尚未重新提交审核'
        : '草稿已保存，仅你本人可见')
    await router.push(`/user/guide/${guide.id}`)
  } catch (error) {
    if (submit && contentSaved) {
      ElMessage.error(`${editingExisting ? '修改' : '草稿'}已保存，但提交审核失败：${error.message}`)
      await router.push(`/user/guide/${persistedId.value}`)
    } else {
      ElMessage.error(error.message)
    }
  } finally {
    saving.value = false
  }
}
</script>
<template><div class="publish-page" v-loading="loading"><header><div class="container"><span class="eyebrow">CREATOR STUDIO</span><h1>{{isEditing?editLabel:'写一篇真正有用的旅行攻略'}}</h1><p>{{isEditing?'修改会保存在原稿件中，重新提交后进入新一轮审核。':'路线、预算、交通、图片与容易踩坑的细节，都是下一位旅行者最需要的信息。'}}</p></div></header><div class="container editor-layout"><main><section class="form-section"><h2>01 · 基本信息</h2><el-form label-position="top"><el-form-item label="攻略标题"><el-input v-model="form.title" size="large" maxlength="100" show-word-limit placeholder="例如：第一次去成都，三天两夜这样安排更轻松"/></el-form-item><el-form-item label="副标题"><el-input v-model="form.subtitle" maxlength="140" placeholder="用一句话说明这篇攻略的特色"/></el-form-item><el-form-item label="攻略摘要"><el-input v-model="form.summary" type="textarea" :rows="4" maxlength="500" show-word-limit/></el-form-item><div class="form-grid"><el-form-item label="目的地"><el-select v-model="form.destinationId" filterable><el-option v-for="d in destinations" :key="d.id" :label="d.name" :value="d.id"/></el-select></el-form-item><el-form-item label="旅行专题"><el-select v-model="form.topicId" clearable><el-option v-for="t in topics" :key="t.id" :label="t.name" :value="t.id"/></el-select></el-form-item><el-form-item label="出行天数"><el-input-number v-model="form.days" :min="1" :max="60"/></el-form-item><el-form-item label="人均预算"><el-input-number v-model="form.budget" :min="0" :step="100"/></el-form-item><el-form-item label="推荐月份"><el-input v-model="form.months"/></el-form-item><el-form-item label="出行方式"><el-select v-model="form.travelMode"><el-option v-for="x in ['公共交通','高铁+步行','自驾','飞机+公交']" :key="x" :label="x" :value="x"/></el-select></el-form-item><el-form-item label="适合人群"><el-select v-model="form.audience"><el-option v-for="x in ['第一次自由行','学生党','情侣','亲子家庭','摄影爱好者']" :key="x" :label="x" :value="x"/></el-select></el-form-item></div></el-form></section><section class="form-section"><h2>02 · 封面图片</h2><label class="cover-upload" :style="form.coverImage?{backgroundImage:`url(${form.coverImage})`}:{}"><input hidden type="file" accept="image/*" @change="upload"><span>{{form.coverImage?'更换封面':'点击上传横版攻略封面'}}</span></label></section><section class="form-section"><h2>03 · 图文正文</h2><RichEditor v-model="form.content"/></section><section class="form-section"><h2>04 · 费用与提醒</h2><el-form label-position="top"><el-form-item label="费用参考"><el-input v-model="form.expenses" type="textarea" :rows="4" placeholder="分别说明交通、住宿、餐饮与体验费用"/></el-form-item><el-form-item label="注意事项"><el-input v-model="form.tips" type="textarea" :rows="4" placeholder="天气、交通、安全、开放时间等提醒"/></el-form-item></el-form></section></main><aside><div class="publish-help"><b>发布检查清单</b><ul><li>标题自然，不夸大事实</li><li>封面与正文图片来源清晰</li><li>路线和交通衔接完整</li><li>预算只作合理参考</li><li>容易变化的信息注明以最新公告为准</li></ul></div></aside></div><div class="publish-bar"><div class="container"><span>{{isEditing?'保存仍使用原稿件，不会重复新建攻略':'内容会自动进入个人草稿箱'}}</span><div><el-button @click="preview=true">预览</el-button><el-button :loading="saving" @click="save(false)">{{isEditing?'保存修改':'保存草稿'}}</el-button><el-button type="primary" :loading="saving" @click="save(true)">{{isEditing?'重新提交审核':'提交审核'}}</el-button></div></div></div><el-dialog v-model="preview" width="min(850px,95%)" title="攻略预览"><article class="preview"><img v-if="form.coverImage" :src="form.coverImage"><h1>{{form.title||'攻略标题'}}</h1><p>{{form.summary}}</p><div v-html="form.content"></div></article></el-dialog></div></template>
<style scoped>.publish-page{background:#f4f7f6;padding-bottom:90px}.publish-page>header{background:#153f47;color:white;padding:55px 0}.publish-page h1{font:38px Georgia,"Songti SC",serif;margin:10px 0}.publish-page header p{color:#a8c1c5}.editor-layout{display:grid;grid-template-columns:minmax(0,820px) 260px;gap:30px;padding-top:35px}.form-section{background:white;border:1px solid #dce7e6;border-radius:16px;padding:28px;margin-bottom:20px}.form-section h2{font:23px Georgia,"Songti SC",serif;margin:0 0 25px}.form-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:0 16px}.form-grid :deep(.el-select){width:100%}.cover-upload{height:320px;border:2px dashed #c9dcda;border-radius:13px;display:grid;place-items:center;background-size:cover;background-position:center;overflow:hidden}.cover-upload span{background:rgba(15,64,72,.78);color:white;padding:10px 18px;border-radius:20px}.publish-help{position:sticky;top:100px;background:#e5f2f0;border-radius:16px;padding:22px}.publish-help li{font-size:12px;color:#60797e;line-height:2.2}.publish-bar{position:fixed;z-index:20;left:0;right:0;bottom:0;background:rgba(255,255,255,.96);border-top:1px solid #dce6e5;padding:12px}.publish-bar>div{display:flex;justify-content:space-between;align-items:center}.publish-bar span{font-size:12px;color:#75898d}.preview>img{width:100%;max-height:420px;object-fit:cover;border-radius:12px}.preview h1{color:#17353d}.preview :deep(img){max-width:100%}@media(max-width:850px){.editor-layout{grid-template-columns:1fr}.editor-layout aside{display:none}.form-grid{grid-template-columns:1fr}.publish-bar span{display:none}.publish-bar>div{justify-content:flex-end}}</style>
