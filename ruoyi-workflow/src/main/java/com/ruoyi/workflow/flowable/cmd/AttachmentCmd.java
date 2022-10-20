package com.ruoyi.workflow.flowable.cmd;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayEntity;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayEntityManager;
import org.flowable.engine.impl.persistence.entity.AttachmentEntity;
import org.flowable.engine.impl.persistence.entity.AttachmentEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;

/**
 * @description: 附件上传
 * @author: gssong
 * @date: 2022/09/25 11:36
 */
public class AttachmentCmd implements Command<Boolean> {

    private final MultipartFile[] fileList;

    private final String taskId;

    private final String processInstanceId;

    public AttachmentCmd(MultipartFile[] fileList, String taskId, String processInstanceId) {
        this.fileList = fileList;
        this.taskId = taskId;
        this.processInstanceId = processInstanceId;
    }

    @Override
    public Boolean execute(CommandContext commandContext) {
        AttachmentEntityManager attachmentEntityManager = CommandContextUtil.getAttachmentEntityManager();
        ByteArrayEntityManager byteArrayEntityManager = CommandContextUtil.getByteArrayEntityManager();
        if (!ArrayUtil.isEmpty(fileList)) {
            try {
                for (MultipartFile multipartFile : fileList) {
                    ByteArrayEntity byteArrayEntity = byteArrayEntityManager.create();
                    byteArrayEntity.setName(multipartFile.getOriginalFilename());
                    byteArrayEntity.setRevision(1);
                    byteArrayEntity.setBytes(multipartFile.getBytes());
                    byteArrayEntityManager.insert(byteArrayEntity);
                    AttachmentEntity attachmentEntity = attachmentEntityManager.create();
                    attachmentEntity.setRevision(1);
                    attachmentEntity.setUserId(LoginHelper.getUserId().toString());
                    attachmentEntity.setName(multipartFile.getOriginalFilename());
                    attachmentEntity.setDescription(multipartFile.getOriginalFilename());
                    String extName = FileUtil.extName(multipartFile.getOriginalFilename());
                    attachmentEntity.setType(extName);
                    attachmentEntity.setTaskId(taskId);
                    attachmentEntity.setProcessInstanceId(processInstanceId);
                    attachmentEntity.setContentId(byteArrayEntity.getId());
                    attachmentEntity.setTime(new Date());
                    attachmentEntityManager.insert(attachmentEntity);
                }
            } catch (IOException e) {
                throw new ServiceException(e.getMessage());
            }
        }
        return true;
    }
}
