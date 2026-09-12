import request from './request'

export const register = (data) => request.post('/auth/register', data)

export const login = (data) => request.post('/auth/login', data)

export const getProfile = () => request.get('/auth/me')

export const uploadFile = (file) => {
  const form = new FormData()
  form.append('file', file)
  return request.post('/files/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const listFiles = () => request.get('/files')
