package com.mmall.service.impl;

import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.common.TokenCache;
import com.mmall.dao.UserMapper;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.MD5Util;
import net.sf.jsqlparser.schema.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * @Description:
 * @Author: Renzi Meng
 * @Date: Created in 20:02 2018/9/21
 * @Copyright： 2018, Renzi Meng, All Rights Reserved.
 */

@Service("iUserService")
public class UserServiceImpl implements IUserService{

    @Autowired
    // 通过 @Autowired 的使用实现自动装配功能
    private UserMapper userMapper;

    @Override
    public ServerResponse<User> login(String username, String password) {
        int resultCount = userMapper.checkUsername ( username );
        if(resultCount == 0){
            return ServerResponse.createByErrorMessage ( "用户名不存在" );
        }

        // todo 密码登录MD5
        // String md5Password = md5Util.MD5EncodeUtf8(password)
        User user = userMapper.selectLogin ( username, password );

        if(user == null){
            return ServerResponse.createByErrorMessage ( "密码错误" );
        }

        user.setPassword ( org.apache.commons.lang3.StringUtils.EMPTY );
        return ServerResponse.createBySuccess ("登录成功", user);

    }


    public ServerResponse<String> register(User user) {

        ServerResponse validResponse = this.checkValid ( user.getUsername (), Const.USERNAME );
        if (validResponse.isSuccess ()){
            return validResponse;
        }

        validResponse = this.checkValid ( user.getEmail (), Const.EMAIL );
        if (validResponse.isSuccess ()){
            return validResponse;
        }

        user.setRole(Const.Role.ROLE_CUSTOMER);
        // MD5加密
        user.setPassword ( MD5Util.MD5EncodeUtf8 ( user.getPassword () ) );

        int resultCount = userMapper.insert ( user );
        if (resultCount == 0){
            return ServerResponse.createByErrorMessage ( "注册失败" );
        }
        return ServerResponse.createBySuccessMessage ("注册成功");
    }


    public ServerResponse<String> checkValid(String str, String type) {
        if (org.apache.commons.lang3.StringUtils.isNotBlank ( type )) {
            // 开始校验
            if (Const.USERNAME.equals ( type )) {
                int resultCount = userMapper.checkUsername ( str );
                if (resultCount > 0) {
                    return ServerResponse.createByErrorMessage ( "用户名已经存在" );
                }
            }
            if (Const.EMAIL.equals ( type )) {
                int resultCount = userMapper.checkEmail ( str );
                if (resultCount > 0) {
                    return ServerResponse.createByErrorMessage ( "Email已经被使用" );
                }
            }
        } else {
            return ServerResponse.createByErrorMessage ( "参数错误" );
        }
        return ServerResponse.createBySuccessMessage ( "校验成功" );
    }

    public ServerResponse selectQuestion(String username){
        ServerResponse validResponse = this.checkValid ( username, Const.USERNAME);
        if(validResponse.isSuccess ()){
            //用户不存在
            return ServerResponse.createByErrorMessage ( "用户不存在" );
        }
        String question = userMapper.selectQuestionByUsername ( username );
        if(org.apache.commons.lang3.StringUtils.isNotBlank ( question )){
            return ServerResponse.createBySuccess (question);
        }
        return ServerResponse.createByErrorMessage ( "找回密码问题是空的" );
    }

    public static void main(String[] args){
        System.out.println ( UUID.randomUUID ().toString () );
    }

    public ServerResponse<String> checkAnswer(String username, String question, String answer){
        int resultCount = userMapper.checkAnswer ( username, question, answer );
        if(resultCount >0){
            // 说明问题及问题答案是这个用户的并且是正确的
            String forgetToken = UUID.randomUUID ().toString ();
            TokenCache.setKey("token_" + username, forgetToken);
            return ServerResponse.createBySuccess (forgetToken);
        }
        return ServerResponse.createByErrorMessage ( "问题答案错误" );
    }
}
