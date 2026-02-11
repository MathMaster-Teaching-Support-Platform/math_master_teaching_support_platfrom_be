package com.fptu.math_master.repository;

import com.fptu.math_master.dto.request.UserSearchRequest;
import com.fptu.math_master.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class UserSpecification {

    public static Specification<User> searchUsers(UserSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Keyword search (userName, fullName, email)
            if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
                String keyword = "%" + request.getKeyword().toLowerCase() + "%";
                Predicate userNamePredicate = cb.like(cb.lower(root.get("userName")), keyword);
                Predicate fullNamePredicate = cb.like(cb.lower(root.get("fullName")), keyword);
                Predicate emailPredicate = cb.like(cb.lower(root.get("email")), keyword);
                predicates.add(cb.or(userNamePredicate, fullNamePredicate, emailPredicate));
            }

            // Specific field filters
            if (request.getUserName() != null && !request.getUserName().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("userName")), 
                    "%" + request.getUserName().toLowerCase() + "%"));
            }

            if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("email")), 
                    "%" + request.getEmail().toLowerCase() + "%"));
            }

            if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("fullName")), 
                    "%" + request.getFullName().toLowerCase() + "%"));
            }

            if (request.getGender() != null) {
                predicates.add(cb.equal(root.get("gender"), request.getGender()));
            }

            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }

            if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("code")), 
                    "%" + request.getCode().toLowerCase() + "%"));
            }

            if (request.getDobFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dob"), request.getDobFrom()));
            }

            if (request.getDobTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dob"), request.getDobTo()));
            }

            // Role filter
            if (request.getRoleName() != null && !request.getRoleName().trim().isEmpty()) {
                Join<Object, Object> roles = root.join("roles", JoinType.LEFT);
                predicates.add(cb.equal(cb.lower(roles.get("name")), 
                    request.getRoleName().toLowerCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
