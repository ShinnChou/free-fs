package com.xddcodec.fs.framework.notify.mail.domain;

import com.xddcodec.fs.framework.notify.mail.constant.MailTemplateConstant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Mail {

    /**
     * 收件人
     */
    private String recipient;
    /**
     * 主题
     */
    private String subject;
    /**
     * 内容
     */
    private String content;

    /**
     * 模板
     */
    private String htmlTemplate;

    /**
     * 模板参数
     */
    private Map<String, Object> params;

    /**
     * 附件
     */
    private String attachment;

    /**
     * 构建验证码邮件
     *
     * @param recipient
     * @param code
     * @return
     */
    public static Mail buildVerifyCodeMail(String recipient, String name, String code) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        params.put("code", code);
        return Mail.builder()
                .subject("验证码")
                .recipient(recipient)
                .params(params)
                .htmlTemplate(MailTemplateConstant.VERIFICATION_CODE_TEMPLATE)
                .build();
    }
}
