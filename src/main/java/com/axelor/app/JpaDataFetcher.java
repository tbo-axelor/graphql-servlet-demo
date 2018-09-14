package com.axelor.app;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.OneToMany;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;
import javax.transaction.Transactional;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class JpaDataFetcher implements DataFetcher {

	private EntityManager entityManager;
	private EntityType<?> entity;

	public JpaDataFetcher(EntityManager entityManager, EntityType<?> entity) {
		this.entityManager = entityManager;
		this.entity = entity;
	}

	@Override
	public Object get(DataFetchingEnvironment environment) {
		return getQuery(environment).getResultList();
	}

	private TypedQuery<?> getQuery(DataFetchingEnvironment environment) {

		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<?> query = builder.createQuery(entity.getJavaType());
		Root<?> entityRoot = query.from(entity);

		Map<String, Object> args = environment.getArguments();

		if (args.get("id") != null) {
			query.where(builder.equal(entityRoot.get("id"), args.get("id")));
		} else if (args.get("data") != null) {
			try {
				Object obj = persistRecord(environment);
				Field field = obj.getClass().getDeclaredField("id");    
				field.setAccessible(true);
				Object value = field.get(obj);
				query.where(builder.equal(entityRoot.get("id"), value));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return entityManager.createQuery(query);

	}

	private Object persistRecord(DataFetchingEnvironment environment)
			throws InstantiationException, IllegalAccessException, NoSuchFieldException, SecurityException {
		Class<?> klass = entity.getJavaType();
		Object obj = klass.newInstance();
		Field[] fields = klass.getDeclaredFields();
		for (Field field : fields) {
			field.setAccessible(true);
			if(field.getAnnotation(OneToMany.class) != null) {
//				List<Object> list = new ArrayList<>();
//				for(Object o : (List<Object>)getValue(environment, field.getName())) {
//					Type elementType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
//					list.add(getObject(o,elementType));
//				}
//				
//				field.set(obj, list);
			} else if (getValue(environment, field.getName()) != null) {
				field.set(obj, getValue(environment, field.getName()));
			}
		}
		entityManager.getTransaction().begin();
		entityManager.persist(obj);
		entityManager.getTransaction().commit();
		return obj;
	}

	private Object getValue(DataFetchingEnvironment env, String name) {
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map = env.getArgument("data");
		return map.get(name);
	}

}
