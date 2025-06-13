import router from './router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { message } from 'ant-design-vue'

router.beforeEach(async (to, from, next) => {
  const loginUserStore = useLoginUserStore()

  try {
    // 首次加载时获取用户信息
    if (!loginUserStore.hasFetched) {
      await loginUserStore.fetchLoginUser()
    }

    const loginUser = loginUserStore.loginUser
    const toUrl = to.fullPath

    // 管理员路径校验
    if (toUrl.startsWith('/admin')) {
      if (!loginUser || loginUser.userRole !== 'admin') {
        message.error('没有权限')
        return next(`/user/login?redirect=${encodeURIComponent(to.fullPath)}`)
      }
    }

    // 其他情况放行
    next()
  } catch (error) {
    console.error('路由守卫错误:', error)
    message.error('系统错误')
    next('/error') // 跳转到错误页
  }
})
