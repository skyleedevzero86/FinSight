package com.sleekydz86.finsight.core.notification.service;

import com.sleekydz86.finsight.core.notification.domain.EmailTemplate;
import com.sleekydz86.finsight.core.notification.domain.RenderedEmailTemplate;
import com.sleekydz86.finsight.core.notification.domain.port.out.EmailTemplatePersistencePort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@Service
public class EmailTemplateQueryService {

    private final EmailTemplatePersistencePort emailTemplatePersistencePort;

    public EmailTemplateQueryService(EmailTemplatePersistencePort emailTemplatePersistencePort) {
        this.emailTemplatePersistencePort = emailTemplatePersistencePort;
    }

    @Transactional(readOnly = true)
    public Optional<RenderedEmailTemplate> renderActive(String name, Map<String, String> variables) {
        return emailTemplatePersistencePort.findActiveByName(name)
                .map(template -> toRendered(template, variables));
    }

    public Optional<RenderedEmailTemplate> renderClasspathFallback(String classpathHtml, String subjectTemplate,
            Map<String, String> variables) {
        try {
            ClassPathResource resource = new ClassPathResource(classpathHtml);
            if (!resource.exists()) {
                return Optional.empty();
            }
            String html = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            return Optional.of(new RenderedEmailTemplate(
                    RenderedEmailTemplate.apply(subjectTemplate, variables),
                    RenderedEmailTemplate.apply(html, variables)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private RenderedEmailTemplate toRendered(EmailTemplate template, Map<String, String> variables) {
        return new RenderedEmailTemplate(
                RenderedEmailTemplate.apply(template.getSubject(), variables),
                RenderedEmailTemplate.apply(template.getHtmlContent(), variables));
    }
}
