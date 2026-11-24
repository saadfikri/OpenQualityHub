package com.openqualityhub.repository;

import com.openqualityhub.entity.DataField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DataFieldRepository extends JpaRepository<DataField, UUID> {

    List<DataField> findByTemplateIdAndDeletedFalseOrderByDisplayOrder(UUID templateId);
}
