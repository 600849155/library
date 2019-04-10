package com.whohim.library.com.whohim.library.pojo;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.codehaus.jackson.annotate.JsonIgnore;

/**
 * @Author WhomHim
 */
@Data
@Getter
@Setter

public class User{
    private String avatarUrl;

    private String nickName;

    private String openId;

    private String userId;
    @JsonIgnore
    private int id;

    private String seat;

    private String barcode;

}
