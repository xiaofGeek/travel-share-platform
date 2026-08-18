package com.travelshare.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travelshare.platform.dto.AuditRequest;
import com.travelshare.platform.entity.*;
import com.travelshare.platform.exception.BusinessException;
import com.travelshare.platform.mapper.*;
import com.travelshare.platform.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    private static final Set<String> EDITABLE_RESOURCES = Set.of(
            "destinations", "routes", "topics", "banners", "recommendations", "configs");

    private final AdminQueryMapper queryMapper;
    private final GuideMapper guideMapper;
    private final UserMapper userMapper;
    private final DestinationMapper destinationMapper;
    private final ActionMapper actionMapper;
    private final RouteMapper routeMapper;
    private final TopicMapper topicMapper;
    private final CommentMapper commentMapper;
    private final BannerMapper bannerMapper;
    private final AdminContentMapper contentMapper;

    @Override
    public Map<String, Object> dashboard() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("summary", queryMapper.summary());
        data.put("userTrend", queryMapper.userTrend());
        data.put("guideTrend", queryMapper.guideTrend());
        data.put("destinationRanking", queryMapper.destinationRanking());
        data.put("guideRanking", queryMapper.guideRanking());
        data.put("creatorRanking", queryMapper.creatorRanking());
        data.put("recentAudits", queryMapper.recentAudits());
        return data;
    }

    @Override
    public Object guides(String status, String keyword, long page, long size) {
        LambdaQueryWrapper<Guide> query = new LambdaQueryWrapper<Guide>().orderByDesc(Guide::getCreateTime);
        if (hasText(status)) query.eq(Guide::getAuditStatus, status.trim());
        if (hasText(keyword)) {
            String value = keyword.trim();
            query.and(item -> item.like(Guide::getTitle, value).or().like(Guide::getSubtitle, value).or().like(Guide::getSummary, value));
        }
        return guideMapper.selectPage(page(page, size), query);
    }

    @Override
    @Transactional
    public void auditGuide(String username, Long guideId, AuditRequest request) {
        User auditor = user(username);
        Guide guide = guideMapper.selectById(guideId);
        if (guide == null) throw BusinessException.notFound("攻略不存在");
        if (!"PENDING".equals(guide.getAuditStatus())) throw BusinessException.badRequest("该攻略不在待审核状态");
        String decision = request.decision().toUpperCase(Locale.ROOT);
        String guideStatus;
        String auditStatus;
        LocalDateTime publishedAt = null;
        if ("APPROVED".equals(decision)) {
            guideStatus = "PUBLISHED";
            auditStatus = "APPROVED";
            publishedAt = LocalDateTime.now();
        } else if ("REJECTED".equals(decision)) {
            guideStatus = "REJECTED";
            auditStatus = "REJECTED";
        } else {
            throw BusinessException.badRequest("审核结果只能是 APPROVED 或 REJECTED");
        }
        if (guideMapper.applyAuditDecision(guideId, guideStatus, auditStatus, request.opinion(), publishedAt) == 0) {
            throw BusinessException.badRequest("该攻略已被其他审核员处理，请刷新列表");
        }
        actionMapper.addAudit("GUIDE", guideId, auditor.getId(), decision, request.opinion());
        actionMapper.addMessage(guide.getAuthorId(), auditor.getId(), "攻略审核结果", "《" + guide.getTitle() + "》审核结果：" + decision + "。" + request.opinion(), "AUDIT_RESULT", "GUIDE", guideId);
    }

    @Override
    @Transactional
    public void revokeGuideApproval(String username, Long guideId, String reason) {
        if (!hasText(reason)) throw BusinessException.badRequest("必须填写撤销通过的原因");
        User auditor = user(username);
        Guide guide = guideMapper.selectById(guideId);
        if (guide == null) throw BusinessException.notFound("攻略不存在");
        if (!"PUBLISHED".equals(guide.getStatus()) || !"APPROVED".equals(guide.getAuditStatus())) {
            throw BusinessException.badRequest("只有已通过且正在公开的攻略才能撤销通过");
        }
        String normalizedReason = reason.trim();
        if (guideMapper.offlinePublished(guideId, "撤销通过：" + normalizedReason) == 0) {
            throw BusinessException.badRequest("攻略状态已经变化，请刷新列表");
        }
        actionMapper.addAudit("GUIDE", guideId, auditor.getId(), "REVOKED", normalizedReason);
        actionMapper.addMessage(guide.getAuthorId(), auditor.getId(), "攻略已撤销通过并下架",
                "《" + guide.getTitle() + "》已撤销通过并下架。你可以修改原稿后重新提交审核。原因：" + normalizedReason,
                "AUDIT_REVOKED", "GUIDE", guideId);
        addOperationLog(auditor, "guides", "撤销攻略通过", guideId, "撤销原因：" + normalizedReason);
    }

    @Override
    public Object users(String keyword, long page, long size) {
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<User>().orderByDesc(User::getCreateTime);
        if (hasText(keyword)) {
            String value = keyword.trim();
            query.and(item -> item.like(User::getUsername, value).or().like(User::getNickname, value).or().like(User::getCity, value));
        }
        Page<User> result = userMapper.selectPage(page(page, size), query);
        result.getRecords().forEach(item -> item.setPassword(null));
        return result;
    }

    @Override
    public Object destinations(String keyword, long page, long size) {
        LambdaQueryWrapper<Destination> query = new LambdaQueryWrapper<Destination>().orderByAsc(Destination::getSortOrder).orderByAsc(Destination::getId);
        if (hasText(keyword)) {
            String value = keyword.trim();
            query.and(item -> item.like(Destination::getName, value).or().like(Destination::getNameEn, value).or().like(Destination::getCode, value));
        }
        return destinationMapper.selectPage(page(page, size), query);
    }

    @Override
    public Object reports() {
        return queryMapper.reports();
    }

    @Override
    @Transactional
    public void handleReport(String username, Long reportId, String result, String note) {
        if (!hasText(result)) throw BusinessException.badRequest("请选择举报处理结果");
        String status = result.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("VALID", "INVALID", "CLOSED").contains(status)) throw BusinessException.badRequest("举报处理结果不合法");
        if (!hasText(note)) throw BusinessException.badRequest("必须填写处理说明");
        if (note.trim().length() > 500) throw BusinessException.badRequest("处理说明不能超过 500 个字符");
        Map<String,Object> report = actionMapper.reportById(reportId);
        if (report == null) throw BusinessException.notFound("举报不存在");
        if (!"PENDING".equals(String.valueOf(report.get("status")))) throw BusinessException.badRequest("举报已经处理，请勿重复操作");
        User handler = user(username);
        String normalizedNote = note.trim();
        if ("VALID".equals(status)) applyValidReport(handler, report, normalizedNote);
        int changed = actionMapper.handleReport(reportId, status, normalizedNote, handler.getId());
        if (changed == 0) throw BusinessException.badRequest("举报不存在或已经处理");
        Long reporterId = mapLong(report, "reporter_id");
        String targetType = String.valueOf(report.get("target_type"));
        Long targetId = mapLong(report, "target_id");
        String resultText = switch (status) { case "VALID" -> "举报成立，违规内容已处理"; case "INVALID" -> "举报不成立，原内容保留"; default -> "举报已关闭"; };
        actionMapper.addMessage(reporterId, handler.getId(), "举报处理结果",
                "你提交的" + targetName(targetType) + "举报已处理：" + resultText + "。处理说明：" + normalizedNote,
                "REPORT_RESULT", targetType, targetId);
        addOperationLog(handler, "reports", "处理举报", reportId, "结果：" + status + "；说明：" + normalizedNote);
    }

    private void applyValidReport(User handler, Map<String,Object> report, String note) {
        String type = String.valueOf(report.get("target_type"));
        Long targetId = mapLong(report, "target_id");
        if ("GUIDE".equals(type)) {
            Guide guide = guideMapper.selectById(targetId);
            if (guide == null) return;
            if ("PUBLISHED".equals(guide.getStatus()) && guideMapper.offlinePublished(targetId, "举报核查成立：" + note) > 0) {
                actionMapper.addAudit("GUIDE", targetId, handler.getId(), "REPORT_OFFLINE", note);
                actionMapper.addMessage(guide.getAuthorId(), handler.getId(), "内容下架通知",
                        "《" + guide.getTitle() + "》因举报核查成立已下架。你可以修改后重新提交审核。处理说明：" + note,
                        "REPORT_VALID", "GUIDE", targetId);
            }
            return;
        }
        if ("COMMENT".equals(type)) {
            Comment comment = commentMapper.selectById(targetId);
            if (comment == null) return;
            comment.setStatus("DELETED");
            comment.setUpdateTime(LocalDateTime.now());
            commentMapper.updateById(comment);
            commentMapper.detachReplies(targetId);
            if (commentMapper.deleteById(targetId) > 0) guideMapper.decrementCommentCount(comment.getGuideId());
            actionMapper.addMessage(comment.getUserId(), handler.getId(), "评论处理通知",
                    "你的评论因举报核查成立已删除。处理说明：" + note,
                    "REPORT_VALID", "COMMENT", targetId);
            return;
        }
        throw BusinessException.badRequest("不支持的举报对象类型");
    }

    @Override
    public Object routes(String keyword, long page, long size) {
        LambdaQueryWrapper<TravelRoute> query = new LambdaQueryWrapper<TravelRoute>().orderByDesc(TravelRoute::getCreateTime);
        if (hasText(keyword)) {
            String value = keyword.trim();
            query.and(item -> item.like(TravelRoute::getName, value).or().like(TravelRoute::getRouteNo, value).or().like(TravelRoute::getSummary, value));
        }
        return routeMapper.selectPage(page(page, size), query);
    }

    @Override
    public Object topics(String keyword, long page, long size) {
        LambdaQueryWrapper<Topic> query = new LambdaQueryWrapper<Topic>().orderByAsc(Topic::getSortOrder).orderByAsc(Topic::getId);
        if (hasText(keyword)) {
            String value = keyword.trim();
            query.and(item -> item.like(Topic::getName, value).or().like(Topic::getSubtitle, value).or().like(Topic::getSummary, value));
        }
        return topicMapper.selectPage(page(page, size), query);
    }

    @Override
    public Object comments(String keyword, long page, long size) {
        LambdaQueryWrapper<Comment> query = new LambdaQueryWrapper<Comment>().orderByDesc(Comment::getCreateTime);
        if (hasText(keyword)) query.like(Comment::getContent, keyword.trim());
        return commentMapper.selectPage(page(page, size), query);
    }

    @Override
    @Transactional
    public void deleteComment(String username, Long id, String reason) {
        if (id == null || id <= 0) throw BusinessException.badRequest("评论 ID 不合法");
        if (!hasText(reason)) throw BusinessException.badRequest("必须填写删除原因");
        String normalizedReason = reason.trim();
        if (normalizedReason.length() > 200) throw BusinessException.badRequest("删除原因不能超过 200 个字符");

        User admin = user(username);
        Comment comment = commentMapper.selectById(id);
        if (comment == null) throw BusinessException.notFound("评论不存在或已删除");

        comment.setStatus("DELETED");
        comment.setUpdateTime(LocalDateTime.now());
        commentMapper.updateById(comment);
        commentMapper.detachReplies(id);
        if (commentMapper.deleteById(id) == 0) throw BusinessException.badRequest("评论删除失败，请刷新后重试");
        guideMapper.decrementCommentCount(comment.getGuideId());

        actionMapper.addMessage(
                comment.getUserId(), admin.getId(), "评论处理通知",
                "您的评论已被管理员删除。原因：" + normalizedReason,
                "COMMENT_REMOVED", "COMMENT", id);
        addOperationLog(admin, "comments", "删除评论", id, "删除原因：" + normalizedReason);
    }

    @Override
    public Object banners(String keyword) {
        List<Banner> result = bannerMapper.selectList(new LambdaQueryWrapper<Banner>().orderByAsc(Banner::getSortOrder));
        if (!hasText(keyword)) return result;
        String value = keyword.trim().toLowerCase(Locale.ROOT);
        return result.stream().filter(item -> contains(item.getTitle(), value) || contains(item.getSubtitle(), value) || contains(item.getLinkUrl(), value)).toList();
    }

    @Override
    public Object recommendations(String keyword) {
        return filterMaps(contentMapper.recommendations(), keyword, "position_code", "target_type", "title");
    }

    @Override
    public Object configs(String keyword) {
        return filterMaps(contentMapper.configs(), keyword, "config_key", "config_name", "config_value", "remark");
    }

    @Override
    public Object logs(String keyword) {
        return filterMaps(contentMapper.logs(), keyword, "username", "module", "operation", "request_uri", "detail");
    }

    @Override
    public Object detail(String resource, Long id) {
        if (id == null || id <= 0) throw BusinessException.badRequest("记录 ID 不合法");
        Object record = switch (resource) {
            case "destinations" -> destinationMapper.selectById(id);
            case "guides" -> guideMapper.selectById(id);
            case "users" -> safeUser(userMapper.selectById(id));
            case "routes" -> routeMapper.selectById(id);
            case "topics" -> topicMapper.selectById(id);
            case "comments" -> commentMapper.selectById(id);
            case "banners" -> bannerMapper.selectById(id);
            case "recommendations" -> contentMapper.recommendation(id);
            case "configs" -> contentMapper.config(id);
            case "logs" -> contentMapper.log(id);
            default -> throw BusinessException.badRequest("不支持的管理资源");
        };
        if (record == null) throw BusinessException.notFound("记录不存在或已删除");
        return record;
    }

    @Override
    @Transactional
    public Object save(String username, String resource, Long id, Map<String, Object> payload) {
        if (!EDITABLE_RESOURCES.contains(resource)) throw BusinessException.badRequest("该页面不支持新增或编辑");
        if (payload == null) throw BusinessException.badRequest("请填写记录内容");
        User operator = user(username);
        Object saved = switch (resource) {
            case "destinations" -> saveDestination(id, payload);
            case "routes" -> saveRoute(operator.getId(), id, payload);
            case "topics" -> saveTopic(id, payload);
            case "banners" -> saveBanner(id, payload);
            case "recommendations" -> saveRecommendation(id, payload);
            case "configs" -> saveConfig(id, payload);
            default -> throw BusinessException.badRequest("不支持的管理资源");
        };
        addOperationLog(operator, resource, id == null ? "新增记录" : "编辑记录", id == null ? idOf(saved) : id);
        return saved;
    }

    private Destination saveDestination(Long id, Map<String, Object> payload) {
        Destination item = id == null ? new Destination() : require(destinationMapper.selectById(id), "目的地不存在");
        String code = required(payload, "code", "目的地编码", 50).toUpperCase(Locale.ROOT);
        Long duplicate = destinationMapper.selectCount(new LambdaQueryWrapper<Destination>().eq(Destination::getCode, code).ne(id != null, Destination::getId, id));
        if (duplicate > 0) throw BusinessException.badRequest("目的地编码已存在");
        item.setCode(code);
        item.setName(required(payload, "name", "目的地名称", 80));
        item.setNameEn(optional(payload, "nameEn", 100));
        item.setType(enumText(payload, "type", "目的地类型", Set.of("CITY", "ISLAND", "ANCIENT_TOWN", "NATURE")));
        item.setParentId(longValue(payload, "parentId", null));
        item.setCoverImage(image(payload, "coverImage", "/uploads/demo/placeholders/placeholder-002.png"));
        item.setSummary(optional(payload, "summary", 500));
        item.setDescription(optional(payload, "description", 20000));
        item.setSeason(optional(payload, "season", 100));
        item.setSuggestedDays(positiveInt(payload, "suggestedDays", 1));
        item.setAverageBudget(nonNegativeDecimal(payload, "averageBudget", BigDecimal.ZERO));
        item.setTags(optional(payload, "tags", 255));
        item.setLocationText(optional(payload, "locationText", 255));
        item.setRecommended(flag(payload, "recommended", 0));
        item.setEnabled(flag(payload, "enabled", 1));
        item.setSortOrder(intValue(payload, "sortOrder", 0));
        item.setUpdateTime(LocalDateTime.now());
        if (id == null) {
            item.setGuideCount(0); item.setFavoriteCount(0); item.setViewCount(0); item.setDeleted(0); item.setCreateTime(LocalDateTime.now());
            destinationMapper.insert(item);
        } else destinationMapper.updateById(item);
        return item;
    }

    private TravelRoute saveRoute(Long operatorId, Long id, Map<String, Object> payload) {
        TravelRoute item = id == null ? new TravelRoute() : require(routeMapper.selectById(id), "路线不存在");
        String routeNo = optional(payload, "routeNo", 40);
        if (!hasText(routeNo)) routeNo = "ADMIN-" + System.currentTimeMillis();
        Long duplicate = routeMapper.selectCount(new LambdaQueryWrapper<TravelRoute>().eq(TravelRoute::getRouteNo, routeNo).ne(id != null, TravelRoute::getId, id));
        if (duplicate > 0) throw BusinessException.badRequest("路线编号已存在");
        item.setRouteNo(routeNo);
        item.setName(required(payload, "name", "路线名称", 120));
        item.setCoverImage(image(payload, "coverImage", "/uploads/demo/placeholders/placeholder-003.png"));
        item.setUserId(id == null ? operatorId : item.getUserId());
        item.setDestinationId(longValue(payload, "destinationId", null));
        if (item.getDestinationId() != null && destinationMapper.selectById(item.getDestinationId()) == null) {
            throw BusinessException.badRequest("主目的地 ID 不存在");
        }
        item.setTotalDays(positiveInt(payload, "totalDays", 1));
        item.setBudget(nonNegativeDecimal(payload, "budget", BigDecimal.ZERO));
        item.setStartPoint(optional(payload, "startPoint", 100));
        item.setEndPoint(optional(payload, "endPoint", 100));
        item.setSeason(optional(payload, "season", 100));
        item.setAudience(optional(payload, "audience", 100));
        item.setSummary(optional(payload, "summary", 1000));
        item.setStatus(enumText(payload, "status", "路线状态", Set.of("DRAFT", "PUBLISHED", "OFFLINE"), "PUBLISHED"));
        item.setIsPublic(flag(payload, "isPublic", 1));
        item.setUpdateTime(LocalDateTime.now());
        if (id == null) {
            item.setFavoriteCount(0); item.setViewCount(0); item.setDeleted(0); item.setCreateTime(LocalDateTime.now());
            routeMapper.insert(item);
        } else routeMapper.updateById(item);
        return item;
    }

    private Topic saveTopic(Long id, Map<String, Object> payload) {
        Topic item = id == null ? new Topic() : require(topicMapper.selectById(id), "专题不存在");
        item.setName(required(payload, "name", "专题名称", 100));
        item.setSubtitle(optional(payload, "subtitle", 180));
        item.setCoverImage(image(payload, "coverImage", "/uploads/demo/placeholders/placeholder-004.png"));
        item.setSummary(optional(payload, "summary", 500));
        item.setContent(optional(payload, "content", 30000));
        item.setRecommended(flag(payload, "recommended", 0));
        item.setEnabled(flag(payload, "enabled", 1));
        item.setSortOrder(intValue(payload, "sortOrder", 0));
        item.setUpdateTime(LocalDateTime.now());
        if (id == null) {
            item.setDeleted(0); item.setCreateTime(LocalDateTime.now());
            topicMapper.insert(item);
        } else topicMapper.updateById(item);
        return item;
    }

    private Banner saveBanner(Long id, Map<String, Object> payload) {
        Banner item = id == null ? new Banner() : require(bannerMapper.selectById(id), "轮播图不存在");
        item.setTitle(required(payload, "title", "轮播标题", 100));
        item.setSubtitle(optional(payload, "subtitle", 255));
        item.setImageUrl(image(payload, "imageUrl", "/uploads/demo/placeholders/placeholder-005.png"));
        item.setLinkUrl(optional(payload, "linkUrl", 255));
        item.setSortOrder(intValue(payload, "sortOrder", 0));
        item.setEnabled(flag(payload, "enabled", 1));
        item.setUpdateTime(LocalDateTime.now());
        if (id == null) {
            item.setDeleted(0); item.setCreateTime(LocalDateTime.now());
            bannerMapper.insert(item);
        } else bannerMapper.updateById(item);
        return item;
    }

    private Map<String, Object> saveRecommendation(Long id, Map<String, Object> payload) {
        if (id != null && contentMapper.recommendation(id) == null) throw BusinessException.notFound("推荐记录不存在");
        Map<String, Object> values = new LinkedHashMap<>();
        if (id != null) values.put("id", id);
        values.put("positionCode", required(payload, "positionCode", "推荐位编码", 50).toUpperCase(Locale.ROOT));
        String targetType = enumText(payload, "targetType", "目标类型", Set.of("DESTINATION", "GUIDE", "ROUTE"));
        Long targetId = positiveLong(payload, "targetId", "目标 ID");
        boolean targetExists = switch (targetType) {
            case "DESTINATION" -> destinationMapper.selectById(targetId) != null;
            case "GUIDE" -> guideMapper.selectById(targetId) != null;
            case "ROUTE" -> routeMapper.selectById(targetId) != null;
            default -> false;
        };
        if (!targetExists) throw BusinessException.badRequest("推荐目标不存在或已删除");
        values.put("targetType", targetType);
        values.put("targetId", targetId);
        values.put("title", optional(payload, "title", 100));
        values.put("sortOrder", intValue(payload, "sortOrder", 0));
        values.put("enabled", flag(payload, "enabled", 1));
        if (id == null) contentMapper.insertRecommendation(values); else contentMapper.updateRecommendation(values);
        return contentMapper.recommendation(((Number) values.getOrDefault("id", id)).longValue());
    }

    private Map<String, Object> saveConfig(Long id, Map<String, Object> payload) {
        if (id != null && contentMapper.config(id) == null) throw BusinessException.notFound("系统参数不存在");
        Map<String, Object> values = new LinkedHashMap<>();
        if (id != null) values.put("id", id);
        values.put("configKey", required(payload, "configKey", "参数键", 100));
        values.put("configName", required(payload, "configName", "参数名称", 100));
        values.put("configValue", required(payload, "configValue", "参数值", 5000));
        values.put("configType", enumText(payload, "configType", "参数类型", Set.of("STRING", "BOOLEAN", "NUMBER", "JSON"), "STRING"));
        values.put("remark", optional(payload, "remark", 255));
        validateConfigValue(values.get("configType").toString(), values.get("configValue").toString());
        if (id == null) contentMapper.insertConfig(values); else contentMapper.updateConfig(values);
        return contentMapper.config(((Number) values.getOrDefault("id", id)).longValue());
    }

    @Override
    @Transactional
    public void toggleUser(String username, Long id) {
        User item = userMapper.selectById(id);
        if (item == null) throw BusinessException.notFound("用户不存在");
        User operator = user(username);
        if (Objects.equals(operator.getId(), item.getId()) && Integer.valueOf(1).equals(item.getStatus())) {
            throw BusinessException.badRequest("不能停用当前登录账号");
        }
        item.setStatus(Integer.valueOf(1).equals(item.getStatus()) ? 0 : 1);
        userMapper.updateById(item);
    }

    @Override
    @Transactional
    public void toggleDestination(Long id) {
        Destination item = destinationMapper.selectById(id);
        if (item == null) throw BusinessException.notFound("目的地不存在");
        item.setRecommended(Integer.valueOf(1).equals(item.getRecommended()) ? 0 : 1);
        destinationMapper.updateById(item);
    }

    @Override
    @Transactional
    public void toggleBanner(Long id) {
        Banner item = bannerMapper.selectById(id);
        if (item == null) throw BusinessException.notFound("轮播图不存在");
        item.setEnabled(Integer.valueOf(1).equals(item.getEnabled()) ? 0 : 1);
        bannerMapper.updateById(item);
    }

    @Override
    @Transactional
    public void offlineGuide(String username, Long id, String reason) {
        if (!hasText(reason)) throw BusinessException.badRequest("必须填写下架原因");
        User admin = user(username);
        Guide guide = guideMapper.selectById(id);
        if (guide == null) throw BusinessException.notFound("攻略不存在");
        if (!"PUBLISHED".equals(guide.getStatus())) throw BusinessException.badRequest("只有正在公开的攻略才能下架");
        if (guideMapper.offlinePublished(id, reason.trim()) == 0) throw BusinessException.badRequest("攻略状态已经变化，请刷新列表");
        actionMapper.addAudit("GUIDE", id, admin.getId(), "OFFLINE", reason.trim());
        actionMapper.addMessage(guide.getAuthorId(), admin.getId(), "内容下架通知", "《" + guide.getTitle() + "》已下架。原因：" + reason.trim(), "OFFLINE", "GUIDE", id);
        addOperationLog(admin, "guides", "下架攻略", id, "下架原因：" + reason.trim());
    }

    private static Long mapLong(Map<String,Object> values, String key) {
        Object value = values.get(key);
        if (value instanceof Number number) return number.longValue();
        try { return Long.valueOf(String.valueOf(value)); }
        catch (Exception error) { throw BusinessException.badRequest("举报数据不完整"); }
    }

    private static String targetName(String type) {
        return switch (type) { case "GUIDE" -> "攻略"; case "COMMENT" -> "评论"; default -> "内容"; };
    }

    private void addOperationLog(User operator, String resource, String operation, Long recordId) {
        addOperationLog(operator, resource, operation, recordId, operation + "成功，记录 ID：" + recordId);
    }

    private void addOperationLog(User operator, String resource, String operation, Long recordId, String detail) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("userId", operator.getId());
        values.put("username", operator.getUsername());
        values.put("module", moduleName(resource));
        values.put("operation", operation);
        values.put("requestUri", "/api/admin/" + resource + (recordId == null ? "" : "/" + recordId));
        values.put("detail", detail);
        contentMapper.addLog(values);
    }

    private static String moduleName(String resource) {
        return switch (resource) {
            case "destinations" -> "目的地管理";
            case "routes" -> "路线管理";
            case "topics" -> "专题管理";
            case "comments" -> "评论管理";
            case "banners" -> "轮播图管理";
            case "recommendations" -> "运营推荐";
            case "configs" -> "系统参数";
            default -> "后台管理";
        };
    }

    private static void validateConfigValue(String type, String value) {
        if ("BOOLEAN".equals(type) && !Set.of("true", "false").contains(value.toLowerCase(Locale.ROOT))) {
            throw BusinessException.badRequest("布尔参数值只能是 true 或 false");
        }
        if ("NUMBER".equals(type)) {
            try { new BigDecimal(value); }
            catch (NumberFormatException ignored) { throw BusinessException.badRequest("数字参数必须填写有效数字"); }
        }
        if ("JSON".equals(type) && !(value.trim().startsWith("{") || value.trim().startsWith("["))) {
            throw BusinessException.badRequest("JSON 参数必须以 { 或 [ 开头");
        }
    }

    private static Long idOf(Object value) {
        if (value instanceof Destination item) return item.getId();
        if (value instanceof TravelRoute item) return item.getId();
        if (value instanceof Topic item) return item.getId();
        if (value instanceof Banner item) return item.getId();
        if (value instanceof Map<?, ?> map && map.get("id") instanceof Number number) return number.longValue();
        return null;
    }

    private User user(String username) {
        User item = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (item == null) throw BusinessException.unauthorized("登录状态已失效");
        return item;
    }

    private static User safeUser(User item) {
        if (item != null) item.setPassword(null);
        return item;
    }

    private static <T> T require(T value, String message) {
        if (value == null) throw BusinessException.notFound(message);
        return value;
    }

    private static <T> Page<T> page(long page, long size) {
        return new Page<>(Math.max(1, page), Math.min(100, Math.max(1, size)));
    }

    private static List<Map<String, Object>> filterMaps(List<Map<String, Object>> rows, String keyword, String... keys) {
        if (!hasText(keyword)) return rows;
        String value = keyword.trim().toLowerCase(Locale.ROOT);
        return rows.stream().filter(row -> Arrays.stream(keys).anyMatch(key -> contains(row.get(key), value))).toList();
    }

    private static boolean contains(Object source, String lowerKeyword) {
        return source != null && source.toString().toLowerCase(Locale.ROOT).contains(lowerKeyword);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String required(Map<String, Object> payload, String key, String label, int maxLength) {
        String value = optional(payload, key, maxLength);
        if (!hasText(value)) throw BusinessException.badRequest(label + "不能为空");
        return value;
    }

    private static String optional(Map<String, Object> payload, String key, int maxLength) {
        Object raw = value(payload, key);
        if (raw == null) return null;
        String result = raw.toString().trim();
        if (result.isEmpty()) return null;
        if (result.length() > maxLength) throw BusinessException.badRequest(key + "长度不能超过 " + maxLength + " 个字符");
        return result;
    }

    private static String image(Map<String, Object> payload, String key, String fallback) {
        String value = optional(payload, key, 255);
        if (!hasText(value)) return fallback;
        if (!value.startsWith("/uploads/")) throw BusinessException.badRequest("图片必须使用本地上传地址");
        return value;
    }

    private static String enumText(Map<String, Object> payload, String key, String label, Set<String> allowed) {
        return enumText(payload, key, label, allowed, null);
    }

    private static String enumText(Map<String, Object> payload, String key, String label, Set<String> allowed, String defaultValue) {
        String value = optional(payload, key, 50);
        if (!hasText(value)) value = defaultValue;
        if (!hasText(value)) throw BusinessException.badRequest(label + "不能为空");
        value = value.toUpperCase(Locale.ROOT);
        if (!allowed.contains(value)) throw BusinessException.badRequest(label + "不合法");
        return value;
    }

    private static Object value(Map<String, Object> payload, String key) {
        if (payload.containsKey(key)) return payload.get(key);
        String snake = key.replaceAll("([A-Z])", "_$1").toLowerCase(Locale.ROOT);
        return payload.get(snake);
    }

    private static Integer intValue(Map<String, Object> payload, String key, Integer defaultValue) {
        Object raw = value(payload, key);
        if (raw == null || raw.toString().isBlank()) return defaultValue;
        try { return raw instanceof Number number ? number.intValue() : Integer.valueOf(raw.toString()); }
        catch (NumberFormatException ignored) { throw BusinessException.badRequest(key + "必须是整数"); }
    }

    private static Integer positiveInt(Map<String, Object> payload, String key, Integer defaultValue) {
        Integer result = intValue(payload, key, defaultValue);
        if (result == null || result <= 0) throw BusinessException.badRequest(key + "必须大于 0");
        return result;
    }

    private static Long longValue(Map<String, Object> payload, String key, Long defaultValue) {
        Object raw = value(payload, key);
        if (raw == null || raw.toString().isBlank()) return defaultValue;
        try { return raw instanceof Number number ? number.longValue() : Long.valueOf(raw.toString()); }
        catch (NumberFormatException ignored) { throw BusinessException.badRequest(key + "必须是整数"); }
    }

    private static Long positiveLong(Map<String, Object> payload, String key, String label) {
        Long result = longValue(payload, key, null);
        if (result == null || result <= 0) throw BusinessException.badRequest(label + "必须大于 0");
        return result;
    }

    private static BigDecimal nonNegativeDecimal(Map<String, Object> payload, String key, BigDecimal defaultValue) {
        Object raw = value(payload, key);
        if (raw == null || raw.toString().isBlank()) return defaultValue;
        try {
            BigDecimal result = new BigDecimal(raw.toString());
            if (result.signum() < 0) throw BusinessException.badRequest(key + "不能小于 0");
            return result;
        } catch (NumberFormatException ignored) {
            throw BusinessException.badRequest(key + "必须是数字");
        }
    }

    private static Integer flag(Map<String, Object> payload, String key, Integer defaultValue) {
        Object raw = value(payload, key);
        if (raw == null || raw.toString().isBlank()) return defaultValue;
        if (raw instanceof Boolean bool) return bool ? 1 : 0;
        Integer result = intValue(payload, key, defaultValue);
        if (!Integer.valueOf(0).equals(result) && !Integer.valueOf(1).equals(result)) throw BusinessException.badRequest(key + "只能是 0 或 1");
        return result;
    }
}
