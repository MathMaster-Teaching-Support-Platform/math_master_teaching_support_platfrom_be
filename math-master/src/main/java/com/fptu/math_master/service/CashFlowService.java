package com.fptu.math_master.service;

import com.fptu.math_master.dto.response.*;
import com.fptu.math_master.entity.Transaction;
import com.fptu.math_master.enums.CashFlowType;
import com.fptu.math_master.enums.TransactionType;
import com.fptu.math_master.repository.TransactionRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CashFlowService {

    TransactionRepository transactionRepository;

    // INFLOW = real money entering the platform (via PayOS deposit only).
    // PAYMENT (subscription) is a student expense — deducted from student wallet, NOT a new inflow.
    private static final List<String> INFLOW_TYPES_STR = List.of(
        TransactionType.DEPOSIT.name()
    );

    private static final List<String> OUTFLOW_TYPES_STR = List.of(
        TransactionType.WITHDRAWAL.name()
    );

    private static final List<TransactionType> INFLOW_TYPES = List.of(
        TransactionType.DEPOSIT
    );

    private static final List<TransactionType> OUTFLOW_TYPES = List.of(
        TransactionType.WITHDRAWAL
    );

    // ─── Summary ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CashFlowSummaryResponse getSummary(Instant from, Instant to) {
        log.info("Getting cash flow summary from {} to {}", from, to);

        BigDecimal totalInflow  = transactionRepository.sumCashFlowByTypesAndDateRange(INFLOW_TYPES_STR, from, to);
        BigDecimal totalOutflow = transactionRepository.sumCashFlowByTypesAndDateRange(OUTFLOW_TYPES_STR, from, to);
        if (totalInflow  == null) totalInflow  = BigDecimal.ZERO;
        if (totalOutflow == null) totalOutflow = BigDecimal.ZERO;

        BigDecimal net = totalInflow.subtract(totalOutflow);

        // Trend vs previous equal-length period
        long millis = to.toEpochMilli() - from.toEpochMilli();
        Instant prevTo   = from;
        Instant prevFrom = prevTo.minusMillis(millis);

        BigDecimal prevInflow  = transactionRepository.sumCashFlowByTypesAndDateRange(INFLOW_TYPES_STR, prevFrom, prevTo);
        BigDecimal prevOutflow = transactionRepository.sumCashFlowByTypesAndDateRange(OUTFLOW_TYPES_STR, prevFrom, prevTo);
        if (prevInflow  == null) prevInflow  = BigDecimal.ZERO;
        if (prevOutflow == null) prevOutflow = BigDecimal.ZERO;

        Double inflowTrend  = calcTrend(totalInflow,  prevInflow);
        Double outflowTrend = calcTrend(totalOutflow, prevOutflow);
        BigDecimal prevNet  = prevInflow.subtract(prevOutflow);
        Double netTrend     = calcTrend(net, prevNet);

        // Category breakdown
        List<Object[]> rawBreakdown = transactionRepository.findCashFlowCategoryBreakdown(from, to);
        BigDecimal grandTotal = totalInflow.add(totalOutflow);
        List<CashFlowSummaryResponse.CategoryBreakdownItem> breakdown = rawBreakdown.stream()
            .map(row -> {
                String typeStr = row[0] != null ? row[0].toString() : "";
                TransactionType txType = TransactionType.valueOf(typeStr);
                CashFlowCategoryResponse cat = mapToCategoryResponse(txType);
                BigDecimal total = new BigDecimal(row[1].toString());
                double pct = grandTotal.compareTo(BigDecimal.ZERO) == 0
                    ? 0.0
                    : total.divide(grandTotal, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue();
                return CashFlowSummaryResponse.CategoryBreakdownItem.builder()
                    .categoryName(cat.getName())
                    .color(cat.getColor())
                    .type(cat.getType().name())
                    .total(total)
                    .percentage(Math.round(pct * 100.0) / 100.0)
                    .build();
            })
            .collect(Collectors.toList());

        return CashFlowSummaryResponse.builder()
            .totalInflow(totalInflow)
            .totalOutflow(totalOutflow)
            .netCashFlow(net)
            .inflowTrend(inflowTrend)
            .outflowTrend(outflowTrend)
            .netTrend(netTrend)
            .fromDate(from.toString())
            .toDate(to.toString())
            .period("Aggregated Data")
            .categoryBreakdown(breakdown)
            .build();
    }

    // ─── Transactions (paginated) ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<CashFlowEntryResponse> getTransactions(
            CashFlowType type, String categoryIdStr,
            Instant from, Instant to, String search, Pageable pageable) {
        log.info("Fetching cash flow entries: type={}, cat={}, from={}, to={}", type, categoryIdStr, from, to);
        String safeSearch = (search == null) ? "" : search.trim();

        List<TransactionType> typesToSearch = new ArrayList<>();
        if (categoryIdStr != null && !categoryIdStr.isEmpty()) {
            try {
                typesToSearch.add(TransactionType.valueOf(categoryIdStr));
            } catch (IllegalArgumentException e) {
                // Ignore invalid category
            }
        } else if (type != null) {
            typesToSearch = type == CashFlowType.INFLOW ? INFLOW_TYPES : OUTFLOW_TYPES;
        } else {
            typesToSearch.addAll(INFLOW_TYPES);
            typesToSearch.addAll(OUTFLOW_TYPES);
        }

        if (typesToSearch.isEmpty()) {
            return Page.empty(pageable);
        }

        return transactionRepository.findCashFlowTransactions(typesToSearch, from, to, safeSearch, pageable)
            .map(this::mapToEntryResponse);
    }

    // ─── Chart data ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CashFlowChartPointResponse> getChartData(String groupBy, Instant from, Instant to) {
        log.info("Getting chart data groupBy={} from={} to={}", groupBy, from, to);

        List<Object[]> raw = switch (groupBy.toLowerCase()) {
            case "hour"  -> transactionRepository.findCashFlowHourlyAggregates(from, to);
            case "week"  -> transactionRepository.findCashFlowWeeklyAggregates(from, to);
            case "month" -> transactionRepository.findCashFlowMonthlyAggregates(from, to);
            default      -> transactionRepository.findCashFlowDailyAggregates(from, to);
        };

        return raw.stream().map(row -> {
            String label    = row[0] != null ? row[0].toString() : "Unknown";
            BigDecimal inflow  = new BigDecimal(row[1] != null ? row[1].toString() : "0");
            BigDecimal outflow = new BigDecimal(row[2] != null ? row[2].toString() : "0");
            return CashFlowChartPointResponse.builder()
                .label(label)
                .inflow(inflow)
                .outflow(outflow)
                .net(inflow.subtract(outflow))
                .build();
        }).collect(Collectors.toList());
    }

    // ─── Categories ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CashFlowCategoryResponse> getCategories() {
        List<CashFlowCategoryResponse> list = new ArrayList<>();
        for (TransactionType t : INFLOW_TYPES) list.add(mapToCategoryResponse(t));
        for (TransactionType t : OUTFLOW_TYPES) list.add(mapToCategoryResponse(t));
        return list;
    }

    // ─── Export ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] exportExcel(Instant from, Instant to) throws IOException {
        log.info("Exporting cash flow to Excel: from={} to={}", from, to);

        List<TransactionType> allTypes = new ArrayList<>();
        allTypes.addAll(INFLOW_TYPES);
        allTypes.addAll(OUTFLOW_TYPES);

        Page<CashFlowEntryResponse> page = transactionRepository.findCashFlowTransactions(
            allTypes, from, to, "",
            org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE,
                org.springframework.data.domain.Sort.by("createdAt").descending()))
            .map(this::mapToEntryResponse);

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Cash Flow");

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {"Thời gian", "Hướng", "Loại Giao Dịch", "Số tiền", "Mã Đơn", "Người Dùng", "Mô tả"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                        .withZone(ZoneId.of("UTC"));
            int rowNum = 1;
            for (CashFlowEntryResponse e : page.getContent()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(e.getTransactionDate() != null ? dtf.format(e.getTransactionDate()) : "");
                row.createCell(1).setCellValue(e.getDirection() == CashFlowType.INFLOW ? "Thu vào" : "Chi ra");
                row.createCell(2).setCellValue(e.getCategory() != null ? e.getCategory().getName() : "");
                row.createCell(3).setCellValue(e.getAmount() != null ? e.getAmount().doubleValue() : 0.0);
                row.createCell(4).setCellValue(e.getOrderCode() != null ? e.getOrderCode().toString() : "");
                row.createCell(5).setCellValue(e.getUserName() != null ? e.getUserName() + " (" + e.getUserEmail() + ")" : "");
                row.createCell(6).setCellValue(e.getDescription() != null ? e.getDescription() : "");
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ─── Mappers ──────────────────────────────────────────────────────────────

    private CashFlowEntryResponse mapToEntryResponse(Transaction t) {
        CashFlowType dir = INFLOW_TYPES.contains(t.getType()) ? CashFlowType.INFLOW : CashFlowType.OUTFLOW;
        String userName = t.getWallet() != null && t.getWallet().getUser() != null ? t.getWallet().getUser().getFullName() : null;
        String userEmail = t.getWallet() != null && t.getWallet().getUser() != null ? t.getWallet().getUser().getEmail() : null;

        return CashFlowEntryResponse.builder()
            .id(t.getId())
            .direction(dir)
            .type(t.getType())
            .category(mapToCategoryResponse(t.getType()))
            .amount(t.getAmount())
            .currency("VND")
            .description(t.getDescription())
            .userName(userName)
            .userEmail(userEmail)
            .orderCode(t.getOrderCode())
            .transactionDate(t.getCreatedAt())
            .createdAt(t.getCreatedAt())
            .build();
    }

    private CashFlowCategoryResponse mapToCategoryResponse(TransactionType t) {
        CashFlowType dir = INFLOW_TYPES.contains(t) ? CashFlowType.INFLOW : CashFlowType.OUTFLOW;
        String name = switch (t) {
            case DEPOSIT -> "Nạp tiền";
            case PAYMENT -> "Thanh toán";
            case COURSE_PURCHASE -> "Mua khóa học";
            case WITHDRAWAL -> "Rút tiền";
            case INSTRUCTOR_REVENUE -> "Thu nhập giảng viên";
            case PLATFORM_COMMISSION -> "Hoa hồng nền tảng";
            default -> t.name();
        };
        String color = switch (t) {
            case DEPOSIT -> "#22c55e";
            case PAYMENT -> "#3b82f6";
            case COURSE_PURCHASE -> "#a855f7";
            case WITHDRAWAL -> "#ef4444";
            case INSTRUCTOR_REVENUE -> "#f59e0b";
            case PLATFORM_COMMISSION -> "#8b5cf6";
            default -> "#9ca3af";
        };
        String icon = switch (t) {
            case DEPOSIT -> "wallet";
            case PAYMENT -> "credit-card";
            case COURSE_PURCHASE -> "book-open";
            case WITHDRAWAL -> "arrow-down-circle";
            case INSTRUCTOR_REVENUE -> "user-check";
            case PLATFORM_COMMISSION -> "briefcase";
            default -> "circle";
        };

        return CashFlowCategoryResponse.builder()
            .id(t.name())
            .name(name)
            .type(dir)
            .color(color)
            .icon(icon)
            .build();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Double calcTrend(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current != null && current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return current.subtract(previous)
            .divide(previous, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .doubleValue();
    }
}
