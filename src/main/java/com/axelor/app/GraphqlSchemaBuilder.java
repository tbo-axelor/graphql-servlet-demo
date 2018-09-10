package com.axelor.app;

import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;

public class GraphqlSchemaBuilder extends GraphQLSchema.Builder {

	private final EntityManager entityManager;

	public GraphqlSchemaBuilder(EntityManager entityManager) {
		this.entityManager = entityManager;
		super.query(getQueryType());
	}
	
	private GraphQLObjectType getQueryType() {
        GraphQLObjectType.Builder queryType = GraphQLObjectType.newObject().name("Query");
        queryType.fields(entityManager.getMetamodel().getEntities().stream().map(this::getFieldDefination).collect(Collectors.toList()));
        return queryType.build();
    }
	
	private GraphQLOutputType findType(Attribute<?, ?> attr) {
		Class<?> type = attr.getJavaType();
		if (String.class.isAssignableFrom(type)) {
			return Scalars.GraphQLString;
		} else if(Long.class.isAssignableFrom(type)) {
			return Scalars.GraphQLLong;
		}
		return Scalars.GraphQLString;
	}
	
	private GraphQLFieldDefinition getFieldDefination(EntityType<?> entity) {
		return GraphQLFieldDefinition.newFieldDefinition().name(entity.getName()).type(new GraphQLList(getType(entity))).dataFetcher(new JpaDataFetcher(entityManager,entity)).build();
	}

	private GraphQLFieldDefinition getTypeField(Attribute<?, ?> attr) {
		return GraphQLFieldDefinition.newFieldDefinition().name(attr.getName()).type(findType(attr)).build();
	}
	
	private GraphQLObjectType getType(EntityType<?> entity) {
		GraphQLObjectType.Builder builder = GraphQLObjectType.newObject().name(entity.getName());
		for (Attribute<?, ?> attr : entity.getAttributes()) {
			builder.field(getTypeField(attr));
		}
		return builder.build();
	}
	
}
