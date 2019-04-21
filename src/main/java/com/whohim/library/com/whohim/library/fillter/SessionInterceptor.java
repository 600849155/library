package com.whohim.library.com.whohim.library.fillter;


import com.alibaba.fastjson.JSONObject;
import com.whohim.library.com.whohim.library.com.whohim.springboot.web.UserController;
import com.whohim.library.com.whohim.library.pojo.User;
import com.whohim.library.com.whohim.library.pojo.UserInfo;
import com.whohim.library.com.whohim.library.util.JsonUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static com.whohim.library.com.whohim.library.common.Constant.USER_INFO;

/**
 * 拦截除了绑定图书卡的所有接口
 *
 * @author WhomHim
 * @description
 * @date Create in 2019/4/19 16:22
 */

public class SessionInterceptor implements HandlerInterceptor {

    private static Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * @param request
     * @param response
     * @param handler
     * @return 返回ture则放行，返回false则拦截
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        Boolean hasBeenBind = false;
        Map<String, String[]> pramMap = request.getParameterMap();
        List<String[]> openIdList = pramMap.keySet().stream().filter("openId"::equals).map(pramMap::get).collect(Collectors.toList());

        /* 初始化user_info表 */
        if (StringUtils.isBlank(stringRedisTemplate.opsForValue().get(USER_INFO))) {
            UserInfo userInfo = new UserInfo();
            CopyOnWriteArrayList<User> userList = new CopyOnWriteArrayList<>();
            User user = new User();
            user.setBarcode("123456");
            user.setOpenId("asd1223asd");
            userList.add(user);
            userInfo.setUserInfo(userList);
            stringRedisTemplate.opsForValue().set(USER_INFO, JsonUtil.obj2StringPretty(userInfo));
            logger.info("user_info表初始化成功！");
        }

        if (CollectionUtils.isNotEmpty(openIdList)) {
            UserInfo userInfo = JsonUtil.string2Obj(stringRedisTemplate.opsForValue().get(USER_INFO), UserInfo.class);
            CopyOnWriteArrayList<User> userList = new CopyOnWriteArrayList<>();
            if (userInfo != null) {
                userList = userInfo.getUserInfo();
            }
            /* openId和barcode是一对一关系 判断是否被绑定过 */
            if (userList != null && userList.stream().anyMatch((user -> user.getOpenId().equals(openIdList.get(0)[0])))) {
                hasBeenBind = true;
            }
        }else {
            serverResponse(response,404,"请传参!");
            return false;
        }

       /* 沒有綁定過則提示綁定  */
        if (!hasBeenBind) {
            serverResponse(response,3,"请先绑定借阅卡!");
            return false;
        }
        return true;
    }

    /**
     *  响应前端
     * @param response servlet
     * @param status 状态码
     * @param msg 信息
     */
    private void serverResponse(HttpServletResponse response,int status,String msg){
        try {
              /* 指定返回的格式为JSON格式 */
            response.setContentType("application/json;charset=utf-8");
            /* setContentType与setCharacterEncoding的顺序不能调换，否则还是无法解决中文乱码的问题 */
            response.setCharacterEncoding("UTF-8");
            JSONObject jsonObject = new JSONObject() {
                {
                    put("status", status);
                    put("msg", msg);
                    put("data", null);
                    put("success", "false");
                }
            };
            PrintWriter out = response.getWriter();
            out.write(jsonObject.toString());
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}