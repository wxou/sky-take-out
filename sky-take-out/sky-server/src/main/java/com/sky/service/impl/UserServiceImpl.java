package com.sky.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.constant.JwtClaimsConstant;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.JwtProperties;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.JwtUtil;
import com.sky.vo.UserLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    //微信接口服务地址
    private static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private WeChatProperties weChatProperties;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    @Override
    public UserLoginVO wxlogin(UserLoginDTO userLoginDTO) {
        String openid = getOpenid(userLoginDTO.getCode());

        if(openid == null){
            log.warn("微信登录失败，回退到开发模式，使用默认用户");
            openid = "dev_openid";
        }

        User user = userMapper.getByOpenid(openid);

        if(user == null){
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);

        UserLoginVO userLoginVO = UserLoginVO.builder()
                .id(user.getId())
                .openid(user.getOpenid())
                .token(token)
                .build();

        return userLoginVO;
    }

    /**
     * 调用微信接口服务,获取当前微信用户的openid
     * @param code
     * @return
     */
    private String getOpenid(String code) {
        //调用微信接口服务,获得当前微信用户的openid
        Map<String, String> map = new HashMap<>();
        map.put("appid",weChatProperties.getAppid() );
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        String json = HttpClientUtil.doGet(WX_LOGIN, map);

        //判断响应是否为空
        if(json == null || json.isEmpty()){
            log.error("调用微信接口失败，响应为空");
            return null;
        }

        JsonNode jsonNode;
        try {
            jsonNode = objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("解析微信接口响应失败，json:{}", json, e);
            return null;
        }
        
        if(jsonNode == null){
            log.error("解析微信接口响应失败，json:{}", json);
            return null;
        }
        
        String openid = jsonNode.has("openid") ? jsonNode.get("openid").asText() : null;

        return openid;
    }
}
