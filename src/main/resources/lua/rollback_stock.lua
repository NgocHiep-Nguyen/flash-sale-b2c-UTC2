-- KEYS[1]: flash_sale:stock:{itemId}
-- ARGV[1]: Số lượng cần hoàn lại (quantity)
-- ARGV[2]: Số lượng tồn kho phục hồi từ DB nếu key không tồn tại (fallback_stock)
-- ARGV[3]: TTL tính bằng giây (remaining_ttl_seconds)

local exists = redis.call('EXISTS', KEYS[1])
if exists == 1 then
    -- Key còn tồn tại trên Redis -> cộng hoàn trả đúng số lượng
    local new_stock = redis.call('INCRBY', KEYS[1], ARGV[1])
    return new_stock
else
    -- Key đã bị mất hoặc hết hạn trên Redis -> tái thiết lập từ công thức DB (available_stock + pending + quantity)
    local fallback = tonumber(ARGV[2])
    if fallback and fallback >= 0 then
        if tonumber(ARGV[3]) > 0 then
            redis.call('SET', KEYS[1], fallback, 'EX', ARGV[3])
        else
            redis.call('SET', KEYS[1], fallback)
        end
        return fallback
    else
        return -1
    end
end
