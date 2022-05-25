package io.codebot.apt.processor;

import com.squareup.javapoet.*;
import io.codebot.apt.annotation.Expose;
import io.codebot.apt.code.Annotation;
import io.codebot.apt.code.Method;
import io.codebot.apt.code.Parameter;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

public class ExposedTypeElementProcessor extends AbstractTypeElementProcessor {
    private static final String VALID_FQN = "javax.validation.Valid";

    private static final String AUTOWIRED_FQN = "org.springframework.beans.factory.annotation.Autowired";

    private static final String REST_CONTROLLER_FQN = "org.springframework.web.bind.annotation.RestController";
    private static final String REQUEST_METHOD_FQN = "org.springframework.web.bind.annotation.RequestMethod";
    private static final String REQUEST_MAPPING_FQN = "org.springframework.web.bind.annotation.RequestMapping";
    private static final String REQUEST_PARAM_FQN = "org.springframework.web.bind.annotation.RequestParam";
    private static final String REQUEST_BODY_FQN = "org.springframework.web.bind.annotation.RequestBody";
    private static final String PATH_VARIABLE_FQN = "org.springframework.web.bind.annotation.PathVariable";

    private static final String PAGEABLE_FQN = "org.springframework.data.domain.Pageable";

    private static final String TAG_FQN = "io.swagger.v3.oas.annotations.tags.Tag";
    private static final String OPERATION_FQN = "io.swagger.v3.oas.annotations.Operation";
    private static final String PAGEABLE_AS_QUERY_PARAM_FQN = "org.springdoc.core.converters.models.PageableAsQueryParam";

    @Override
    public void process(TypeElement element) throws Exception {
        DeclaredType type = typeOps.getDeclaredType(element);
        Annotation typeAnnotation = annotationUtils.find(element, Expose.class);
        if (typeAnnotation == null) {
            return;
        }

        ClassName typeName = ClassName.get(element);
        ClassName controllerName = ClassName.get(
                typeName.packageName().replaceAll("[^.]+$", "controller"),
                typeName.simpleName().replaceAll("Service$", "Controller")
        );

        TypeSpec.Builder controller = TypeSpec
                .classBuilder(controllerName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.bestGuess(REST_CONTROLLER_FQN));

        boolean typeExposed = typeAnnotation.getBoolean("value");
        String typePath = typeAnnotation.getString("path");
        String typeTitle = typeAnnotation.getString("title");

        controller.addAnnotation(AnnotationSpec
                .builder(ClassName.bestGuess(REQUEST_MAPPING_FQN))
                .addMember("path", "$S", typePath)
                .build()
        );
        controller.addField(FieldSpec
                .builder(typeName, "target", Modifier.PRIVATE)
                .addAnnotation(ClassName.bestGuess(AUTOWIRED_FQN))
                .build()
        );
        if (!typeTitle.isEmpty()) {
            controller.addAnnotation(AnnotationSpec
                    .builder(ClassName.bestGuess(TAG_FQN))
                    .addMember("name", "$S", typeTitle)
                    .build()
            );
        }
        for (Method method : methodUtils.allOf(type)) {
            Annotation methodAnnotation = annotationUtils.find(method.getElement(), Expose.class);
            if (methodAnnotation == null && !typeExposed
                    || methodAnnotation != null && !methodAnnotation.getBoolean("value")) {
                continue;
            }
            MethodSpec.Builder controllerMethod = MethodSpec
                    .methodBuilder(method.getSimpleName())
                    .addModifiers(Modifier.PUBLIC)
                    .returns(TypeName.get(method.getReturnType()));

            StringBuilder path = new StringBuilder();
            boolean pathCustomized = false;
            if (methodAnnotation != null) {
                String methodTitle = methodAnnotation.getString("title");
                if (!methodTitle.isEmpty()) {
                    controllerMethod.addAnnotation(AnnotationSpec
                            .builder(ClassName.bestGuess(OPERATION_FQN))
                            .addMember("summary", "$S", methodTitle)
                            .build()
                    );
                }
                String methodPath = methodAnnotation.getString("path");
                if (!methodPath.isEmpty()) {
                    path.append(methodPath);
                    pathCustomized = true;
                }
            }
            if (!pathCustomized) {
                path.append("/").append(method.getSimpleName());
            }
            boolean hasBodyParam = false;
            for (Parameter param : method.getParameters()) {
                ParameterSpec.Builder paramBuilder = ParameterSpec
                        .builder(TypeName.get(param.getType()), param.getName());
                if (annotationUtils.isPresent(param.getElement(), Expose.Body.class)) {
                    paramBuilder.addAnnotation(ClassName.bestGuess(REQUEST_BODY_FQN));
                    paramBuilder.addAnnotation(ClassName.bestGuess(VALID_FQN));
                    hasBodyParam = true;
                } //
                else if (annotationUtils.isPresent(param.getElement(), Expose.Param.class)) {
                    paramBuilder.addAnnotation(AnnotationSpec
                            .builder(ClassName.bestGuess(REQUEST_PARAM_FQN))
                            .addMember("name", "$S", param.getName())
                            .build()
                    );
                } //
                else if (annotationUtils.isPresent(param.getElement(), Expose.Path.class)) {
                    paramBuilder.addAnnotation(AnnotationSpec
                            .builder(ClassName.bestGuess(PATH_VARIABLE_FQN))
                            .addMember("name", "$S", param.getName())
                            .build()
                    );
                    if (!pathCustomized) {
                        path.append("/{").append(param.getName()).append("}");
                    }
                } //
                else if (typeOps.isAssignable(param.getType(), PAGEABLE_FQN)) {
                    controllerMethod.addAnnotation(ClassName.bestGuess(PAGEABLE_AS_QUERY_PARAM_FQN));
                }
                controllerMethod.addParameter(paramBuilder.build());
            }

            String exposeMethod = "";
            if (methodAnnotation != null) {
                exposeMethod = methodAnnotation.getString("method");
            }
            if (exposeMethod.isEmpty()) {
                exposeMethod = hasBodyParam ? "POST" : "GET";
            }
            controllerMethod.addAnnotation(AnnotationSpec
                    .builder(ClassName.bestGuess(REQUEST_MAPPING_FQN))
                    .addMember("method", "$T.$L", ClassName.bestGuess(REQUEST_METHOD_FQN), exposeMethod)
                    .addMember("path", "$S", path)
                    .build()
            );

            if (!typeOps.isVoid(method.getReturnType())) {
                controllerMethod.addCode("return ");
            }
            controllerMethod.addCode("this.target.$N($L);\n",
                    method.getSimpleName(),
                    method.getParameters().stream()
                            .map(it -> it.asExpression().getCode())
                            .collect(CodeBlock.joining(", "))
            );

            controller.addMethod(controllerMethod.build());
        }
        JavaFile.builder(controllerName.packageName(), controller.build()).build()
                .writeTo(processingEnv.getFiler());
    }
}
