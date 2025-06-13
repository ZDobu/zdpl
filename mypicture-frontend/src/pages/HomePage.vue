<template>
  <div id="homePage">
    <!--    搜索框-->
    <div class="search-bar">
      <a-input-search
        v-model:value="searchParams.searchText"
        placeholder="从图片中搜索"
        enter-button="搜索"
        size="large"
        @search="doSearch"
      />
    </div>
    <!--    分类和标签筛选-->
    <a-tabs v-model:active-key="selectedCategoryList" @change="doSearch">
      <a-tab-pane tab="全部" key="all"/>
      <a-tab-pane v-for="category in categoryList" :key="category" :tab="category"/>
    </a-tabs>
    <div class="tag-bar">
      <span style="margin-right: 8px">标签:</span>
      <a-space :size="[0, 8]" wrap>
        <a-checkable-tag
          v-for="(tag, index) in tagList"
          :key="tag"
          v-model:checked="selectedTagList[index]"
          @change="doSearch"
        >
          {{ tag }}
        </a-checkable-tag>
      </a-space>
    </div>
    <a-list :grid="{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 4, xl: 4 ,xxl: 4 }" :data-source="dataList" :pagination="pagination" :loading="loading" style="padding: 0">
      <template #renderItem="{ item: picture}">
        <a-list-item style="padding: 0">
          <!-- 单张图片 -->
          <a-card hoverable @click="doClickPicture(picture)" style="height: 100%; display: flex; flex-direction: column;">
            <template #cover>
              <img
                style="width: 100%; height: 200px; object-fit: cover;"
                :alt="picture.name"
                :src="picture.url"
              />
            </template>
            <a-card-meta :title="picture.name" style="flex: 1;">
              <template #description>
                <a-flex>
                  <a-tag color="green">
                    {{ picture.category ?? '默认' }}
                  </a-tag>
                  <a-tag v-for="tag in picture.tags" :key="tag">
                    {{ tag }}
                  </a-tag>
                </a-flex>
              </template>
            </a-card-meta>
          </a-card>
        </a-list-item>
      </template>
    </a-list>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted} from 'vue'
import {
  listPictureTagCategoryUsingGet,
  listPictureVoPageUsingPost
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'

// 定义数据
const dataList = ref<API.PictureVO[]>([])
const total = ref(0)
const loading = ref(true)
const categoryList = ref<string[]>([])
const tagList = ref<string[]>([])
const selectedCategoryList = ref<string>('all')
const selectedTagList = ref<boolean[]>([])
const router = useRouter()

const doClickPicture = (picture: API.PictureVO) => {
  router.push({
    path: `/picture/${picture.id}`,
  })
}



//搜索条件
const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 12,
  sortField: 'createTime',
  sortOrder: 'descend',
})

//分页参数
const pagination = computed(() => {
  return {
    current: searchParams.current,
    pageSize: searchParams.pageSize,
    total: total.value,
    onChange: (page: number, pageSize: number): void => {
      searchParams.current = page
      searchParams.pageSize = pageSize
      fetchData()
    }
  }
})

onMounted(() => {
  fetchData()
  // getTagCatgoryOptions()
})

const doSearch = (): void => {
  searchParams.current = 1
  fetchData()
}


const fetchData = async () => {
  loading.value = true
  //转换搜索参数
  const params = {
    ...searchParams,
    tags: [] as string[],
  }
  if(selectedCategoryList.value !== 'all') {
    params.category = selectedCategoryList.value
  }
  selectedTagList.value.forEach((useTag,index) => {
    if (useTag) {
      params.tags.push(tagList.value[index])
    }
  })
  const res = await listPictureVoPageUsingPost(params)

  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败' + res.data.message)
  }
  loading.value = false
}

/**
 * 获取标签和分类选项
 * @param values
 */
const getTagCatgoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()

  if (res.data.code === 0 && res.data.data) {
    tagList.value = res.data.data.tagList ?? []
    categoryList.value = res.data.data.categoryList ?? []
  } else {
    message.error('获取标签分页列表失败' + res.data.message)
  }
}

onMounted(()=> {
  getTagCatgoryOptions()

})


</script>

<style scoped>
#homePage {
  margin-bottom: 10px;
}

#homePage .search-bar {
  max-width: 480px;
  margin: 0 auto 16px;
}

#homePage .tag-bar {
  margin-bottom: 16px;
}

</style>



