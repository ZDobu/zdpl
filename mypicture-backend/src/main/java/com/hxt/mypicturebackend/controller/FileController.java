package com.hxt.mypicturebackend.controller;


import com.hxt.mypicturebackend.annotation.AuthCheck;
import com.hxt.mypicturebackend.common.BaseResponse;
import com.hxt.mypicturebackend.common.ErrorCode;
import com.hxt.mypicturebackend.common.ResultUtils;
import com.hxt.mypicturebackend.constant.UserConstant;
import com.hxt.mypicturebackend.exception.BusinessException;
import com.hxt.mypicturebackend.manager.CosManager;
import com.hxt.mypicturebackend.model.dto.picture.PictureUploadRequest;
import com.hxt.mypicturebackend.model.entity.User;
import com.hxt.mypicturebackend.model.vo.PictureVO;
import com.hxt.mypicturebackend.service.PictureService;
import com.hxt.mypicturebackend.service.UserService;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;


@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {


    @Resource
    private CosManager cosManager;
    @Autowired
    private UserService userService;
    @Autowired
    private PictureService pictureService;


    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> uploadFile(@RequestPart("file") MultipartFile multipartFile) {
        //文件目录
        String fileName = multipartFile.getOriginalFilename();
        String filepath = String.format("/test/%s", fileName);
        File file = null;

        try {
            file = File.createTempFile(filepath,null);
            multipartFile.transferTo(file);
            cosManager.putObject(filepath,file);

            return ResultUtils.success(filepath);
        } catch (Exception e) {
            log.error("file upload error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"");
        } finally {
            //删除临时文件
            boolean delete = file.delete();
            if (!delete) {
                log.error("file delete error, filepath =  + {}", filepath);
            }
        }

    }

    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download")
    public void downloadFile(String filepath, HttpServletResponse response) throws IOException {
        InputStream cosObjectInput = null;
        byte[] bytes = null;
        try {
            COSObject cosManagerObject = cosManager.getObject(filepath);
            cosObjectInput = cosManagerObject.getObjectContent();
            bytes = IOUtils.toByteArray(cosObjectInput);
            response.setContentType("application/octet-stream;charset=utf-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath );
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"下载失败");
        } finally {
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }
    }




}

