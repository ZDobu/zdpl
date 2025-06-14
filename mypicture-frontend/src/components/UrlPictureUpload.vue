<template>
  <div class="url-picture-upload">
    <!-- 输入框部分保持不变 -->
    <a-input-group compact>
      <a-input
        v-model:value="fileUrl"
        style="width: calc(100% - 120px)"
        placeholder="请输入图片地址"
      />
      <a-button type="primary" style="width: 120px" :loading="loading" @click="handleUpload">
        提交
      </a-button>
    </a-input-group>

    <!-- 新增图片容器，添加阴影、圆角和居中效果 -->
    <div class="image-preview" v-if="picture?.url">
      <img :src="picture.url" :alt="picture.name || '上传的图片'" />
    </div>
  </div>
</template>
<script lang="ts" setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { uploadPictureByUrlUsingPost } from '@/api/pictureController.ts'

interface Props {
  picture?: API.PictureVO
  onSuccess?: (newPicture: API.PictureVO) => void
}

const props = defineProps<Props>()
const fileUrl = ref<string>()

const loading = ref<boolean>(false)

const handleUpload = async () => {
  loading.value = true
  try {
    const params: API.PictureUploadRequest = { fileUrl: fileUrl.value }
    if (props.picture) {
      params.id = props.picture.id
    }
    const res = await uploadPictureByUrlUsingPost(params)
    if (res.data.code === 0 && res.data.data) {
      message.success('图片上传成功')
      props.onSuccess?.(res.data.data)
    } else {
      console.log(res.data.data)
      message.error('图片上传失败 ' + res.data.code)
    }
  } catch (error) {
    message.error('图片上传失败')
  } finally {
    loading.value = false
  }
}
</script>
<style scoped>
.url-picture-upload {
  max-width: 800px;
  margin: 0 auto;
  padding: 16px;
}

/* 图片预览容器 */
.image-preview {
  margin-top: 24px;
  text-align: center;
  padding: 12px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

/* 图片样式 */
.image-preview img {
  max-width: 100%;
  max-height: 60vh;
  border-radius: 4px;
  object-fit: contain;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

/* 响应式调整 */
@media (max-width: 768px) {
  .image-preview img {
    max-height: 50vh;
  }
}

/* 输入框组微调 */
.a-input-group {
  margin-bottom: 16px;
}
</style>
