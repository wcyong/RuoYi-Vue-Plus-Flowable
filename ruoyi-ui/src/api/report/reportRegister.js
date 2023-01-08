import request from '@/utils/request'

// 查询角色
export function getRoleListByReportRegisterIdAndRoleIds(data) {
  return request({
    url: '/report/reportRegister/getRoleListByReportRegisterIdAndRoleIds',
    method: 'post',
    data: data
  })
}

export function selectReportDbPage(query) {
  return request({
    url: '/report/reportRegister/selectReportDbPage',
    method: 'get',
    params: query
  })
}

// 查询报表注册列表
export function listReportRegister(query) {
  return request({
    url: '/report/reportRegister/list',
    method: 'get',
    params: query
  })
}

// 查询报表注册详细
export function getReportRegister(id) {
  return request({
    url: '/report/reportRegister/' + id,
    method: 'get'
  })
}

// 新增报表注册
export function addReportRegister(data) {
  return request({
    url: '/report/reportRegister',
    method: 'post',
    data: data
  })
}

// 修改报表注册
export function updateReportRegister(data) {
  return request({
    url: '/report/reportRegister',
    method: 'put',
    data: data
  })
}

// 删除报表注册
export function delReportRegister(id) {
  return request({
    url: '/report/reportRegister/' + id,
    method: 'delete'
  })
}

// 报表授权
export function reportAuth(data) {
  return request({
    url: '/report/reportRegister/reportAuth',
    method: 'post',
    data: data
  })
}
