package io.github.shenzhang.jmx;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.github.shenzhang.jmx.annotation.JmxBean;
import io.github.shenzhang.jmx.annotation.JmxBeanAttribute;
import io.github.shenzhang.jmx.annotation.JmxBeanOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.DynamicMBean;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * JMX管理器，用于发布MBean<br/>
 * 目前构造MBean有两种模式，annotation声明和使用DynamicJmxBean包装
 */
@JmxBean(name = "JmxManager")
public class JmxManager {
	private static final Logger log = LoggerFactory.getLogger(JmxManager.class);
	private MBeanServer server = ManagementFactory.getPlatformMBeanServer();

	@JmxBeanAttribute(readable = true, writable = false)
	private String domain = "ruite";

	public void registerSelf() throws Exception {
		 registerJmxBean(this);
	}

	@JmxBeanOperation
	public void log(String message) {
		log.info(message);
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	/**
	 * 发布标准的DynamicMBean
	 */
	public void registerJmxBean(DynamicMBean bean, String name) {
		Preconditions.checkNotNull(bean);

		try {
			server.registerMBean(bean, newObjectName(name));
		} catch (Exception e) {
			log.error("JmxBean注册失败", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * 发布被annotation声明的MBean
	 */
	public void registerJmxBean(Object bean) {
		Preconditions.checkNotNull(bean);

		if (bean instanceof DynamicMBean) {
			throw new RuntimeException("请使用registerJmxBean(DynamicMBean bean, String name)方法来注册");
		}

		Class<?> clazz = bean.getClass();
		JmxBean ab = clazz.getAnnotation(JmxBean.class);
		if (ab == null) {
			throw new IllegalArgumentException(String.format("需要注册的JmxBean需要声明%s注解", JmxBean.class.getName()));
		}

		// attributes
		List<MBeanAttributeInfo> attributes = Lists.newArrayList();
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			JmxBeanAttribute a = field.getAnnotation(JmxBeanAttribute.class);
			if (a != null) {
				if (!checkFiledType(field)) {
					log.warn("注册的MBean属性：{}->{}不是jdk标准类型，在客户端可能无法正常显示", clazz.getName(), field.getName());
				}

				MBeanAttributeInfo info = new MBeanAttributeInfo(field.getName(), field.getType().getName(), a.description(), a.readable(), a.writable(), a.is());
				attributes.add(info);
			}
		}

		// operations
		List<MBeanOperationInfo> operations = Lists.newArrayList();
		Method[] methods = clazz.getMethods();
		for (Method method : methods) {
			JmxBeanOperation o = method.getAnnotation(JmxBeanOperation.class);
			if (o != null) {
				if (!checkMethodType(method)) {
					log.warn("注册的MBean操作：{}->{}含有非jdk标准类型，在客户端可能无法正常显示", clazz.getName(), method.getName());
				}
				MBeanOperationInfo info = new MBeanOperationInfo(o.description(), method);
				operations.add(info);
			}
		}

		MBeanInfo info = new MBeanInfo(clazz.getName(), "", attributes.toArray(new MBeanAttributeInfo[0]), null, operations.toArray(new MBeanOperationInfo[0]), null);

		DynamicMBeanContainer container = new DynamicMBeanContainer(bean, info);

		try {
			server.registerMBean(container, newObjectName(ab.name()));
		} catch (Exception e) {
			log.error("JmxBean注册失败", e);
			throw new RuntimeException(e);
		}

		log.info("注册MBean成功[{}]", clazz.getName());
	}

	/**
	 * 发布被DynamicJmxBean包装的MBean
	 */
	public void registerJmxBean(DynamicJmxBean dynamicBean) {
		Preconditions.checkNotNull(dynamicBean);

		Object bean = dynamicBean.getBean();
		Class<?> clazz = bean.getClass();

		// attributes
		List<MBeanAttributeInfo> attributes = Lists.newArrayList();
		if (dynamicBean.getAttributes() != null) {
			for (String attribute : dynamicBean.getAttributes()) {
				Field field = null;
				try {
					field = clazz.getDeclaredField(attribute);
				} catch (Exception e) {
					log.error(String.format("获取属性出错：[bean=%s, attribute=%s]", clazz.getName(), attribute), e);
					continue;
				}
				
				if (!checkFiledType(field)) {
					log.warn("注册的MBean属性：{}->{}不是jdk标准类型，在客户端可能无法正常显示", clazz.getName(), field.getName());
				}

				MBeanAttributeInfo info = new MBeanAttributeInfo(attribute, field.getType().getName(), null, true, true, false);
				attributes.add(info);
			}
		}

		// operations
		List<MBeanOperationInfo> operations = Lists.newArrayList();
		if (dynamicBean.getOperations() != null) {
			Method[] methods = clazz.getMethods(); // just public
			for (String operation : dynamicBean.getOperations()) {
				for (Method method : methods) {
					String name = method.getName();
					if (name.equals(operation)) {
						if (!checkMethodType(method)) {
							log.warn("注册的MBean操作：{}->{}含有非jdk标准类型，在客户端可能无法正常显示", clazz.getName(), method.getName());
						}
						MBeanOperationInfo info = new MBeanOperationInfo("", method);
						operations.add(info);
					}
				}
			}
		}

		MBeanInfo info = new MBeanInfo(clazz.getName(), "", attributes.toArray(new MBeanAttributeInfo[0]), null, operations.toArray(new MBeanOperationInfo[0]), null);

		DynamicMBeanContainer container = new DynamicMBeanContainer(bean, info);

		try {
			server.registerMBean(container, newObjectName(dynamicBean.getName()));
		} catch (Exception e) {
			log.error("JmxBean注册失败", e);
			throw new RuntimeException(e);
		}

		log.info("注册MBean成功[{}]", clazz.getName());
	}

	private ObjectName newObjectName(String name) throws MalformedObjectNameException {
		return ObjectName.getInstance(domain, "name", name);
	}

	/** 检查指定的field的类型是否可以在client端正常显示 */
	private boolean checkFiledType(Field field) {
		Class<?> clazz = field.getType();

		return isStandardType(clazz);
	}

	/** 检查指定的method所设计的类型是否可以在client正常显示 */
	private boolean checkMethodType(Method method) {
		if (!isStandardType(method.getReturnType())) return false;

		for (Class<?> type : method.getParameterTypes()) {
			if (!isStandardType(type)) return false;
		}

		return true;
	}

	private boolean isStandardType(Class<?> type) {
		String name = type.getName();
		return name.startsWith("java.") || name.startsWith("javax.") || name.equals("void");
	}
}
