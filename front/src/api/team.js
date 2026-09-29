import request from './request'

// ---------- 团队 ----------
export const createTeam = (data) => request.post('/teams', data)

export const searchTeams = (params) => request.get('/teams/search', { params })

export const recommendedTeams = () => request.get('/teams/recommended')

export const listMyTeams = () => request.get('/teams/mine')

export const listMyTeamTasks = () => request.get('/teams/my-tasks')

export const getTeamDetail = (teamId) => request.get(`/teams/${teamId}`)

export const listMembers = (teamId) => request.get(`/teams/${teamId}/members`)

export const listApplications = (teamId) => request.get(`/teams/${teamId}/applications`)

export const applyJoin = (teamId) => request.post(`/teams/${teamId}/apply`)

export const reviewApplication = (teamId, targetUserId, approve) =>
  request.put(`/teams/${teamId}/applications/${targetUserId}`, { approve })

export const kickMember = (teamId, targetUserId) =>
  request.delete(`/teams/${teamId}/members/${targetUserId}`)

export const quitTeam = (teamId) => request.post(`/teams/${teamId}/quit`)

export const transferLeader = (teamId, targetUserId) =>
  request.post(`/teams/${teamId}/transfer`, { targetUserId })

export const dissolveTeam = (teamId) => request.delete(`/teams/${teamId}`)

// ---------- 团队任务 ----------
export const createTeamTask = (teamId, data) => request.post(`/teams/${teamId}/tasks`, data)

export const listTeamTasks = (teamId) => request.get(`/teams/${teamId}/tasks`)

export const getTeamTask = (teamId, taskId) => request.get(`/teams/${teamId}/tasks/${taskId}`)

export const updateTeamTask = (teamId, taskId, data) =>
  request.put(`/teams/${teamId}/tasks/${taskId}`, data)

export const deleteTeamTask = (teamId, taskId) =>
  request.delete(`/teams/${teamId}/tasks/${taskId}`)

export const markTaskComplete = (teamId, taskId) =>
  request.post(`/teams/${teamId}/tasks/${taskId}/complete`)

export const markTaskOverallComplete = (teamId, taskId) =>
  request.post(`/teams/${teamId}/tasks/${taskId}/overall-complete`)