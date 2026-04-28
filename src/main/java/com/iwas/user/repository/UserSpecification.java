package com.iwas.user.repository;

import com.iwas.user.dto.UserFilterRequest;
import com.iwas.user.entity.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class UserSpecification {

    private UserSpecification() {}

    public static Specification<User> fromFilter(UserFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("isDeleted"), false));

            if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
                String pattern = "%" + filter.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern)
                ));
            }

            if (filter.getRole() != null) {
                predicates.add(cb.equal(root.get("role"), filter.getRole()));
            }

            if (filter.getPosition() != null && !filter.getPosition().isBlank()) {
                String pattern = "%" + filter.getPosition().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("position")), pattern));
            }

            if (filter.getActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), filter.getActive()));
            }

            if (filter.getVerified() != null) {
                predicates.add(cb.equal(root.get("isVerified"), filter.getVerified()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}