package com.hackathon.seoul.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** http://localhost:8080/swagger-ui.html 에서 API를 직접 눌러 테스트 */
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("서울 가챠 API")
                .description("가챠로 서울의 구를 뽑고, 5개 분야 스테이지에서 AI NPC와 한국어 회화 미션을 깨는 서비스")
                .version("0.0.1"));
    }
}
