-- KEYS[1]: flash_sale:stock:{itemId}
-- KEYS[2]: flash_sale:user_limit:{slotId}:{userId}:{itemId}
-- ARGV[1]: Số lượng muốn mua (quantity)
-- ARGV[2]: Giới hạn mua tối đa của user trong toàn bộ phiên (user_purchase_limit)
-- ARGV[3]: Thời gian còn lại của phiên sale tính bằng giây (slot_remaining_ttl_seconds)

-- Bước 1: Kiểm tra user đã mua hoặc đang giữ chỗ vượt quá giới hạn chưa
local current_purchased = redis.call('GET', KEYS[2])
if current_purchased and (tonumber(current_purchased) + tonumber(ARGV[1]) > tonumber(ARGV[2])) then
    return -1 -- Mã lỗi: Vượt quá giới hạn mua trên mỗi khách hàng trong phiên sale
end

-- Bước 2: Kiểm tra tồn kho khả dụng
local current_stock = redis.call('GET', KEYS[1])
if not current_stock or tonumber(current_stock) < tonumber(ARGV[1]) then
    return 0 -- Mã lỗi: Hết hàng hoặc không đủ số lượng yêu cầu
end

-- Bước 3: Trừ tồn kho nguyên tử & Tăng số lượng user đã giữ chỗ
redis.call('DECRBY', KEYS[1], ARGV[1])
redis.call('INCRBY', KEYS[2], ARGV[1])

-- Thiết lập TTL cho purchase limit sống theo toàn bộ thời gian diễn ra của Slot
if tonumber(ARGV[3]) > 0 then
    redis.call('EXPIRE', KEYS[2], ARGV[3])
end

-- Trả về số lượng tồn kho còn lại trên Redis sau khi trừ thành công (>= 0)
return tonumber(current_stock) - tonumber(ARGV[1])
