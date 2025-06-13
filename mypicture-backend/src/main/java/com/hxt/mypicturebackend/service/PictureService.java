package com.hxt.mypicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hxt.mypicturebackend.model.dto.picture.PictureQueryRequest;
import com.hxt.mypicturebackend.model.dto.picture.PictureReviewRequest;
import com.hxt.mypicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.hxt.mypicturebackend.model.dto.picture.PictureUploadRequest;
import com.hxt.mypicturebackend.model.entity.Picture;
import com.hxt.mypicturebackend.model.entity.User;
import com.hxt.mypicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;


public interface PictureService extends IService<Picture> {
    /**
     * 上传图片
     *
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 获取脱敏图片类，带上了用户
     *
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 分页获取图片封装
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 数据验证
     *
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser
    );


    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 填充审核参数
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture, User loginUser);


}
