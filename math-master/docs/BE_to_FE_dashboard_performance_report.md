# Báo BE gửi FE: Dashboard fetch chậm khi chuyển screen

## 1) Kết luận ngắn

- FE nhận định đúng: hiện tượng chậm chủ yếu đến từ việc FE re-fetch khi remount screen.
- Tuy nhiên BE đã bổ sung thêm tối ưu để giảm độ trễ khi FE gọi lại liên tục.
- Sau patch này, cùng một endpoint dashboard trong khoảng TTL sẽ trả nhanh hơn nhờ cache server-side.

## 2) Những gì BE đã triển khai

### 2.1 Server-side cache cho dashboard (Redis + Spring Cache)

BE đã bật cache theo endpoint/hàm service với TTL như sau:

- `adminDashboardStats`: 30 giây
- `adminRevenueByMonth`: 10 phút
- `adminSystemStatus`: 15 giây
- `studentDashboardSummary`: 30 giây
- `studentDashboardUpcomingTasks`: 30 giây
- `studentDashboardRecentGrades`: 30 giây
- `studentDashboardLearningProgress`: 60 giây
- `studentDashboardWeeklyActivity`: 30 giây
- `studentDashboardStreak`: 30 giây
- `studentDashboardOverview`: 30 giây

Kết quả: nếu FE gọi lại trong thời gian cache còn hiệu lực, BE không cần tính toán lại toàn bộ.

### 2.2 Giảm query lặp ở dashboard student

BE đã tối ưu `learning-progress`:

- Trước: vòng lặp theo enrollment và gọi query `count` lặp nhiều lần.
- Sau: chuyển sang batch query + group theo danh sách ID.

Kết quả: giảm số query DB khi user có nhiều enrollment, giảm tail latency.

### 2.3 Bổ sung header cache-control cho response dashboard

BE đã set `Cache-Control: private, max-age=...` cho các endpoint dashboard chính để FE/proxy/browser có thể tận dụng cơ chế cache phù hợp.

### 2.4 Bổ sung log đo latency + payload

BE đã thêm metrics filter cho các endpoint dashboard:

- Log dạng: `dashboard-metric method=... path=... status=... latencyMs=... payloadBytes=...`
- Dùng để tổng hợp P50/P95/P99 và payload size trung bình theo từng endpoint.

## 3) Endpoint FE đang dùng và trạng thái tối ưu

- `GET /student/dashboard` (và các nhánh con): đã cache + đã đo metric.
- `GET /admin/dashboard/stats`: đã cache + đã đo metric.
- `GET /admin/system/status`: đã cache + đã đo metric.
- `GET /admin/dashboard/revenue-by-month`: đã cache + đã đo metric.

## 4) FE cần làm song song (quan trọng)

Để xử lý triệt để việc chuyển screen bị fetch lại, FE cần:

1. Chuẩn hóa các màn dashboard sang `react-query` (thay vì fetch tay trong `useEffect`).
2. Tránh cấu hình luôn-refetch cho dữ liệu không realtime:
   - không dùng mặc định `staleTime: 0`
   - tránh `refetchOnMount: 'always'` nếu không bắt buộc
3. Prefetch dữ liệu cho màn sắp điều hướng.
4. Tách query realtime và query aggregate để giảm số call nặng.

## 5) Kỳ vọng sau khi FE + BE cùng tối ưu

- Chuyển screen mượt hơn do giảm request tính toán nặng lặp lại.
- Nếu FE vẫn remount và gọi lại API, BE vẫn phản hồi nhanh trong TTL cache.
- Có số liệu thực tế từ log để theo dõi regression/performance theo release.

## 6) Đề nghị FE xác nhận lại sau khi pull bản mới

Nhờ FE test lại 3 kịch bản:

1. Vào dashboard lần đầu (cold load).
2. Chuyển qua màn khác rồi quay lại trong vòng 30-60 giây.
3. Để quá TTL rồi quay lại, so sánh thời gian phản hồi.

Nếu cần, BE sẽ cung cấp thêm bảng thống kê latency theo giờ cao điểm từ log `dashboard-metric`.
