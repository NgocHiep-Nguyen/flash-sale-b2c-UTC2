package com.b2c.flash_sale_b2c_UTC2.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite Primary Key cho bảng group_permissions.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupPermissionId implements Serializable {

    @Column(name = "group_id")
    private Integer groupId;

    @Column(name = "permission_id")
    private Integer permissionId;
}
