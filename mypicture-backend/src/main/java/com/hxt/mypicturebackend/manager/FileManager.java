package com.hxt.mypicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.hxt.mypicturebackend.common.ErrorCode;
import com.hxt.mypicturebackend.config.CosClientConfig;
import com.hxt.mypicturebackend.exception.BusinessException;
import com.hxt.mypicturebackend.exception.ThrowUtils;
import com.hxt.mypicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.COSClient;
import javax.annotation.Resource;

import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 已废弃，改用PictureUploadTemplate 的实现方法
 */
@Slf4j
@Service
@Deprecated
public class FileManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    @Resource
    CosManager cosManager;

    /**
     * 上传图片并获取图片信息
     * @param multipartFile
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile,String uploadPathPrefix) {
        //校验数据
        validPicture(multipartFile);
        //拼写路径
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()),uuid, FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("%s/%s", uploadPathPrefix,uploadFileName);
        //上传图片
        File file = null;
        try {
            file = File.createTempFile(uploadPath,null);
            multipartFile.transferTo(file);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            int width = imageInfo.getWidth();
            int height = imageInfo.getHeight();
            String Format = imageInfo.getFormat();
            double PicScale = NumberUtil.round(width * 1.0 / height,2).doubleValue();

            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(width);
            uploadPictureResult.setPicHeight(height);
            uploadPictureResult.setPicScale(PicScale);
            uploadPictureResult.setPicFormat(Format);

            return uploadPictureResult;
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            deleteTempFile(file);
        }
    }


    private static void validPicture(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR,"文件不能为空");
        long fileSize = multipartFile.getSize();
        final long ONE_M = 1024 * 1024;
        ThrowUtils.throwIf(fileSize > 2 * ONE_M,ErrorCode.PARAMS_ERROR,"文件大小不能超过2M");
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final List<String> ALLOW_FORMATS_List = Arrays.asList("jpg", "jpeg", "png", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMATS_List.contains(fileSuffix),ErrorCode.PARAMS_ERROR,"文件类型错误");
    }

    private static void deleteTempFile(File file) {
        if (file==null) {
            return;
        }
        //删除临时文件
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath =  + {}", file.getAbsolutePath());
        }
    }


}


