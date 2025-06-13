package com.hxt.mypicturebackend.model.dto.picture;

import lombok.Data;

@Data
public class PictureUploadRequest {

    /**
     * 图片id(用于修改)
     */
    private Long id;

    /**
     * 图片 URL
     */
    private String fileUrl;

    /**
     * 图片名称
     */
    private String picName;
}
