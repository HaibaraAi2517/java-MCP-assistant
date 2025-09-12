package com.ygk.bean;

import lombok.Data;

/**
 *封装对话对象
 */
@Data
public class ChatForm {
    private Long memoryId;//对话id
    private String message;//用户问题
}
