import request from '@/utils/request'

// 查询工作流接口用户
export function getWorkflowUserListByPage(data) {
    return request({
      url: '/workflow/user/getWorkflowUserListByPage',
      method: 'post',
      data: data
    })
}
// 分页查询工作流选择加签人员
export function getWorkflowAddMultiListByPage(data) {
    return request({
        url: '/workflow/user/getWorkflowAddMultiListByPage',
        method: 'post',
        data: data
    })
}
// 分页查询人员
export function getUserListByPage(data) {
    return request({
      url: '/workflow/user/getUserListByPage',
      method: 'post',
      data: data
    })
}
// 分页查询人员
export function getRoleListByPage(data) {
    return request({
      url: '/workflow/user/getRoleListByPage',
      method: 'post',
      data: data
    })
}
// 查询部门
export function getDeptList() {
    return request({
      url: '/workflow/user/getDeptList',
      method: 'get'
    })
}