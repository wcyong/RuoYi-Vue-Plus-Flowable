import request from '@/utils/request'

// 查询流程分类构建树形结构
export function queryTreeList() {
  return request({
    url: '/workflow/category/queryTreeList',
    method: 'get'
  })
}

// 查询流程分类
export function categoryList() {
  return request({
    url: '/workflow/category/list',
    method: 'get'
  })
}

// 新增流程分类
export function addCategory(data) {
  return request({
    url: '/workflow/category',
    method: 'post',
    data: data
  })
}

// 修改流程分类
export function updateCategory(data) {
  return request({
    url: '/workflow/category',
    method: 'put',
    data: data
  })
}

// 删除流程分类
export function delCategory(id) {
  return request({
    url: '/workflow/category/' + id,
    method: 'delete'
  })
}
