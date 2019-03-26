package com.whohim.library.com.whohim.library.com.whohim.springboot.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.whohim.library.com.whohim.library.common.DateTimeUtil;
import com.whohim.library.com.whohim.library.common.HttpUtil;
import com.whohim.library.com.whohim.library.common.ServerResponse;
import com.whohim.library.com.whohim.library.pojo.User;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
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

    private static final String HVAE_SEAT = "1";

    /** 午餐时间 **/
    private static final String LUNCH_BREAK = "11:30-12:30";

    /** 晚餐时间 **/
    private static final String DINNER_TIME = "17:00-18:00";

    /** 日期格式 **/
    private static final String HOUR_MINNI = "HH:mm";

    private static final String BIND_SUCCESS = "ok";

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
                if (HVAE_SEAT.equals(stringRedisTemplate.opsForValue().get(userId))
                        || user.getOpenId().equals(stringRedisTemplate.opsForValue().get(openId))) {
                    return ServerResponse.createByErrorMessage("同学，一个人只能占一个位哦！");
                }
            }
            if (isInTime(LUNCH_BREAK, DateTimeUtil.dateToStr(new Date(), HOUR_MINNI))) {
                //向redis里存入数据和设置缓存时间
                stringRedisTemplate.opsForValue().set(seat, personal, 60 * 90, TimeUnit.SECONDS);
                return ServerResponse.createBySuccessMessage("占座成功！");
            }
            if (isInTime(DINNER_TIME, DateTimeUtil.dateToStr(new Date(), HOUR_MINNI))) {
                //向redis里存入数据和设置缓存时间
                stringRedisTemplate.opsForValue().set(seat, personal, 60 * 90, TimeUnit.SECONDS);
                return ServerResponse.createBySuccessMessage("占座成功！");
            }
            //向redis里存入数据和设置缓存时间
            stringRedisTemplate.opsForValue().set(seat, personal, 60 * 30, TimeUnit.SECONDS);
            //用于判断此学号是否占两个座位
            stringRedisTemplate.opsForValue().set(userId, HVAE_SEAT, 60 * 30, TimeUnit.SECONDS);
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
        String dateTimeUserId = stringRedisTemplate.opsForValue().get(seat);
        Long ttlTime = stringRedisTemplate.getExpire(seat);
        logger.info(String.valueOf(ttlTime / 60.0));
        String[] strs = dateTimeUserId.split(",");
        HashMap<String, String> res = new HashMap<>(6,1);
        res.put("Datetime", strs[0]);
        res.put("userId", strs[1]);
        res.put("openId", strs[2]);
        res.put("avatarUrl", strs[3]);
        res.put("nickName", strs[4]);
        return ServerResponse.createByErrorMessage("该座位有人！", res);
    }

    @PostMapping("/user/bindLibrary")
    @ResponseBody
    public ServerResponse bindLibrary(@RequestParam(name = "barcode")String barcode,@RequestParam(name = "password")String password){
        if(StringUtils.isBlank(barcode)||StringUtils.isBlank(password)){
            return ServerResponse.createByErrorMessage("读者条码或密码不能为空!");
        }
        Map parms = new HashMap<>(4,1);
        parms.put("login_type","barcode");
        parms.put("barcode",barcode);
        parms.put("password",password);
        String response = HttpUtil.sendPost("http://61.142.33.201:8080/opac_two/include/login_app.jsp",parms);
        if (response.equals(BIND_SUCCESS)){
             return ServerResponse.createBySuccessMessage("绑定成功！");
        }
        return ServerResponse.createByErrorMessage("绑定失败！");
    }

    @PostMapping("/user/get_sessionkey/")
    @ResponseBody
    public JSONObject getSessionKeyOropenId(String code) {
        //微信端登录code值
        String wxCode = code;
        //请求地址 https://api.weixin.qq.com/sns/jscode2session
        String requestUrl = "https://api.weixin.qq.com/sns/jscode2session"; 
        Map<String, String> requestUrlParam = new HashMap<>(5,1);
        //开发者设置中的appId
        requestUrlParam.put("appid", "wx4a326c92f0674dd7");
        //开发者设置中的appSecret
        requestUrlParam.put("secret", "0323d6209b119f50f86cb1c19d979752");
        //小程序调用wx.login返回的code
        requestUrlParam.put("js_code", wxCode);
        //默认参数
        requestUrlParam.put("grant_type", "authorization_code");
        //发送post请求读取调用微信 https://api.weixin.qq.com/sns/jscode2session 接口获取openid用户唯一标识
        JSONObject jsonObject = JSON.parseObject(HttpUtil.sendPost(requestUrl, requestUrlParam)); 
        logger.info(jsonObject.toString());
        return jsonObject;
    }


}
