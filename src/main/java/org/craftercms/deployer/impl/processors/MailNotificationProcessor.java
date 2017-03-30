package org.craftercms.deployer.impl.processors;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.mail.Email;
import org.craftercms.commons.mail.EmailFactory;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Created by alfonsovasquez on 12/30/16.
 */
public class MailNotificationProcessor extends AbstractPostDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MailNotificationProcessor.class);

    public static final String TEMPLATE_NAME_CONFIG_KEY = "templateName";
    public static final String FROM_CONFIG_KEY = "from";
    public static final String TO_CONFIG_KEY = "to";
    public static final String SUBJECT_CONFIG_KEY = "subject";
    public static final String HTML_CONFIG_KEY = "html";
    public static final String SERVER_NAME_CONFIG_KEY = "serverName";
    public static final String DATETIME_PATTERN_CONFIG_KEY = "dateTimePattern";

    public static final String SERVER_NAME_MODEL_KEY = "serverName";
    public static final String TARGET_ID_MODEL_KEY = "targetId";
    public static final String START_MODEL_KEY = "start";
    public static final String END_MODEL_KEY = "end";
    public static final String STATUS_MODEL_KEY = "status";
    public static final String OUTPUT_ATTACHED_MODEL_KEY = "outputAttached";

    protected String templateName;
    protected String from;
    protected String[] to;
    protected String subject;
    protected boolean html;
    protected String serverName;
    protected String defaultTemplateName;
    protected String defaultFrom;
    protected String defaultSubject;
    protected boolean defaultHtml;
    protected String defaultDateTimePattern;
    protected DateTimeFormatter dateTimeFormatter;
    protected EmailFactory emailFactory;

    @Required
    public void setDefaultTemplateName(String defaultTemplateName) {
        this.defaultTemplateName = defaultTemplateName;
    }

    @Required
    public void setDefaultFrom(String defaultFrom) {
        this.defaultFrom = defaultFrom;
    }

    @Required
    public void setDefaultSubject(String defaultSubject) {
        this.defaultSubject = defaultSubject;
    }

    @Required
    public void setDefaultHtml(boolean defaultHtml) {
        this.defaultHtml = defaultHtml;
    }

    @Required
    public void setDefaultDateTimePattern(String defaultDateTimePattern) {
        this.defaultDateTimePattern = defaultDateTimePattern;
    }

    @Required
    public void setEmailFactory(EmailFactory emailFactory) {
        this.emailFactory = emailFactory;
    }

    @Override
    public void configure(Configuration config) throws DeployerException {
        templateName = ConfigUtils.getStringProperty(config, TEMPLATE_NAME_CONFIG_KEY, defaultTemplateName);
        from = ConfigUtils.getStringProperty(config, FROM_CONFIG_KEY, defaultFrom);
        to = ConfigUtils.getRequiredStringArrayProperty(config, TO_CONFIG_KEY);
        subject = ConfigUtils.getStringProperty(config, SUBJECT_CONFIG_KEY, defaultSubject);
        html = ConfigUtils.getBooleanProperty(config, HTML_CONFIG_KEY, defaultHtml);
        serverName = ConfigUtils.getStringProperty(config, SERVER_NAME_CONFIG_KEY);

        if (StringUtils.isEmpty(serverName)) {
            try {
                serverName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                throw new DeployerException("Unable to retrieve localhost address", e);
            }
        }

        String dateTimePattern = ConfigUtils.getStringProperty(config, DATETIME_PATTERN_CONFIG_KEY, defaultDateTimePattern);
        dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern);
    }

    @Override
    protected void doExecute(Deployment deployment, Map<String, Object> params) throws DeployerException {
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(SERVER_NAME_MODEL_KEY, serverName);
        templateModel.put(TARGET_ID_MODEL_KEY, deployment.getTarget().getId());
        templateModel.put(START_MODEL_KEY, deployment.getStart().format(dateTimeFormatter));
        templateModel.put(END_MODEL_KEY, deployment.getEnd().format(dateTimeFormatter));
        templateModel.put(STATUS_MODEL_KEY, deployment.getStatus());

        File attachment = (File)deployment.getAttribute(FileOutputProcessor.OUTPUT_FILE_ATTRIBUTE_NAME);

        templateModel.put(OUTPUT_ATTACHED_MODEL_KEY, attachment != null);

        try {
            Email email;

            if (attachment != null) {
                email = emailFactory.getEmail(from, to, null, null, subject, templateName, templateModel, html, attachment);
            } else {
                email = emailFactory.getEmail(from, to, null, null, subject, templateName, templateModel, html);
            }

            email.send();

            logger.info("Deployment notification successfully sent to {}", Arrays.toString(to));
        } catch (Exception e) {
            throw new DeployerException("Error while sending email with deployment report", e);
        }
    }

}
