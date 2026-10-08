package in.opt.sfa.tenant.report.activity.dto;

public record ActivityRow(Long oid, String reportKey, String reportLabel, String filters, String summary, String createdAt) {}
