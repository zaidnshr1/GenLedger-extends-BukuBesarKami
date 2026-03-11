package com.bukubesarkami.common.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class EntryNumberGenerator {

    /**
     * Format: JRN-YYYYMM-XXXXXXXX
     * Contoh: JRN-202506-A1B2C3D4
     */
    public String generate(LocalDate date) {
        String yearMonth = String.format("%d%02d", date.getYear(), date.getMonthValue());
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "JRN-" + yearMonth + "-" + random;
    }
}