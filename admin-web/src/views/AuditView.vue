<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../services/api.js'
import SafeImage from '../components/SafeImage.vue'

const pendingRows = ref([])
const approvedRows = ref([])
const tab = ref('PENDING')
const loading = ref(false)
const saving = ref(false)
const dialog = ref(false)
const current = ref(null)
const decision = ref('APPROVED')
const opinion = ref('内容结构完整，路线、预算与注意事项说明清楚，符合发布规范。')
const rows = computed(() => tab.value === 'PENDING' ? pendingRows.value : approvedRows.value)

const load = async () => {
  loading.value = true
  try {
    const [pending, approved] = await Promise.all([
      api.get('/admin/guides', { params: { status: 'PENDING', page: 1, size: 100 } }),
      api.get('/admin/guides', { params: { status: 'APPROVED', page: 1, size: 100 } })
    ])
    pendingRows.value = pending.records || []
    approvedRows.value = approved.records || []
  } catch (error) {
    pendingRows.value = []
    approvedRows.value = []
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

onMounted(load)

const open = (row, result) => {
  if (result !== 'REVOKED' && (row.status !== 'PENDING' || row.auditStatus !== 'PENDING')) {
    ElMessage.warning('该攻略已被处理，请刷新列表')
    return
  }
  if (result === 'REVOKED' && (row.status !== 'PUBLISHED' || row.auditStatus !== 'APPROVED')) {
    ElMessage.warning('只有正在公开的已通过攻略可以撤销通过')
    return
  }
  current.value = row
  decision.value = result
  opinion.value = result === 'APPROVED'
    ? '内容结构完整，路线、预算与注意事项说明清楚，符合发布规范。'
    : result === 'REJECTED'
      ? '请补充路线衔接、费用说明或图片来源后重新提交。'
      : '复核发现内容仍需修改，现撤销通过并下架，请作者修改后重新提交。'
  dialog.value = true
}

const submit = async () => {
  if (!opinion.value.trim()) {
    ElMessage.warning(decision.value === 'REVOKED' ? '请填写撤销原因' : '请填写审核意见')
    return
  }
  saving.value = true
  try {
    if (decision.value === 'REVOKED') {
      await api.post(`/admin/guides/${current.value.id}/revoke`, { reason: opinion.value.trim() })
      ElMessage.success('已撤销通过并下架，作者将收到修改通知')
    } else {
      await api.post(`/admin/guides/${current.value.id}/audit`, { decision: decision.value, opinion: opinion.value.trim() })
      ElMessage.success('审核结果已提交，作者将收到站内消息')
    }
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
    <div class="page-title">
      <div><h1>攻略审核工作台</h1><p>待审核内容可通过或驳回；已发布内容如需更正，必须使用留痕的“撤销通过并下架”</p></div>
      <el-tag type="warning" effect="dark">{{ pendingRows.length }} 篇待审核</el-tag>
    </div>
    <div class="audit-summary">
      <div><b>{{ pendingRows.length }}</b><span>等待审核</span></div>
      <div><b>不可覆盖</b><span>同一轮审核结果只允许提交一次</span></div>
      <div><b>全程留痕</b><span>审核、撤销与原因均写入记录</span></div>
    </div>
    <section class="panel">
      <div class="table-toolbar">
        <el-radio-group v-model="tab">
          <el-radio-button label="PENDING">待审核 {{ pendingRows.length }}</el-radio-button>
          <el-radio-button label="APPROVED">已通过 {{ approvedRows.length }}</el-radio-button>
        </el-radio-group>
        <span class="flow-tip">通过后不能再次“驳回”；发现问题请撤销通过并下架</span>
      </div>
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="封面" width="105"><template #default="scope"><SafeImage class="cover" :src="scope.row.coverImage" alt="攻略封面" /></template></el-table-column>
        <el-table-column prop="title" label="攻略标题" min-width="250"><template #default="scope"><b>{{ scope.row.title }}</b><small class="sub">{{ scope.row.summary }}</small></template></el-table-column>
        <el-table-column prop="authorId" label="作者 ID" width="90" />
        <el-table-column prop="destinationId" label="目的地 ID" width="100" />
        <el-table-column prop="days" label="天数" width="70" />
        <el-table-column prop="budget" label="预算" width="90" />
        <el-table-column :prop="tab === 'PENDING' ? 'createTime' : 'publishedAt'" :label="tab === 'PENDING' ? '提交时间' : '发布时间'" width="165" />
        <el-table-column label="审核操作" width="205" fixed="right">
          <template #default="scope">
            <template v-if="tab === 'PENDING'">
              <el-button type="success" size="small" :disabled="saving" @click="open(scope.row, 'APPROVED')">通过</el-button>
              <el-button type="danger" plain size="small" :disabled="saving" @click="open(scope.row, 'REJECTED')">驳回</el-button>
            </template>
            <el-button v-else-if="scope.row.status === 'PUBLISHED'" type="danger" plain size="small" :disabled="saving" @click="open(scope.row, 'REVOKED')">撤销通过并下架</el-button>
            <el-tag v-else type="info" size="small">{{ scope.row.status === 'OFFLINE' ? '已下架' : scope.row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </section>
    <el-dialog v-model="dialog" :title="decision === 'APPROVED' ? '确认通过攻略' : decision === 'REJECTED' ? '驳回并退回作者' : '撤销通过并下架'" width="560px" :close-on-click-modal="false">
      <el-alert v-if="decision === 'REVOKED'" title="该操作会立即停止公开展示，作者可修改原稿并重新提交审核。" type="warning" :closable="false" show-icon />
      <el-descriptions v-if="current" :column="1" border class="guide-summary"><el-descriptions-item label="攻略">{{ current.title }}</el-descriptions-item><el-descriptions-item label="当前状态">{{ current.status }} / {{ current.auditStatus }}</el-descriptions-item></el-descriptions>
      <el-form label-position="top" class="opinion"><el-form-item :label="decision === 'REVOKED' ? '撤销原因（必填）' : '审核意见（必填）'"><el-input v-model="opinion" type="textarea" :rows="5" maxlength="500" show-word-limit /></el-form-item></el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button :type="decision === 'APPROVED' ? 'success' : 'danger'" :loading="saving" @click="submit">{{ decision === 'REVOKED' ? '确认撤销并下架' : '提交审核结果' }}</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.audit-summary{display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin-bottom:16px}.audit-summary>div{background:white;border:1px solid #dce5e4;border-radius:11px;padding:17px}.audit-summary b{font:21px Georgia;display:block}.audit-summary span{font-size:10px;color:#7d9093}.cover{width:74px;height:48px;border-radius:6px;object-fit:cover}.sub{display:block;color:#8a9b9e;font-weight:400;margin-top:5px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.opinion{margin-top:20px}.guide-summary{margin-top:16px}.flow-tip{margin-left:auto;align-self:center;color:#7f9093;font-size:11px}
</style>
