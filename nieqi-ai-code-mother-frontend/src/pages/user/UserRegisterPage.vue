<template>
  <div id="userRegisterPage">
    <h2 class="title">乜七的代码生成器 - 用户注册</h2>
    <div class="desc">不写一行代码，生成完整应用</div>
    <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
      <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
        <a-input v-model:value="formState.userAccount" placeholder="请输入账号" />
      </a-form-item>
      <a-form-item
        name="userPassword"
        :rules="[
          { required: true, message: '请输入密码' },
          { min: 8, message: '密码不能小于 8 位' },
        ]"
      >
        <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" />
      </a-form-item>
      <a-form-item
        name="checkPassword"
        :rules="[
          { required: true, message: '请确认密码' },
          { min: 8, message: '密码不能小于 8 位' },
          { validator: validateCheckPassword },
        ]"
      >
        <a-input-password v-model:value="formState.checkPassword" placeholder="请确认密码" />
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
        已有账号？
        <RouterLink to="/user/login">去登录</RouterLink>
      </div>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">注册</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { userRegister } from '@/api/userController.ts'
import { getCaptcha } from '@/api/captchaController'
import { message } from 'ant-design-vue'
import { reactive, ref, onMounted } from 'vue'
import axios from 'axios'

const router = useRouter()

const formState = reactive<API.UserRegisterRequest & { captchaCode: string }>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
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

const validateCheckPassword = (rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value && value !== formState.userPassword) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const handleSubmit = async (values: typeof formState) => {
  try {
    const res = await userRegister(values)
    if (res.data.code === 0) {
      message.success('注册成功')
      router.push({
        path: '/user/login',
        replace: true,
      })
    } else {
      message.error('注册失败，' + res.data.message)
      refreshCaptcha()
    }
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const status = error.response?.status
      const msg =
        status != null
          ? `注册请求失败（HTTP ${status}），请确认后端是否已实现 /api/user/register 接口`
          : '注册请求失败，请确认后端服务是否启动（8123端口）'
      message.error(msg)
      return
    }
    message.error('注册请求失败，请稍后重试')
    refreshCaptcha()
  }
}
</script>

<style scoped>
#userRegisterPage {
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
  margin-bottom: 16px;
  color: #bbb;
  font-size: 13px;
  text-align: right;
}

.captcha-img {
  height: 42px;
  cursor: pointer;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
}
</style>
