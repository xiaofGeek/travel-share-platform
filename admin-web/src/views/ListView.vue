<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../services/api.js'
import { useAdminAuth } from '../stores/auth.js'
import SafeImage from '../components/SafeImage.vue'

const route = useRoute()
const auth = useAdminAuth()
const loading = ref(false)
const dialogLoading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const keyword = ref('')
const page = ref(1)
const size = ref(20)
const rows = ref([])
const total = ref(0)
const formDialog = ref(false)
const detailDialog = ref(false)
const editId = ref(null)
const detailData = ref(null)
const formRef = ref()
const form = reactive({})
let loadSequence = 0

const imageField = (key, label, fallback) => ({ key, label, type: 'image', fallback, full: true })
const textField = (key, label, required = false, extra = {}) => ({ key, label, type: 'text', required, ...extra })
const numberField = (key, label, defaultValue = 0, min = 0) => ({ key, label, type: 'number', default: defaultValue, min })
const switchField = (key, label, defaultValue = 1) => ({ key, label, type: 'switch', default: defaultValue })
const selectField = (key, label, options, required = false, defaultValue = '') => ({ key, label, type: 'select', options, required, default: defaultValue })

const configs = {
  destinations: {
    url: '/admin/destinations', creatable: true, editable: true, toggle: 'recommend',
    cols: [['coverImage', '封面', 'image'], ['name', '目的地'], ['nameEn', '英文名'], ['type', '类型'], ['guideCount', '攻略数'], ['viewCount', '浏览量'], ['recommended', '推荐']],
    fields: [
      textField('code', '目的地编码', true, { placeholder: '例如 HANGZHOU' }),
      textField('name', '目的地名称', true), textField('nameEn', '英文名'),
      selectField('type', '目的地类型', [['CITY', '城市'], ['ISLAND', '海岛'], ['ANCIENT_TOWN', '古镇'], ['NATURE', '自然风光']], true, 'CITY'),
      imageField('coverImage', '封面图片', '/uploads/demo/placeholders/placeholder-002.png'),
      textField('summary', '简介', false, { type: 'textarea', full: true, rows: 2 }),
      textField('description', '详细介绍', false, { type: 'textarea', full: true, rows: 4 }),
      textField('season', '推荐季节'), numberField('suggestedDays', '建议天数', 1, 1),
      numberField('averageBudget', '人均预算', 0, 0), textField('tags', '标签', false, { placeholder: '用英文逗号分隔' }),
      textField('locationText', '位置说明'), numberField('sortOrder', '排序', 0, 0),
      switchField('recommended', '首页推荐', 0), switchField('enabled', '启用', 1)
    ]
  },
  guides: {
    url: '/admin/guides', toggle: 'offline',
    cols: [['coverImage', '封面', 'image'], ['title', '攻略标题'], ['status', '状态'], ['auditStatus', '审核状态'], ['viewCount', '浏览'], ['likeCount', '点赞'], ['publishedAt', '发布时间']]
  },
  users: {
    url: '/admin/users', toggle: 'status',
    cols: [['avatar', '头像', 'image'], ['username', '账号'], ['nickname', '昵称'], ['role', '角色'], ['city', '城市'], ['guideCount', '攻略'], ['status', '状态']]
  },
  routes: {
    url: '/admin/routes', creatable: true, editable: true,
    cols: [['coverImage', '封面', 'image'], ['name', '路线名称'], ['totalDays', '天数'], ['budget', '预算'], ['status', '状态'], ['isPublic', '公开'], ['viewCount', '浏览']],
    fields: [
      textField('routeNo', '路线编号', false, { placeholder: '留空时自动生成' }), textField('name', '路线名称', true),
      numberField('destinationId', '主目的地 ID', null, 1), numberField('totalDays', '行程天数', 1, 1),
      imageField('coverImage', '封面图片', '/uploads/demo/placeholders/placeholder-003.png'),
      numberField('budget', '预算', 0, 0), textField('startPoint', '起点'), textField('endPoint', '终点'),
      textField('season', '适合季节'), textField('audience', '适合人群'),
      textField('summary', '路线简介', false, { type: 'textarea', full: true, rows: 3 }),
      selectField('status', '路线状态', [['PUBLISHED', '已发布'], ['DRAFT', '草稿'], ['OFFLINE', '已下架']], true, 'PUBLISHED'),
      switchField('isPublic', '公开展示', 1)
    ]
  },
  topics: {
    url: '/admin/topics', creatable: true, editable: true,
    cols: [['coverImage', '封面', 'image'], ['name', '专题名称'], ['subtitle', '副标题'], ['recommended', '推荐'], ['enabled', '启用'], ['sortOrder', '排序']],
    fields: [
      textField('name', '专题名称', true), textField('subtitle', '副标题'),
      imageField('coverImage', '封面图片', '/uploads/demo/placeholders/placeholder-004.png'),
      textField('summary', '专题简介', false, { type: 'textarea', full: true, rows: 2 }),
      textField('content', '专题内容', false, { type: 'textarea', full: true, rows: 4 }),
      numberField('sortOrder', '排序', 0, 0), switchField('recommended', '首页推荐', 0), switchField('enabled', '启用', 1)
    ]
  },
  comments: {
    url: '/admin/comments', deletable: true,
    cols: [['content', '评论内容'], ['guideId', '攻略 ID'], ['userId', '用户 ID'], ['likeCount', '点赞'], ['status', '状态'], ['createTime', '发布时间']]
  },
  banners: {
    url: '/admin/banners', creatable: true, editable: true, toggle: 'status',
    cols: [['imageUrl', '图片', 'image'], ['title', '标题'], ['subtitle', '副标题'], ['linkUrl', '跳转'], ['sortOrder', '排序'], ['enabled', '启用']],
    fields: [
      textField('title', '轮播标题', true), textField('subtitle', '副标题'),
      imageField('imageUrl', '轮播图片', '/uploads/demo/placeholders/placeholder-005.png'),
      textField('linkUrl', '站内跳转地址', false, { placeholder: '例如 /destinations' }),
      numberField('sortOrder', '排序', 0, 0), switchField('enabled', '启用', 1)
    ]
  },
  recommendations: {
    url: '/admin/recommendations', creatable: true, editable: true,
    cols: [['positionCode', '推荐位'], ['targetType', '类型'], ['targetId', '目标 ID'], ['title', '标题'], ['sortOrder', '排序'], ['enabled', '启用']],
    fields: [
      selectField('positionCode', '推荐位', [['HOME_DESTINATION', '首页目的地'], ['HOME_FEATURED_GUIDE', '首页精选攻略'], ['HOME_ROUTE', '首页路线']], true, 'HOME_DESTINATION'),
      selectField('targetType', '目标类型', [['DESTINATION', '目的地'], ['GUIDE', '攻略'], ['ROUTE', '路线']], true, 'DESTINATION'),
      numberField('targetId', '目标 ID', null, 1), textField('title', '展示标题'),
      numberField('sortOrder', '排序', 0, 0), switchField('enabled', '启用', 1)
    ]
  },
  configs: {
    url: '/admin/configs', creatable: true, editable: true,
    cols: [['configKey', '参数键'], ['configName', '参数名称'], ['configValue', '参数值'], ['configType', '类型'], ['updateTime', '更新时间']],
    fields: [
      textField('configKey', '参数键', true, { placeholder: '例如 site.name' }), textField('configName', '参数名称', true),
      textField('configValue', '参数值', true, { type: 'textarea', full: true, rows: 3 }),
      selectField('configType', '参数类型', [['STRING', '字符串'], ['BOOLEAN', '布尔值'], ['NUMBER', '数字'], ['JSON', 'JSON']], true, 'STRING'),
      textField('remark', '备注', false, { type: 'textarea', full: true, rows: 2 })
    ]
  },
  logs: {
    url: '/admin/logs',
    cols: [['username', '用户'], ['module', '模块'], ['operation', '操作'], ['requestUri', '请求地址'], ['status', '状态'], ['createTime', '时间']]
  }
}

const resource = computed(() => route.meta.resource)
const config = computed(() => configs[resource.value] || { url: '', cols: [], fields: [] })
const canCreate = computed(() => auth.isAdmin && config.value.creatable)
const canEdit = computed(() => auth.isAdmin && config.value.editable)
const canDelete = computed(() => auth.isAdmin && config.value.deletable)
const hasToggle = computed(() => auth.isAdmin && Boolean(config.value.toggle))
const formTitle = computed(() => `${editId.value ? '编辑' : '新增'}${route.meta.title?.replace('管理', '') || '记录'}`)

const cell = (row, key) => row?.[key] ?? row?.[key.replace(/[A-Z]/g, letter => `_${letter.toLowerCase()}`)]

const load = async () => {
  const active = config.value
  if (!active.url) return
  const sequence = ++loadSequence
  loading.value = true
  try {
    const data = await api.get(active.url, { params: { page: page.value, size: size.value, keyword: keyword.value.trim() || undefined } })
    if (sequence !== loadSequence) return
    rows.value = Array.isArray(data) ? data : (data.records || [])
    total.value = Array.isArray(data) ? data.length : Number(data.total || 0)
  } catch (error) {
    if (sequence === loadSequence) {
      rows.value = []
      total.value = 0
      ElMessage.error(error.message)
    }
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}

watch(resource, next => {
  if (!next || !configs[next]) return
  keyword.value = ''
  page.value = 1
  load()
}, { immediate: true })

const resetForm = source => {
  Object.keys(form).forEach(key => delete form[key])
  for (const field of config.value.fields || []) {
    const raw = source ? cell(source, field.key) : undefined
    form[field.key] = raw ?? field.default ?? (field.type === 'switch' ? 0 : '')
  }
}

const openCreate = async () => {
  editId.value = null
  resetForm(null)
  formDialog.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const openEdit = async row => {
  dialogLoading.value = true
  try {
    const data = await api.get(`${config.value.url}/${row.id}`)
    editId.value = row.id
    resetForm(data)
    formDialog.value = true
    await nextTick()
    formRef.value?.clearValidate()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    dialogLoading.value = false
  }
}

const openDetail = async row => {
  dialogLoading.value = true
  try {
    detailData.value = await api.get(`${config.value.url}/${row.id}`)
    detailDialog.value = true
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    dialogLoading.value = false
  }
}

const rules = computed(() => Object.fromEntries((config.value.fields || []).filter(field => field.required).map(field => [field.key, [{ required: true, message: `请填写${field.label}`, trigger: field.type === 'select' ? 'change' : 'blur' }]])))

const submit = async () => {
  try {
    await formRef.value?.validate()
    saving.value = true
    if (editId.value) await api.put(`${config.value.url}/${editId.value}`, { ...form })
    else await api.post(config.value.url, { ...form })
    ElMessage.success(editId.value ? '记录已保存' : '记录已新增')
    formDialog.value = false
    page.value = 1
    await load()
  } catch (error) {
    if (error instanceof Error) ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

const uploadImage = async (request, key) => {
  const body = new FormData()
  body.append('file', request.file)
  body.append('category', resource.value)
  uploading.value = true
  try {
    const result = await api.post('/admin/upload', body)
    form[key] = result.url
    request.onSuccess?.(result)
    ElMessage.success('图片上传成功')
  } catch (error) {
    request.onError?.(error)
    ElMessage.error(error.message)
  } finally {
    uploading.value = false
  }
}

const cancelled = error => ['cancel', 'close'].includes(error) || ['cancel', 'close'].includes(error?.action)

const toggle = async row => {
  try {
    if (resource.value === 'users') {
      await ElMessageBox.confirm(`确定要${Number(cell(row, 'status')) === 1 ? '停用' : '启用'}账号“${cell(row, 'username')}”吗？`, '用户状态确认', { type: 'warning' })
      await api.patch(`/admin/users/${row.id}/status`)
    } else if (resource.value === 'destinations') {
      await api.patch(`/admin/destinations/${row.id}/recommend`)
    } else if (resource.value === 'banners') {
      await api.patch(`/admin/banners/${row.id}/status`)
    } else if (resource.value === 'guides') {
      const result = await ElMessageBox.prompt('请输入本次下架的具体原因', '下架攻略', { inputValidator: value => Boolean(value?.trim()) || '必须填写原因', type: 'warning' })
      await api.post(`/admin/guides/${row.id}/offline`, { reason: result.value.trim() })
    }
    ElMessage.success('状态已更新')
    await load()
  } catch (error) {
    if (!cancelled(error)) ElMessage.error(error.message || '操作失败')
  }
}

const remove = async row => {
  const content = String(cell(row, 'content') || '').replace(/\s+/g, ' ').slice(0, 36)
  try {
    const result = await ElMessageBox.prompt(
      `确定删除评论“${content}${String(cell(row, 'content') || '').length > 36 ? '…' : ''}”吗？删除后用户端将不再显示。`,
      '删除评论',
      {
        type: 'warning',
        inputType: 'textarea',
        inputPlaceholder: '请输入删除原因（必填，最多 200 字）',
        inputValidator: value => {
          const reason = value?.trim() || ''
          if (!reason) return '必须填写删除原因'
          return reason.length <= 200 || '删除原因不能超过 200 个字符'
        },
        confirmButtonText: '确认删除',
        cancelButtonText: '取消'
      }
    )
    await api.delete(`/admin/comments/${row.id}`, { data: { reason: result.value.trim() } })
    ElMessage.success('评论已删除，作者已收到站内通知')
    await load()
  } catch (error) {
    if (!cancelled(error)) ElMessage.error(error.message || '删除失败')
  }
}

const statusLabels = {
  PUBLISHED: '已发布', DRAFT: '草稿', OFFLINE: '已下架', REJECTED: '已拒绝',
  PENDING: '待审核', APPROVED: '已通过', NOT_SUBMITTED: '未提交', NORMAL: '正常',
  ADMIN: '系统管理员', AUDITOR: '内容审核员', CREATOR: '创作者', USER: '普通用户'
}

const displayValue = (value, key) => {
  if (value === null || value === undefined || value === '') return '—'
  if (['enabled', 'recommended', 'isPublic', 'status'].includes(key) && [0, 1, '0', '1'].includes(value)) return Number(value) === 1 ? '是' : '否'
  return statusLabels[value] || String(value)
}

const labelFor = key => {
  const field = (config.value.fields || []).find(item => item.key === key)
  const column = (config.value.cols || []).find(item => item[0] === key || item[0].replace(/[A-Z]/g, letter => `_${letter.toLowerCase()}`) === key)
  const common = { id: 'ID', createTime: '创建时间', create_time: '创建时间', updateTime: '更新时间', update_time: '更新时间' }
  return field?.label || column?.[1] || common[key] || key.replaceAll('_', ' ')
}

const detailEntries = computed(() => {
  if (!detailData.value) return []
  const hidden = new Set(['password', 'deleted'])
  return Object.entries(detailData.value).filter(([key, value]) => !hidden.has(key) && value !== null && value !== '')
})

const isImageKey = key => ['coverImage', 'cover_image', 'imageUrl', 'image_url', 'avatar'].includes(key)
const isLongValue = (key, value) => ['content', 'description', 'summary', 'detail', 'auditOpinion', 'audit_opinion'].includes(key) || String(value).length > 100

const toggleLabel = row => {
  if (resource.value === 'guides') return '下架'
  if (resource.value === 'destinations') return Number(cell(row, 'recommended')) === 1 ? '取消推荐' : '设为推荐'
  return Number(cell(row, resource.value === 'users' ? 'status' : 'enabled')) === 1 ? '停用' : '启用'
}

const resetSearch = () => {
  keyword.value = ''
  page.value = 1
  load()
}
</script>

<template>
  <div>
    <div class="page-title">
      <div>
        <h1>{{ route.meta.title }}</h1>
        <p>数据来自后端接口；新增、编辑和状态修改都会实时同步到数据库</p>
      </div>
      <el-button v-if="canCreate" type="primary" @click="openCreate">+ 新增记录</el-button>
    </div>

    <section class="panel">
      <div class="table-toolbar">
        <el-input v-model="keyword" clearable placeholder="输入关键词搜索" @keyup.enter="page = 1; load()" />
        <el-button type="primary" @click="page = 1; load()">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
        <span class="result-count">共 {{ total }} 条数据</span>
      </div>

      <el-table v-loading="loading || dialogLoading" :data="rows" stripe height="calc(100vh - 250px)">
        <el-table-column type="selection" width="44" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column v-for="column in config.cols" :key="column[0]" :label="column[1]" :min-width="column[2] === 'image' ? 92 : (['title', 'content'].includes(column[0]) ? 220 : 110)" show-overflow-tooltip>
          <template #default="scope">
            <SafeImage v-if="column[2] === 'image'" class="table-image" :src="cell(scope.row, column[0])" :alt="column[1]" />
            <template v-else-if="['status', 'auditStatus', 'enabled', 'recommended', 'isPublic'].includes(column[0])">
              <span class="status-dot" :class="{ warn: ['PENDING', 0, '0'].includes(cell(scope.row, column[0])), off: ['OFFLINE', 'REJECTED'].includes(cell(scope.row, column[0])) }" />
              {{ displayValue(cell(scope.row, column[0]), column[0]) }}
            </template>
            <template v-else>{{ displayValue(cell(scope.row, column[0]), column[0]) }}</template>
          </template>
        </el-table-column>
        <el-table-column label="操作" :width="canEdit || hasToggle || canDelete ? 220 : 90" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openDetail(scope.row)">详情</el-button>
            <el-button v-if="canEdit" link type="primary" @click="openEdit(scope.row)">编辑</el-button>
            <el-button v-if="hasToggle && (resource !== 'guides' || cell(scope.row, 'status') === 'PUBLISHED')" link :type="resource === 'guides' ? 'danger' : 'warning'" @click="toggle(scope.row)">{{ toggleLabel(scope.row) }}</el-button>
            <el-button v-if="canDelete" link type="danger" @click="remove(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="size" v-model:current-page="page" @current-change="load" />
      </div>
    </section>

    <el-dialog v-model="formDialog" :title="formTitle" width="760px" destroy-on-close :close-on-click-modal="false">
      <el-form ref="formRef" v-loading="dialogLoading" :model="form" :rules="rules" label-position="top" class="record-form">
        <el-form-item v-for="field in config.fields" :key="field.key" :label="field.label" :prop="field.key" :class="{ full: field.full }">
          <el-input v-if="field.type === 'text' || field.type === 'textarea'" v-model="form[field.key]" :type="field.type === 'textarea' ? 'textarea' : 'text'" :rows="field.rows || 3" :placeholder="field.placeholder || `请输入${field.label}`" clearable />
          <el-input-number v-else-if="field.type === 'number'" v-model="form[field.key]" :min="field.min" :precision="['budget', 'averageBudget'].includes(field.key) ? 2 : 0" controls-position="right" />
          <el-select v-else-if="field.type === 'select'" v-model="form[field.key]" placeholder="请选择" filterable>
            <el-option v-for="option in field.options" :key="option[0]" :label="option[1]" :value="option[0]" />
          </el-select>
          <el-switch v-else-if="field.type === 'switch'" v-model="form[field.key]" :active-value="1" :inactive-value="0" active-text="是" inactive-text="否" />
          <div v-else-if="field.type === 'image'" class="image-editor">
            <SafeImage :src="form[field.key]" :fallback="field.fallback" :alt="field.label" />
            <div>
              <el-upload :show-file-list="false" accept="image/jpeg,image/png,image/webp,image/gif" :http-request="request => uploadImage(request, field.key)">
                <el-button type="primary" plain :loading="uploading">选择并上传图片</el-button>
              </el-upload>
              <el-input v-model="form[field.key]" placeholder="也可填写 /uploads/ 开头的本地图片地址" />
              <small>支持 JPG、PNG、WebP、GIF，单张不超过 10MB。</small>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存记录</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialog" title="记录详情" width="760px" destroy-on-close>
      <div v-if="detailData" class="detail-grid">
        <div v-for="entry in detailEntries" :key="entry[0]" :class="{ full: isImageKey(entry[0]) || isLongValue(entry[0], entry[1]) }">
          <span>{{ labelFor(entry[0]) }}</span>
          <SafeImage v-if="isImageKey(entry[0])" class="detail-image" :src="entry[1]" :alt="labelFor(entry[0])" />
          <pre v-else-if="isLongValue(entry[0], entry[1])">{{ entry[1] }}</pre>
          <b v-else>{{ displayValue(entry[1], entry[0]) }}</b>
        </div>
      </div>
      <template #footer><el-button type="primary" @click="detailDialog = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.result-count { margin-left: auto; color: #829397; font-size: 11px; align-self: center }
.pager { display: flex; justify-content: flex-end; margin-top: 16px }
.table-image { width: 62px; height: 42px; border-radius: 7px; object-fit: cover; display: block; background: #edf3f2 }
.record-form { display: grid; grid-template-columns: 1fr 1fr; gap: 0 20px }
.record-form .full { grid-column: 1 / -1 }
.record-form :deep(.el-input-number), .record-form :deep(.el-select) { width: 100% }
.image-editor { width: 100%; display: grid; grid-template-columns: 190px 1fr; gap: 18px; align-items: center; padding: 13px; border: 1px dashed #cbd8d7; border-radius: 10px; background: #f8fbfa }
.image-editor > img { width: 190px; height: 112px; object-fit: cover; border-radius: 8px; background: #e8efee }
.image-editor > div { display: flex; flex-direction: column; gap: 9px; align-items: flex-start }
.image-editor :deep(.el-input) { width: 100% }
.image-editor small { color: #829397 }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; max-height: 65vh; overflow: auto }
.detail-grid > div { padding: 12px 14px; border: 1px solid #e2eae9; border-radius: 8px; min-width: 0 }
.detail-grid > div.full { grid-column: 1 / -1 }
.detail-grid span { display: block; color: #7a8e92; font-size: 12px; margin-bottom: 6px }
.detail-grid b { font-size: 14px; font-weight: 500; color: #263f45; word-break: break-all }
.detail-grid pre { margin: 0; white-space: pre-wrap; word-break: break-word; font: 13px/1.7 "Microsoft YaHei", sans-serif; color: #354f55 }
.detail-image { width: min(100%, 520px); max-height: 280px; object-fit: cover; border-radius: 9px; background: #edf3f2 }
</style>
