import { ElMessageBox } from 'element-plus'

const warningFor = guide => {
  if (guide.status === 'PUBLISHED') {
    return `删除后，《${guide.title}》将立即从公开页面、搜索结果、目的地、专题和作者主页移除，相关点赞、收藏与评论也不再展示。用户侧不可恢复，确认删除吗？`
  }
  if (guide.status === 'PENDING') {
    return `删除后将撤回《${guide.title}》的本次审核并移除原稿，用户侧不可恢复，确认删除吗？`
  }
  return `删除《${guide.title}》后用户侧不可恢复，确认继续吗？`
}

export const confirmGuideDeletion = guide => ElMessageBox.confirm(
  warningFor(guide),
  '确认删除攻略',
  {
    type: 'warning',
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    confirmButtonClass: 'el-button--danger',
    distinguishCancelAndClose: true
  }
)

export const isDeleteCancelled = error => error === 'cancel' || error === 'close'
