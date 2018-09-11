package com.axelor.app;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;

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

	    Map<String,Object> args = environment.getArguments();
	    
	    if(args.get("id") != null) {
	    	query.where(builder.equal(entityRoot.get("id"),args.get("id")));
	    }
	    	
		return entityManager.createQuery(query);

	}

}
