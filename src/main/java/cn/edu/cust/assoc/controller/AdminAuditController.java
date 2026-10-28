package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.domain.AuditLogEntity;
import cn.edu.cust.assoc.dto.AuditRow;
import cn.edu.cust.assoc.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/audit")
public class AdminAuditController {

    private final AuditService auditService;

    public AdminAuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String operator,
                       @RequestParam(required = false) String action,
                       @RequestParam(required = false) String targetType,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Page<AuditLogEntity> logs = auditService.search(operator, action, targetType,
                from == null ? null : from.atStartOfDay(),
                to == null ? null : to.plusDays(1).atStartOfDay(),
                PageRequest.of(Math.max(0, page), 50));
        model.addAttribute("logs", logs.map(this::row));
        model.addAttribute("operator", operator);
        model.addAttribute("action", action);
        model.addAttribute("targetType", targetType);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "admin/audit";
    }

    private AuditRow row(AuditLogEntity log) {
        return new AuditRow(log.getId(), log.getOperatorName(), log.getAction(), log.getTargetType(),
                log.getTargetId(), log.getDetailBefore(), log.getDetailAfter(), log.getIp(),
                log.getRequestId(), log.getCreatedAt());
    }
}
