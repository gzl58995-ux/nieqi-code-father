<template>
  <div id="userLoginPage">
    <h2 class="title">乜七的代码生成器 - 用户登录</h2>
    <div class="desc">不写一行代码，生成完整应用</div>
    <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
      <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
        <a-input v-model:value="formState.userAccount" placeholder="请输入账号" />
      </a-form-item>
      <a-form-item
        name="userPassword"
        :rules="[
          { required: true, message: '请输入密码' },
          { min: 8, message: '密码长度不能小于 8 位' },
        ]"
      >
        <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" />
      </a-form-item>
      <a-form-item name="captchaCode" :rules="[{ required: true, message: '请输入验证码' }]">
        <a-row :gutter="12">
          <a-col :span="14">
            <a-input v-model:value="formState.captchaCode" placeholder="请输入验证码" />
          </a-col>
          <a-col :span="10">
            <img
              :src="captchaImage"
              alt="验证码"
              class="captcha-img"
              @click="refreshCaptcha"
              title="点击刷新验证码"
            />
          </a-col>
        </a-row>
      </a-form-item>
      <a-form-item name="captchaKey" style="display:none">
        <a-input v-model:value="formState.captchaKey" />
      </a-form-item>
      <div class="tips">
        没有账号
        <RouterLink to="/user/register">去注册</RouterLink>
      </div>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">登录</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>
<script lang="ts" setup>
import { reactive, ref, onMounted } from 'vue'
import { userLogin } from '@/api/userController.ts'
import { getCaptcha } from '@/api/captchaController'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import axios from 'axios'

const formState = reactive<API.UserLoginRequest & { captchaCode: string }>({
  userAccount: '',
  userPassword: '',
  captchaKey: '',
  captchaCode: '',
})

const captchaImage = ref('')

const refreshCaptcha = async () => {
  try {
    const res = await getCaptcha()
    if (res.data.code === 0 && res.data.data) {
      formState.captchaKey = res.data.data.captchaKey
      captchaImage.value = res.data.data.captchaImage
      formState.captchaCode = ''
    }
  } catch {
    message.error('获取验证码失败')
  }
}

onMounted(() => {
  refreshCaptcha()
})

const router = useRouter()
const loginUserStore = useLoginUserStore()

const handleSubmit = async (values: any) => {
  try {
    const res = await userLogin(values)
    if (res.data.code === 0 && res.data.data) {
      await loginUserStore.fetchLoginUser()
      message.success('登录成功')
      router.push({
        path: '/',
        replace: true,
      })
    } else {
      message.error('登录失败，' + res.data.message)
      refreshCaptcha()
    }
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const status = error.response?.status
      const msg =
        status != null
          ? `登录请求失败（HTTP ${status}），请确认后端是否已实现 /api/user/login 接口`
          : '登录请求失败，请确认后端服务是否启动（8123端口）'
      message.error(msg)
      return
    }
    message.error('登录请求失败，请稍后重试')
    refreshCaptcha()
  }
}
</script>

<style scoped>
#userLoginPage {
  background: white;
  max-width: 720px;
  padding: 24px;
  margin: 24px auto;
}

.title {
  text-align: center;
  margin-bottom: 16px;
}

.desc {
  text-align: center;
  color: #bbb;
  margin-bottom: 16px;
}

.tips {
  text-align: right;
  color: #bbb;
  font-size: 13px;
  margin-bottom: 16px;
}

.captcha-img {
  height: 42px;
  cursor: pointer;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
}
</style>
