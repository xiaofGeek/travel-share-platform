package com.travelshare.platform.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelshare.platform.dto.GuideRequest;
import com.travelshare.platform.dto.ReportRequest;
import com.travelshare.platform.entity.Guide;
import com.travelshare.platform.entity.User;
import com.travelshare.platform.exception.BusinessException;
import com.travelshare.platform.mapper.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserActionServiceImplTest {
    @Mock private UserMapper userMapper;
    @Mock private GuideMapper guideMapper;
    @Mock private DestinationMapper destinationMapper;
    @Mock private TopicMapper topicMapper;
    @Mock private CommentMapper commentMapper;
    @Mock private RouteMapper routeMapper;
    @Mock private RouteDayMapper routeDayMapper;
    @Mock private RouteItemMapper routeItemMapper;
    @Mock private ActionMapper actionMapper;
    @Mock private ObjectMapper objectMapper;
    @InjectMocks private UserActionServiceImpl service;

    @Test
    void updateRejectedGuideEditsTheOriginalRecordInsteadOfCreatingAnotherGuide() {
        User author = user(4L, "user01");
        Guide guide = guide(272L, 4L, "REJECTED");
        guide.setAuditStatus("REJECTED");
        when(guideMapper.selectById(272L)).thenReturn(guide);
        when(userMapper.selectOne(any())).thenReturn(author);

        Guide updated = (Guide) service.updateGuide("user01", 272L, guideRequest("补充后的原稿"));

        assertEquals(272L, updated.getId());
        assertEquals("补充后的原稿", updated.getTitle());
        assertEquals("REJECTED", updated.getStatus());
        verify(guideMapper).updateById(guide);
        verify(guideMapper, never()).insert((Guide) any());
    }

    @Test
    void submitOfflineGuideReturnsTheSameRecordToPendingReview() {
        User author = user(4L, "user01");
        Guide guide = guide(271L, 4L, "OFFLINE");
        guide.setAuditStatus("APPROVED");
        guide.setAuditOpinion("撤销通过：需要补充图片来源");
        guide.setPublishedAt(LocalDateTime.now().minusDays(1));
        when(guideMapper.selectById(271L)).thenReturn(guide);
        when(userMapper.selectOne(any())).thenReturn(author);

        service.submitGuide("user01", 271L);

        assertEquals(271L, guide.getId());
        assertEquals("PENDING", guide.getStatus());
        assertEquals("PENDING", guide.getAuditStatus());
        assertNull(guide.getAuditOpinion());
        assertNull(guide.getPublishedAt());
        verify(guideMapper).updateById(guide);
        verify(guideMapper, never()).insert((Guide) any());
    }

    @Test
    void authorCanSoftDeletePublishedGuideAndKeepAnAuditTrail() {
        User author = user(4L, "user01");
        Guide guide = guide(271L, 4L, "PUBLISHED");
        guide.setTitle("已发布攻略");
        when(userMapper.selectOne(any())).thenReturn(author);
        when(guideMapper.selectById(271L)).thenReturn(guide);
        when(guideMapper.softDeleteOwned(271L, 4L, "PUBLISHED")).thenReturn(1);

        service.deleteGuide("user01", 271L);

        verify(guideMapper).softDeleteOwned(271L, 4L, "PUBLISHED");
        verify(userMapper).decrementGuideCount(4L);
        verify(actionMapper).addAudit("GUIDE", 271L, 4L, "AUTHOR_DELETED", "作者主动删除，删除前状态：PUBLISHED");
    }

    @Test
    void userCannotDeleteAnotherAuthorsGuide() {
        User user = user(4L, "user01");
        Guide guide = guide(271L, 12L, "PUBLISHED");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(guideMapper.selectById(271L)).thenReturn(guide);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.deleteGuide("user01", 271L));

        assertEquals(403, error.getCode());
        assertEquals("不能删除他人的攻略", error.getMessage());
        verify(guideMapper, never()).softDeleteOwned(any(), any(), any());
        verify(userMapper, never()).decrementGuideCount(any());
        verify(actionMapper, never()).addAudit(any(), any(), any(), any(), any());
    }

    @Test
    void deleteGuideDoesNotWriteCountersOrAuditWhenStatusChangedConcurrently() {
        User author = user(4L, "user01");
        Guide guide = guide(271L, 4L, "PENDING");
        when(userMapper.selectOne(any())).thenReturn(author);
        when(guideMapper.selectById(271L)).thenReturn(guide);
        when(guideMapper.softDeleteOwned(271L, 4L, "PENDING")).thenReturn(0);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.deleteGuide("user01", 271L));

        assertEquals("攻略状态已经变化，请刷新后重试", error.getMessage());
        verify(userMapper, never()).decrementGuideCount(any());
        verify(actionMapper, never()).addAudit(any(), any(), any(), any(), any());
    }

    @Test
    void reportPublishedGuideCreatesPendingCase() {
        User reporter = user(4L, "user01");
        Guide guide = guide(1L, 12L, "PUBLISHED");
        when(userMapper.selectOne(any())).thenReturn(reporter);
        when(guideMapper.selectById(1L)).thenReturn(guide);
        when(actionMapper.hasPendingReport(4L, "GUIDE", 1L)).thenReturn(0);

        service.report("user01", new ReportRequest("guide", 1L, "内容不实", "票价信息错误"));

        verify(actionMapper).addReport(4L, "GUIDE", 1L, "内容不实", "票价信息错误");
    }

    @Test
    void userCannotReportOwnGuide() {
        User reporter = user(4L, "user01");
        Guide guide = guide(1L, 4L, "PUBLISHED");
        when(userMapper.selectOne(any())).thenReturn(reporter);
        when(guideMapper.selectById(1L)).thenReturn(guide);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.report("user01", new ReportRequest("GUIDE", 1L, "其他", "")));

        assertEquals("不能举报自己发布的攻略", error.getMessage());
        verify(actionMapper, never()).addReport(any(), any(), any(), any(), any());
    }

    @Test
    void duplicatePendingReportIsRejected() {
        User reporter = user(4L, "user01");
        Guide guide = guide(1L, 12L, "PUBLISHED");
        when(userMapper.selectOne(any())).thenReturn(reporter);
        when(guideMapper.selectById(1L)).thenReturn(guide);
        when(actionMapper.hasPendingReport(4L, "GUIDE", 1L)).thenReturn(1);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.report("user01", new ReportRequest("GUIDE", 1L, "其他", "")));

        assertEquals("你已举报过该内容，请等待处理结果", error.getMessage());
        verify(actionMapper, never()).addReport(any(), any(), any(), any(), any());
    }

    private static User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private static Guide guide(Long id, Long authorId, String status) {
        Guide guide = new Guide();
        guide.setId(id);
        guide.setAuthorId(authorId);
        guide.setStatus(status);
        return guide;
    }

    private static GuideRequest guideRequest(String title) {
        return new GuideRequest(title, "副标题", "/uploads/test.png", "补充后的攻略摘要", 1L, null,
                3, new BigDecimal("1200"), "全年", "公共交通", "自由行",
                "补充路线衔接、费用说明以及图片来源后的完整攻略正文内容。", "费用参考", "注意事项");
    }
}
