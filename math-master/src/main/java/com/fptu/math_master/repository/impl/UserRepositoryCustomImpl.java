package com.fptu.math_master.repository.impl;

import com.fptu.math_master.dto.request.UserSearchRequest;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.repository.UserRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<User> searchUsers(UserSearchRequest request, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> user = query.from(User.class);

        List<Predicate> predicates = new ArrayList<>();

        // Keyword search (userName, fullName, email)
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            String keyword = "%" + request.getKeyword().toLowerCase() + "%";
            Predicate userNamePredicate = cb.like(cb.lower(user.get("userName")), keyword);
            Predicate fullNamePredicate = cb.like(cb.lower(user.get("fullName")), keyword);
            Predicate emailPredicate = cb.like(cb.lower(user.get("email")), keyword);
            predicates.add(cb.or(userNamePredicate, fullNamePredicate, emailPredicate));
        }

        // Specific field filters
        if (request.getUserName() != null && !request.getUserName().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(user.get("userName")), "%" + request.getUserName().toLowerCase() + "%"));
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(user.get("email")), "%" + request.getEmail().toLowerCase() + "%"));
        }

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(user.get("fullName")), "%" + request.getFullName().toLowerCase() + "%"));
        }

        if (request.getGender() != null) {
            predicates.add(cb.equal(user.get("gender"), request.getGender()));
        }

        if (request.getStatus() != null) {
            predicates.add(cb.equal(user.get("status"), request.getStatus()));
        }

        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(user.get("code")), "%" + request.getCode().toLowerCase() + "%"));
        }

        if (request.getDobFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(user.get("dob"), request.getDobFrom()));
        }

        if (request.getDobTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(user.get("dob"), request.getDobTo()));
        }

        // Role filter
        if (request.getRoleName() != null && !request.getRoleName().trim().isEmpty()) {
            Join<Object, Object> roles = user.join("roles", JoinType.LEFT);
            predicates.add(cb.equal(cb.lower(roles.get("name")), request.getRoleName().toLowerCase()));
        }

        query.where(predicates.toArray(new Predicate[0]));

        // Get total count
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<User> countRoot = countQuery.from(User.class);
        countQuery.select(cb.count(countRoot));

        // Apply same predicates for count
        List<Predicate> countPredicates = new ArrayList<>();
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            String keyword = "%" + request.getKeyword().toLowerCase() + "%";
            Predicate userNamePredicate = cb.like(cb.lower(countRoot.get("userName")), keyword);
            Predicate fullNamePredicate = cb.like(cb.lower(countRoot.get("fullName")), keyword);
            Predicate emailPredicate = cb.like(cb.lower(countRoot.get("email")), keyword);
            countPredicates.add(cb.or(userNamePredicate, fullNamePredicate, emailPredicate));
        }
        if (request.getUserName() != null && !request.getUserName().trim().isEmpty()) {
            countPredicates.add(cb.like(cb.lower(countRoot.get("userName")), "%" + request.getUserName().toLowerCase() + "%"));
        }
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            countPredicates.add(cb.like(cb.lower(countRoot.get("email")), "%" + request.getEmail().toLowerCase() + "%"));
        }
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            countPredicates.add(cb.like(cb.lower(countRoot.get("fullName")), "%" + request.getFullName().toLowerCase() + "%"));
        }
        if (request.getGender() != null) {
            countPredicates.add(cb.equal(countRoot.get("gender"), request.getGender()));
        }
        if (request.getStatus() != null) {
            countPredicates.add(cb.equal(countRoot.get("status"), request.getStatus()));
        }
        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            countPredicates.add(cb.like(cb.lower(countRoot.get("code")), "%" + request.getCode().toLowerCase() + "%"));
        }
        if (request.getDobFrom() != null) {
            countPredicates.add(cb.greaterThanOrEqualTo(countRoot.get("dob"), request.getDobFrom()));
        }
        if (request.getDobTo() != null) {
            countPredicates.add(cb.lessThanOrEqualTo(countRoot.get("dob"), request.getDobTo()));
        }
        if (request.getRoleName() != null && !request.getRoleName().trim().isEmpty()) {
            Join<Object, Object> rolesCount = countRoot.join("roles", JoinType.LEFT);
            countPredicates.add(cb.equal(cb.lower(rolesCount.get("name")), request.getRoleName().toLowerCase()));
        }

        countQuery.where(countPredicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        // Execute paginated query
        List<User> results = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        return new PageImpl<>(results, pageable, total);
    }
}

