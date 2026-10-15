package com.techmart.shared.i18n;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.MessageSource;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class I18nConfigTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(I18nConfig.class));

    @Test
    void shouldRegisterMessageSourceAndLocaleResolverBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MessageSource.class);
            assertThat(context).hasSingleBean(LocaleResolver.class);

            MessageSource messageSource = context.getBean(MessageSource.class);
            String enMessage = messageSource.getMessage("response.success", null, Locale.ENGLISH);
            assertThat(enMessage).isEqualTo("Operation completed successfully.");

            String viMessage = messageSource.getMessage("response.success", null, Locale.forLanguageTag("vi"));
            assertThat(viMessage).isEqualTo("Thao tác thành công.");
        });
    }
}
