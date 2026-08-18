package com.travelshare.platform.service.impl;

import com.travelshare.platform.entity.Comment;
import com.travelshare.platform.entity.Guide;
import com.travelshare.platform.entity.User;
import com.travelshare.platform.dto.AuditRequest;
import com.travelshare.platform.exception.BusinessException;
import com.travelshare.platform.mapper.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {
    @Mock private AdminQueryMapper queryMapper;
    @Mock private GuideMapper guideMapper;
    @Mock private UserMapper userMapper;
    @Mock private DestinationMapper destinationMapper;
    @Mock private ActionMapper actionMapper;
    @Mock private RouteMapper routeMapper;
    @Mock private TopicMapper topicMapper;
    @Mock private CommentMapper commentMapper;
    @Mock private BannerMapper bannerMapper;
    @Mock private AdminContentMapper contentMapper;
    @InjectMocks private AdminServiceImpl service;

    @Test
    void deleteCommentSoftDeletesAndRecordsGovernanceActions() {
        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        Comment comment = new Comment();
        comment.setId(41L);
        comment.setGuideId(9L);
        comment.setUserId(18L);
        comment.setContent("测试评论");
        comment.setStatus("NORMAL");
        comment.setDeleted(0);

        when(userMapper.selectOne(any())).thenReturn(admin);
        when(commentMapper.selectById(41L)).thenReturn(comment);
        when(commentMapper.deleteById(41L)).thenReturn(1);

        service.deleteComment("admin", 41L, "  包含广告链接  ");

        assertEquals("DELETED", comment.getStatus());
        verify(commentMapper).updateById(comment);
        verify(commentMapper).detachReplies(41L);
        verify(commentMapper).deleteById(41L);
        verify(guideMapper).decrementCommentCount(9L);
        verify(actionMapper).addMessage(
                eq(18L), eq(1L), eq("评论处理通知"), contains("包含广告链接"),
                eq("COMMENT_REMOVED"), eq("COMMENT"), eq(41L));

        ArgumentCaptor<Map<String, Object>> log = ArgumentCaptor.forClass(Map.class);
        verify(contentMapper).addLog(log.capture());
        assertEquals("评论管理", log.getValue().get("module"));
        assertEquals("删除评论", log.getValue().get("operation"));
        assertEquals("删除原因：包含广告链接", log.getValue().get("detail"));
    }

    @Test
    void deleteCommentRequiresReason() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.deleteComment("admin", 41L, "  "));
        assertEquals(400, error.getCode());
        assertEquals("必须填写删除原因", error.getMessage());
    }

    @Test
    void auditGuideUsesAtomicPendingTransitionAndNotifiesAuthor() {
        User auditor = user(2L, "auditor01");
        Guide guide = guide(9L, 18L, "PENDING", "PENDING");
        when(userMapper.selectOne(any())).thenReturn(auditor);
        when(guideMapper.selectById(9L)).thenReturn(guide);
        when(guideMapper.applyAuditDecision(eq(9L), eq("REJECTED"), eq("REJECTED"), eq("图片来源不清晰"), isNull())).thenReturn(1);

        service.auditGuide("auditor01", 9L, new AuditRequest("REJECTED", "图片来源不清晰"));

        verify(guideMapper).applyAuditDecision(eq(9L), eq("REJECTED"), eq("REJECTED"), eq("图片来源不清晰"), isNull());
        verify(actionMapper).addAudit("GUIDE", 9L, 2L, "REJECTED", "图片来源不清晰");
        verify(actionMapper).addMessage(eq(18L), eq(2L), eq("攻略审核结果"), contains("REJECTED"), eq("AUDIT_RESULT"), eq("GUIDE"), eq(9L));
    }

    @Test
    void auditGuideDoesNotWriteSideEffectsWhenAnotherAuditorWon() {
        User auditor = user(2L, "auditor01");
        Guide guide = guide(9L, 18L, "PENDING", "PENDING");
        when(userMapper.selectOne(any())).thenReturn(auditor);
        when(guideMapper.selectById(9L)).thenReturn(guide);
        when(guideMapper.applyAuditDecision(eq(9L), anyString(), anyString(), anyString(), any())).thenReturn(0);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.auditGuide("auditor01", 9L, new AuditRequest("APPROVED", "符合规范")));

        assertEquals("该攻略已被其他审核员处理，请刷新列表", error.getMessage());
        verify(actionMapper, never()).addAudit(anyString(), anyLong(), anyLong(), anyString(), anyString());
        verify(actionMapper, never()).addMessage(anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void auditGuideCannotOverwriteAnApprovedDecisionWithRejection() {
        User auditor = user(2L, "auditor01");
        Guide guide = guide(9L, 18L, "PUBLISHED", "APPROVED");
        when(userMapper.selectOne(any())).thenReturn(auditor);
        when(guideMapper.selectById(9L)).thenReturn(guide);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.auditGuide("auditor01", 9L, new AuditRequest("REJECTED", "事后发现问题")));

        assertEquals("该攻略不在待审核状态", error.getMessage());
        verify(guideMapper, never()).applyAuditDecision(anyLong(), anyString(), anyString(), anyString(), any());
        verify(actionMapper, never()).addAudit(anyString(), anyLong(), anyLong(), anyString(), anyString());
        verify(actionMapper, never()).addMessage(anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void revokeApprovalOfflinesPublishedGuideAndReturnsItToAuthor() {
        User auditor = user(2L, "auditor01");
        Guide guide = guide(9L, 18L, "PUBLISHED", "APPROVED");
        when(userMapper.selectOne(any())).thenReturn(auditor);
        when(guideMapper.selectById(9L)).thenReturn(guide);
        when(guideMapper.offlinePublished(9L, "撤销通过：图片授权需要补充")).thenReturn(1);

        service.revokeGuideApproval("auditor01", 9L, "图片授权需要补充");

        verify(guideMapper).offlinePublished(9L, "撤销通过：图片授权需要补充");
        verify(actionMapper).addAudit("GUIDE", 9L, 2L, "REVOKED", "图片授权需要补充");
        verify(actionMapper).addMessage(eq(18L), eq(2L), contains("撤销通过"), contains("修改原稿"), eq("AUDIT_REVOKED"), eq("GUIDE"), eq(9L));
    }

    @Test
    void validGuideReportOfflinesContentAndNotifiesBothSides() {
        User handler = user(2L, "auditor01");
        Guide guide = guide(9L, 18L, "PUBLISHED", "APPROVED");
        Map<String,Object> report = Map.of("id", 31L, "reporter_id", 7L, "target_type", "GUIDE", "target_id", 9L, "status", "PENDING");
        when(actionMapper.reportById(31L)).thenReturn(report);
        when(userMapper.selectOne(any())).thenReturn(handler);
        when(guideMapper.selectById(9L)).thenReturn(guide);
        when(guideMapper.offlinePublished(9L, "举报核查成立：存在商业广告")).thenReturn(1);
        when(actionMapper.handleReport(31L, "VALID", "存在商业广告", 2L)).thenReturn(1);

        service.handleReport("auditor01", 31L, "VALID", "存在商业广告");

        verify(guideMapper).offlinePublished(9L, "举报核查成立：存在商业广告");
        verify(actionMapper).addMessage(eq(18L), eq(2L), eq("内容下架通知"), contains("举报核查成立"), eq("REPORT_VALID"), eq("GUIDE"), eq(9L));
        verify(actionMapper).addMessage(eq(7L), eq(2L), eq("举报处理结果"), contains("举报成立"), eq("REPORT_RESULT"), eq("GUIDE"), eq(9L));
    }

    @Test
    void validCommentReportDeletesCommentAndNotifiesBothSides() {
        User handler = user(2L, "auditor01");
        Comment comment = new Comment();
        comment.setId(41L);
        comment.setGuideId(9L);
        comment.setUserId(18L);
        comment.setContent("违规评论");
        comment.setStatus("NORMAL");
        comment.setDeleted(0);
        Map<String,Object> report = Map.of("id", 31L, "reporter_id", 7L, "target_type", "COMMENT", "target_id", 41L, "status", "PENDING");
        when(actionMapper.reportById(31L)).thenReturn(report);
        when(userMapper.selectOne(any())).thenReturn(handler);
        when(commentMapper.selectById(41L)).thenReturn(comment);
        when(commentMapper.deleteById(41L)).thenReturn(1);
        when(actionMapper.handleReport(31L, "VALID", "包含人身攻击", 2L)).thenReturn(1);

        service.handleReport("auditor01", 31L, "VALID", "包含人身攻击");

        assertEquals("DELETED", comment.getStatus());
        verify(commentMapper).updateById(comment);
        verify(commentMapper).detachReplies(41L);
        verify(commentMapper).deleteById(41L);
        verify(guideMapper).decrementCommentCount(9L);
        verify(actionMapper).addMessage(eq(18L), eq(2L), eq("评论处理通知"), contains("举报核查成立"), eq("REPORT_VALID"), eq("COMMENT"), eq(41L));
        verify(actionMapper).addMessage(eq(7L), eq(2L), eq("举报处理结果"), contains("举报成立"), eq("REPORT_RESULT"), eq("COMMENT"), eq(41L));
    }

    @Test
    void invalidReportKeepsOriginalContent() {
        User handler = user(2L, "auditor01");
        Map<String,Object> report = Map.of("id", 31L, "reporter_id", 7L, "target_type", "GUIDE", "target_id", 9L, "status", "PENDING");
        when(actionMapper.reportById(31L)).thenReturn(report);
        when(userMapper.selectOne(any())).thenReturn(handler);
        when(actionMapper.handleReport(31L, "INVALID", "未发现违规", 2L)).thenReturn(1);

        service.handleReport("auditor01", 31L, "INVALID", "未发现违规");

        verify(guideMapper, never()).offlinePublished(anyLong(), anyString());
        verify(commentMapper, never()).deleteById(anyLong());
        verify(actionMapper).addMessage(eq(7L), eq(2L), eq("举报处理结果"), contains("举报不成立"), eq("REPORT_RESULT"), eq("GUIDE"), eq(9L));
    }

    @Test
    void handleReportRequiresExplicitResultBeforeReadingReport() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.handleReport("auditor01", 31L, null, "核查完成"));

        assertEquals(400, error.getCode());
        assertEquals("请选择举报处理结果", error.getMessage());
        verify(actionMapper, never()).reportById(anyLong());
    }

    private static User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private static Guide guide(Long id, Long authorId, String status, String auditStatus) {
        Guide guide = new Guide();
        guide.setId(id);
        guide.setAuthorId(authorId);
        guide.setTitle("测试攻略");
        guide.setStatus(status);
        guide.setAuditStatus(auditStatus);
        return guide;
    }
}
