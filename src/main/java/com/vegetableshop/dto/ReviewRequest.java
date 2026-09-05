package com.vegetableshop.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReviewRequest {

    @NotNull(message = "Vui lòng chọn đơn hàng đã hoàn tất")
    @Min(value = 1, message = "Chi tiết đơn hàng không hợp lệ")
    private Long orderDetailId;
    @Size(max = 5, message = "Chỉ được gửi tối đa 5 ảnh")
    private java.util.List<org.springframework.web.multipart.MultipartFile> images = new java.util.ArrayList<>();

    public Long getOrderDetailId() { return orderDetailId; }
    public void setOrderDetailId(Long orderDetailId) { this.orderDetailId = orderDetailId; }
    public java.util.List<org.springframework.web.multipart.MultipartFile> getImages() { return images; }
    public void setImages(java.util.List<org.springframework.web.multipart.MultipartFile> images) { this.images = images; }

    @NotNull(message = "Vui lòng chọn số sao")
    @Min(value = 1, message = "Đánh giá tối thiểu 1 sao")
    @Max(value = 5, message = "Đánh giá tối đa 5 sao")
    private Integer rating;

    @Size(max = 1000, message = "Nội dung đánh giá không được vượt quá 1000 ký tự")
    private String comment;

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
