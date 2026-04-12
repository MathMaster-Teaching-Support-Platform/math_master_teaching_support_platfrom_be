package com.fptu.math_master.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.configuration.properties.MinioProperties;
import com.fptu.math_master.dto.response.OcrComparisonResult;
import com.fptu.math_master.entity.TeacherProfile;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.TeacherProfileRepository;
import com.fptu.math_master.service.GeminiService;
import com.fptu.math_master.service.OcrService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Implementation of OCR service using Gemini AI.
 * Handles document verification by extracting data from images and comparing with profile data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiOcrServiceImpl implements OcrService {

    private final TeacherProfileRepository teacherProfileRepository;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    @Override
    public OcrComparisonResult processProfileVerification(UUID profileId) {
        log.info("Processing Gemini OCR verification for profile: {}", profileId);

        // Get teacher profile
        TeacherProfile profile = teacherProfileRepository.findById(profileId)
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_FOUND));

        // Get verification document URL
        String documentPath = profile.getVerificationDocumentPath();
        if (documentPath == null || documentPath.isEmpty()) {
            throw new AppException(ErrorCode.DOCUMENT_NOT_FOUND);
        }

        // Read verification document directly from MinIO to avoid nginx/presigned-url mismatch.
        String objectKey = normalizeObjectKey(documentPath, minioProperties.getVerificationBucket());
        byte[] fileBytes = readObjectFromMinio(objectKey);

        // Extract data from image/zip using Gemini
        OcrComparisonResult.OcrExtractedData extractedData = extractDataFromFileBytes(objectKey, fileBytes);

        // Build profile data for comparison
        OcrComparisonResult.ProfileData profileData = buildProfileData(profile);

        // Compare extracted data with profile data
        return compareData(extractedData, profileData);
    }

    /**
     * Extract data from file bytes using Gemini AI
     */
    private OcrComparisonResult.OcrExtractedData extractDataFromFileBytes(String fileNameOrKey, byte[] fileBytes) {
        log.info("Extracting OCR data from object: {}", fileNameOrKey);

        try {
            // Check if file is a ZIP
            if (isZipFile(fileNameOrKey)) {
                log.info("Detected ZIP file, extracting images...");
                return extractDataFromZipBytes(fileBytes);
            }

            // Direct image processing
            return extractDataFromImageBytes(fileBytes);

        } catch (Exception e) {
            log.error("Failed to extract data from image", e);
            throw new RuntimeException("OCR extraction failed: " + e.getMessage(), e);
        }
    }

    /**
     * Check if file is a ZIP by extension and magic bytes
     */
    private boolean isZipFile(String url) {
        return url.toLowerCase().contains(".zip");
    }

    /**
     * Extract data from ZIP bytes (extract first image, process)
     */
    private OcrComparisonResult.OcrExtractedData extractDataFromZipBytes(byte[] zipBytes) {
        try {
            // Extract first image from ZIP
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String fileName = entry.getName().toLowerCase();
                    
                    // Check if it's an image file
                    if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
                        fileName.endsWith(".png") || fileName.endsWith(".gif")) {
                        
                        log.info("Found image in ZIP: {}", entry.getName());
                        
                        // Read image bytes
                        byte[] imageBytes = zis.readAllBytes();
                        
                        // Process image with Gemini
                        return extractDataFromImageBytes(imageBytes);
                    }
                    
                    zis.closeEntry();
                }
            }

            throw new RuntimeException("No image files found in ZIP");

        } catch (Exception e) {
            log.error("Failed to extract data from ZIP", e);
            throw new RuntimeException("Failed to process ZIP file from object bytes: " + e.getMessage(), e);
        }
    }

    private byte[] readObjectFromMinio(String objectKey) {
        try (InputStream in = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(minioProperties.getVerificationBucket())
                    .object(objectKey)
                    .build())) {
            return in.readAllBytes();
        } catch (Exception e) {
            log.error("Failed to read verification document from MinIO. bucket={}, objectKey={}",
                    minioProperties.getVerificationBucket(), objectKey, e);
            throw new RuntimeException("Failed to read verification document from MinIO", e);
        }
    }

    private String normalizeObjectKey(String rawPath, String bucket) {
        if (rawPath == null) {
            return null;
        }

        String value = rawPath.trim();

        // Support historical format where full URL was stored in DB.
        if (value.startsWith("http://") || value.startsWith("https://")) {
            try {
                String path = URI.create(value).getPath();
                value = path == null ? value : path;
            } catch (Exception ignored) {
                // Keep original value if URI parsing fails.
            }
        }

        while (value.startsWith("/")) {
            value = value.substring(1);
        }

        String bucketPrefix = bucket + "/";
        if (value.startsWith(bucketPrefix)) {
            value = value.substring(bucketPrefix.length());
        }

        return value;
    }

    /**
     * Extract data from image bytes using Gemini
     */
    private OcrComparisonResult.OcrExtractedData extractDataFromImageBytes(byte[] imageBytes) {
        try {
            // Create prompt for Gemini
            String prompt = buildOcrPrompt();

            // Call Gemini API with image
            String geminiResponse = geminiService.analyzeImageWithPrompt(imageBytes, prompt);

            // Parse Gemini response
            return parseGeminiResponse(geminiResponse);

        } catch (Exception e) {
            log.error("Failed to extract data using Gemini", e);
            throw new RuntimeException("Gemini OCR failed: " + e.getMessage(), e);
        }
    }

    /**
     * Build OCR prompt for Gemini - Only extract 3 mandatory fields
     */
    private String buildOcrPrompt() {
        return """
                Analyze this Vietnamese teacher ID card (Thẻ viên chức giáo viên) and extract ONLY the following 3 fields in JSON format:
                
                {
                  "fullName": "Full name of the teacher as shown on the card",
                  "position": "Complete position/title with subject specialization",
                  "schoolName": "Complete school name"
                }
                
                CRITICAL REQUIREMENTS FOR MATH TEACHER VERIFICATION:
                
                1. FULL NAME (Họ và tên):
                   - Extract the teacher's full name exactly as shown on the card
                   - Preserve Vietnamese diacritics (á, à, ả, ã, ạ, etc.)
                
                2. POSITION (Chức danh + Chuyên môn) - MOST IMPORTANT:
                   - Must extract the COMPLETE position including teaching role AND subject
                   - Teaching role: "Giáo viên", "Giảng viên", "Tổ trưởng", etc.
                   - Subject specialization: "Toán", "Tổ Toán", "Khoa Toán", "Bộ môn Toán", etc.
                   - Examples of VALID positions:
                     * "Giáo viên Toán"
                     * "Giảng viên Khoa Toán"
                     * "Tổ trưởng Tổ Toán"
                     * "Giáo viên THPT - Bộ môn Toán"
                   - If you see separate lines for role and subject, combine them
                
                3. SCHOOL NAME (Tên trường):
                   - Extract the complete official school name
                   - Look for: "Trường Tiểu học", "THCS", "THPT", "Đại học", "Cao đẳng", etc.
                   - Include full name: "Trường THPT Nguyễn Huệ", "Đại học Sư phạm Hà Nội", etc.
                
                IMPORTANT:
                - Return ONLY these 3 fields, nothing else
                - If a field is not found or not visible, use null
                - Extract text exactly as shown, preserving Vietnamese diacritics
                - Return only valid JSON, no additional text or explanation
                """;
    }

    /**
     * Parse Gemini response to extract structured data - Only 3 fields
     */
    private OcrComparisonResult.OcrExtractedData parseGeminiResponse(String response) {
        try {
            // Clean response (remove markdown code blocks if present)
            String cleanedResponse = response
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            // Parse JSON
            JsonNode jsonNode = objectMapper.readTree(cleanedResponse);

            return OcrComparisonResult.OcrExtractedData.builder()
                    .fullName(getJsonString(jsonNode, "fullName"))
                    .position(getJsonString(jsonNode, "position"))
                    .schoolName(getJsonString(jsonNode, "schoolName"))
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", response, e);
            throw new RuntimeException("Failed to parse OCR response", e);
        }
    }

    /**
     * Safely get string from JSON node
     */
    private String getJsonString(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }
        return fieldNode.asText();
    }

    /**
     * Build profile data from teacher profile entity - Only 3 fields
     */
    private OcrComparisonResult.ProfileData buildProfileData(TeacherProfile profile) {
        return OcrComparisonResult.ProfileData.builder()
                .fullName(profile.getFullName())
                .position(profile.getPosition())
                .schoolName(profile.getSchoolName())
                .build();
    }

    /**
     * Compare OCR extracted data with profile data - ONLY 3 MANDATORY FIELDS
     */
    private OcrComparisonResult compareData(
            OcrComparisonResult.OcrExtractedData extractedData,
            OcrComparisonResult.ProfileData profileData) {

        log.info("Comparing Gemini OCR data with profile (3 mandatory fields only): {}", profileData);

        List<OcrComparisonResult.FieldComparison> comparisons = new ArrayList<>();

        // ========== 3 TRƯỜNG BẮT BUỘC (MANDATORY FIELDS) ==========
        
        // 1. FULL NAME (Họ và tên) - BẮT BUỘC
        OcrComparisonResult.FieldComparison nameComparison = compareField("Họ và tên", 
                extractedData.getFullName(), 
                profileData.getFullName());
        comparisons.add(nameComparison);

        // 2. POSITION/CHUYÊN MÔN (Chức danh) - BẮT BUỘC + VALIDATE TỪ KHÓA
        OcrComparisonResult.FieldComparison positionComparison = validateTeacherPosition(
                extractedData.getPosition());
        comparisons.add(positionComparison);

        // 3. SCHOOL NAME (Tên trường) - BẮT BUỘC
        OcrComparisonResult.FieldComparison schoolComparison = compareField("Tên trường", 
                extractedData.getSchoolName(), 
                profileData.getSchoolName());
        comparisons.add(schoolComparison);

        // ========== LOGIC DUYỆT: CHỈ PASS KHI CẢ 3 TRƯỜNG ĐỀU HỢP LỆ ==========
        
        boolean allFieldsPass = nameComparison.getMatches() 
                && positionComparison.getMatches() 
                && schoolComparison.getMatches();

        // Calculate match score (all 3 fields have equal weight)
        long matchCount = comparisons.stream().filter(OcrComparisonResult.FieldComparison::getMatches).count();
        double matchScore = (matchCount * 100.0) / 3.0; // Only 3 fields

        String summary;
        if (!allFieldsPass) {
            summary = String.format("❌ XÁC MINH THẤT BẠI: Không đủ 3 trường bắt buộc. " +
                    "Họ tên: %s, Chức danh+Toán: %s, Tên trường: %s", 
                    nameComparison.getMatches() ? "✓" : "✗",
                    positionComparison.getMatches() ? "✓" : "✗",
                    schoolComparison.getMatches() ? "✓" : "✗");
        } else {
            summary = String.format("✅ XÁC MINH THÀNH CÔNG: Cả 3 trường bắt buộc đều hợp lệ (%.0f%% khớp)", 
                    matchScore);
        }

        return OcrComparisonResult.builder()
                .isMatch(allFieldsPass)
                .matchScore(matchScore)
                .summary(summary)
                .fieldComparisons(comparisons)
                .extractedData(extractedData)
                .profileData(profileData)
                .build();
    }

    /**
     * Validate teacher position with keyword checking
     * MUST contain: "Giáo viên" OR "Giảng viên" 
     * MUST contain: "Toán"
     * REJECT: "Cán bộ", "Nhân viên", "Staff" without "Toán"
     */
    private OcrComparisonResult.FieldComparison validateTeacherPosition(String position) {
        if (position == null || position.trim().isEmpty()) {
            return OcrComparisonResult.FieldComparison.builder()
                    .fieldName("Chức danh + Chuyên môn")
                    .ocrValue(null)
                    .profileValue("Bắt buộc: Giáo viên/Giảng viên + Toán")
                    .matches(false)
                    .similarity(0.0)
                    .notes("❌ THẤT BẠI: Không đọc được chức danh trên giấy tờ")
                    .build();
        }

        String normalizedPosition = position.toLowerCase().trim();
        
        // Check for teaching role keywords
        boolean hasTeachingRole = normalizedPosition.contains("giáo viên") 
                || normalizedPosition.contains("giảng viên")
                || normalizedPosition.contains("giao vien")
                || normalizedPosition.contains("giang vien");

        // Check for Math subject keyword
        boolean hasMathSubject = normalizedPosition.contains("toán") 
                || normalizedPosition.contains("toan")
                || normalizedPosition.contains("mathematics")
                || normalizedPosition.contains("math");

        // Check for rejected keywords (without Math)
        boolean hasRejectedRole = (normalizedPosition.contains("cán bộ") 
                || normalizedPosition.contains("can bo")
                || normalizedPosition.contains("nhân viên")
                || normalizedPosition.contains("nhan vien")
                || normalizedPosition.contains("staff")
                || normalizedPosition.contains("chuyên viên")
                || normalizedPosition.contains("chuyen vien"))
                && !hasMathSubject;

        boolean isValid = hasTeachingRole && hasMathSubject && !hasRejectedRole;

        String notes;
        if (hasRejectedRole) {
            notes = "❌ TỪ CHỐI: Chỉ là cán bộ/nhân viên, không phải giáo viên Toán";
        } else if (!hasTeachingRole) {
            notes = "❌ THẤT BẠI: Thiếu chức danh giảng dạy (Giáo viên/Giảng viên)";
        } else if (!hasMathSubject) {
            notes = "❌ THẤT BẠI: Thiếu chuyên môn Toán";
        } else {
            notes = "✅ HỢP LỆ: Giáo viên/Giảng viên Toán";
        }

        return OcrComparisonResult.FieldComparison.builder()
                .fieldName("Chức danh + Chuyên môn")
                .ocrValue(position)
                .profileValue("Bắt buộc: Giáo viên/Giảng viên + Toán")
                .matches(isValid)
                .similarity(isValid ? 100.0 : 0.0)
                .notes(notes)
                .build();
    }

    /**
     * Compare individual field
     */
    private OcrComparisonResult.FieldComparison compareField(String fieldName, String ocrValue, String profileValue) {
        // Handle null values
        if (ocrValue == null && profileValue == null) {
            return OcrComparisonResult.FieldComparison.builder()
                    .fieldName(fieldName)
                    .ocrValue(null)
                    .profileValue(null)
                    .matches(true)
                    .similarity(100.0)
                    .notes("Both values are null")
                    .build();
        }

        if (ocrValue == null || profileValue == null) {
            return OcrComparisonResult.FieldComparison.builder()
                    .fieldName(fieldName)
                    .ocrValue(ocrValue)
                    .profileValue(profileValue)
                    .matches(false)
                    .similarity(0.0)
                    .notes("One value is null")
                    .build();
        }

        // Normalize strings for comparison
        String normalizedOcr = normalizeString(ocrValue);
        String normalizedProfile = normalizeString(profileValue);

        // Calculate similarity
        double similarity = calculateSimilarity(normalizedOcr, normalizedProfile);
        boolean matches = similarity >= 0.8; // 80% similarity threshold

        return OcrComparisonResult.FieldComparison.builder()
                .fieldName(fieldName)
                .ocrValue(ocrValue)
                .profileValue(profileValue)
                .matches(matches)
                .similarity(similarity * 100)
                .notes(matches ? "Match" : "Mismatch")
                .build();
    }

    /**
     * Normalize string for comparison (lowercase, trim, remove extra spaces)
     */
    private String normalizeString(String str) {
        if (str == null) {
            return "";
        }
        return str.toLowerCase()
                .trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[^a-z0-9\\s]", "");
    }

    /**
     * Calculate similarity between two strings using Levenshtein distance
     */
    private double calculateSimilarity(String s1, String s2) {
        if (s1.equals(s2)) {
            return 1.0;
        }

        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) {
            return 1.0;
        }

        int distance = levenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLength);
    }

    /**
     * Calculate Levenshtein distance between two strings
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }
}
