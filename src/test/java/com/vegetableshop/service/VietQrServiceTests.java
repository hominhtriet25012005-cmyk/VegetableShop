package com.vegetableshop.service;
import org.junit.jupiter.api.Test;
import com.vegetableshop.entity.BankTransferPayment;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import javax.imageio.ImageIO;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class VietQrServiceTests {
    private BankTransferPayment sample() {
        var p=new BankTransferPayment();p.setBankBin("970422");p.setAccountNumber("0000000000");
        p.setReference("VS20260904120000AABBCCDD");p.setAmount(new BigDecimal("185000.00"));return p;
    }
    static Map<String,String> tags(String payload) {
        var result=new LinkedHashMap<String,String>();
        for(int pos=0;pos<payload.length();) {
            String tag=payload.substring(pos,pos+2);int n=Integer.parseInt(payload.substring(pos+2,pos+4));
            result.put(tag,payload.substring(pos+4,pos+4+n));pos+=4+n;
        }return result;
    }
    static String decode(java.awt.image.BufferedImage image) throws Exception {
        return new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image))),
            Map.of(DecodeHintType.TRY_HARDER,true)).getText();
    }
    @Test void pngRoundTripContainsExactMoneyAccountCurrencyReferenceAndCrc() throws Exception {
        var service=new VietQrService();var p=sample();String data=decode(ImageIO.read(new ByteArrayInputStream(service.png(p))));
        assertEquals(service.payload(p),data);var top=tags(data);assertEquals("12",top.get("01"));
        assertEquals("185000",top.get("54"));assertEquals("704",top.get("53"));assertEquals("VN",top.get("58"));
        var merchant=tags(top.get("38"));assertEquals("A000000727",merchant.get("00"));assertEquals("QRIBFTTA",merchant.get("02"));
        assertEquals("970422",tags(merchant.get("01")).get("00"));assertEquals("0000000000",tags(merchant.get("01")).get("01"));
        assertEquals(p.getReference(),tags(top.get("62")).get("08"));assertEquals(VietQrService.crc(data.substring(0,data.length()-4)),top.get("63"));
        assertEquals("29B1",VietQrService.crc("123456789"));
    }
    @Test void invalidAccountReferenceAndAmountsRejected() {
        var s=new VietQrService();var p=sample();
        for(String amount:List.of("0","-1","0.5","500000000")) {p.setAmount(new BigDecimal(amount));assertThrows(RuntimeException.class,()->s.payload(p));}
        var bad=sample();bad.setAccountNumber("123\n45");assertThrows(IllegalArgumentException.class,()->s.payload(bad));
    }
    @Test void suppliedQrMatchesPrivateConfigurationWhenRequested() throws Exception {
        String source=System.getProperty("qr.source");if(source==null)return;
        String data=decode(ImageIO.read(new File(source)));var top=tags(data);var merchant=tags(top.get("38"));var receiver=tags(merchant.get("01"));
        assertEquals("970422",receiver.get("00"));assertEquals(System.getProperty("qr.expectedAccount"),receiver.get("01"));
        assertEquals("QRIBFTTA",merchant.get("02"));assertEquals(VietQrService.crc(data.substring(0,data.length()-4)),top.get("63").toUpperCase(Locale.ROOT));
        System.out.println("Supplied VietQR decoded: MB bank/account match; CRC valid.");
    }
}
