package com.fptu.math_master.repository;

import com.fptu.math_master.dto.request.UserSearchRequest;
import com.fptu.math_master.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepositoryCustom {
    Page<User> searchUsers(UserSearchRequest request, Pageable pageable);
}

