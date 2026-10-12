package com.onlinemenu.menu.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.onlinemenu.common.constant.AppConstants;
import com.onlinemenu.common.constant.SettingKeys;
import com.onlinemenu.common.util.SettingProvider;
import com.onlinemenu.menu.dto.MenuLinkDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class QrCodeService {

    private static final int MARGIN = 2;

    private final SettingProvider settingProvider;
    private final MenuSupport menuSupport;

    @Value("${app.public-menu-base-url}")
    private String defaultBaseUrl;

    public MenuLinkDto link(String cafeSlug, Integer table) {
        menuSupport.findCafeOrThrow(cafeSlug);
        String base = settingProvider.getString(SettingKeys.MENU_PUBLIC_BASE_URL, defaultBaseUrl).trim();
        String url = (base.endsWith("/") ? base : base + "/") + cafeSlug
                + (table != null ? "?" + AppConstants.MENU_TABLE_QUERY_PARAM + "=" + table : "");
        return new MenuLinkDto(url, table);
    }

    public String svg(String cafeSlug, Integer table) {
        BitMatrix matrix = encode(link(cafeSlug, table).url());
        int size = matrix.getWidth();
        StringBuilder path = new StringBuilder();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (matrix.get(x, y)) {
                    path.append('M').append(x).append(',').append(y).append("h1v1h-1z");
                }
            }
        }
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + size + " " + size
                + "\" shape-rendering=\"crispEdges\"><rect width=\"100%\" height=\"100%\" fill=\"#fff\"/>"
                + "<path fill=\"#000\" d=\"" + path + "\"/></svg>";
    }

    private BitMatrix encode(String content) {
        try {
            return new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, MARGIN,
                    EncodeHintType.CHARACTER_SET, "UTF-8"));
        } catch (WriterException ex) {
            throw new IllegalStateException("Could not encode QR code for " + content, ex);
        }
    }
}
