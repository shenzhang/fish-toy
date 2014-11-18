package io.github.shenzhang.jmx.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 标注需要暴露的MBean属性 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface JmxBeanAttribute {
	boolean readable();
	boolean writable();
	boolean is() default false;
	String description() default "";
}
