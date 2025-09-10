package com.sleekydz86.finsight.core.auth.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Slf4j
@Service
public class OtpService {

    @Value("${otp.issuer:FinSight}")
    private String issuer;

    @Value("${otp.algorithm:HmacSHA256}")
    private String algorithm;

    @Value("${otp.digits:6}")
    private int digits;

    @Value("${otp.period:30}")
    private int period;

    @Value("${otp.window:1}")
    private int window;

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int SECRET_SIZE = 32;

    public String generateSecretKey() {
        log.debug("OTP 시크릿 키 생성 시작");
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[SECRET_SIZE];
        random.nextBytes(bytes);
        Base32 base32 = new Base32();
        String secret = base32.encodeToString(bytes);
        String result = secret.replace("=", "");
        log.debug("OTP 시크릿 키 생성 완료: {}자", result.length());
        return result;
    }

    public String generateTOTP(String secret) {
        log.debug("TOTP 코드 생성 시작");
        Base32 base32 = new Base32();
        byte[] bytes = base32.decode(secret);
        String hexKey = Hex.encodeHexString(bytes);

        long time = Instant.now().getEpochSecond() / period;
        String hexTime = Long.toHexString(time);

        while (hexTime.length() < 16) {
            hexTime = "0" + hexTime;
        }

        String result = generateHOTP(hexKey, hexTime);
        log.debug("TOTP 코드 생성 완료");
        return result;
    }

    private String generateHOTP(String key, String time) {
        try {
            byte[] msg = hexStringToBytes(time);
            byte[] k = hexStringToBytes(key);

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec spec = new SecretKeySpec(k, HMAC_ALGORITHM);
            mac.init(spec);
            byte[] hash = mac.doFinal(msg);

            return truncateHash(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("OTP 생성 중 오류 발생", e);
            throw new RuntimeException("OTP 생성 중 오류 발생", e);
        }
    }

    private String truncateHash(byte[] hash) {
        int offset = hash[hash.length - 1] & 0xf;

        long truncatedHash = 0;
        for (int i = 0; i < 4; ++i) {
            truncatedHash <<= 8;
            truncatedHash |= (hash[offset + i] & 0xff);
        }

        truncatedHash &= 0x7FFFFFFF;
        truncatedHash %= Math.pow(10, digits);

        String result = String.valueOf(truncatedHash);
        while (result.length() < digits) {
            result = "0" + result;
        }

        return result;
    }

    public boolean verifyCode(String secret, String code) {
        log.debug("OTP 코드 검증 시작");
        long currentTime = Instant.now().getEpochSecond() / period;

        for (int i = -window; i <= window; i++) {
            String validCode = generateTOTPForTime(secret, currentTime + i);
            if (code.equals(validCode)) {
                log.debug("OTP 코드 검증 성공");
                return true;
            }
        }

        log.debug("OTP 코드 검증 실패");
        return false;
    }

    private String generateTOTPForTime(String secret, long time) {
        Base32 base32 = new Base32();
        byte[] bytes = base32.decode(secret);
        String hexKey = Hex.encodeHexString(bytes);

        String hexTime = Long.toHexString(time);
        while (hexTime.length() < 16) {
            hexTime = "0" + hexTime;
        }

        return generateHOTP(hexKey, hexTime);
    }

    public String generateQRUrl(String username, String secret) {
        log.debug("QR URL 생성 시작: username={}", username);
        String encodedIssuer = java.net.URLEncoder.encode(issuer, java.nio.charset.StandardCharsets.UTF_8);
        String encodedUsername = java.net.URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8);

        String result = String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA256&digits=%d&period=%d",
                encodedIssuer, encodedUsername, secret, encodedIssuer, digits, period);

        log.debug("QR URL 생성 완료");
        return result;
    }

    public String generateQRCodeImage(String username, String secret) {
        log.debug("QR 코드 이미지 생성 시작: username={}", username);
        String qrUrl = generateQRUrl(username, secret);

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrUrl, BarcodeFormat.QR_CODE, 256, 256);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            byte[] qrCodeImage = outputStream.toByteArray();
            String base64Image = Base64.getEncoder().encodeToString(qrCodeImage);

            log.debug("QR 코드 이미지 생성 완료: {} bytes", qrCodeImage.length);
            return base64Image;
        } catch (WriterException | IOException e) {
            log.error("QR 코드 생성 실패", e);
            throw new RuntimeException("QR 코드 생성 실패", e);
        }
    }

    private byte[] hexStringToBytes(String hex) {
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            result[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return result;
    }
}