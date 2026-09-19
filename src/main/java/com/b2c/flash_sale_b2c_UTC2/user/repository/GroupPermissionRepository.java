package com.b2c.flash_sale_b2c_UTC2.user.repository;

import com.b2c.flash_sale_b2c_UTC2.user.entity.GroupPermission;
import com.b2c.flash_sale_b2c_UTC2.user.entity.GroupPermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupPermissionRepository extends JpaRepository<GroupPermission, GroupPermissionId> {
    List<GroupPermission> findByIdGroupId(Integer groupId);
}
