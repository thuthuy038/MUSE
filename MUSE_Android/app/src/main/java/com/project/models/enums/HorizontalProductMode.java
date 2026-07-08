package com.project.models.enums;

public enum HorizontalProductMode {

    // Giỏ hàng
    // Cho phép chỉnh màu, size, số lượng
    CART,

    // Đơn hàng, lịch sử đơn hàng, chi tiết đơn hàng
    // Chỉ hiển thị thông tin
    READ_ONLY,

    // Gợi ý sản phẩm trong Chatbot
    // Cho phép vuốt ngang để thêm vào giỏ hàng
    SUGGEST
}