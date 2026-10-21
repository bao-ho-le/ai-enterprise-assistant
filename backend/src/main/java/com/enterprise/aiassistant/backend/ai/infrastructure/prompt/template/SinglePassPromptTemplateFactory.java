package com.enterprise.aiassistant.backend.ai.infrastructure.prompt.template;

import dev.langchain4j.spi.prompt.PromptTemplateFactory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Thay bộ điền template mặc định của LangChain4j (đăng ký qua META-INF/services, dùng cho cả
// PromptTemplate lẫn AI Services). Bản mặc định thay lần lượt từng biến trên cả chuỗi, nên giá trị
// đã điền có chứa "{{tên-biến}}" (vd tài liệu có "{{title}}", memory có "{{message}}") bị thay tiếp.
// Bản này chỉ quét chữ của template đúng một lượt: giá trị điền vào luôn giữ nguyên như String.formatted cũ.
public class SinglePassPromptTemplateFactory implements PromptTemplateFactory {

    // Cùng pattern với DefaultPromptTemplateFactory của LangChain4j
    private static final Pattern VARIABLE = Pattern.compile("\\{\\{\\s*(.+?)\\s*}}");

    @Override
    public Template create(Input input) {
        String template = input.getTemplate();
        return variables -> render(template, variables);
    }

    // Giữ đúng 2 lỗi của bản mặc định: biến có trong template mà không truyền, và giá trị null.
    private static String render(String template, Map<String, Object> variables) {

        variables.forEach((name, value) -> {
            if (value == null) {
                throw new IllegalArgumentException("Value for the variable '%s' is null".formatted(name));
            }
        });

        Matcher matcher = VARIABLE.matcher(template);
        StringBuilder rendered = new StringBuilder();

        while (matcher.find()) {
            String name = matcher.group(1);

            if (!variables.containsKey(name)) {
                throw new IllegalArgumentException("Value for the variable '%s' is missing".formatted(name));
            }

            matcher.appendReplacement(rendered, Matcher.quoteReplacement(variables.get(name).toString()));
        }

        return matcher.appendTail(rendered).toString();
    }
}
