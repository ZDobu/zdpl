<template>
  <div id="pictureDetailPage">
    <a-row :gutter="24">
      <!-- 左侧图片展示 -->
      <a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
        <div class="picture-container">
          <a-card title="图片预览">
            <a-image
              :src="picture.url"
              :alt="picture.name"
              :width="'100%'"
              :preview="true"
            style="max-height: 600px; object-fit: contain;"
            />
          </a-card>
        </div>
      </a-col>

      <!-- 右侧图片信息 -->
      <a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
        <div class="info-container">
          <!-- 操作按钮区域 -->
          <div class="action-buttons">
            <a-space>
              <a-button v-if="canEdit" type="default" @click="doEdit">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-button v-if="canEdit" danger @click="doDelete">
                <template #icon><DeleteOutlined /></template>
                删除
              </a-button>
              <a-button type="primary" @click="doDownload">
                <template #icon><DownloadOutlined /></template>
                免费下载
              </a-button>
            </a-space>
          </div>

          <a-descriptions
            title="图片详情"
            bordered
            :column="1"
            size="default"
          >
            <a-descriptions-item label="名称">{{ picture.name || '暂无' }}</a-descriptions-item>
            <a-descriptions-item label="分类">{{ picture.category || '暂无' }}</a-descriptions-item>
            <a-descriptions-item label="简介">{{ picture.introduction || '暂无' }}</a-descriptions-item>
            <a-descriptions-item label="尺寸">
              {{ picture.picWidth }} × {{ picture.picHeight }} 像素
            </a-descriptions-item>
            <a-descriptions-item label="文件大小">
              {{ formatSize(picture.picSize) }}
            </a-descriptions-item>
            <a-descriptions-item label="格式">{{ picture.picFormat || '暂无' }}</a-descriptions-item>
            <a-descriptions-item label="上传时间">{{ picture.createTime || '暂无' }}</a-descriptions-item>
            <a-descriptions-item label="更新时间">{{ picture.updateTime || '暂无' }}</a-descriptions-item>
            <a-descriptions-item label="标签">
              <a-tag v-for="(tag, index) in picture.tags" :key="index" color="blue">
                {{ tag }}
              </a-tag>
              <span v-if="!picture.tags || picture.tags.length === 0">暂无</span>
            </a-descriptions-item>
            <a-descriptions-item label="上传用户" v-if="picture.user">
              <a-avatar
                :src="picture.user.userAvatar"
                :size="24"
                class="user-avatar"
              >
                {{ picture.user.userName?.charAt(0) || '无' }}
              </a-avatar>
              {{ picture.user.userName || '匿名用户' }}
            </a-descriptions-item>
          </a-descriptions>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { deletePictureUsingPost, getPictureVoByIdUsingPost } from '@/api/pictureController.ts'
import { downloadImage, formatSize } from '@/utils'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { useRouter } from 'vue-router'
import { EditOutlined, DeleteOutlined, DownloadOutlined } from '@ant-design/icons-vue'

const picture = ref<API.PictureVO>({})

const props = defineProps<{
  id: string | number
}>()

// 获取图片详情
const fetchPictureDetail = async () => {
  try {
    const res = await getPictureVoByIdUsingPost({
      id: props.id,
    })
    if (res.data.code === 0 && res.data.data) {
      picture.value = res.data.data
    } else {
      message.error('获取图片详情失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('获取图片详情失败：' + e.message)
  }
}

onMounted(() => {
  fetchPictureDetail()
})

const loginUserStore = useLoginUserStore()

const canEdit = computed(()=>{
  const loginUser = loginUserStore.loginUser
  if(!loginUser) return false
  const user = picture.value.user || {}
  return loginUser.id === user.id || loginUser.userRole === 'admin'
})

const router = useRouter()

const doEdit = () => {
  router.push('/add_picture?id=' + picture.value.id)
}

const doDelete = async () => {
  const id = picture.value.id
  if (!id) return
  const res = await deletePictureUsingPost({id})
  if (res.data.code === 0 ) {
    message.success('删除成功')
    router.push('/') // 删除成功后跳转到首页或其他页面
  }
  else {
    message.error('删除失败')
  }
}

// 处理下载
const doDownload = () => {
  if (!picture.value.url) {
    message.warning('图片URL无效')
    return
  }
  downloadImage(picture.value.url)
}
</script>

<style scoped>
#pictureDetailPage {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.picture-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  padding: 20px;
  background-color: #f5f5f5;
  border-radius: 4px;
}

.info-container {
  height: 100%;
  padding: 20px;
}

/* 操作按钮样式 */
.action-buttons {
  margin-bottom: 16px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .picture-container, .info-container {
    padding: 10px;
  }

  .action-buttons {
    justify-content: flex-start;
  }
}

:deep(.ant-image) {
  display: block;
  max-width: 100%;
  max-height: 80vh;
  margin: 0 auto;
}

:deep(.ant-descriptions-title) {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 16px;
}

:deep(.ant-tag) {
  margin-right: 8px;
  margin-bottom: 8px;
}

.user-avatar {
  margin-right: 8px;
  vertical-align: middle;
}
</style>
