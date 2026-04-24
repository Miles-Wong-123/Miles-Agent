package com.miles.milesagent.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 新增知识请求体。
 * 调用 /insert 接口时，会把前端传来的知识内容映射成这个对象。
 */
@Data
public class KnowledgeRequest implements Serializable {


    /**
     * 知识中的“问题”部分，例如：这个软件叫什么名字？
     */
    private String question;

    /**
     * 知识中的“答案”部分，例如：本软件名为「迈尔斯」...
     */
    private String answer;

    /**
     * 可选的来源文件名，用于写入文档和向量库元数据。
     */
    private String sourceName;

}

