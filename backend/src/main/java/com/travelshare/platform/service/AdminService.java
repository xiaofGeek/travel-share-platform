package com.travelshare.platform.service;
import com.travelshare.platform.dto.AuditRequest;
import java.util.Map;
public interface AdminService {
    Map<String,Object> dashboard();
    Object guides(String status, String keyword, long page, long size);
    void auditGuide(String username, Long guideId, AuditRequest request);
    void revokeGuideApproval(String username, Long guideId, String reason);
    Object users(String keyword, long page, long size);
    Object destinations(String keyword, long page, long size);
    Object reports();
    void handleReport(String username, Long reportId, String result, String note);
    Object routes(String keyword, long page, long size);
    Object topics(String keyword, long page, long size);
    Object comments(String keyword, long page, long size);
    void deleteComment(String username, Long id, String reason);
    Object banners(String keyword);
    Object recommendations(String keyword);
    Object configs(String keyword);
    Object logs(String keyword);
    Object detail(String resource, Long id);
    Object save(String username, String resource, Long id, Map<String,Object> payload);
    void toggleUser(String username, Long id);
    void toggleDestination(Long id);
    void toggleBanner(Long id);
    void offlineGuide(String username, Long id, String reason);
}
