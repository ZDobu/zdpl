<template>
  <div id="addPicturePage">
    <h2 style="margin-bottom: 16px">批量创建</h2>
    <a-form layout="vertical" :model="formData" @finish="handleSubmit">
      <a-form-item label="关键词" name="searchText">
        <a-input v-model:value="formData.searchText" placeholder="请输入关键词" allow-clear/>
      </a-form-item>
      <a-form-item label="抓取数量" name="count">
        <a-input-number
          v-model:value="formData.count"
          placeholder="请输入抓取数"
          :min="1"
          :max="30"
          style="width: 180px"
          autoSize
          allowClear
        />
      </a-form-item>
      <a-form-item label="名称前缀" name="namePrefix">
        <a-auto-complete
          v-model:value="formData.namePrefix"
          placeholder="请输入名称前缀，会自动补充序号"
          allowClear
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%" :loading="loading">批量提交</a-button
        >
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import {  reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { uploadPictureByBatchUsingPost } from '@/api/pictureController.ts'
import { useRouter } from 'vue-router'

const formData = reactive<API.PictureUploadByBatchRequest>({
  count: 10,
})

const router = useRouter()
const loading = ref(false)

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  loading.value = true
  const res = await uploadPictureByBatchUsingPost({
    ...formData,
  },)

  if (res.data.code === 0 && res.data.data) {
    message.success(`创建成功,共 ${res.data.data} 条`)

    router.push({
      path: '/',
    })
  } else {
    message.error('创建失败' + res.data.message)
  }
  loading.value = false
}
</script>

<style scoped>
#addPicturePage {
  max-width: 720px;
  margin: 0 auto;
}
</style>
