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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.whohim.library.com.whohim.library.common.Constant.*;
import static com.whohim.library.com.whohim.library.util.DateTimeUtil.isInTime;


/**
 * @author WhomHim
 */

@Controller
@RequestMapping("/user")
public class UserController {

    private static Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @PostMapping("/markLeave")
    @ResponseBody
    public ServerResponse postMarkLeave(User user, String userId) throws Exception {
        /* 迁就前端的不规范写法 */
        String barcode = userId;
        String seat = user.getSeat();
        String openId = user.getOpenId();
        String avatarUrl = user.getAvatarUrl();
        String nickName = user.getNickName();
        String personal = DateTimeUtil.dateToStr(new Date(), "yyyy-MM-dd HH:mm:ss") + "," + barcode + "," + openId + "," + avatarUrl + "," + nickName + ",";

        if (StringUtils.isBlank(openId) || StringUtils.isBlank(avatarUrl) || StringUtils.isBlank(seat) || StringUtils.isBlank(barcode)) {
            return ServerResponse.createByErrorMessage("参数不能为空!");
        }

        String userInfo = stringRedisTemplate.opsForValue().get(seat);
        String[] detailInfo = userInfo.split(",");

        if (detailInfo[BARCOCE].equals(barcode) || stringRedisTemplate.opsForValue().get(barcode) != null) {
            return ServerResponse.createByErrorCodeMessage(2, "已经留座，请不要重复留座！");
        }
        if (stringRedisTemplate.opsForValue().get(seat) != null) {
            return ServerResponse.createByErrorMessage("该座位有人！");
        }

        if (StringUtils.isBlank(userInfo)) {
            if (stringRedisTemplate.opsForValue().get(barcode) != null) {
                if (HVAE_SEAT.equals(stringRedisTemplate.opsForValue().get(barcode))
                        || user.getOpenId().equals(stringRedisTemplate.opsForValue().get(openId))) {
                    return ServerResponse.createByErrorMessage("只能留一个位哦！");
                }
            }
            if (isInTime(LUNCH_BREAK, DateTimeUtil.dateToStr(new Date(), HOUR_MINNI))) {
                return setStringBySeatAndBarcode(user, personal, 60);
            }
            if (isInTime(DINNER_TIME, DateTimeUtil.dateToStr(new Date(), HOUR_MINNI))) {
                return setStringBySeatAndBarcode(user, personal, 60);
            }
            return setStringBySeatAndBarcode(user, personal, 30);
        }

        return ServerResponse.createByErrorMessage("留座失败！");
    }

    @GetMapping("/markLeave/{seat}")
    @ResponseBody
    public ServerResponse getMarkLeave(@PathVariable("seat") String seat) throws Exception {
        if (StringUtils.isBlank(seat)) {
            return ServerResponse.createByErrorMessage("参数不能为空！");
        }
        if (StringUtils.isBlank(stringRedisTemplate.opsForValue().get(seat))) {
            return ServerResponse.createBySuccessMessage("该座位没人！");
        }
        String userInfo = stringRedisTemplate.opsForValue().get(seat);
        int ttl = 0;
        if (stringRedisTemplate.getExpire(seat) != null) {
            ttl = Math.toIntExact(stringRedisTemplate.getExpire(seat));
        }
        String ttlTime = DateTimeUtil.getTime(ttl);
        String[] detailInfo = userInfo != null ? userInfo.split(",") : new String[0];
        HashMap<String, String> res = new HashMap<String, String>(8, 1) {
            {
                put("Datetime", detailInfo[DATE_TIME]);
                put("userId", detailInfo[BARCOCE]);
                put("openId", detailInfo[OPEN_ID]);
                put("avatarUrl", detailInfo[AVATARURL]);
                put("nickName", detailInfo[NICKNAME]);
                put("seat", seat);
                put("ttlTime", String.valueOf(ttlTime));
            }
        };
        return ServerResponse.createByErrorMessage("该座位有人！", res);
    }

    @PostMapping("/checkOneselfSeat")
    @ResponseBody
    public ServerResponse checkOneselfSeat(@RequestParam("openId") String openId) {
        if (StringUtils.isBlank(openId)) {
            return ServerResponse.createByErrorMessage("传值不能为空!");
        }
        /* 从redis拿整个表出来 */
        UserInfo userInfo = JsonUtil.string2Obj(stringRedisTemplate.opsForValue().get(USER_INFO), UserInfo.class);
        if (userInfo == null) {
            return ServerResponse.createByErrorMessage("user_info未初始化！");
        }
        CopyOnWriteArrayList<User> userList = userInfo.getUserInfo();
        AtomicReference<String> barcode = new AtomicReference<>();
        userList.stream().filter(user -> user.getOpenId().equals(openId)).map(User::getBarcode).forEach(barcode::set);
        String onselfSeatInfo = stringRedisTemplate.opsForValue().get(barcode.get());
        String[] detailInfo = new String[0];
        if (onselfSeatInfo != null) {
            detailInfo = onselfSeatInfo.split(",");
        }
        if (onselfSeatInfo == null) {
            return ServerResponse.createByErrorMessage("尚未留座！");
        }
        int ttl = 0;
        if (stringRedisTemplate.getExpire(barcode.get()) != null) {
            ttl = Math.toIntExact(stringRedisTemplate.getExpire(barcode.get()));
        }
        String ttlTime = DateTimeUtil.getTime(ttl);
        String[] finalDetailInfo = detailInfo;
        HashMap<String, String> res = new HashMap<String, String>(8, 1) {
            {
                put("Datetime", finalDetailInfo[DATE_TIME]);
                put("userId", finalDetailInfo[BARCOCE]);
                put("openId", finalDetailInfo[OPEN_ID]);
                put("avatarUrl", finalDetailInfo[AVATARURL]);
                put("nickName", finalDetailInfo[NICKNAME]);
                put("ttlTime", String.valueOf(ttlTime));
            }
        };
        return ServerResponse.createBySuccess(res);
    }

    @PostMapping("/cancelSeat")
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
        stringRedisTemplate.delete(detailInfo[BARCOCE]);
        stringRedisTemplate.delete(seat);
        return ServerResponse.createBySuccessMessage("取消留座成功！");
    }


    @PostMapping("/bindLibrary")
    @ResponseBody
    public ServerResponse bindLibrary(@RequestParam("openId") String openId,
                                      @RequestParam(name = "barcode") String barcode,
                                      @RequestParam(name = "password") String password) {
        if (StringUtils.isBlank(barcode) || StringUtils.isBlank(password) || StringUtils.isBlank(openId)) {
            return ServerResponse.createByErrorMessage("借阅卡卡号或密码不能为空!");
        }

        Map<String, String> parms = new HashMap<String, String>(4, 1) {
            {
                put("login_type", "barcode");
                put("barcode", barcode);
                put("password", password);
            }
        };
        String response = HttpUtil.sendPost("http://61.142.33.201:8080/opac_two/include/login_app.jsp", parms);
        if (response.equals(BIND_SUCCESS)) {
            UserInfo userInfo = JsonUtil.string2Obj(stringRedisTemplate.opsForValue().get(USER_INFO), UserInfo.class);
            CopyOnWriteArrayList<User> userList = null;
            if (userInfo != null) {
                userList = userInfo.getUserInfo();
            }
            /* openId和barcode是一对一关系 判断是否被绑定过 */
            if (userList != null) {
                if (userList.stream().anyMatch(user -> user.getOpenId().equals(openId) && user.getBarcode().equals(barcode))) {
                    return ServerResponse.createByErrorMessage("您已绑定过了！");
                }
                if (userList.stream().anyMatch(user -> user.getBarcode().equals(barcode))) {
                    return ServerResponse.createByErrorMessage("该借阅卡已被他人绑定！");
                }
            }

            /* 没有被绑定过则添加进redis里 */
            User user = new User();
            user.setBarcode(barcode);
            user.setOpenId(openId);
            if (userList != null) {
                userList.add(user);
            }
            if (userInfo != null) {
                userInfo.setUserInfo(userList);
            }
            stringRedisTemplate.opsForValue().set(USER_INFO, JsonUtil.obj2StringPretty(userInfo));
            return ServerResponse.createBySuccessMessage("绑定成功！");
        }
        return ServerResponse.createByErrorMessage("绑定失败！");
    }

    @PostMapping("/cancelBindLibrary")
    @ResponseBody
    public ServerResponse cancleBindLibrary(@RequestParam("openId") String openId, @RequestParam(name = "barcode") String barcode) {
        if (StringUtils.isBlank(barcode) || StringUtils.isBlank(openId)) {
            return ServerResponse.createByErrorMessage("传值不能为空!");
        }
        /* 从redis拿整个表出来 */
        UserInfo userInfo = JsonUtil.string2Obj(stringRedisTemplate.opsForValue().get(USER_INFO), UserInfo.class);
        if (userInfo == null) {
            return ServerResponse.createByErrorMessage("user_info未初始化！");
        }
        CopyOnWriteArrayList<User> userList = userInfo.getUserInfo();
        /* 如果redis有存到对应的借阅卡和用户信息则删除 */
        for (User user : userList) {
            if (user.getBarcode().equals(barcode) && user.getOpenId().equals(openId)) {
                userList.remove(user);
                /* 删除之后再将整个表放进redis */
                userInfo.setUserInfo(userList);
                try {
                    stringRedisTemplate.opsForValue().set(USER_INFO, JsonUtil.obj2StringPretty(userInfo));
                } catch (Exception e) {
                    return ServerResponse.createByErrorMessage("取消绑定失败！");
                }
                return ServerResponse.createBySuccessMessage("取消绑定成功！");
            }
        }
        return ServerResponse.createBySuccessMessage("取消绑定失败！");
    }

    @PostMapping("/getSessionkey")
    @ResponseBody
    public JSONObject getSessionKeyOropenId(String code) {
        //请求地址 https://api.weixin.qq.com/sns/jscode2session
        String requestUrl = "https://api.weixin.qq.com/sns/jscode2session";
        Map<String, String> requestUrlParam = new HashMap<>(5, 1);
        //开发者设置中的appId
        requestUrlParam.put("appid", "wx4a326c92f0674dd7");
        //开发者设置中的appSecret
        requestUrlParam.put("secret", "c7bc37c888cf10b18f16260ce33a889b");
        //小程序调用wx.login返回的微信端登录code值
        requestUrlParam.put("js_code", code);
        //默认参数
        requestUrlParam.put("grant_type", "authorization_code");
        //发送post请求读取调用微信 https://api.weixin.qq.com/sns/jscode2session 接口获取openid用户唯一标识
        JSONObject jsonObject = JSON.parseObject(HttpUtil.sendPost(requestUrl, requestUrlParam));
        logger.info(jsonObject.toString());
        return jsonObject;
    }

    /**
     * 重用的向redis存入留座信息及判断是否留两个座位的方法
     *
     * @param user     用户实体类
     * @param personal 存进redis的用户信息
     * @param time     留座时间（单位分钟）
     * @return 是否留座成功信息
     */
    private ServerResponse setStringBySeatAndBarcode(User user, String personal, int time) {
        try {
            /* 向redis里存入数据和设置缓存时间 */
            stringRedisTemplate.opsForValue().set(user.getSeat(), personal, 60 * time, TimeUnit.SECONDS);
            /* 用于判断此借阅卡是否留两个座位 */
            stringRedisTemplate.opsForValue().set(user.getUserId(), personal, 60 * time, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return ServerResponse.createByErrorMessage("留座失败！");
        }
        return ServerResponse.createBySuccessMessage("留座成功！");
    }


}
