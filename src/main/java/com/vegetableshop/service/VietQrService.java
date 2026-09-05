package com.vegetableshop.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.vegetableshop.entity.BankTransferPayment;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

/** VietQR NAPAS 247 transfer payload, generated locally: no bank/account data leaves the server. */
@Service
public class VietQrService {
    private String tlv(String tag, String value) {
        int length = value.getBytes(StandardCharsets.UTF_8).length;
        if (length > 99) throw new IllegalArgumentException("QR field too long");
        return tag + String.format(java.util.Locale.ROOT, "%02d", length) + value;
    }
    public String payload(BankTransferPayment p) {
        if (!p.getBankBin().matches("[0-9]{6}") || !p.getAccountNumber().matches("[0-9]{6,19}")
            || !p.getReference().matches("[A-Z0-9]{1,25}")) throw new IllegalArgumentException("Invalid receiving account/reference");
        BigDecimal amount = p.getAmount();
        if (amount == null || amount.signum() <= 0 || amount.compareTo(new BigDecimal("500000000")) >= 0)
            throw new IllegalArgumentException("QR amount outside supported transfer range");
        String value = amount.toBigIntegerExact().toString();
        String beneficiary = tlv("00", p.getBankBin()) + tlv("01", p.getAccountNumber());
        String merchant = tlv("00", "A000000727") + tlv("01", beneficiary) + tlv("02", "QRIBFTTA");
        String data = tlv("00", "01") + tlv("01", "12") + tlv("38", merchant)
            + tlv("53", "704") + tlv("54", value) + tlv("58", "VN")
            + tlv("62", tlv("08", p.getReference())) + "6304";
        return data + crc(data);
    }
    public static String crc(String value) {
        int crc = 0xffff;
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (b & 0xff) << 8;
            for (int i=0;i<8;i++) crc = ((crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1) & 0xffff;
        }
        return String.format(java.util.Locale.ROOT, "%04X", crc);
    }
    public byte[] png(BankTransferPayment payment) {
        try {
            var matrix = new QRCodeWriter().encode(payload(payment), BarcodeFormat.QR_CODE, 420, 420);
            var image = new BufferedImage(420,420,BufferedImage.TYPE_INT_RGB);
            for(int y=0;y<420;y++) for(int x=0;x<420;x++) image.setRGB(x,y,matrix.get(x,y)?0x000000:0xffffff);
            var out = new ByteArrayOutputStream();
            ImageIO.write(image,"PNG",out);
            return out.toByteArray();
        } catch (Exception e) { throw new IllegalArgumentException("Không thể tạo QR cho thông tin thanh toán này", e); }
    }
}
