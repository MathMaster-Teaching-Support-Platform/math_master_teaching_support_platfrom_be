package com.fptu.math_master.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OCR comparison result DTO.
 * Contains the result of comparing OCR-extracted data with profile data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrComparisonResult {
    
    /**
     * Overall match result
     */
    private Boolean isMatch;
    
    /**
     * Match score percentage (0-100)
     */
    private Double matchScore;
    
    /**
     * Summary message
     */
    private String summary;
    
    /**
     * Detailed field comparisons
     */
    private List<FieldComparison> fieldComparisons;
    
    /**
     * OCR extracted data (raw)
     */
    private OcrExtractedData extractedData;
    
    /**
     * Profile data for comparison
     */
    private ProfileData profileData;
    
    /**
     * Field comparison details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldComparison {
        private String fieldName;
        private String ocrValue;
        private String profileValue;
        private Boolean matches;
        private Double similarity;
        private String notes;
    }
    
    /**
     * OCR extracted data - Only 3 mandatory fields for Math teacher verification
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OcrExtractedData {
        private String fullName;      // Họ và tên
        private String position;      // Chức danh + Chuyên môn (must contain Giáo viên/Giảng viên + Toán)
        private String schoolName;    // Tên trường/Cơ sở giáo dục
    }
    
    /**
     * Profile data for comparison - Only 3 mandatory fields
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileData {
        private String fullName;      // Họ và tên từ profile
        private String position;      // Chức danh từ profile
        private String schoolName;    // Tên trường từ profile
    }
}
