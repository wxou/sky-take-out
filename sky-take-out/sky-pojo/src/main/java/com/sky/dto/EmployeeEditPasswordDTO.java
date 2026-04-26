package com.sky.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(description = "员工修改密码")
public class EmployeeEditPasswordDTO {
    private Long empId;

    @ApiModelProperty("旧密码")
    private String oldPassword;

    @ApiModelProperty("新密码")
    private String newPassword;


    private LocalDateTime updateTime;


    private Long updateUser;
}
