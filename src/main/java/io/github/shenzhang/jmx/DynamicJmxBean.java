package io.github.shenzhang.jmx;

/**
 * 动态MBean的包装器，可以将任意的bean包装起来提交给JmxManager，可以不用直接依赖frame.jar
 * <br/>
 * 暂时不支持自动暴露父类属性
 */
public class DynamicJmxBean {
	private Object bean;
	private String name;
	private String[] attributes;
	private String[] operations;
	
	/**
	 * 包装器构造函数
	 * @param bean 要包装的对象
	 * @param name MBean的名称
	 * @param attributes 需要暴露的attributes(所有的attributes会自动具有读、写权限)
	 * @param operations 需要暴露的operations(所有的operations需要是public的)
	 */
	public DynamicJmxBean(Object bean, String name, String[] attributes, String[] operations) {
		this.bean = bean;
		this.name = name;
		this.attributes = attributes;
		this.operations = operations;
	}
	
	public Object getBean() {
		return bean;
	}
	
	public String getName() {
		return name;
	}

	public String[] getAttributes() {
		return attributes;
	}

	public String[] getOperations() {
		return operations;
	}
}
