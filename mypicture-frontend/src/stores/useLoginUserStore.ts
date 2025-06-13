import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getLoginUserUsingGet } from '@/api/userController.ts'

export const useLoginUserStore = defineStore('loginUser', () => {
  const hasFetched = ref(false)

  const loginUser = ref<API.LoginUserVO>({
    userName: '未登录',
  })

  async function fetchLoginUser() {
    if(hasFetched.value) return

    const res = await getLoginUserUsingGet()
    if (res.data.code === 0 && res.data.data) {
      loginUser.value = res.data.data
    }
  }

  function setLoginUser(newLoginUser: any) {
    loginUser.value = newLoginUser
  }

  return { loginUser, setLoginUser, fetchLoginUser,hasFetched }
})
