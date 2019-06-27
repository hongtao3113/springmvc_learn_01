package com.google.ext.springmvc.servlet;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.google.extspringmvc.extannotation.ExtController;
import com.google.extspringmvc.extannotation.ExtRequestMapping;
import com.google.utils.ClassUtil;

/**
 * �Զ���ǰ�˿�����
 * 
 * @author wk
 *
 */
@SuppressWarnings("serial")
public class ExtDispatcherServlet extends HttpServlet {

	// mvc bean key=beanid ,value=����
	private ConcurrentHashMap<String, Object> springMvcBeans = new ConcurrentHashMap<String, Object>();
	// mvc ���󷽷� key=requestUrl,value=����
	private ConcurrentHashMap<String, Object> mvcBeanUrl = new ConcurrentHashMap<String, Object>();
	// mvc ���󷽷� key=requestUrl,value=����
	private ConcurrentHashMap<String, String> mvcMethodUrl = new ConcurrentHashMap<String, String>();

	@Override
	public void init() throws ServletException {
		// 1.��ȡ��ǰ���µ����е���
		List<Class<?>> classes = ClassUtil.getClasses("com.google.controller");
		try {
			// 2.��ɨ����Χ���е��࣬ע�뵽springmvc�������棬�����Map������ keyĬ������Сд��value����
			findClassMVCBeans(classes);
			// 3.��urlӳ���뷽�����й���
			handlerMapping();
			System.out.println();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doPost(req, resp);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		//1.��ȡ�����ַ
		String url = req.getRequestURI();
		if(StringUtils.isEmpty(url)) {
			return;
		}
		//2.��map�л�ȡ
		Object object = mvcBeanUrl.get(url);
		if(object==null) {
			System.out.println("404 url");
			resp.getWriter().println("404 url");
			return;
		}
		String methodName = mvcMethodUrl.get(url);
		if(StringUtils.isEmpty(methodName)) {
			System.out.println("404 method");
			resp.getWriter().println("404 method");
			return;
		}
		//ʹ��java������Ƶ��÷���
		String resultPage = (String)methonInvoke(object,methodName,req);
		//������ص���Ϣ�������ؽ���
		//resp.getWriter().println(resultPage);
		extResourceViewResolver(resultPage,req,resp);
	}
	
	private void extResourceViewResolver(String pageName, HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		// ��·��
		String prefix = "/";
		String suffix = ".jsp";
		req.getRequestDispatcher(prefix + pageName + suffix).forward(req, res);
	}
	
	private Object methonInvoke(Object object,String methodName,HttpServletRequest req) {
	//3.ͨ��url��ȡ����
		try {
			Class<? extends Object> classInfo = object.getClass();
			Method method = classInfo.getMethod(methodName,HttpServletRequest.class);
			Object result = method.invoke(object,req);
			return result;
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public void findClassMVCBeans(List<Class<?>> classes)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		for (Class<?> classInfo : classes) {
			// �ж������Ƿ���ע��
			ExtController extController = classInfo.getAnnotation(ExtController.class);
			if (extController != null) {
				// Ĭ��������Сд
				String beanId = ClassUtil.toLowerCaseFirstOne(classInfo.getSimpleName());
				// ʵ��������
				Object object = ClassUtil.newInstance(classInfo);
				springMvcBeans.put(beanId, object);
			}
		}

	}

	public void handlerMapping() {
		// 1.��ȡspringmvc bean��������
		// 2.����springmvc bean����
		for (Map.Entry<String, Object> mvcBean : springMvcBeans.entrySet()) {
			Object object = mvcBean.getValue();
			// �ж������Ƿ���urlӳ��ע��
			Class<? extends Object> classInfo = object.getClass();
			ExtRequestMapping extRequestMapping = classInfo.getAnnotation(ExtRequestMapping.class);
			String baseUrl = "";
			if (extRequestMapping != null) {
				// ��ȡ���ϵ�urlӳ���ַ
				baseUrl = extRequestMapping.value();
			}
			// 3.�������еķ������Ƿ�urlӳ��ע��,�жϷ������Ƿ��м�urlӳ���ַ
			Method[] declaredMethods = classInfo.getDeclaredMethods();
			for (Method method : declaredMethods) {
				ExtRequestMapping annotation = method.getAnnotation(ExtRequestMapping.class);
				if(annotation!=null) {
					String methodUrl = baseUrl+annotation.value();
					mvcBeanUrl.put(methodUrl, object);
					mvcMethodUrl.put(methodUrl,method.getName());
				}
			}
		}

	}
}
