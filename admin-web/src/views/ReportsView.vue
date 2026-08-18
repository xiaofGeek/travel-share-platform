<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../services/api.js'

const rows = ref([])
const loading = ref(false)
const saving = ref(false)
const dialog = ref(false)
const current = ref(null)
const filter = ref('ALL')
const form = ref({ result: 'VALID', note: '' })

const statusLabels = { PENDING: '待处理', VALID: '举报成立', INVALID: '举报不成立', CLOSED: '已关闭' }
const targetLabels = { GUIDE: '攻略', COMMENT: '评论' }
const filteredRows = computed(() => filter.value === 'ALL' ? rows.value : rows.value.filter(item => item.status === filter.value))
const pendingCount = computed(() => rows.value.filter(item => item.status === 'PENDING').length)
const impactText = computed(() => {
  if (form.value.result === 'INVALID') return '认定举报不成立：原内容保持不变，举报人会收到处理结果。'
  if (form.value.result === 'CLOSED') return '关闭举报：不改变内容状态，适用于重复举报或目标已处理。'
  if (current.value?.target_type === 'COMMENT') return '认定举报成立：该评论将被删除，攻略评论数同步扣减，并通知评论发布者和举报人。'
  return '认定举报成立：该攻略将立即下架并取消精选/置顶，作者可修改后重新提交审核；作者和举报人都会收到通知。'
})

const load = async () => {
  loading.value = true
  try {
    rows.value = await api.get('/admin/reports')
  } catch (error) {
    rows.value = []
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

onMounted(load)

const open = row => {
  if (row.status !== 'PENDING') return
  current.value = row
  const active = row.target_status === 'PUBLISHED' || row.target_status === 'NORMAL'
  form.value = { result: active ? 'VALID' : 'CLOSED', note: '' }
  dialog.value = true
}

const submit = async () => {
  if (!form.value.note.trim()) {
    ElMessage.warning('请填写处理说明')
    return
  }
  if (form.value.result === 'VALID') {
    try {
      await ElMessageBox.confirm(impactText.value, '确认执行内容处置', { type: 'warning', confirmButtonText: '确认处理', cancelButtonText: '返回检查' })
    } catch {
      return
    }
  }
  saving.value = true
  try {
    await api.post(`/admin/reports/${current.value.id}/handle`, { ...form.value, note: form.value.note.trim() })
    ElMessage.success('举报处理完成，处置结果和通知已同步')
    dialog.value = false
    await load()
  } catch (error) {
    ElMessage.error(error.message)
    await load()
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div>
    <div class="page-title"><div><h1>举报处理中心</h1><p>攻略和评论举报统一进入人工核查；提交举报本身不会自动删除内容</p></div><el-tag type="danger">待处理 {{ pendingCount }}</el-tag></div>
    <section class="panel">
      <div class="table-toolbar"><el-radio-group v-model="filter"><el-radio-button label="ALL">全部</el-radio-button><el-radio-button label="PENDING">待处理</el-radio-button><el-radio-button label="VALID">举报成立</el-radio-button><el-radio-button label="INVALID">举报不成立</el-radio-button><el-radio-button label="CLOSED">已关闭</el-radio-button></el-radio-group><span class="filter-count">当前 {{ filteredRows.length }} 条</span></div>
      <el-table v-loading="loading" :data="filteredRows" stripe>
        <el-table-column prop="id" label="编号" width="70" />
        <el-table-column label="举报人" width="120"><template #default="scope"><b>{{ scope.row.reporter_name || `用户 #${scope.row.reporter_id}` }}</b></template></el-table-column>
        <el-table-column label="举报对象" min-width="230"><template #default="scope"><div class="target"><span>{{ targetLabels[scope.row.target_type] || scope.row.target_type }} #{{ scope.row.target_id }}</span><b>{{ scope.row.target_title }}</b><small>{{ scope.row.target_content || '目标内容当前不可用' }}</small></div></template></el-table-column>
        <el-table-column prop="target_status" label="对象状态" width="100" />
        <el-table-column prop="reason" label="举报原因" width="115" />
        <el-table-column prop="description" label="举报说明" min-width="220" />
        <el-table-column prop="status" label="处理状态" width="110"><template #default="scope"><el-tag :type="scope.row.status === 'PENDING' ? 'warning' : scope.row.status === 'VALID' ? 'danger' : scope.row.status === 'INVALID' ? 'success' : 'info'" size="small">{{ statusLabels[scope.row.status] || scope.row.status }}</el-tag></template></el-table-column>
        <el-table-column prop="create_time" label="举报时间" width="165" />
        <el-table-column label="操作" width="110" fixed="right"><template #default="scope"><el-button link type="primary" :disabled="scope.row.status !== 'PENDING'" @click="open(scope.row)">{{ scope.row.status === 'PENDING' ? '核查处理' : '已处理' }}</el-button></template></el-table-column>
      </el-table>
    </section>
    <el-dialog v-model="dialog" title="核查并处理举报" width="620px" :close-on-click-modal="false">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="举报对象">{{ current.target_title }}（{{ targetLabels[current.target_type] }} #{{ current.target_id }}）</el-descriptions-item>
        <el-descriptions-item label="对象状态">{{ current.target_status || '已不存在' }}</el-descriptions-item>
        <el-descriptions-item label="举报原因">{{ current.reason }}：{{ current.description || '未补充说明' }}</el-descriptions-item>
        <el-descriptions-item label="内容摘要">{{ current.target_content || '目标内容当前不可用' }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-position="top" class="report-form"><el-form-item label="处理结果"><el-radio-group v-model="form.result"><el-radio label="VALID">举报成立</el-radio><el-radio label="INVALID">举报不成立</el-radio><el-radio label="CLOSED">关闭举报</el-radio></el-radio-group></el-form-item><el-alert :title="impactText" :type="form.result === 'VALID' ? 'warning' : 'info'" :closable="false" show-icon /><el-form-item label="处理说明（必填）" class="note"><el-input v-model="form.note" type="textarea" :rows="5" maxlength="500" show-word-limit placeholder="写明核查依据和处置原因，用户将看到该说明" /></el-form-item></el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button :type="form.result === 'VALID' ? 'danger' : 'primary'" :loading="saving" @click="submit">确认处理</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.filter-count{margin-left:auto;color:#829397;font-size:11px;align-self:center}.target{display:grid;gap:4px}.target>span{font-size:10px;color:#16808e;font-weight:700}.target>b{font-size:13px}.target>small{color:#819397;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.report-form{margin-top:18px}.note{margin-top:18px}
</style>
