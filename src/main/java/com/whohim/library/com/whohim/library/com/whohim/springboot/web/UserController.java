package com.whohim.library.com.whohim.library.com.whohim.springboot.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.whohim.library.com.whohim.library.pojo.UserInfo;
import com.whohim.library.com.whohim.library.util.DateTimeUtil;
import com.whohim.library.com.whohim.library.util.HttpUtil;
import com.whohim.library.com.whohim.library.common.ServerResponse;
import com.whohim.library.com.whohim.library.pojo.User;
import com.whohim.library.com.whohim.library.util.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.whohim.library.com.whohim.library.util.DateTimeUtil.isInTime;


/**
 * @Author: WhomHim
 */
@Controller
public class UserController {

    private static Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String HVAE_SEAT = "1";
    /**
     * 午餐时间
     **/
    private static final String LUNCH_BREAK = "11:30-12:30";
    /**
     * 晚餐时间
     **/
    private static final String DINNER_TIME = "17:00-18:00";
    /**
     * 日期格式
     **/
    private static final String HOUR_MINNI = "HH:mm";

    private static final String BIND_SUCCESS = "ok";
    /**
     * 留座时间
     **/
    private static final int DATE_TIME = 0;
    /**
     * 图书证号
     **/
    private static final int USER_ID = 1;
    /**
     * openId
     **/
    private static final int OPEN_ID = 2;
    /**
     * 头像
     **/
    private static final int AVATARURL = 3;
    /**
     * 昵称
     **/
    private static final int NICKNAME = 4;

    /** 存进redis的user_info表 **/
    private static final String USER_INFO = "user_info";

    @PostMapping("/markLeave/")
    @ResponseBody
    public ServerResponse postMarkLeave(User user) throws Exception {
        /* 这个是卡号 */
        String userId = user.getUserId();
        String seat = user.getSeat();
        String openId = user.getOpenId();
        String avatarUrl = user.getAvatarUrl();
        String nickName = user.getNickName();
        String personal = DateTimeUtil.dateToStr(new Date(), "yyyy-MM-dd HH:mm:ss") + "," + userId + "," + openId + "," + avatarUrl + "," + nickName + ",";

        if (StringUtils.isBlank(openId) || StringUtils.isBlank(avatarUrl) || StringUtils.isBlank(seat) || StringUtils.isBlank(userId)) {
            return ServerResponse.createByErrorMessage("参数不能为空!");
        }

        String userInfo = stringRedisTemplate.opsForValue().get(seat);
        if (StringUtils.isBlank(userInfo)) {
            if (stringRedisTemplate.opsForValue().get(userId) != null) {
                if (HVAE_SEAT.equals(stringRedisTemplate.opsForValue().get(userId))
                        || user.getOpenId().equals(stringRedisTemplate.opsForValue().get(openId))) {
                    return ServerResponse.createByErrorMessage("只能留一个位哦！");
                }
            }
            if (isInTime(LUNCH_BREAK, DateTimeUtil.dateToStr(new Date(), HOUR_MINNI))) {
                //向redis里存入数据和设置缓存时间
                stringRedisTemplate.opsForValue().set(seat, personal, 60 * 90, TimeUnit.SECONDS);
                return ServerResponse.createBySuccessMessage("留座成功！");
            }
            if (isInTime(DINNER_TIME, DateTimeUtil.dateToStr(new Date(), HOUR_MINNI))) {
                //向redis里存入数据和设置缓存时间
                stringRedisTemplate.opsForValue().set(seat, personal, 60 * 90, TimeUnit.SECONDS);
                return ServerResponse.createBySuccessMessage("留座成功！");
            }
            //向redis里存入数据和设置缓存时间
            stringRedisTemplate.opsForValue().set(seat, personal, 60 * 30, TimeUnit.SECONDS);
            //用于判断此学号是否留两个座位
            stringRedisTemplate.opsForValue().set(userId, HVAE_SEAT, 60 * 30, TimeUnit.SECONDS);
            return ServerResponse.createBySuccessMessage("留座成功！");
        }
        String[] detailInfo = userInfo.split(",");

        if (detailInfo[USER_ID].equals(userId)) {
            return ServerResponse.createByErrorCodeMessage(2, "已经留座，请不要重复留座！");
        }
        if (stringRedisTemplate.opsForValue().get(seat) != null) {
            return ServerResponse.createByErrorMessage("该座位有人！");
        }
        return ServerResponse.createByErrorMessage("留座失败！");
    }

    @GetMapping("/markLeave/{seat}")
    @ResponseBody
    public ServerResponse getMarkLeave(@PathVariable("seat") String seat) throws Exception {
        if (StringUtils.isBlank(stringRedisTemplate.opsForValue().get(seat))) {
            return ServerResponse.createBySuccessMessage("该座位没人！");
        }
        String userInfo = stringRedisTemplate.opsForValue().get(seat);
        Long ttlTime = stringRedisTemplate.getExpire(seat);
        logger.info(String.valueOf(ttlTime / 60.0));
        String[] detailInfo = userInfo.split(",");
        HashMap<String, String> res = new HashMap<String, String>(6, 1) {
            {
                put("Datetime", detailInfo[DATE_TIME]);
                put("userId", detailInfo[USER_ID]);
                put("openId", detailInfo[OPEN_ID]);
                put("avatarUrl", detailInfo[AVATARURL]);
                put("nickName", detailInfo[NICKNAME]);
            }
        };
        return ServerResponse.createByErrorMessage("该座位有人！", res);
    }

    @PostMapping("/canceSeat/")
    @ResponseBody
    public ServerResponse cancelSeat(@RequestParam("seat") String seat, @RequestParam("openId") String openId) {
        if (StringUtils.isBlank(seat) || StringUtils.isBlank(openId)) {
            return ServerResponse.createByErrorMessage("参数不能为空!");
        }
        String userInfo = stringRedisTemplate.opsForValue().get(seat);
        if (StringUtils.isBlank(userInfo)) {
            return ServerResponse.createByErrorMessage("同学，你没有留座！");
        }
        String[] detailInfo = userInfo.split(",");
        if (!detailInfo[OPEN_ID].equals(openId)) {
            return ServerResponse.createByErrorMessage("传错参了哦！");
        }

         /* 取消留座逻辑 */
        stringRedisTemplate.delete(detailInfo[USER_ID]);
        stringRedisTemplate.delete(seat);
        return ServerResponse.createBySuccessMessage("取消留座成功！");
    }


    @PostMapping("/user/bindLibrary")
    @ResponseBody
    public ServerResponse bindLibrary(@RequestParam("openId") String openId,
                                      @RequestParam(name = "barcode") String barcode,
                                      @RequestParam(name = "password") String password) {
        if (StringUtils.isBlank(barcode) || StringUtils.isBlank(password) || StringUtils.isBlank(openId)) {
            return ServerResponse.createByErrorMessage("借阅卡卡号或密码不能为空!");
        }

        /* 初始化user_info表 */
        if (StringUtils.isBlank(stringRedisTemplate.opsForValue().get(USER_INFO))){
            UserInfo userInfo = new UserInfo();
            List<User>userList = new ArrayList<>();
            User user = new User();
            user.setBarcode("123456");
            user.setOpenId("asd1223asd");
            userList.add(user);
            userInfo.setUserList(userList);
            stringRedisTemplate.opsForValue().set(USER_INFO, JsonUtil.obj2StringPretty(userInfo));
            logger.info("user_info表初始化成功！");
        }

        Map parms = new HashMap<String, String>(4, 1) {
            {
                put("login_type", "barcode");
                put("barcode", barcode);
                put("password", password);
            }
        };
        String response = HttpUtil.sendPost("http://61.142.33.201:8080/opac_two/include/login_app.jsp", parms);
        if (response.equals(BIND_SUCCESS)) {
            UserInfo userInfo = JsonUtil.string2Obj(stringRedisTemplate.opsForValue().get(USER_INFO),UserInfo.class);
            List<User>userList= userInfo.getUserList();
            /* openId和barcode是一对一关系 判断是否被绑定过 */
            for (User user : userList) {
                if (user.getBarcode().equals(barcode)) {
                    if (user.getOpenId().equals(openId)) {
                        return ServerResponse.createByErrorMessage("您已绑定过了！");
                    }
                }
                if (user.getOpenId().equals(openId)) {
                    return ServerResponse.createByErrorMessage("该借阅卡已绑定！");
                }
            }
            /* 没有被绑定过则添加进redis里 */
            User user = new User();
            user.setBarcode(barcode);
            user.setOpenId(openId);
            userList.add(user);
            userInfo.setUserList(userList);
            stringRedisTemplate.opsForValue().set(USER_INFO, JsonUtil.obj2StringPretty(userInfo));
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
        Map<String, String> requestUrlParam = new HashMap<>(5, 1);
        //开发者设置中的appId
        requestUrlParam.put("appid", "wx4a326c92f0674dd7");
        //开发者设置中的appSecret
        requestUrlParam.put("secret", "c7bc37c888cf10b18f16260ce33a889b");
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
