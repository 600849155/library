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

    @JsonIgnore
    private String avatarUrl;

    @JsonIgnore
    private String nickName;

    private String openId;

    @JsonIgnore
    private String userId;

    @JsonIgnore
    private int id;

    private String seat;

    private String barcode;

}
