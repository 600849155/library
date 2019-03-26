package com.whohim.library.com.whohim.library.com.whohim.springboot.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.whohim.library.com.whohim.library.common.DateTimeUtil;
import com.whohim.library.com.whohim.library.common.HttpUtil;
import com.whohim.library.com.whohim.library.common.ServerResponse;

import com.whohim.library.com.whohim.library.pojo.User;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.apache.commons.lang3.StringUtils.*;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.whohim.library.com.whohim.library.common.DateTimeUtil.isInTime;
import static org.apache.commons.lang3.StringUtils.isBlank;


/**
 * @Author: WhomHim
 */
@Controller
public class UserController {

    private static Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static int EXIT

    @PostMapping("/markLeave/")
    @ResponseBody
    public ServerResponse postMarkLeave(User user) throws Exception {
        String userId = user.getUserId();
        String seat = user.getSeat();
        String openId = user.getOpenId();
        String avatarUrl = user.getAvatarUrl();
        String nickName = user.getNickName();
        String personal = DateTimeUtil.dateToStr(new Date(), "yyyy-MM-dd HH:mm:ss") + "," + userId + "," + openId + "," + avatarUrl + "," + nickName + ",";

        if (isBlank(seat) || isBlank(userId)) {
            return ServerResponse.createByErrorMessage("座位号或学号不能为空！");
        }
        String dateTimeUserIdRedis = stringRedisTemplate.opsForValue().get(seat);

        if (isBlank(dateTimeUserIdRedis)) {
            if (stringRedisTemplate.opsForValue().get(userId) != null) {
                if (stringRedisTemplate.opsForValue().get(userId).equals("1") || stringRedisTemplate.opsForValue().get(openId).equals(user.getOpenId())) {
                    return ServerResponse.createByErrorMessage("同学，一个人只能占一个位哦！");
                }
            }
            if (isInTime("11:30-12:30", DateTimeUtil.dateToStr(new Date(), "HH:mm"))) {
                stringRedisTemplate.opsForValue().set(seat, personal, 60 * 90, TimeUnit.SECONDS);//向redis里存入数据和设置缓存时间
                return ServerResponse.createBySuccessMessage("占座成功！");
            }
            if (isInTime("17:00-18:00", DateTimeUtil.dateToStr(new Date(), "HH:mm"))) {
                stringRedisTemplate.opsForValue().set(seat, personal, 60 * 90, TimeUnit.SECONDS);//向redis里存入数据和设置缓存时间
                return ServerResponse.createBySuccessMessage("占座成功！");
            }
            stringRedisTemplate.opsForValue().set(seat, personal, 60 * 30, TimeUnit.SECONDS);//向redis里存入数据和设置缓存时间
            stringRedisTemplate.opsForValue().set(userId, "1", 60 * 30, TimeUnit.SECONDS);//用于判断此学号是否占两个座位
            return ServerResponse.createBySuccessMessage("占座成功！");
        }
        String[] durs = dateTimeUserIdRedis.split(",");

        if (durs[1].equals(userId)) {
            return ServerResponse.createByErrorCodeMessage(2, "已经占座，请不要重复留座！");
        }
        if (stringRedisTemplate.opsForValue().get(seat) != null) {
            return ServerResponse.createByErrorMessage("该座位有人！");
        }
        return ServerResponse.createByErrorMessage("占座失败！");
    }

    @GetMapping("/markLeave/{seat}")
    @ResponseBody
    public ServerResponse getMarkLeave(@PathVariable("seat") String seat) throws Exception {
        if (isBlank(stringRedisTemplate.opsForValue().get(seat))) {
            return ServerResponse.createBySuccessMessage("该座位没人！");
        }
        String DateTime_userId = stringRedisTemplate.opsForValue().get(seat);
        Long ttl_time = stringRedisTemplate.getExpire(seat);
        logger.info(String.valueOf(ttl_time / 60.0));
        String[] strs = DateTime_userId.split(",");
        HashMap<String, String> res = new HashMap<>();
        res.put("Datetime", strs[0]);
        res.put("userId", strs[1]);
        res.put("openId", strs[2]);
        res.put("avatarUrl", strs[3]);
        res.put("nickName", strs[4]);
        return ServerResponse.createByErrorMessage("该座位有人！", res);
    }

    @PostMapping("/user/get_sessionkey/")
    @ResponseBody
    public JSONObject getSessionKeyOropenId(String code) {
        String wxCode = code;//微信端登录code值
        String requestUrl = "https://api.weixin.qq.com/sns/jscode2session"; //请求地址 https://api.weixin.qq.com/sns/jscode2session
        Map<String, String> requestUrlParam = new HashMap<String, String>();
        requestUrlParam.put("appid", "wx4a326c92f0674dd7"); //开发者设置中的appId
        requestUrlParam.put("secret", "0323d6209b119f50f86cb1c19d979752"); //开发者设置中的appSecret
        requestUrlParam.put("js_code", wxCode); //小程序调用wx.login返回的code
        requestUrlParam.put("grant_type", "authorization_code");  //默认参数
        JSONObject jsonObject = JSON.parseObject(HttpUtil.sendPost(requestUrl, requestUrlParam)); //发送post请求读取调用微信 https://api.weixin.qq.com/sns/jscode2session 接口获取openid用户唯一标识
        System.out.println(jsonObject);
        return jsonObject;
    }


}
