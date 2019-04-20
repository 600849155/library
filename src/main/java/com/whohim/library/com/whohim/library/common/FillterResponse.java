package com.whohim.library.com.whohim.library.common;

import lombok.Data;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.Serializable;

/**
 * @author WhomHim
 * @description
 * @date Create in 2019/4/19 18:10
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@Data
public class FillterResponse<T> implements Serializable {
    private int status;
    private String msg;
    private T data;
    private int result;
}
