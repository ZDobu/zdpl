package com.hxt.mypicturebackend.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hxt.mypicturebackend.annotation.AuthCheck;
import com.hxt.mypicturebackend.common.*;
import com.hxt.mypicturebackend.constant.UserConstant;
import com.hxt.mypicturebackend.exception.BusinessException;
import com.hxt.mypicturebackend.exception.ThrowUtils;
import com.hxt.mypicturebackend.model.dto.picture.*;
import com.hxt.mypicturebackend.model.entity.Picture;
import com.hxt.mypicturebackend.model.entity.User;
import com.hxt.mypicturebackend.model.enums.PictureReviewStatusEnum;
import com.hxt.mypicturebackend.model.vo.PictureTagCategory;
import com.hxt.mypicturebackend.model.vo.PictureVO;
import com.hxt.mypicturebackend.service.PictureService;
import com.hxt.mypicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/picture")
@Slf4j
public class PictureController {

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 上传图片（管理员）
     *
     * @param pictureUploadRequest
     * @param request
     * @return
     */
    @PostMapping("/upload/url")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequest pictureUploadRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(pictureUploadRequest.getFileUrl(),pictureUploadRequest,loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 上传图片（管理员）
     *
     * @param multipartFile
     * @param pictureUploadRequest
     * @param request
     * @return
     */
    @PostMapping("/upload")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile,pictureUploadRequest,loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //找用户id
        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        //看图片是否存在
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //看权限（本人或管理员）
        if (!oldPicture.getUserId().equals(loginUser.getId()) || !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = pictureService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新图片(管理员)
     *
     * @param pictureUpdateRequest
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/update")
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        //写更新对象
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);

        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        //验证更新是否合理
        pictureService.validPicture(picture);
        //看看有没有这个图片
        long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        //补充审核参数
        User loginUser = userService.getLoginUser(request);
        pictureService.fillReviewParams(picture,loginUser);

        //更新结果
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(true);
    }

    /**
     * 根据id获取图片
     *
     * @param id
     * @param request
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/get")
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);

        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        return ResultUtils.success(picture);
    }

    /**
     * 根据id获取图片(脱敏数据)，都能用
     *
     * @param id
     * @param request
     * @return
     */
    @PostMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);

        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        return ResultUtils.success(pictureService.getPictureVO(picture, request));
    }

    /**
     * 分页获取图片列表
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPicturePage(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);

        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        Page<Picture> picturePage = pictureService.page(new Page<>(current,size),pictureService.getQueryWrapper(pictureQueryRequest));

        return ResultUtils.success(picturePage);
    }


    /**
     * 分页获取图片列表(脱敏)
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOPage(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);

        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        //普通用户只能看到审核通过的数据
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        Page<Picture> picturePage = pictureService.page(new Page<>(current,size),pictureService.getQueryWrapper(pictureQueryRequest));

        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage,request);

        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 分页获取图片列表(脱敏) + 缓存
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);

        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        //普通用户只能看到审核通过的数据
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        //构建缓存 key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String redisKey = "mypicture:listPictureVOByPage:" + hashKey;

        //从 Redis 缓存中查询
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
        String cachedValue = valueOps.get(redisKey);
        if (cachedValue != null) {
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }

        //查数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current,size),pictureService.getQueryWrapper(pictureQueryRequest));

        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage,request);

        //存入 Redis 缓存
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        // 5 - 10 分钟随机过期, 防止雪崩
        int cacheExpireTime = 300 + RandomUtil.randomInt(0,300);
        valueOps.set(redisKey,cacheValue,cacheExpireTime, TimeUnit.SECONDS);

        //返回结果
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 用户或管理员编辑图片(脱敏)
     * @param pictureEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest,HttpServletRequest request) {
        if(pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Picture picture = new Picture();

        BeanUtils.copyProperties(pictureEditRequest, picture);

        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        picture.setEditTime(new Date());

        pictureService.validPicture(picture);
        User loginUser = userService.getLoginUser(request);

        //补充审核参数
        pictureService.fillReviewParams(picture,loginUser);

        long id = pictureEditRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        if (!oldPicture.getUserId().equals(picture.getUserId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 设置固定标签分类列表给前端
     * @return
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 批量上传图片（管理员）
     *
     * @param pictureUploadByBatchRequest
     * @param request
     * @return
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        int uploadUser = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadUser);
    }
}
