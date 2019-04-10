package com.whohim.library.com.whohim.library.pojo;

import lombok.*;

import java.util.List;

/**
 * @Author:WhomHim
 * @Description:
 * @Date: Create in 2019/4/10 16:40
 * @Modified by:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserInfo {
    private List<User>userList;
}
