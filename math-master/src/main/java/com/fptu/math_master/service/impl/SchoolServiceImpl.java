package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.SchoolRequest;
import com.fptu.math_master.dto.response.SchoolResponse;
import com.fptu.math_master.entity.School;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.SchoolRepository;
import com.fptu.math_master.service.SchoolService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SchoolServiceImpl implements SchoolService {

    SchoolRepository schoolRepository;

    @Override
    @Transactional
    public SchoolResponse createSchool(SchoolRequest request) {
        log.info("Creating new school: {}", request.getName());

        // Check if school already exists
        if (schoolRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.SCHOOL_ALREADY_EXISTS);
        }

        School school = School.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .district(request.getDistrict())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .website(request.getWebsite())
                .build();

        school = schoolRepository.save(school);
        log.info("School created successfully with id: {}", school.getId());

        return mapToResponse(school);
    }

    @Override
    @Transactional
    public SchoolResponse updateSchool(Long schoolId, SchoolRequest request) {
        log.info("Updating school id: {}", schoolId);

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHOOL_NOT_FOUND));

        // Check if new name conflicts with existing school
        if (!school.getName().equals(request.getName()) && 
            schoolRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.SCHOOL_ALREADY_EXISTS);
        }

        school.setName(request.getName());
        school.setAddress(request.getAddress());
        school.setCity(request.getCity());
        school.setDistrict(request.getDistrict());
        school.setPhoneNumber(request.getPhoneNumber());
        school.setEmail(request.getEmail());
        school.setWebsite(request.getWebsite());

        school = schoolRepository.save(school);
        log.info("School updated successfully");

        return mapToResponse(school);
    }

    @Override
    public SchoolResponse getSchoolById(Long schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHOOL_NOT_FOUND));

        return mapToResponse(school);
    }

    @Override
    public Page<SchoolResponse> getAllSchools(Pageable pageable) {
        return schoolRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Override
    public List<SchoolResponse> getAllSchools() {
        return schoolRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SchoolResponse> searchSchoolsByName(String name) {
        // Simple search - could be enhanced with more sophisticated search
        return schoolRepository.findAll().stream()
                .filter(school -> school.getName().toLowerCase().contains(name.toLowerCase()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSchool(Long schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHOOL_NOT_FOUND));

        schoolRepository.delete(school);
        log.info("School deleted: {}", schoolId);
    }

    private SchoolResponse mapToResponse(School school) {
        return SchoolResponse.builder()
                .id(school.getId())
                .name(school.getName())
                .address(school.getAddress())
                .city(school.getCity())
                .district(school.getDistrict())
                .phoneNumber(school.getPhoneNumber())
                .email(school.getEmail())
                .website(school.getWebsite())
                .createdAt(school.getCreatedAt())
                .updatedAt(school.getUpdatedAt())
                .build();
    }
}
