package io.cruder.apt;

import com.google.auto.service.AutoService;
import com.google.common.base.Throwables;
import io.cruder.apt.model.CrudService;
import io.cruder.apt.model.JpaCreateMethodImplementor;
import io.cruder.apt.model.JpaUpdateMethodImplementor;
import io.cruder.apt.model.Service;
import io.cruder.apt.type.TypeFactory;
import io.cruder.apt.util.AnnotationUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.Arrays;
import java.util.Set;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes(CrudServiceProcessor.ANNOTATION_FQN)
public class CrudServiceProcessor extends AbstractProcessor {
    public static final String ANNOTATION_FQN = "io.cruder.CrudService";

    private Elements elementUtils;
    private Types typeUtils;
    private Messager messager;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
        this.messager = processingEnv.getMessager();
        this.filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        TypeFactory typeFactory = new TypeFactory(processingEnv);
        TypeElement annotation = elementUtils.getTypeElement(ANNOTATION_FQN);
        for (TypeElement element : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(annotation))) {
            try {
                Service service = new Service(typeFactory.getType(element), Arrays.asList(
                        new JpaCreateMethodImplementor(),
                        new JpaUpdateMethodImplementor()
                ));
                service.implement().writeTo(filer);
            } catch (Exception e) {
                messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        Throwables.getStackTraceAsString(e),
                        element,
                        AnnotationUtils.find(element, ANNOTATION_FQN).orElse(null));
            }
        }
        return false;
    }
}
