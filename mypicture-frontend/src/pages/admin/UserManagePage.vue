<template>
  <div id="userManagePage">
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="账号">
        <a-input v-model:value="searchParams.userAccount" placeholder="输入账号" allow-clear/>
      </a-form-item>
      <a-form-item label="用户名">
        <a-input v-model:value="searchParams.userName" placeholder="输入用户名" allow-clear/>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
      </a-form-item>
    </a-form>

    <a-table :columns="columns" :data-source="dataList" :pagination="pagination" @change="doTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'userAvatar'">
          <a-image :src="record.userAvatar" :width="120"/>
        </template>
        <template v-else-if="column.dataIndex === 'userRole'">
          <div v-if="record.userRole === 'admin'">
            <a-tag color="green">管理员</a-tag>
          </div>
          <div v-else-if="record.userRole === 'user'">
            <a-tag color="blue">普通用户</a-tag>
          </div>
          <div v-else-if="record.userRole === 'ban'">
            <a-tag color="red">被禁用户</a-tag>
          </div>
          <div v-else>
            <a-tag color="white">未知</a-tag>
          </div>
        </template>
        <template v-else-if="column.key === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button danger @click="doDelte(record.id)">删除</a-button>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive ,ref , computed} from 'vue'
import { listUserVoByPageUsingPost ,deleteUserUsingPost} from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'

const columns = [
  {
    title: 'id',
    dataIndex: 'id',
  },
  {
    title: '账号',
    dataIndex: 'userAccount',
  },
  {
    title: '用户名',
    dataIndex: 'userName',
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
  },
  {
    title: '简介',
    dataIndex: 'userProfile',
  },
  {
    title: '用户角色',
    dataIndex: 'userRole',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '操作',
    key: 'action',
  },
]

// 定义数据
const dataList = ref<API.UserVO[]>([])
const total = ref(0)

//搜索条件
const searchParams = reactive<API.UserQueryRequest>({
  current: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'ascend',
})

//分页参数
const pagination = computed(() => {
  return {
    current: searchParams.current,
    pageSize: searchParams.pageSize,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total) => `共${total}条`
  }
})


onMounted(() => {
  fetchData()
})

/**
 * 更改分页大小，更改当前页
 * @param page
 */
const doTableChange = (page:any) => {
  searchParams.current = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}


const fetchData = async () => {
  const res = await listUserVoByPageUsingPost({
    ...searchParams,
  })

  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data.records ?? []
    console.log(dataList)
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败' + res.data.message)
  }
}

// 获取数据
const doSearch = () => {
  // 重置页码
  searchParams.current = 1
  fetchData()
}

const doDelte = async (id: string) => {
  if (!id) return

  const res = await deleteUserUsingPost({id})
  if (res.data.code === 0) {
    message.success('删除成功')

    fetchData()
  } else {
    message.error('删除失败')
  }
}


</script>

<style scoped>
#userManagePage {
}
</style>
