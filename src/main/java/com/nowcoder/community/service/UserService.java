package com.nowcoder.community.service;

import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class UserService implements CommunityConstant {
    @Autowired
    private UserMapper userMapper;
    public User findUserById(int id){
        return userMapper.selectById(id);
    }

    //用户注册
    @Autowired
    private MailClient mailClient;
    @Autowired
    private TemplateEngine templateEngine;


    @Value("${community.path.domain}")
    //community.path.domain=http://localhost:8080
    private String domain;


    @Value("${server.servlet.context-path}")
    //
    private String contextPath;




    //注册返回的数据各式各样，因此用map返回
    public Map<String ,Object> register(User user){
        //string值于输入框的名称相同 object存放的时应该显示 的信息
        Map<String,Object> map = new HashMap<>();

        //IF null
        if(user == null){
            throw new IllegalArgumentException("cant null");
        }
        if(StringUtils.isAllBlank(user.getUsername())){
            map.put("usernameMsg","账号不能为空");
            return map;

        }
        if(StringUtils.isAllBlank(user.getPassword())){
            map.put("passwordMsg","密码不能为空");
            return map;

        }
        if(StringUtils.isAllBlank(user.getEmail())){
            map.put("emailMsg","邮箱不能为空");
            return map;

        }
        //验证账号
        User u=userMapper.selectByName(user.getUsername());
        if (u!=null){
            map.put("usernameMsg","账号已存在");
            return map;
        }
        //验证邮箱
        u=userMapper.selectByName(user.getEmail());
        if (u!=null){
            map.put("emailMsg","邮箱已存在");
            return map;
        }
        //注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
        user.setPassword(CommunityUtil.md5(user.getPassword()+user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        //set 头像
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png",new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        //发邮件
        Context context=new Context();
        context.setVariable("email",user.getEmail());
        //发出的邮件有一个激活链接 形式如下
        //http://localhost:8081/community/avtivation/101/code
        String url=domain + contextPath + "/activation/" + user.getId() + "/" +
                user.getActivationCode();
        context.setVariable("url",url);
        String content = templateEngine.process("/mail/activation",context);
        mailClient.sendMail(user.getEmail(),"激活",content);

         return  map;
    }

    //点击邮件激活：
    public  int activation(int userId,String code){
        User user=userMapper.selectById(userId);
        if(user.getStatus() == 1){
            return ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(code)) {
            userMapper.updateStatus(userId,1);
            return ACTIVATION_SUCCESS;
        }else{
            return ACTIVATION_FAILURE;
        }

    }


}