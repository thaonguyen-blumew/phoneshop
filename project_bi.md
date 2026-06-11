Gửi cho Codex một “project rehydration prompt” ngắn nhưng đủ ngữ cảnh để nó quay lại làm việc ngay. Không cần nhồi lại toàn bộ lịch sử chat nữa.

Dùng gần như nguyên khối này:

---

Project context:

Tôi đang làm đồ án BI + Data Warehouse cho hệ thống ecommerce bán điện thoại tại Việt Nam.

Current architecture:

Data Sources:

* MySQL OLTP database (Spring Boot ecommerce backend)
* 2 Excel files

Pipeline:
Data Sources -> Staging -> Data Warehouse -> BI/Dashboard

Tech stack:

* Backend: Spring Boot + MySQL
* ETL: Python + pandas + SQLAlchemy + MySQL
* DWH: MySQL warehouse schema
* BI/Dashboard: Power BI

Important constraints:

* Keep current ecommerce architecture intact
* Do NOT redesign the entire schema
* Do NOT reset IDs or foreign keys
* Do NOT rewrite historical transactional meaning
* Changes should be additive and stabilization-focused only

Current project state:

The stabilization pass has already been completed:

* OLTP schema alignment updated
* Entity compatibility updated
* mockdata.py stabilized
* etl_to_dwh.py stabilized
* backfill_oltp.py added
* deterministic mappings added
* review/order/shipment linkage stabilized

Important:
The ETL architecture previously assumed by the model was partially incorrect.
The real architecture is:

(MySQL OLTP + Excel files)
↓
Staging
↓
Data Warehouse
↓
BI / Dashboard

The ETL must follow this architecture strictly.

Current goal:
Continue implementation from the current stabilized state.
Focus only on:

1. validating OLTP
2. running backfill
3. generating realistic mock data
4. loading staging
5. loading warehouse
6. validating BI metrics and dashboards

Do NOT restart architecture discussions.
Do NOT redesign the warehouse unless explicitly requested.

Also:

* ETL must remain Python + pandas + SQLAlchemy
* prioritize deterministic mappings over heuristics
* preserve existing relationships and ecommerce flow

First task:
Inspect current repo state and determine:

1. what is already completed
2. what still blocks the ETL/DWH pipeline
3. what should be executed next in exact order

---

Sau đó bảo nó:

“Read the repository first before making changes.”

Câu này cực quan trọng. Nếu không nó sẽ hallucinate architecture mới 😑


backfill_oltp.py

Chạy đầu tiên nếu DB OLTP đang có product/name/color lệch.



mock_excel.py

Sinh 2 file Excel mock: inventory và bank statement.



staging/phoneshop_stg_schema.sql

Tạo schema staging.



dwh/phoneshop_dw_target_schema.sql

Tạo DWH mục tiêu.



etl_to_dwh.py

File này giờ là entrypoint chạy pipeline mới.



etl_target_pipeline.py

Đây là logic ETL thật phía sau.



product_normalization.py

Helper normalize product/color, được backfill và ETL dùng.
Có một cách rất thực dụng để học debug ETL: đừng nghĩ nó là “sửa code”, mà nghĩ nó là “truy dấu dữ liệu”.

Mình gợi ý bạn học theo 6 lớp này:

1. **Hiểu hợp đồng dữ liệu trước**
   - Mỗi bảng nguồn có những cột nào?
   - Kiểu dữ liệu gì?
   - Cột nào có thể `NULL`?
   - Một dòng đại diện cho cái gì?
   
   Nếu chưa rõ mấy câu này, debug sẽ rất mù.

2. **Chia ETL thành từng checkpoint**
   - đọc source
   - chuẩn hóa source
   - load staging
   - load DWH
   - đối soát cuối

   Mỗi checkpoint nên có:
   - số dòng
   - số null ở cột quan trọng
   - vài sample row

3. **Luôn kiểm tra 3 thứ**
   - `row count`
   - `schema/columns`
   - `join coverage`
   
   Ví dụ:
   - source có 10,000 dòng
   - sau merge còn 9,200 dòng
   - vậy mất 800 dòng ở đâu?

4. **Debug bằng dữ liệu mẫu nhỏ**
   - đừng chạy cả 30,000 dòng ngay
   - lấy 5 đến 20 dòng đại diện
   - bao gồm dòng “đẹp”, dòng có `NULL`, dòng có lỗi, dòng biên
   
   ETL hay nổ vì một dòng rất dị, không phải vì 10,000 dòng còn lại.

5. **Thêm assert và log sớm**
   Ví dụ:
   - cột bắt buộc không được thiếu
   - `variant_id` không được âm
   - `order_id` phải match source
   - `amount` không được NaN
   
   ETL tốt là ETL fail sớm, fail rõ.

6. **So sánh trước và sau**
   - trước merge có bao nhiêu dòng
   - sau merge bao nhiêu dòng
   - bao nhiêu key không match
   - bao nhiêu giá trị bị fill
   - bao nhiêu record bị normalize
   
   Đây là cách debug DE rất thật: không chỉ hỏi “chạy được chưa”, mà hỏi “dữ liệu đã biến đổi thế nào”.

Nếu học trên project của bạn, mình khuyên bạn debug theo thứ tự này:

- `mock_excel.py` trước
- rồi `staging`
- rồi `etl_target_pipeline.py`
- cuối cùng mới nhìn dashboard

Vì nếu nguồn mock còn lệch thì ETL sẽ bị đổ lỗi oan.

**Một thói quen rất đáng tiền**
Mỗi khi ETL lỗi, bạn ghi ra 4 dòng:
- lỗi ở stage nào
- input là gì
- output mong đợi là gì
- output thực tế là gì

Làm 10 lần như vậy là bạn tự xây được “cơ bắp debug” cho data engineering.

Nếu muốn, mình có thể viết cho bạn một **checklist debug ETL 1 trang** dùng riêng cho project này, kiểu mỗi lần pipeline nổ là bạn chỉ cần mở ra làm theo.