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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
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
    private final MinioUploadServiceImpl minioUploadService;
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

        // Generate pre-signed URL for document
        String documentUrl = minioUploadService.getPresignedUrl(
                documentPath,
                minioProperties.getVerificationBucket()
        );

        // Extract data from image using Gemini
        OcrComparisonResult.OcrExtractedData extractedData = extractDataFromImageUrl(documentUrl);

        // Build profile data for comparison
        OcrComparisonResult.ProfileData profileData = buildProfileData(profile);

        // Compare extracted data with profile data
        return compareData(extractedData, profileData);
    }

    /**
     * Extract data from image URL using Gemini AI
     */
    private OcrComparisonResult.OcrExtractedData extractDataFromImageUrl(String imageUrl) {
        log.info("Extracting data from image URL using Gemini: {}", imageUrl);

        try {
            // Check if file is a ZIP
            if (isZipFile(imageUrl)) {
                log.info("Detected ZIP file, extracting images...");
                return extractDataFromZip(imageUrl);
            }

            // Direct image processing
            return extractDataFromImage(imageUrl);

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
     * Extract data from ZIP file (download, extract first image, process)
     */
    private OcrComparisonResult.OcrExtractedData extractDataFromZip(String zipUrl) {
        try {
            // Download ZIP file
            URL url = new URL(zipUrl);
            byte[] zipBytes = url.openStream().readAllBytes();

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
            throw new RuntimeException("Failed to process ZIP file: " + e.getMessage(), e);
        }
    }

    /**
     * Extract data from image URL (direct image)
     */
    private OcrComparisonResult.OcrExtractedData extractDataFromImage(String imageUrl) {
        try {
            // Download image
            URL url = new URL(imageUrl);
            byte[] imageBytes = url.openStream().readAllBytes();

            return extractDataFromImageBytes(imageBytes);

        } catch (Exception e) {
            log.error("Failed to download and process image", e);
            throw new RuntimeException("Failed to process image: " + e.getMessage(), e);
        }
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
     * Build OCR prompt for Gemini
     */
    private String buildOcrPrompt() {
        return """
                Analyze this Vietnamese teacher ID card (Thẻ viên chức giáo viên) and extract the following information in JSON format:
                
                {
                  "fullName": "Full name of the teacher as shown on the card",
                  "idNumber": "Employee ID number or card number if visible",
                  "dateOfBirth": "Date of birth in DD/MM/YYYY format if visible",
                  "placeOfBirth": "Place of birth if visible",
                  "address": "Address if visible",
                  "position": "Position/title (e.g., Giáo viên, Chức vụ)",
                  "schoolName": "School name (e.g., Trường Tiểu học...)",
                  "issueDate": "Issue date in DD/MM/YYYY format if visible",
                  "expiryDate": "Expiry date in DD/MM/YYYY format if visible"
                }
                
                Important:
                - This is a TEACHER ID CARD (Thẻ viên chức), not a national ID card
                - Extract the teacher's full name prominently displayed on the card
                - Look for school name (Trường Tiểu học, THCS, THPT, etc.)
                - Look for position/title (Giáo viên, Hiệu trưởng, etc.)
                - Extract text exactly as shown, preserving Vietnamese diacritics
                - If a field is not found or not visible, use null
                - Return only valid JSON, no additional text or explanation
                """;
    }

    /**
     * Parse Gemini response to extract structured data
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
                    .idNumber(getJsonString(jsonNode, "idNumber"))
                    .dateOfBirth(getJsonString(jsonNode, "dateOfBirth"))
                    .placeOfBirth(getJsonString(jsonNode, "placeOfBirth"))
                    .address(getJsonString(jsonNode, "address"))
                    .position(getJsonString(jsonNode, "position"))
                    .schoolName(getJsonString(jsonNode, "schoolName"))
                    .issueDate(getJsonString(jsonNode, "issueDate"))
                    .expiryDate(getJsonString(jsonNode, "expiryDate"))
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
     * Build profile data from teacher profile entity
     */
    private OcrComparisonResult.ProfileData buildProfileData(TeacherProfile profile) {
        return OcrComparisonResult.ProfileData.builder()
                .fullName(profile.getFullName())
                .idNumber(profile.getIdNumber())
                .dateOfBirth(profile.getDateOfBirth() != null ? profile.getDateOfBirth().toString() : null)
                .placeOfBirth(profile.getPlaceOfBirth())
                .address(profile.getAddress())
                .build();
    }

    /**
     * Compare OCR extracted data with profile data
     */
    private OcrComparisonResult compareData(
            OcrComparisonResult.OcrExtractedData extractedData,
            OcrComparisonResult.ProfileData profileData) {

        log.info("Comparing Gemini OCR data with profile: {}", profileData);

        List<OcrComparisonResult.FieldComparison> comparisons = new ArrayList<>();

        // Compare full name
        comparisons.add(compareField("Full Name", 
                extractedData.getFullName(), 
                profileData.getFullName()));

        // Compare ID number
        comparisons.add(compareField("ID Number", 
                extractedData.getIdNumber(), 
                profileData.getIdNumber()));

        // Compare date of birth
        comparisons.add(compareField("Date of Birth", 
                extractedData.getDateOfBirth(), 
                profileData.getDateOfBirth()));

        // Compare place of birth
        comparisons.add(compareField("Place of Birth", 
                extractedData.getPlaceOfBirth(), 
                profileData.getPlaceOfBirth()));

        // Compare address
        comparisons.add(compareField("Address", 
                extractedData.getAddress(), 
                profileData.getAddress()));

        // Calculate overall match score
        long matchCount = comparisons.stream().filter(OcrComparisonResult.FieldComparison::getMatches).count();
        double matchScore = (matchCount * 100.0) / comparisons.size();

        // Determine if overall match
        boolean isMatch = matchScore >= 80.0; // 80% threshold

        String summary = String.format("Verification %s: %.2f%% match (%d/%d fields)", 
                isMatch ? "PASSED" : "FAILED", 
                matchScore, 
                matchCount, 
                comparisons.size());

        return OcrComparisonResult.builder()
                .isMatch(isMatch)
                .matchScore(matchScore)
                .summary(summary)
                .fieldComparisons(comparisons)
                .extractedData(extractedData)
                .profileData(profileData)
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
