import request from "@/utils/request";
/**
 * 分页查询
 * @param {条件} query
 * @returns
 */
export function list(query) {
  return request({
    url: '/workflow/definition/list',
    method: 'get',
    params: query
  })
}

/**
 * 分页查询
 * @param {条件} query
 * @returns
 */
 export function getHistByPage(query) {
    return request({
      url: '/workflow/definition/getHistByPage',
      method: 'get',
      params: query
    })
}

/**
 * 挂起或激活流程定义
 * @param {参数} data
 * @returns
 */
export function updateProcDefState(data) {
  return request({
    url: '/workflow/definition/updateProcDefState',
    method: 'put',
    data: data
  })
}

/**
 * 按流程部署id删除
 * @param {流程部署id} deploymentId
 * @returns
 */
 export function del(deploymentId,definitionId) {
  return request({
    url: `/workflow/definition/${deploymentId}/${definitionId}`,
    method: 'delete'
  })
}

/**
 * 通过zip或xml部署流程定义
 * @returns
 */
export function deployProcessFile(data) {
  return request({
    url: '/workflow/definition/deployByFile',
    method: 'post',
    data: data
  })
}

/**
 * 流程定义设置
 * @param definitionId
 * @returns
 */
export function setting(definitionId) {
  return request({
    url: '/workflow/definition/setting/'+definitionId,
    method: 'get'
  })
}

/**
 * 查看xml
 * @param definitionId
 * @returns
 */
export function getXml(definitionId) {
  return request({
    url: '/workflow/definition/getXml/'+definitionId,
    method: 'get'
  })
}

/**
 * 迁移流程定义版本
 * @param currentProcessDefinitionId
 * @param fromProcessDefinitionId
 * @returns
 */
 export function migrationProcessDefinition(currentProcessDefinitionId,fromProcessDefinitionId) {
    return request({
      url: `/workflow/definition/migrationProcessDefinition/${currentProcessDefinitionId}/${fromProcessDefinitionId}`,
      method: 'put'
    })
 }




