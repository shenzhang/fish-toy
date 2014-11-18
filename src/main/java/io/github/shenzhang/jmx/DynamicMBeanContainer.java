package io.github.shenzhang.jmx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

class DynamicMBeanContainer implements DynamicMBean {
	private static Logger log = LoggerFactory.getLogger(DynamicMBeanContainer.class);

	private Object bean;
	private MBeanInfo info;

	public DynamicMBeanContainer(Object bean, MBeanInfo info) {
		this.bean = bean;
		this.info = info;
	}

	@Override
	public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
		Field field = getAttributeField(attribute);

		Object object = null;
		try {
			field.setAccessible(true);
			object = field.get(bean);
		} catch (Exception e) {
			throw new MBeanException(e);
		}

		return object;
	}

	@Override
	public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
		String name = attribute.getName();
		Field field = getAttributeField(name);

		try {
			field.setAccessible(true);
			field.set(bean, attribute.getValue());
		} catch (Exception e) {
			throw new InvalidAttributeValueException();
		}
	}

	private Field getAttributeField(String name) throws AttributeNotFoundException {
		Class<?> clazz = bean.getClass();
		Field field = null;
		try {
			field = clazz.getDeclaredField(name);
		} catch (Exception e) {
			throw new AttributeNotFoundException("没有找到属性:" + name);
		}

		return field;
	}

	@Override
	public AttributeList getAttributes(String[] attributes) {
		AttributeList list = new AttributeList();

		for (int i = 0; i < attributes.length; i++) {
			try {
				Object object = getAttribute(attributes[i]);
				list.add(i, object);
			} catch (Exception e) {
				log.error("获取MBean属性输错", e);
			}
		}
		return list;
	}

	@Override
	public AttributeList setAttributes(AttributeList attributes) {
		AttributeList newValues = new AttributeList();

		for (Object o : attributes) {
			Attribute attribute = (Attribute) o;
			try {
				setAttribute(attribute);
				newValues.add(attribute);
			} catch (Exception e) {
				log.error("设置MBean属性出错", e);
			}
		}
		return newValues;
	}

	@Override
	public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
		Class<?> clazz = bean.getClass();

		Object ret = null;
		
		try {
			Class<?> types[] = new Class<?>[signature.length];
			for (int i = 0; i < signature.length; i++) {
				types[i] = this.getClass().getClassLoader().loadClass(signature[i]);
			}

			Method method = clazz.getMethod(actionName, types);
			if (method == null) throw new RuntimeException("没有找到方法：" + actionName);
			
			ret = method.invoke(bean, params);
			
		} catch (Exception e) {
			log.error("JMX调用出错", e);
			throw new MBeanException(e);
		}
		
		return ret;
	}

	@Override
	public MBeanInfo getMBeanInfo() {
		return info;
	}
}
