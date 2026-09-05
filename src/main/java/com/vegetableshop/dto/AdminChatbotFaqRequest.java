package com.vegetableshop.dto;

import com.vegetableshop.entity.ChatbotFaq;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AdminChatbotFaqRequest {

    @NotBlank(message = "Câu hỏi không được để trống")
    @Size(max = 200, message = "Câu hỏi không được vượt quá 200 ký tự")
    private String question;

    @NotBlank(message = "Câu trả lời không được để trống")
    @Size(max = 1500, message = "Câu trả lời không được vượt quá 1500 ký tự")
    private String answer;

    @NotBlank(message = "Từ khóa không được để trống")
    @Size(max = 500, message = "Từ khóa không được vượt quá 500 ký tự")
    private String keywords;

    @Min(value = 0, message = "Thứ tự không được âm")
    @Max(value = 9999, message = "Thứ tự tối đa là 9999")
    private Integer displayOrder = 0;

    private boolean status = true;

    public static AdminChatbotFaqRequest from(ChatbotFaq faq) {
        AdminChatbotFaqRequest request = new AdminChatbotFaqRequest();
        request.setQuestion(faq.getQuestion());
        request.setAnswer(faq.getAnswer());
        request.setKeywords(faq.getKeywords());
        request.setDisplayOrder(faq.getDisplayOrder());
        request.setStatus(faq.isStatus());
        return request;
    }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }
}
