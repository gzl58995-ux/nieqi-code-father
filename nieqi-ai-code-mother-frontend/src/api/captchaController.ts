// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 获取图片验证码 GET /user/captcha */
export async function getCaptcha(options?: { [key: string]: any }) {
  return request<API.BaseResponseCaptchaVO>('/user/captcha', {
    method: 'GET',
    ...(options || {}),
  })
}
