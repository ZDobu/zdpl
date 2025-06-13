package com.hxt.mypicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hxt.mypicturebackend.common.ErrorCode;
import com.hxt.mypicturebackend.exception.BusinessException;
import com.hxt.mypicturebackend.exception.ThrowUtils;
import com.hxt.mypicturebackend.manager.FileManager;
import com.hxt.mypicturebackend.manager.upload.FilePictureUpload;
import com.hxt.mypicturebackend.manager.upload.PictureUploadTemplate;
import com.hxt.mypicturebackend.manager.upload.UrlPictureUpload;
import com.hxt.mypicturebackend.model.dto.file.UploadPictureResult;
import com.hxt.mypicturebackend.model.dto.picture.PictureQueryRequest;
import com.hxt.mypicturebackend.model.dto.picture.PictureReviewRequest;
import com.hxt.mypicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.hxt.mypicturebackend.model.dto.picture.PictureUploadRequest;
import com.hxt.mypicturebackend.model.entity.Picture;
import com.hxt.mypicturebackend.model.entity.User;
import com.hxt.mypicturebackend.model.enums.PictureReviewStatusEnum;
import com.hxt.mypicturebackend.model.vo.PictureVO;
import com.hxt.mypicturebackend.model.vo.UserVO;
import com.hxt.mypicturebackend.service.PictureService;
import com.hxt.mypicturebackend.mapper.PictureMapper;
import com.hxt.mypicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    @Resource
    private FileManager fileManager;

    @Resource
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;


    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        //判读用户是否存在
        ThrowUtils.throwIf(loginUser == null , ErrorCode.NO_AUTH_ERROR);
        //判断是创建还是更新
        Long pictureId = null;
        if(pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }

        if(pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null  , ErrorCode.NOT_FOUND_ERROR,"图片不存在");
            //本人,管理员
            if(!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }


        String uploadPathPrefix = String.format("public/%s",loginUser.getId());
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if(inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        String url = uploadPictureResult.getUrl();
        String picName = uploadPictureResult.getPicName();
        //支持外层请求更改名称
        if(pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        Long picSize = uploadPictureResult.getPicSize();
        int picWidth = uploadPictureResult.getPicWidth();
        int picHeight = uploadPictureResult.getPicHeight();
        Double picScale = uploadPictureResult.getPicScale();
        String picFormat = uploadPictureResult.getPicFormat();


        Picture picture = new Picture();
        picture.setUrl(url);
        picture.setName(picName);
        picture.setPicSize(picSize);
        picture.setPicWidth(picWidth);
        picture.setPicHeight(picHeight);
        picture.setPicScale(picScale);
        picture.setPicFormat(picFormat);
        picture.setUserId(loginUser.getId());
        //补充审核参数
        this.fillReviewParams(picture,loginUser);

        if(pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(picture.getEditTime());
        }

        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"图片上传失败");

        return PictureVO.toPictureVO(picture);

    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.toPictureVO(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        // userId > 0 是一种防御性编程
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }

        List<PictureVO> pictureVOList = Collections.unmodifiableList(pictureList.stream().map(PictureVO::toPictureVO).collect(Collectors.toList()));

        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));

        pictureVOList.forEach(pictureVO -> {
            // 关联查询用户信息
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });

        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;

    }

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);

        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();

        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id不能为空");

        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url过长");
        }

        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        //1.校验参数
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if(id==null || reviewStatusEnum==null || reviewStatusEnum.equals(PictureReviewStatusEnum.REVIEWING) ) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2.图片是否存在
        Picture Oldpicture = this.getById(id);
        ThrowUtils.throwIf(Oldpicture == null, ErrorCode.NOT_FOUND_ERROR);
        //3.传入的状态参数和之前的一样
        if(Oldpicture.getReviewStatus().equals(reviewStatusEnum.getValue())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //4.数据库操作
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, picture);
        picture.setReviewerId(loginUser.getId());
        picture.setReviewTime(new Date());
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);

    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR,"最多 30 条");
        // 抓取内容
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1",searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"获取页面失败");
        }
        // 解析内容
        Element div = document.getElementsByClass("dgControl").first();
        if(ObjUtil.isEmpty(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        //遍历元素，依次上传图片
        int uploadCount = 0;
        for(Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空.已跳过: {}", fileUrl);
                continue;
            }
            // 处理图片的地址，防止转移或者和对象存储冲突的问题
            // 处理的是链接里面的问号后面的可以不要
            // <img class="mimg" style="background-color:#c82303;color:#c82303" height="180" width="302" src="https://tse1-mm.cn.bing.net/th/id/OIP-C.fHrSVnFs1tJXSmELwGyz9QHaEK?w=302&amp;h=180&amp;c=7&amp;r=0&amp;o=7&amp;pid=1.7&amp;rm=3" alt="好看的图片 的图像结果">
            int questionMarkIndex = fileUrl.indexOf("?");
            if(questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
            if(StrUtil.isBlank(namePrefix)) {
                namePrefix = pictureUploadByBatchRequest.getSearchText();
            }
            pictureUploadRequest.setPicName(namePrefix + "_" + (uploadCount+1));
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl,pictureUploadRequest,loginUser);
                log.info("图片上传成功,id= {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败",e);
                continue;
            }
            if(uploadCount >= count) {
                break;
            }
        }

        return uploadCount;
    }


    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }

        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();

        if (StrUtil.isNotBlank(searchText)) {
            // or 的优先级 没有 and 高 , 要加个()
            queryWrapper.and(qw -> qw.like("name", searchText).or().like("introduction", searchText));
        }

        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId),"reviewerId",reviewerId);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);

        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                // [" " , "tag1"]  数据库中用的字符串存的，带上引号能找到完整的
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }

        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);

        return queryWrapper;
    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if(userService.isAdmin(loginUser)) {
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
            picture.setReviewerId(loginUser.getId());
        } else {
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

}




