package com.whohim.library.com.whohim.library.fillter;


import com.alibaba.fastjson.JSONObject;
import com.whohim.library.com.whohim.library.com.whohim.springboot.web.UserController;
import com.whohim.library.com.whohim.library.common.FillterResponse;
import com.whohim.library.com.whohim.library.common.ResponseCode;
import com.whohim.library.com.whohim.library.common.ServerResponse;
import com.whohim.library.com.whohim.library.pojo.User;
import com.whohim.library.com.whohim.library.pojo.UserInfo;
import com.whohim.library.com.whohim.library.util.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.whohim.library.com.whohim.library.common.Constant.USER_INFO;

/**
 * @author WhomHim
 * @description
 * @date Create in 2019/4/19 16:22
 */
public class SessionInterceptor implements HandlerInterceptor {

    private static Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
//        PrintWriter printWriter = response.getWriter();
//        Boolean responseBoolean = true;
//        Map<String, String[]> pramMap = request.getParameterMap();
//        String openId = null;
//
//        for (String key:pramMap.keySet()){
//            if (key.equals("openId")){
//                openId = String.valueOf(pramMap.get(key));
//            }
//        }
//
//        if (openId != null){
//            /* 初始化user_info表 */
//            if (StringUtils.isBlank(stringRedisTemplate.opsForValue().get(USER_INFO))) {
//                UserInfo userInfo = new UserInfo();
//                CopyOnWriteArrayList<User> userList = new CopyOnWriteArrayList<>();
//                User user = new User();
//                user.setBarcode("123456");
//                user.setOpenId("asd1223asd");
//                userList.add(user);
//                userInfo.setUserInfo(userList);
//                stringRedisTemplate.opsForValue().set(USER_INFO, JsonUtil.obj2StringPretty(userInfo));
//                logger.info("user_info表初始化成功！");
//            }
//            UserInfo userInfo = JsonUtil.string2Obj(stringRedisTemplate.opsForValue().get(USER_INFO), UserInfo.class);
//            CopyOnWriteArrayList<User> userList = null;
//            if (userInfo != null) {
//                userList = userInfo.getUserInfo();
//            }
//            /* openId和barcode是一对一关系 判断是否被绑定过 */
//            if (userList != null) {
//                for (User user : userList) {
//                    if (user.equals(openId)){
//                        responseBoolean = false;
//                    }
//                }
//            }
//            if (!responseBoolean) {
//                printWriter.write("请先绑定借阅卡！");
//                printWriter.flush();
//                return HandlerInterceptor.super.preHandle(request, response, handler);
//            }
//        }
//        return responseBoolean;
//        System.out.println("开始请求地址拦截");
//        HttpSession session = request.getSession(false);
//
//        FillterResponse fillterResponse = new FillterResponse();
//        fillterResponse.setStatus(ResponseCode.ERROR.getCode());
//        fillterResponse.setMsg("请先绑定借阅卡！");
////        String str = "{\"result\":\""  "\",\"text\": \"" "\"}";
//        JSONObject Object = JSONObject.toJSON(str);
//        if (session != null && session.getAttribute("user") != null) {
//            return true;
//        } else {
            PrintWriter printWriter = response.getWriter();
           // printWriter.write(String.valueOf(fillterResponse));
            printWriter.flush();
            return false;



    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        System.out.println("返回视图或String之前的处理");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        System.out.println("返回视图或String之前的处理");
    }
}