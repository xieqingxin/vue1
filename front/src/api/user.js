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

export const createTask = (data) => request.post('/tasks', data)

export const listTasks = () => request.get('/tasks')

export const updateTaskStatus = (id, status) => request.put(`/tasks/${id}/status`, { status })

export const deleteTask = (id) => request.delete(`/tasks/${id}`)
