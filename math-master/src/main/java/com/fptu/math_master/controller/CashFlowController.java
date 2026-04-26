package com.fptu.math_master.controller;

import com.fptu.math_master.dto.response.*;
import com.fptu.math_master.enums.CashFlowType;
import com.fptu.math_master.service.CashFlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/cash-flow")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Cash Flow", description = "Cash flow management and reporting APIs (Read-only)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class CashFlowController {

    CashFlowService cashFlowService;

    // ─── 1. Summary ───────────────────────────────────────────────────────────

    @GetMapping("/summary")
    @Operation(summary = "Get cash flow summary",
               description = "Returns total inflow, outflow, net cash, and category breakdown for a date range")
    public ResponseEntity<ApiResponse<CashFlowSummaryResponse>> getSummary(
            @Parameter(description = "Start date (YYYY-MM-DD), defaults to start of current month")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date (YYYY-MM-DD), defaults to today")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDate safeFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate safeTo   = to   != null ? to   : LocalDate.now();

        Instant instantFrom = safeFrom.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant instantTo   = safeTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        CashFlowSummaryResponse result = cashFlowService.getSummary(instantFrom, instantTo);
        return ResponseEntity.ok(ApiResponse.<CashFlowSummaryResponse>builder()
            .code(200).message("Summary retrieved successfully").result(result).build());
    }

    // ─── 2. Transactions (paginated) ──────────────────────────────────────────

    @GetMapping("/transactions")
    @Operation(summary = "List cash flow entries",
               description = "Paginated, filterable list of cash flow entries")
    public ResponseEntity<ApiResponse<Page<CashFlowEntryResponse>>> getTransactions(
            @RequestParam(required = false) CashFlowType type,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String order) {

        Sort.Direction dir = "ASC".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable  = PageRequest.of(page, size, Sort.by(dir, sortBy));

        Instant instantFrom = from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : Instant.EPOCH;
        Instant instantTo   = to != null ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : Instant.now().plus(365, java.time.temporal.ChronoUnit.DAYS);

        Page<CashFlowEntryResponse> result = cashFlowService.getTransactions(
            type, categoryId, instantFrom, instantTo, search, pageable);

        return ResponseEntity.ok(ApiResponse.<Page<CashFlowEntryResponse>>builder()
            .code(200).message("Transactions retrieved successfully").result(result).build());
    }

    // ─── 3. Chart data ────────────────────────────────────────────────────────

    @GetMapping("/chart")
    @Operation(summary = "Get chart time-series data",
               description = "Returns inflow/outflow/net aggregated by day, week, or month")
    public ResponseEntity<ApiResponse<List<CashFlowChartPointResponse>>> getChartData(
            @Parameter(description = "Grouping: day | week | month")
            @RequestParam(defaultValue = "day") String groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDate safeFrom = from != null ? from : LocalDate.now().minusDays(29);
        LocalDate safeTo   = to   != null ? to   : LocalDate.now();

        Instant instantFrom = safeFrom.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant instantTo   = safeTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<CashFlowChartPointResponse> result = cashFlowService.getChartData(groupBy, instantFrom, instantTo);
        return ResponseEntity.ok(ApiResponse.<List<CashFlowChartPointResponse>>builder()
            .code(200).message("Chart data retrieved successfully").result(result).build());
    }

    // ─── 4. Categories ────────────────────────────────────────────────────────

    @GetMapping("/categories")
    @Operation(summary = "List cash flow categories")
    public ResponseEntity<ApiResponse<List<CashFlowCategoryResponse>>> getCategories() {
        List<CashFlowCategoryResponse> result = cashFlowService.getCategories();
        return ResponseEntity.ok(ApiResponse.<List<CashFlowCategoryResponse>>builder()
            .code(200).message("Categories retrieved successfully").result(result).build());
    }

    // ─── 5. Export ────────────────────────────────────────────────────────────

    @GetMapping("/export")
    @Operation(summary = "Export cash flow report as Excel")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) throws IOException {

        LocalDate safeFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate safeTo   = to   != null ? to   : LocalDate.now();

        Instant instantFrom = safeFrom.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant instantTo   = safeTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        byte[] data = cashFlowService.exportExcel(instantFrom, instantTo);
        String filename = "cash_flow_" + safeFrom.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                        + "_" + safeTo.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(data);
    }
}
