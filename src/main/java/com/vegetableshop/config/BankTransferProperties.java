package com.vegetableshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import com.vegetableshop.exception.OrderOperationException;

@Component
@ConfigurationProperties(prefix = "app.bank-transfer")
public class BankTransferProperties {
    private boolean enabled;
    private String bin = "";
    private String bankName = "";
    private String accountNumber = "";
    private String accountName = "";
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { enabled = v; }
    public String getBin() { return bin; }
    public void setBin(String v) { bin = v; }
    public String getBankName() { return bankName; }
    public void setBankName(String v) { bankName = v; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String v) { accountNumber = v; }
    public String getAccountName() { return accountName; }
    public void setAccountName(String v) { accountName = v; }
    public boolean isReady() {
        return enabled && bin.matches("[0-9]{6}") && accountNumber.matches("[0-9]{6,19}")
            && !bankName.isBlank() && bankName.length() <= 100 && !accountName.isBlank() && accountName.length() <= 100;
    }
    public void requireReady() {
        if (!isReady()) throw new OrderOperationException("Chuyển khoản QR chưa được cấu hình. Vui lòng chọn COD hoặc liên hệ cửa hàng.");
    }
}
